package terrasaur.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import net.jafama.FastMath;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.intervals.IntervalSet;
import picante.math.intervals.UnwritableInterval;
import picante.time.TimeConversion;
import spice.basic.KernelDatabase;
import spice.basic.SpiceException;
import spice.basic.SpiceQuaternion;
import spice.basic.TDBTime;
import spice.basic.Vector3;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.ImmutableSumFile.Builder;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.math.RotationUtils;
import terrasaur.utils.spice.SpiceBundle;

public class SumFilesFromFlyby implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Create sumfiles for a simplified flyby.";
  }

  @Override
  public String fullDescription(Options options) {

    String header = "";
    String footer =
        """

            This tool creates sumfiles at points along a straight line trajectory past a body to be imaged.

            Assumptions made:
            1) The flyby is entirely in the equatorial (XY) plane.
            2) the Sun lies along the body-fixed X axis.
            3) the flyby happens on the eastern side of the body.
            4) Rotation and orbital motion of the body are ignored.
            5) Image (0,0) is at the upper left.  Body north is "up".

            Given these assumptions, the trajectory can be specified using closest approach distance and phase along with speed.
            """;
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private SumFile sumfile;
  private Function<Double, Vector3D> scPosFunc;
  private Function<Double, Vector3D> scVelFunc;

  private SumFilesFromFlyby() {}

  public SumFilesFromFlyby(SumFile sumfile, double distance, double phase, double speed) {
    this.sumfile = sumfile;

    // given phase angle p, closest approach point is (cos p, sin p)
    Vector3D closestApproach =
        new Vector3D(FastMath.cos(phase), FastMath.sin(phase), 0.).scalarMultiply(distance);
    Vector3D velocity =
        new Vector3D(-FastMath.sin(phase), FastMath.cos(phase), 0.).scalarMultiply(speed);

    /*-
     * Assumptions:
     *
     * Sun lies along the X axis
     * flyby is in the equatorial (XY) plane
     *
     */
    scPosFunc = t -> closestApproach.add(velocity.scalarMultiply(t));

    scVelFunc = t -> velocity;
  }

  public SumFile getSumFile(double t) {

    TimeConversion tc = TimeConversion.createUsingInternalConstants();

    Builder builder = ImmutableSumFile.builder().from(sumfile);
    double imageTime = t + tc.utcStringToTDB(sumfile.utcString());
    builder.picnm(String.format("%s%d", sumfile.picnm(), (int) Math.round(imageTime)));
    builder.utcString(tc.format("C").apply(imageTime));

    Vector3D scPos = scPosFunc.apply(t);

    builder.scobj(scPos.negate());

    Rotation bodyFixedToCamera = RotationUtils.KprimaryJsecondary(scPos.negate(), Vector3D.MINUS_K);
    builder.cx(bodyFixedToCamera.applyInverseTo(Vector3D.PLUS_I));
    builder.cy(bodyFixedToCamera.applyInverseTo(Vector3D.PLUS_J));
    builder.cz(bodyFixedToCamera.applyInverseTo(Vector3D.PLUS_K));

    builder.sz(Vector3D.PLUS_I.scalarMultiply(1e8));

    SumFile s = builder.build();

    logger.info(
        "{}: S/C position {}, phase {}",
        s.utcString(),
        s.scobj().negate(),
        Math.toDegrees(Vector3D.angle(s.scobj().negate(), s.sz())));

    return s;
  }

  private String writeMSOPCKFiles(
      String basename, IntervalSet intervals, int frameID, SpiceBundle bundle)
      throws SpiceException {

    File commentFile = new File(basename + "_msopck-comments.txt");
    if (commentFile.exists()) commentFile.delete();
    String setupFile = basename + "_msopck.setup";
    String inputFile = basename + "_msopck.inp";

    try (PrintWriter pw = new PrintWriter(commentFile)) {

      String allComments = "";
      for (String comment : allComments.split("\\r?\\n")) pw.println(WordUtils.wrap(comment, 80));
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    File fk = bundle.findKernel(String.format(".*%s\\.tf", basename));
    File lsk = bundle.findKernel(".*naif[0-9]{4}\\.tls");

    Map<String, String> map = new TreeMap<>();
    map.put("LSK_FILE_NAME", "'" + lsk + "'");
    map.put("MAKE_FAKE_SCLK", String.format("'%s.tsc'", basename));
    map.put("CK_TYPE", "3");
    map.put("COMMENTS_FILE_NAME", String.format("'%s'", commentFile.getPath()));
    map.put("INSTRUMENT_ID", Integer.toString(frameID));
    map.put("REFERENCE_FRAME_NAME", "'J2000'");
    map.put("FRAMES_FILE_NAME", "'" + fk.getPath() + "'");
    map.put("ANGULAR_RATE_PRESENT", "'MAKE UP/NO AVERAGING'");
    map.put("INPUT_TIME_TYPE", "'UTC'");
    map.put("INPUT_DATA_TYPE", "'SPICE QUATERNIONS'");
    map.put("PRODUCER_ID", "'Hari.Nair@jhuapl.edu'");

    try (PrintWriter pw = new PrintWriter(setupFile)) {
      pw.println("\\begindata");
      for (String key : map.keySet()) {
        pw.printf("%s = %s\n", key, map.get(key));
      }
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    NavigableMap<Double, SpiceQuaternion> attitudeMap = new TreeMap<>();

    double t0 = bundle.getTimeConversion().utcStringToTDB(sumfile.utcString());

    for (UnwritableInterval interval : intervals) {
      for (double t = interval.getBegin(); t < interval.getEnd(); t += interval.getLength() / 100) {

        double imageTime = t + t0;

        Vector3D scPos = scPosFunc.apply(t);
        SpiceQuaternion q =
            new SpiceQuaternion(
                MathConversions.toMatrix33(
                    RotationUtils.KprimaryJsecondary(scPos.negate(), Vector3D.MINUS_K)));

        attitudeMap.put(imageTime, q);
      }
    }

    try (PrintWriter pw = new PrintWriter(new FileWriter(inputFile))) {
      for (double t : attitudeMap.keySet()) {
        SpiceQuaternion q = attitudeMap.get(t);
        Vector3 v = q.getVector();
        pw.printf(
            "%s %.14e %.14e %.14e %.14e\n",
            new TDBTime(t).toUTCString("ISOC", 6),
            q.getScalar(),
            v.getElt(0),
            v.getElt(1),
            v.getElt(2));
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    return String.format("msopck %s %s %s.bc", setupFile, inputFile, basename);
  }

  /**
   * @param basename file basename
   * @param intervals time intervals
   * @param centerID NAIF id of center body
   * @param bundle SPICE bundle
   * @return command to run MKSPK
   */
  private String writeMKSPKFiles(
      String basename, IntervalSet intervals, int centerID, SpiceBundle bundle) {

    String commentFile = basename + "_mkspk-comments.txt";
    String setupFile = basename + "_mkspk.setup";
    String inputFile = basename + "_mkspk.inp";

    try (PrintWriter pw = new PrintWriter(commentFile)) {

      String allComments = "";
      for (String comment : allComments.split("\\r?\\n")) pw.println(WordUtils.wrap(comment, 80));
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    File lsk = bundle.findKernel(".*naif[0-9]{4}.tls");

    Map<String, String> map = new TreeMap<>();
    map.put("INPUT_DATA_TYPE", "'STATES'");
    map.put("OUTPUT_SPK_TYPE", "13"); // hermite polynomial, unevenly spaced in time
    map.put("OBJECT_ID", "-999");
    map.put("CENTER_ID", String.format("%d", centerID));
    map.put("COMMENT_FILE", String.format("'%s'", commentFile));
    map.put("REF_FRAME_NAME", "'J2000'");
    map.put("PRODUCER_ID", "'Hari.Nair@jhuapl.edu'");
    map.put("DATA_ORDER", "'EPOCH X Y Z VX VY VZ'");
    map.put("DATA_DELIMITER", "' '");
    map.put("LEAPSECONDS_FILE", String.format("'%s'", lsk));
    map.put("TIME_WRAPPER", "'# ETSECONDS'");
    map.put("POLYNOM_DEGREE", "7");
    map.put("SEGMENT_ID", "'SPK_STATES_13'");
    map.put("LINES_PER_RECORD", "1");
    try (PrintWriter pw = new PrintWriter(setupFile)) {
      pw.println("\\begindata");
      for (String key : map.keySet()) {
        pw.printf("%s = %s\n", key, map.get(key));
      }
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    double t0 = bundle.getTimeConversion().utcStringToTDB(sumfile.utcString());

    try (PrintWriter pw = new PrintWriter(inputFile)) {
      for (UnwritableInterval interval : intervals) {
        for (double t = interval.getBegin();
            t < interval.getEnd();
            t += interval.getLength() / 100) {

          Vector3D scPos = scPosFunc.apply(t);
          Vector3D scVel = scVelFunc.apply(t);
          pw.printf(
              "%.16e %.16e %.16e %.16e %.16e %.16e %.16e\n",
              t + t0,
              scPos.getX(),
              scPos.getY(),
              scPos.getZ(),
              scVel.getX(),
              scVel.getY(),
              scVel.getZ());
        }
      }
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
    return String.format(
        "mkspk -setup %s -input %s -output %s.bsp", setupFile, inputFile, basename);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("distance")
            .hasArg()
            .required()
            .desc("Required.  Flyby distance from body center in km.")
            .build());
    options.addOption(
        Option.builder("logFile")
            .hasArg()
            .desc("If present, save screen output to log file.")
            .build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values()) sb.append(String.format("%s ", l.name()));
    options.addOption(
        Option.builder("logLevel")
            .hasArg()
            .desc(
                "If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim()
                    + ".  Default is INFO.")
            .build());
    options.addOption(
        Option.builder("mk")
            .hasArg()
            .desc(
                "Path to NAIF metakernel.  This should contain LSK, FK for the central body, and FK for the spacecraft.  This is used by -mkspk and -msopck.")
            .build());
    options.addOption(
        Option.builder("mkspk")
            .hasArg()
            .desc(
                "If present, create input files for MKSPK.  The argument is the NAIF id for the central body (e.g. 10 for the Sun).  This option requires -lsk.")
            .build());
    options.addOption(
        Option.builder("msopck")
            .desc("If present, create input files for MSOPCK.  This option requires -lsk.")
            .build());
    options.addOption(
        Option.builder("phase")
            .hasArg()
            .required()
            .desc("Required.  Phase angle at closest approach in degrees.")
            .build());
    options.addOption(
        Option.builder("speed")
            .hasArg()
            .required()
            .desc("Required.  Flyby speed at closest approach in km/s.")
            .build());
    options.addOption(
        Option.builder("template")
            .hasArg()
            .required()
            .desc(
                "Required.  An existing sumfile to use as a template.  Camera parameters are taken from this "
                    + "file, while camera position and orientation are calculated.")
            .build());
    options.addOption(
        Option.builder("times")
            .hasArgs()
            .desc(
                "If present, list of times separated by white space.  In seconds, relative to closest approach.")
            .build());
    return options;
  }

  public static void main(String[] args) throws IOException, SpiceException {
    TerrasaurTool defaultOBJ = new SumFilesFromFlyby();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    double phase = Double.parseDouble(cl.getOptionValue("phase"));
    if (phase < 0 || phase > 180) {
      logger.error("Phase angle {} out of range [0, 180]", phase);
      System.exit(0);
    }

    String sumFileTemplate = cl.getOptionValue("template");

    String base = FilenameUtils.getBaseName(sumFileTemplate);
    String ext = FilenameUtils.getExtension(sumFileTemplate);
    SumFilesFromFlyby app =
        new SumFilesFromFlyby(
            SumFile.fromFile(new File(sumFileTemplate)),
            Double.parseDouble(cl.getOptionValue("distance")),
            Math.toRadians(phase),
            Double.parseDouble(cl.getOptionValue("speed")));

    NavigableSet<Double> times = new TreeSet<>();
    if (cl.hasOption("times")) {
      for (String s : cl.getOptionValues("times")) times.add(Double.parseDouble(s));
    } else times.add(0.);

    SpiceBundle bundle = null;
    if (cl.hasOption("mk")) {
      NativeLibraryLoader.loadSpiceLibraries();
      bundle =
          new SpiceBundle.Builder()
              .addMetakernels(Collections.singletonList(cl.getOptionValue("mk")))
              .build();
      KernelDatabase.load(cl.getOptionValue("mk"));
    }

    TimeConversion tc =
        bundle == null ? TimeConversion.createUsingInternalConstants() : bundle.getTimeConversion();

    for (double t : times) {
      SumFile s = app.getSumFile(t);

      try (PrintWriter pw =
          new PrintWriter(
              String.format("%s_%d.%s", base, (int) tc.utcStringToTDB(s.utcString()), ext))) {

        pw.println(s);

      } catch (FileNotFoundException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }

    if (cl.hasOption("mkspk")) {
      if (bundle == null) {
        logger.error("Need -mk to use -mkspk!");
      } else {
        IntervalSet.Builder builder = IntervalSet.builder();
        for (Double t : times) {
          Double next = times.higher(t);
          if (next != null) builder.add(new UnwritableInterval(t, next));
        }
        int centerID = Integer.parseInt(cl.getOptionValue("mkspk"));

        String command = app.writeMKSPKFiles(base, builder.build(), centerID, bundle);
        logger.info("Command to create SPK:\n{}", command);
      }
    }

    if (cl.hasOption("msopck")) {
      if (bundle == null) {
        logger.error("Need -mk to use -msopck!");
      } else {
        IntervalSet.Builder builder = IntervalSet.builder();
        for (Double t : times) {
          Double next = times.higher(t);
          if (next != null) builder.add(new UnwritableInterval(t, next));
        }

        final int scID = -999;
        final int frameID = scID * 1000;

        File spacecraftFK = new File(String.format("%s.tf", base));
        try (PrintWriter pw = new PrintWriter(spacecraftFK)) {
          pw.println("\\begindata");
          pw.printf("FRAME_%s_FIXED = %d\n", base, frameID);
          pw.printf("FRAME_%d_NAME = '%s_FIXED'\n", frameID, base);
          pw.printf("FRAME_%d_CLASS = 3\n", frameID);
          pw.printf("FRAME_%d_CENTER = %d\n", frameID, scID);
          pw.printf("FRAME_%d_CLASS_ID = %d\n", frameID, frameID);
          pw.println("\\begintext");
        }

        List<File> kernels = new ArrayList<>(bundle.getKernels());
        kernels.add(spacecraftFK);

        bundle = new SpiceBundle.Builder().addKernelList(kernels).build();

        File spacecraftSCLK = new File(String.format("%s.tsc", base));
        if (spacecraftSCLK.exists()) spacecraftSCLK.delete();

        String command = app.writeMSOPCKFiles(base, builder.build(), frameID, bundle);
        logger.info("Command to create SPK:\n{}", command);
      }
    }
  }
}
