package terrasaur.apps;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.*;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;
import vtk.vtkCellArray;
import vtk.vtkLine;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

/**
 * Find the impact point and time given an initial position and velocity.
 *
 * @author nairah1
 */
public class ImpactLocator implements UnivariateFunction, TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Calculate impact time and position from a sumFile.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer =
        """
               Given a sum file, shape model, and spacecraft velocity in the J2000 frame,
               this program will calculate an impact time and position.

               NOTE: Spacecraft position is assumed to be in kilometers.  If not, use the
               -distanceScale option to convert to km.

               NOTE:  Do not include a "pinpoint" or impact SPK in the kernels to
               load.""";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private ReferenceFrame J2000;
  private ReferenceFrame bodyFixed;
  private SmallBodyModel sbm;
  private Double finalHeight;
  private Double finalStep;
  private TDBTime t0;
  private StateVector initialObserverJ2000;
  private StateVector initialTargetJ2000;

  private Vector3 observerAccelerationJ2000;
  private Vector3 targetAccelerationJ2000;

  private StateVector lastState;

  private vtkPolyData rayBundlePolyData;
  private vtkCellArray rayBundleCells;
  private vtkPoints rayBundlePoints;

  private ImpactLocator() {}

  public ImpactLocator(
      ReferenceFrame J2000,
      ReferenceFrame bodyFixed,
      SmallBodyModel sbm,
      Double finalHeight,
      Double finalStep,
      TDBTime t0,
      StateVector initialObserverJ2000,
      StateVector initialTargetJ2000,
      TDBTime t1,
      StateVector finalObserverJ2000,
      StateVector finalTargetJ2000)
      throws SpiceErrorException {
    this.J2000 = J2000;
    this.bodyFixed = bodyFixed;
    this.sbm = sbm;
    this.finalHeight = finalHeight;
    this.finalStep = finalStep;
    this.t0 = t0;
    this.initialObserverJ2000 = initialObserverJ2000;
    this.initialTargetJ2000 = initialTargetJ2000;

    if (t1 == null) {
      observerAccelerationJ2000 = new Vector3();
      targetAccelerationJ2000 = new Vector3();
    } else {
      double duration = t1.getTDBSeconds() - t0.getTDBSeconds();
      observerAccelerationJ2000 =
          finalObserverJ2000
              .getVelocity()
              .sub(initialObserverJ2000.getVelocity())
              .scale(1. / duration);
      targetAccelerationJ2000 =
          finalTargetJ2000.getVelocity().sub(initialTargetJ2000.getVelocity()).scale(1. / duration);
    }
  }

  /**
   * find the body state at time t. Assume a constant velocity in the J2000 frame.
   *
   * @param et ephemeris time
   * @return Body state at time et
   */
  public StateVector getStateBodyFixed(TDBTime et) {

    try {
      double delta = et.sub(t0).getMeasure();

      Vector3 observerPosJ2000 =
          initialObserverJ2000
              .getPosition()
              .add(initialObserverJ2000.getVelocity().scale(delta))
              .add(observerAccelerationJ2000.scale(0.5 * delta * delta));

      Vector3 targetPosJ2000 =
          initialTargetJ2000
              .getPosition()
              .add(initialTargetJ2000.getVelocity().scale(delta))
              .add(targetAccelerationJ2000.scale(0.5 * delta * delta));

      Vector3 scPosJ2000 = observerPosJ2000.sub(targetPosJ2000);
      Vector3 observerVelJ2000 =
          initialObserverJ2000.getVelocity().add(observerAccelerationJ2000.scale(delta));
      Vector3 targetVelJ2000 =
          initialTargetJ2000.getVelocity().add(targetAccelerationJ2000.scale(delta));
      Vector3 scVelJ2000 = observerVelJ2000.sub(targetVelJ2000);

      StateVector scStateJ2000 = new StateVector(scPosJ2000, scVelJ2000);
      StateVector scStateBodyFixed =
          new StateVector(J2000.getStateTransformation(bodyFixed, et).mxv(scStateJ2000));

      if (lastState == null) {
        lastState = scStateBodyFixed;
        rayBundlePolyData = new vtkPolyData();
        rayBundleCells = new vtkCellArray();
        rayBundlePoints = new vtkPoints();

        rayBundlePolyData.SetPoints(rayBundlePoints);
        rayBundlePolyData.SetLines(rayBundleCells);
      }
      long id0 = rayBundlePoints.InsertNextPoint(lastState.getPosition().toArray());
      long id1 = rayBundlePoints.InsertNextPoint(scStateBodyFixed.getPosition().toArray());
      lastState = scStateBodyFixed;

      vtkLine line = new vtkLine();
      line.GetPointIds().SetId(0, id0);
      line.GetPointIds().SetId(1, id1);

      rayBundleCells.InsertNextCell(line);

      return scStateBodyFixed;

    } catch (SpiceException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
    return null;
  }

  /** return range from surface at time t */
  @Override
  public double value(double t) {

    TDBTime thisTime = new TDBTime(t);

    StateVector scStateBodyFixed = getStateBodyFixed(thisTime);

    Vector3 closestPoint =
        new Vector3(sbm.findClosestPoint(scStateBodyFixed.getPosition().toArray()));

    Vector3 toSurface = scStateBodyFixed.getPosition().sub(closestPoint);
    return toSurface.norm();
  }

  private NavigableSet<ImpactRecord> findTrajectory() {
    NavigableSet<ImpactRecord> records = new TreeSet<>();
    try {

      lastState = null;

      TDBTime et = t0;

      StateVector scStateBodyFixed = getStateBodyFixed(et);

      Vector3 closestPoint =
          new Vector3(sbm.findClosestPoint(scStateBodyFixed.getPosition().toArray()));
      Vector3 toSurface = scStateBodyFixed.getPosition().sub(closestPoint);
      double altitude = toSurface.norm();

      // time it takes to get halfway to the surface
      TDBDuration delta = new TDBDuration(altitude / (2 * scStateBodyFixed.getVelocity().norm()));

      boolean keepGoing = true;
      while (keepGoing) {
        LatitudinalCoordinates lc = new LatitudinalCoordinates(scStateBodyFixed.getPosition());
        records.add(
            new ImpactRecord(
                et,
                scStateBodyFixed,
                new LatitudinalCoordinates(altitude, lc.getLongitude(), lc.getLatitude())));

        et = et.add(delta);

        scStateBodyFixed = getStateBodyFixed(et);

        closestPoint = new Vector3(sbm.findClosestPoint(scStateBodyFixed.getPosition().toArray()));
        toSurface = scStateBodyFixed.getPosition().sub(closestPoint);
        altitude = toSurface.norm();

        // check that we're still moving towards the target
        if (scStateBodyFixed.getPosition().dot(scStateBodyFixed.getVelocity()) > 0) {
          logger.warn(
              "Stopping at {}; passed closest approach to the body center.",
              et.toUTCString("ISOC", 3));
          keepGoing = false;
        }

        if (altitude > finalHeight) {
          delta = new TDBDuration(toSurface.norm() / (2 * scStateBodyFixed.getVelocity().norm()));
        } else if (altitude > finalStep) {
          delta = new TDBDuration(finalStep / scStateBodyFixed.getVelocity().norm());
        } else {
          keepGoing = false;
        }
      }

      LatitudinalCoordinates lc = new LatitudinalCoordinates(scStateBodyFixed.getPosition());
      records.add(
          new ImpactRecord(
              et,
              scStateBodyFixed,
              new LatitudinalCoordinates(altitude, lc.getLongitude(), lc.getLatitude())));

    } catch (SpiceException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
    return records;
  }

  static class ImpactRecord implements Comparable<ImpactRecord> {
    TDBTime et;
    StateVector scStateBodyFixed;
    LatitudinalCoordinates lc;

    private ImpactRecord(TDBTime et, StateVector scStateBodyFixed, LatitudinalCoordinates lc) {
      this.et = et;
      this.scStateBodyFixed = scStateBodyFixed;
      this.lc = lc;
    }

    @Override
    public int compareTo(ImpactRecord o) {
      try {
        return Double.compare(et.getTDBSeconds(), o.et.getTDBSeconds());
      } catch (SpiceErrorException e) {
        // completely unnecessary exception
        return 0;
      }
    }
  }

  private static Vector3 correctForAberration(
      Vector3 targetLTS, Body observer, Body target, TDBTime t) throws SpiceException {
    RemoveAberration ra = new RemoveAberration(target, observer);

    return ra.getGeometricPosition(t, targetLTS);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("date")
            .hasArgs()
            .desc("Initial UTC date.  Required if -sumFile is not used.")
            .build());
    options.addOption(
        Option.builder("finalHeight")
            .hasArg()
            .desc("Height above surface in meters to consider \"impact\".  Default is 1 meter.")
            .build());
    options.addOption(
        Option.builder("finalStep")
            .hasArg()
            .desc(
                "Continue printing output below finalHeight in increments of approximate finalStep "
                    + "(in meters) until zero.  Default is to stop at finalHeight.")
            .build());
    options.addOption(
        Option.builder("frame")
            .required()
            .hasArg()
            .desc("Required.  Name of body fixed frame.")
            .build());
    options.addOption(
        Option.builder("instrumentFrame")
            .hasArg()
            .desc(
                "SPICE ID for the camera reference frame.  Required if -outputTransform "
                    + "AND -sumFile are used.")
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
        Option.builder("objFile")
            .required()
            .hasArg()
            .desc("Required.  Name of OBJ shape file.")
            .build());
    options.addOption(
        Option.builder("observer")
            .required()
            .hasArg()
            .desc("Required.  SPICE ID for the impactor.")
            .build());
    options.addOption(
        Option.builder("observerFrame")
            .hasArg()
            .desc(
                "SPICE ID for the impactor's reference frame.  Required if -outputTransform is used.")
            .build());
    options.addOption(
        Option.builder("outputTransform")
            .hasArg()
            .desc(
                "If present, write out a transform file that can be used by TransformShape to place "
                    + "coordinates in the spacecraft frame in the body fixed frame.  The rotation "
                    + " is evaluated at the sumfile time.  The translation is evaluated at the impact time.  "
                    + "Requires -observerFrame option.")
            .build());
    options.addOption(
        Option.builder("position")
            .hasArg()
            .desc(
                "Spacecraft to body vector in body fixed coordinates.  Units are km.  "
                    + "Spacecraft is at the origin to be consistent with sumFile convention.")
            .build());
    options.addOption(
        Option.builder("spice")
            .required()
            .hasArgs()
            .desc(
                "Required.  SPICE metakernel file containing body fixed frame and spacecraft kernels.  "
                    + "Can specify more than one kernel, separated by whitespace.")
            .build());
    options.addOption(
        Option.builder("sumFile")
            .hasArg()
            .desc(
                "Name of sum file to read.  Coordinate system is assumed to be in the body "
                    + "fixed frame with the spacecraft at the origin.")
            .build());
    options.addOption(
        Option.builder("target")
            .required()
            .hasArg()
            .desc("Required.  SPICE ID for the target.")
            .build());
    options.addOption(
        Option.builder("trajectory")
            .hasArg()
            .desc(
                "If present, name of output VTK file containing trajectory in body fixed coordinates.")
            .build());
    options.addOption(
        Option.builder("verbosity")
            .hasArg()
            .desc("This option does nothing!  Use -logLevel instead.")
            .build());
    options.addOption(
        Option.builder("velocity")
            .hasArg()
            .desc(
                "Spacecraft velocity in J2000 relative to the body.  Units are km/s.  "
                    + "If not specified, velocity is calculated using SPICE.")
            .build());
    return options;
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new ImpactLocator();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();
    NativeLibraryLoader.loadSpiceLibraries();

    String objFile = cl.getOptionValue("objFile");
    SmallBodyModel sbm = new SmallBodyModel(PolyDataUtil.loadShapeModel(objFile));

    for (String kernel : cl.getOptionValues("spice")) KernelDatabase.load(kernel);
    ReferenceFrame J2000 = new ReferenceFrame("J2000");
    ReferenceFrame bodyFixed = new ReferenceFrame(cl.getOptionValue("frame"));
    Body observer = new Body(cl.getOptionValue("observer"));
    Body target = new Body(cl.getOptionValue("target"));

    final double finalHeight =
        cl.hasOption("finalHeight")
            ? Double.parseDouble(cl.getOptionValue("finalHeight")) / 1e3
            : 1e-3;
    if (finalHeight <= 0) {
      logger.warn("Argument to -finalHeight must be positive!");
      System.exit(0);
    }

    final double finalStep =
        cl.hasOption("finalStep")
            ? Double.parseDouble(cl.getOptionValue("finalStep")) / 1e3
            : Double.MAX_VALUE;
    if (finalStep <= 0) {
      logger.warn("Argument to -finalStep must be positive!");
      System.exit(0);
    }

    // initial spacecraft position relative to target body
    Vector3 initialPos = new Vector3();
    TDBTime et = null;
    SumFile sumFile = null;
    if (cl.hasOption("sumFile")) {
      sumFile = SumFile.fromFile(new File(cl.getOptionValue("sumFile")));

      et = new TDBTime(sumFile.utcString());

      Matrix33 bodyFixedToJ2000 = bodyFixed.getPositionTransformation(J2000, et);
      Vector3 scObjJ2000 = bodyFixedToJ2000.mxv(MathConversions.toVector3(sumFile.scobj()));
      initialPos = correctForAberration(scObjJ2000, observer, target, et);
      initialPos = bodyFixedToJ2000.mtxv(initialPos).negate();

    } else if (cl.hasOption("date")) {
      String[] parts = cl.getOptionValues("date");
      StringBuilder sb = new StringBuilder();
      for (String part : parts) sb.append(part).append(" ");
      et = new TDBTime(sb.toString());
    } else {
      logger.warn("Either -sumFile or -date must be specified.");
      System.exit(0);
    }
    TDBTime et0 = et;
    AberrationCorrection abCorrNone = new AberrationCorrection("NONE");

    // target's state relative to observer
    StateRecord sr = new StateRecord(target, et, bodyFixed, abCorrNone, observer);

    /*-
    // aberration test
    sr = new StateRecord(target, et, J2000, abCorrNone, observer);
    StateRecord srLTS =
        new StateRecord(target, et, J2000, new AberrationCorrection("LT+S"), observer);
    RemoveAberration ra = new RemoveAberration(target, observer);
    Vector3 estimatedGeometric = ra.getGeometricPosition(et, srLTS.getPosition());

    System.out.printf("LT+S position:       %s\n", new Vector3(srLTS.getPosition()));
    System.out.printf("geometric position:  %s\n", new Vector3(sr.getPosition()));
    Vector3 difference = sr.getPosition().sub(srLTS.getPosition());
    System.out.printf("difference:          %s %f\n", difference, difference.norm());
    System.out.printf("aberration angle:    %.3e\n", srLTS.getPosition().sep(sr.getPosition()));
    System.out.printf("estimated geometric: %s\n", estimatedGeometric);
    difference = sr.getPosition().sub(estimatedGeometric);
    System.out.printf("difference:          %s %f\n", difference, difference.norm());
    System.out.printf("angle:               %.3e\n", estimatedGeometric.sep(sr.getPosition()));
    System.out.println();
    System.exit(0);
    */

    // this is only true with aberration correction NONE!
    Vector3 scPosBodyFixed = sr.getPosition().negate();

    if (cl.hasOption("position")) {
      String[] parts = cl.getOptionValue("position").split(",");
      double[] tmp = new double[3];
      for (int i = 0; i < 3; i++) tmp[i] = Double.parseDouble(parts[i].trim());
      initialPos.assign(tmp);
    } else if (!cl.hasOption("sumFile")) {
      // use position calculated by SPICE
      initialPos = scPosBodyFixed;
    }

    if (Math.abs(scPosBodyFixed.sub(initialPos).norm()) > 0) {
      logger.warn(
          String.format(
              "Warning!  Spacecraft position relative to target from SPICE is %s while input position is %s",
              new Vector3(scPosBodyFixed), initialPos.toString()));
      logger.warn(String.format("Difference is %e km", initialPos.sub(scPosBodyFixed).norm()));
      logger.warn("Continuing with input position");
    }

    Vector3 initialPosJ2000 = bodyFixed.getPositionTransformation(J2000, et).mxv(initialPos);

    // relative to solar system barycenter in J2000
    StateVector initialTargetJ2000 =
        new StateRecord(target, et, J2000, abCorrNone, new Body(0)).getStateVector();
    StateVector initialObserverJ2000 =
        new StateVector(
            initialPosJ2000.add(initialTargetJ2000.getPosition()),
            new StateRecord(observer, et, J2000, abCorrNone, new Body(0)).getVelocity());

    if (cl.hasOption("velocity")) {
      String[] parts = cl.getOptionValue("velocity").split(",");
      double[] tmp = new double[3];
      for (int i = 0; i < 3; i++) tmp[i] = Double.parseDouble(parts[i].trim());
      Vector3 scVelJ2000 = new Vector3(tmp);

      initialObserverJ2000 =
          new StateVector(
              initialObserverJ2000.getPosition(), scVelJ2000.add(initialTargetJ2000.getVelocity()));

      StateRecord obs = new StateRecord(observer, et, J2000, abCorrNone, new Body(0));
      logger.info(
          String.format(
              "spacecraft velocity relative to target from SPICE at %s is %s",
              et.toUTCString("ISOC", 3), obs.getVelocity().sub(initialTargetJ2000.getVelocity())));
      logger.info(String.format("Specified velocity is %s", scVelJ2000));
    }

    ImpactLocator ifsf =
        new ImpactLocator(
            J2000,
            bodyFixed,
            sbm,
            finalHeight,
            finalStep,
            et,
            initialObserverJ2000,
            initialTargetJ2000,
            null,
            null,
            null);

    NavigableSet<ImpactRecord> records = ifsf.findTrajectory();
    TDBTime first = records.first().et;

    StateVector scStateBodyFixed = ifsf.getStateBodyFixed(first);
    StateVector scStateJ2000 =
        new StateVector(bodyFixed.getStateTransformation(J2000, first).mxv(scStateBodyFixed));

    System.out.printf("T0: %s%n", first.toUTCString("ISOC", 3));
    System.out.printf("Frame %s: %s%n", bodyFixed.getName(), scStateBodyFixed);
    System.out.printf("Frame J2000: %s%n", scStateJ2000);
    System.out.printf(
        "%s: Observer velocity relative to SSB (J2000): %s%n",
        first.toUTCString("ISOC", 3), initialObserverJ2000.getVelocity());

    // find coverage of observer and target
    int numSPK = KernelDatabase.ktotal("SPK");
    double lastTarget = -Double.MAX_VALUE;
    double lastObserver = -Double.MAX_VALUE;
    for (int i = 0; i < numSPK; i++) {
      String filename = KernelDatabase.getFileName(i, "SPK");
      SPK thisSPK = SPK.openForRead(filename);
      SpiceWindow coverage = thisSPK.getCoverage(target.getIDCode());
      if (coverage.card() > 0) {
        double[] lastInterval = coverage.getInterval(coverage.card() - 1);
        lastTarget = Math.max(lastTarget, lastInterval[1]);
        logger.debug(
            "SPK {}: body {}, last time is {}",
            filename,
            target.getName(),
            new TDBTime(lastTarget).toUTCString("ISOC", 3));
      }
      coverage = thisSPK.getCoverage(observer.getIDCode());
      if (coverage.card() > 0) {
        double[] lastInterval = coverage.getInterval(coverage.card() - 1);
        lastObserver = Math.max(lastObserver, lastInterval[1]);
        logger.debug(
            "SPK {}: body {}, last time is {}",
            filename,
            observer.getName(),
            new TDBTime(lastObserver).toUTCString("ISOC", 3));
      }
    }

    double lastET = Math.min(records.last().et.getTDBSeconds(), lastTarget);
    lastET = Math.min(lastET, lastObserver);
    TDBTime last = new TDBTime(lastET);
    StateRecord finalObserverJ2000 =
        new StateRecord(observer, last, J2000, abCorrNone, new Body(0));
    StateRecord finalTargetJ2000 = new StateRecord(target, last, J2000, abCorrNone, new Body(0));
    System.out.printf(
        "%s: Observer velocity relative to SSB (J2000): %s%n",
        last.toUTCString("ISOC", 3), finalObserverJ2000.getVelocity());

    double duration = last.getTDBSeconds() - first.getTDBSeconds();
    Vector3 observerAccelerationJ2000 =
        finalObserverJ2000
            .getVelocity()
            .sub(initialObserverJ2000.getVelocity())
            .scale(1. / duration);
    Vector3 targetAccelerationJ2000 =
        finalTargetJ2000.getVelocity().sub(initialTargetJ2000.getVelocity()).scale(1. / duration);

    System.out.printf("Estimated time of impact %s\n", last.toUTCString("ISOC", 6));
    System.out.printf("Estimated time to impact %.6f seconds\n", duration);
    System.out.printf("Estimated observer acceleration (J2000): %s\n", observerAccelerationJ2000);
    System.out.printf("Estimated target acceleration (J2000): %s\n", targetAccelerationJ2000);
    System.out.printf(
        "observer acceleration relative to target: %s\n",
        observerAccelerationJ2000.sub(targetAccelerationJ2000));

    System.out.println();

    // Run again with constant accelerations for target and observer
    ifsf =
        new ImpactLocator(
            J2000,
            bodyFixed,
            sbm,
            finalHeight,
            finalStep,
            first,
            initialObserverJ2000,
            initialTargetJ2000,
            last,
            finalObserverJ2000,
            finalTargetJ2000);
    records = ifsf.findTrajectory();

    System.out.printf(
        "%26s, %13s, %13s, %13s, %13s, %13s, %13s, %12s, %12s, %12s",
        "UTC",
        "X (km)",
        "Y (km)",
        "Z (km)",
        "VX (km/s)",
        "VY (km/s)",
        "VZ (km/s)",
        "Lat (deg)",
        "Lon (deg)",
        "Alt (m)\n");
    for (ImpactRecord record : records) {
      PositionVector p = record.scStateBodyFixed.getPosition();
      VelocityVector v = record.scStateBodyFixed.getVelocity();
      System.out.printf(
          String.format(
              "%26s, %13.6e, %13.6e, %13.6e, %13.6e, %13.6e, %13.6e, %12.4f, %12.4f, %12.4f\n",
              record.et.toUTCString("ISOC", 6),
              p.getElt(0),
              p.getElt(1),
              p.getElt(2),
              v.getElt(0),
              v.getElt(1),
              v.getElt(2),
              Math.toDegrees(record.lc.getLatitude()),
              Math.toDegrees(record.lc.getLongitude()),
              record.lc.getRadius() * 1e3));
    }

    if (cl.hasOption("trajectory")) {

      File trajectoryFile = new File(cl.getOptionValue("trajectory"));
      File parent = trajectoryFile.getParentFile();
      if (parent != null && !parent.exists()) parent.mkdirs();

      vtkPolyDataWriter writer = new vtkPolyDataWriter();
      writer.SetInputData(ifsf.rayBundlePolyData);
      writer.SetFileName(cl.getOptionValue("trajectory"));
      writer.SetFileTypeToBinary();
      writer.Update();
    }

    if (cl.hasOption("outputTransform")) {
      if (cl.hasOption("observerFrame")) {
        // evaluate rotation at -date or -sumFile time

        File transformFile = new File(cl.getOptionValue("outputTransform"));
        File parent = transformFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        ReferenceFrame scFrame =
            new ReferenceFrame(cl.getOptionValue("observerFrame").toUpperCase());
        Matrix33 scToBodyFixed;

        if (sumFile != null) {

          // scToBodyFixed = scFrame.getPositionTransformation(bodyFixed, et0);
          // logger.info("scToBody (SPICE):\n" + scToBodyFixed);

          ReferenceFrame instrFrame = null;
          if (cl.hasOption("instrumentFrame"))
            instrFrame = new ReferenceFrame(cl.getOptionValue("instrumentFrame").toUpperCase());
          if (instrFrame == null) {
            logger.error("-instrumentFrame needed for -outputTransform.  Exiting.");
            System.exit(0);
          }
          Matrix33 scToCamera = scFrame.getPositionTransformation(instrFrame, et0);

          // DART SPECIFIC!!!!!! TODO: create a Terrasaur config file with project-specific
          // defaults,
          // like spice kernel, camera flips, etc.

          // flip -1, 2, -3

          Vector3 row0 = MathConversions.toVector3(sumFile.cx().negate());
          Vector3 row1 = MathConversions.toVector3(sumFile.cy());
          Vector3 row2 = MathConversions.toVector3(sumFile.cz().negate());

          Matrix33 bodyFixedToCamera = new Matrix33(row0, row1, row2);

          scToBodyFixed = bodyFixedToCamera.mtxm(scToCamera);

          // logger.info("scToBody (SUMFILE):\n" + scToBodyFixed);
        } else {
          scToBodyFixed = scFrame.getPositionTransformation(bodyFixed, et0);
        }

        PositionVector p = records.last().scStateBodyFixed.getPosition();
        try (PrintWriter pw = new PrintWriter(transformFile)) {

          List<String> transform = new ArrayList<>();
          for (int i = 0; i < 3; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 3; j++) sb.append(String.format("%f ", scToBodyFixed.getElt(i, j)));
            sb.append(String.format("%f ", p.getElt(i)));
            transform.add(sb.toString());
          }
          transform.add("0 0 0 1");
          StringBuilder sb = new StringBuilder();
          for (String s : transform) sb.append(String.format("%s\n", s));

          pw.println(sb);
        }
        /*-
        Pair<Vector3, Matrix33> transform =
            CommandLineOptionsUtil.getTransformation(cl.getOptionValue("outputTransform"));
        System.out.printf("translate\n\t%s\n\t%s\n", p.toString(),
            transform.getFirst().toString());
        System.out.printf("rotate\n\t%s\n\t%s\n", scToBodyFixed.toString(),
            transform.getSecond().toString());
            */
      } else {
        logger.warn("-observerFrame needed for -outputTransform");
      }
    }
  }
}
