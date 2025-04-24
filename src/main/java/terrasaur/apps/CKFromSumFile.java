package terrasaur.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.KernelDatabase;
import spice.basic.Matrix33;
import spice.basic.ReferenceFrame;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.SpiceQuaternion;
import spice.basic.TDBTime;
import spice.basic.Vector3;
import terrasaur.config.CKFromSumFileConfig;
import terrasaur.config.CKFromSumFileConfigFactory;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;

public class CKFromSumFile implements TerrasaurTool {

  private final static Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Create a CK from a list of sumfiles.";
  }

  @Override
  public String fullDescription(Options options) {
    String footer = "Create a CK from a list of sumfiles.";
    return TerrasaurTool.super.fullDescription(options, "", footer);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("config").required().hasArg()
            .desc("Required.  Name of configuration file.").build());
    options.addOption(Option.builder("dumpConfig").hasArg()
            .desc("Write out an example configuration to the named file.").build());
    options.addOption(Option.builder("logFile").hasArg()
            .desc("If present, save screen output to log file.").build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values())
      sb.append(String.format("%s ", l.name()));
    options.addOption(Option.builder("logLevel").hasArg()
            .desc("If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim() + ".  Default is INFO.")
            .build());
    options.addOption(Option.builder("sumFile").hasArg().required().desc("""
        Required.  File listing sumfiles to read.  This is a text file, one per line.
        Lines starting with # are ignored.

        Example:

        D717506120G0.SUM
        D717506126G0.SUM
        D717506127G0.SUM
        D717506128G0.SUM
        D717506129G0.SUM
        D717506131G0.SUM
        # This is a comment
        D717506132G0.SUM
        """).build());
    return options;
  }

  private final CKFromSumFileConfig config;
  private final NavigableMap<SumFile, String> sumFiles;

  private CKFromSumFile(){config=null;sumFiles=null;}

  public CKFromSumFile(CKFromSumFileConfig config, NavigableMap<SumFile, String> sumFiles) {
    this.config = config;
    this.sumFiles = sumFiles;
  }

  public String writeMSOPCKFiles(String basename, List<String> comments) throws SpiceException {

    ReferenceFrame instrFrame = new ReferenceFrame(config.instrumentFrameName());
    ReferenceFrame scFrame = new ReferenceFrame(config.spacecraftFrame());
    ReferenceFrame j2000 = new ReferenceFrame("J2000");
    ReferenceFrame bodyFixed = new ReferenceFrame(config.bodyFrame());

    ReferenceFrame ref = config.J2000() ? j2000 : bodyFixed;

    logger.debug("Body fixed frame: {}", bodyFixed.getName());
    logger.debug("Instrument frame: {}", instrFrame.getName());
    logger.debug("Spacecraft frame: {}", scFrame.getName());
    logger.debug(" Reference frame: {}", ref.getName());

    File commentFile = new File(basename + "-comments.txt");
    if (commentFile.exists())
      if (!commentFile.delete())
        logger.error("{} exists but cannot be deleted!", commentFile.getPath());

    String setupFile = basename + ".setup";
    String inputFile = basename + ".inp";

    try (PrintWriter pw = new PrintWriter(commentFile)) {
      StringBuilder sb = new StringBuilder();

      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss z");
      ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

      sb.append("This CK was created on ").append(dtf.format(now)).append(" from the following sumFiles:\n");
      for (SumFile sumFile : sumFiles.keySet()) {
        sb.append(String.format("\t%s %s\n", sumFile.utcString(), sumFiles.get(sumFile)));
      }
      sb.append("\n");
      sb.append("providing the orientation of ").append(scFrame.getName()).append(" with respect to ").append(config.J2000() ? "J2000" : bodyFixed.getName()).append(".  ");
      double first = new TDBTime(sumFiles.firstKey().utcString()).getTDBSeconds();
      double last = new TDBTime(sumFiles.lastKey().utcString()).getTDBSeconds() + config.extend();
      sb.append("The coverage period is ").append(new TDBTime(first).toUTCString("ISOC", 3)).append(" to ").append(new TDBTime(last).toUTCString("ISOC", 3)).append(" UTC.");

      String allComments = sb.toString();
      for (String comment : allComments.split("\\r?\\n"))
        pw.println(WordUtils.wrap(comment, 80));
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    Map<String, String> map = new TreeMap<>();
    map.put("LSK_FILE_NAME", "'" + config.lsk() + "'");
    map.put("SCLK_FILE_NAME", "'" + config.sclk() + "'");
    map.put("CK_TYPE", "3");
    map.put("COMMENTS_FILE_NAME", String.format("'%s'", commentFile.getPath()));
    map.put("INSTRUMENT_ID", String.format("%d", scFrame.getIDCode()));
    map.put("REFERENCE_FRAME_NAME",
        String.format("'%s'", config.J2000() ? "J2000" : bodyFixed.getName()));
    if (!config.fk().isEmpty())
      map.put("FRAMES_FILE_NAME", "'" + config.fk() + "'");
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
    for (SumFile s : sumFiles.keySet()) {
      TDBTime t = new TDBTime(s.utcString());

      Vector3[] rows = new Vector3[3];
      rows[0] = MathConversions.toVector3(s.cx());
      rows[1] = MathConversions.toVector3(s.cy());
      rows[2] = MathConversions.toVector3(s.cz());

      Vector3 row0 = rows[Math.abs(config.flipX()) - 1];
      Vector3 row1 = rows[Math.abs(config.flipY()) - 1];
      Vector3 row2 = rows[Math.abs(config.flipZ()) - 1];

      if (config.flipX() < 0)
        row0 = row0.negate();
      if (config.flipY() < 0)
        row1 = row1.negate();
      if (config.flipZ() < 0)
        row2 = row2.negate();

      Matrix33 refToInstr = new Matrix33(row0, row1, row2);

      if (config.J2000()) {
        Matrix33 j2000ToBodyFixed = j2000.getPositionTransformation(bodyFixed, t);
        refToInstr = refToInstr.mxm(j2000ToBodyFixed);
      }

      Matrix33 instrToSc = instrFrame.getPositionTransformation(scFrame, t);
      Matrix33 refToSc = instrToSc.mxm(refToInstr);

      SpiceQuaternion q = new SpiceQuaternion(refToSc);
      attitudeMap.put(t.getTDBSeconds(), q);
    }

    if (config.extend() > 0) {
      var lastEntry = attitudeMap.lastEntry();
      attitudeMap.put(lastEntry.getKey() + config.extend(), lastEntry.getValue());
    }

    try (PrintWriter pw = new PrintWriter(new FileWriter(inputFile))) {
      for (double t : attitudeMap.keySet()) {
        SpiceQuaternion q = attitudeMap.get(t);
        pw.printf("%s %.14e %.14e %.14e %.14e\n", new TDBTime(t).toUTCString("ISOC", 6),
            q.getElt(0), q.getElt(1), q.getElt(2), q.getElt(3));
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

      return String.format("msopck %s %s %s.bc", setupFile, inputFile, basename);
  }

  public static void main(String[] args) throws SpiceException, IOException {
    TerrasaurTool defaultOBJ = new CKFromSumFile();
    
    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    if (cl.hasOption("dumpConfig")){
      CKFromSumFileConfigFactory factory = new CKFromSumFileConfigFactory();
      PropertiesConfiguration config = factory.toConfig(factory.getTemplate());
      try {
        String filename = cl.getOptionValue("dumpConfig");
        config.write(new PrintWriter(filename));
        logger.info("Wrote {}", filename);
      } catch (ConfigurationException | IOException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
      System.exit(0);
    }

    NativeLibraryLoader.loadSpiceLibraries();

    PropertiesConfiguration config = null;
    CKFromSumFileConfigFactory factory = new CKFromSumFileConfigFactory();
    try {
      config = new Configurations().properties(new File(cl.getOptionValue("config")));
    } catch (ConfigurationException e1) {
      logger.error(e1.getLocalizedMessage(), e1);
    }

    CKFromSumFileConfig appConfig = factory.fromConfig(config);

    for (String kernel : appConfig.metakernel())
      KernelDatabase.load(kernel);

    NavigableMap<SumFile, String> sumFiles = new TreeMap<>((o1, o2) -> {
      try {
        return Double.compare(new TDBTime(o1.utcString()).getTDBSeconds(),
            new TDBTime(o2.utcString()).getTDBSeconds());
      } catch (SpiceErrorException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
      return 0;
    });

    List<String> lines =
        FileUtils.readLines(new File(cl.getOptionValue("sumFile")), Charset.defaultCharset());
    for (String line : lines) {
      if (line.strip().startsWith("#"))
        continue;
      String[] parts = line.strip().split("\\s+");
      String filename = parts[0].trim();
      sumFiles.put(SumFile.fromFile(new File(filename)), FilenameUtils.getBaseName(filename));
    }

    CKFromSumFile app = new CKFromSumFile(appConfig, sumFiles);
    TDBTime begin = new TDBTime(sumFiles.firstKey().utcString());
    TDBTime end = new TDBTime(sumFiles.lastKey().utcString());
    String picture = "YYYY_DOY";
    String command = app.writeMSOPCKFiles(
        String.format("ck_%s_%s", begin.toString(picture), end.toString(picture)),
        new ArrayList<>());

    logger.info("To generate the CK, run:\n\t{}", command);

    logger.info("Finished.");

  }

}
