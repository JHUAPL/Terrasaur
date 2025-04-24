package terrasaur.apps;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.coords.CoordConverters;
import picante.math.vectorspace.VectorIJK;
import spice.basic.LatitudinalCoordinates;
import spice.basic.Matrix33;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import terrasaur.enums.FORMATS;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.MapPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionOrthographic;
import terrasaur.utils.saaPlotLib.canvas.symbol.Circle;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp.TYPE;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.util.StringFunctions;
import vtk.vtkPoints;

public class PointCloudToPlane implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Find a rotation and translation to transform a point cloud to a height field above the best fit plane.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer =
        "\nThis program finds a rotation and translation to transform a point cloud to a height field above the best fit plane.  "
            + "Supported input formats are ASCII, BINARY, L2, OBJ, and VTK.\n\n"
            + "ASCII format is white spaced delimited x y z coordinates.  BINARY files must contain double precision x y z coordinates.  ";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private GMTGridUtil gmu;

  public GMTGridUtil getGMU() {
    return gmu;
  }

  public void writeOutput(String outputFile) {
    if (outputFile != null) {
      try (PrintStream ps = new PrintStream(outputFile)) {
        double[][] transformation = gmu.getTransformation();
        for (int i = 0; i < 4; i++) {
          for (int j = 0; j < 4; j++) {
            ps.printf("%24.16e ", transformation[i][j]);
          }
          ps.println();
        }
      } catch (FileNotFoundException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }
  }

  private PointCloudToPlane() {}

  public PointCloudToPlane(vtkPoints points) {
    this(points, 0, 0.);
  }

  public PointCloudToPlane(vtkPoints points, int halfSize, double groundSampleDistance) {
    double[] x = new double[(int) points.GetNumberOfPoints()];
    double[] y = new double[(int) points.GetNumberOfPoints()];
    double[] z = new double[(int) points.GetNumberOfPoints()];

    for (int i = 0; i < points.GetNumberOfPoints(); i++) {
      double[] thisPoint = points.GetPoint(i);
      x[i] = thisPoint[0];
      y[i] = thisPoint[1];
      z[i] = thisPoint[2];
    }

    gmu = new GMTGridUtil(halfSize, groundSampleDistance);
    gmu.setXYZ(x, y, z);
  }

  public BufferedImage makePlot(List<Vector3> points, String name) throws SpiceException {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    VectorStatistics vStats = new VectorStatistics();
    DiscreteDataSet data = new DiscreteDataSet(name);

    PlotConfig config = ImmutablePlotConfig.builder().title(name).build();

    DiscreteDataPlot canvas;
    boolean orthographic = false;
    if (orthographic) {
      for (Vector3 p : points) {
        stats.addValue(p.getElt(2));
        vStats.add(p);
        LatitudinalCoordinates lc = new LatitudinalCoordinates(p);
        data.add(lc.getLongitude(), lc.getLatitude(), 0, p.getElt(2));
      }

      double min = stats.getMin();
      double max = stats.getMax();
      ColorRamp ramp = ColorRamp.create(TYPE.CBSPECTRAL, min, max).createReverse();
      data.setSymbol(new Circle().setSize(1.0));
      data.setColorRamp(ramp);

      Vector3 center = MathConversions.toVector3(vStats.getMean());

      double halfExtent = 0;
      for (Vector3 p : points) {
        double dist = center.sep(p);
        if (dist > halfExtent) halfExtent = dist;
      }

      ProjectionOrthographic p =
          new ProjectionOrthographic(
              config.width(),
              config.height(),
              CoordConverters.convertToLatitudinal(
                  new VectorIJK(center.getElt(0), center.getElt(1), center.getElt(2))));
      p.setRadius(Math.max(0.5, .6 / halfExtent));

      canvas = new MapPlot(config, p);
      canvas.drawAxes();
      canvas.plot(data);
      ((MapPlot) canvas).drawLatLonGrid(Math.toRadians(5), Math.toRadians(5), true);

      canvas.drawColorBar(
          ImmutableColorBar.builder()
              .rect(new Rectangle(config.leftMargin(), 40, config.width(), 10))
              .ramp(ramp)
              .numTicks(5)
              .tickFunction(StringFunctions.fixedFormat("%.3f"))
              .build());
    } else {
      for (Vector3 p : points) {
        stats.addValue(p.getElt(2));
        vStats.add(p);
        data.add(p.getElt(0), p.getElt(1), 0, p.getElt(2));
      }

      double min = stats.getMin();
      double max = stats.getMax();
      ColorRamp ramp = ColorRamp.create(TYPE.CBSPECTRAL, min, max).createReverse();
      data.setSymbol(new Circle().setSize(1.0));
      data.setColorRamp(ramp);

      canvas = new DiscreteDataPlot(config);
      AxisX xAxis = data.defaultXAxis("X");
      AxisY yAxis = data.defaultYAxis("Y");
      canvas.setAxes(xAxis, yAxis);
      canvas.drawAxes();
      canvas.plot(data);

      canvas.drawColorBar(
          ImmutableColorBar.builder()
              .rect(new Rectangle(config.leftMargin(), 40, config.width(), 10))
              .ramp(ramp)
              .numTicks(5)
              .tickFunction(StringFunctions.fixedFormat("%.3f"))
              .build());
    }
    return canvas.getImage();
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("inputFormat")
            .hasArg()
            .desc(
                "Format of input file.  If not present format is inferred from inputFile extension.")
            .build());
    options.addOption(
        Option.builder("inputFile")
            .required()
            .hasArg()
            .desc("Required.  Name of input file.")
            .build());
    options.addOption(
        Option.builder("inllr")
            .desc(
                "If present, input values are assumed to be lon, lat, rad.  Default is x, y, z.  Only used with ASCII or BINARY formats.")
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
        Option.builder("outputFile")
            .hasArg()
            .desc(
                "Name of output file to contain 4x4 transformation matrix.  The top left 3x3 matrix is the rotation matrix. The top "
                    + "three entries in the right hand column are the translation vector. The bottom row is always 0 0 0 1.\nTo convert "
                    + "from global to local:\n  transformed = rotation.mxv(point.sub(translation))")
            .build());
    options.addOption(
        Option.builder("translate")
            .hasArg()
            .desc(
                "Translate surface points and spacecraft position.  "
                    + "Specify by three floating point numbers separated by commas.  "
                    + "Default is to use centroid of input point cloud.")
            .build());
    options.addOption(
        Option.builder("plotXYZ")
            .hasArg()
            .desc(
                "Plot X vs Y (in the local frame) colored by Z.  "
                    + "Argument is the name of PNG file to write.")
            .build());
    options.addOption(
        Option.builder("plotXYR")
            .hasArg()
            .desc(
                "Plot X vs Y (in the local frame) colored by R.  "
                    + "Argument is the name of PNG file to write.")
            .build());
    options.addOption(
        Option.builder("slope")
            .desc(
                "Choose local coordinate frame such that Z points normal to the plane "
                    + "and X points along the direction of steepest descent.")
            .build());
    return options;
  }

  public static void main(String[] args) throws SpiceException {
    TerrasaurTool defaultOBJ = new PointCloudToPlane();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();
    NativeLibraryLoader.loadSpiceLibraries();

    String inFile = cl.getOptionValue("inputFile");
    boolean inLLR = cl.hasOption("inllr");

    FORMATS inFormat =
        cl.hasOption("inputFormat")
            ? FORMATS.valueOf(cl.getOptionValue("inputFormat").toUpperCase())
            : FORMATS.formatFromExtension(inFile);

    PointCloudFormatConverter pcfc = new PointCloudFormatConverter(inFormat, FORMATS.VTK);
    pcfc.read(inFile, inLLR);
    System.out.printf("%d points read from %s\n", pcfc.getPoints().GetNumberOfPoints(), inFile);

    int halfSize = 0;
    double groundSampleDistance = 0;

    vtkPoints points = pcfc.getPoints();
    PointCloudToPlane pctp = new PointCloudToPlane(points, halfSize, groundSampleDistance);

    Vector3 translation;
    if (cl.hasOption("translate")) {
      translation =
          MathConversions.toVector3(VectorUtils.stringToVector3D(cl.getOptionValue("translate")));
      pctp.getGMU().setTranslation(translation.toArray());
    }
    pctp.getGMU().calculateTransformation();

    List<Vector3> globalPts = new ArrayList<>();
    for (int i = 0; i < points.GetNumberOfPoints(); i++)
      globalPts.add(new Vector3(points.GetPoint(i)));

    double[][] transformation = pctp.getGMU().getTransformation();
    StringBuilder sb =
        new StringBuilder(
            String.format(
                "translation vector:\n%24.16e%24.16e%24.16e\n",
                transformation[0][3], transformation[1][3], transformation[2][3]));
    logger.info(sb.toString());
    sb = new StringBuilder("rotation matrix:\n");
    for (int i = 0; i < 3; i++)
      sb.append(
          String.format(
              "%24.16e%24.16e%24.16e\n",
              transformation[i][0], transformation[i][1], transformation[i][2]));
    logger.info(sb.toString());

    Matrix33 rotation = new Matrix33(pctp.getGMU().getRotation());
    translation = new Vector3(pctp.getGMU().getTranslation());

    if (cl.hasOption("slope")) {
      Vector3 z = rotation.xpose().mxv(new Vector3(0, 0, 1));
      VectorStatistics vStats = new VectorStatistics();
      for (Vector3 pt : globalPts) vStats.add(pt);

      Vector3 r = MathConversions.toVector3(vStats.getMean());

      Vector3 y = r.cross(z).hat();
      Vector3 x = y.cross(z).hat();
      rotation = new Matrix33(x, y, z);
    }

    List<Vector3> localPts = new ArrayList<>();
    for (Vector3 p : globalPts) localPts.add(rotation.mxv(p.sub(translation)));

    VectorStatistics vStats = new VectorStatistics();
    for (Vector3 localPt : localPts) vStats.add(localPt);

    if (cl.hasOption("plotXYZ")) {
      BufferedImage image = pctp.makePlot(localPts, "Z (height above plane)");
      PlotCanvas.writeImage(cl.getOptionValue("plotXYZ"), image);
    }

    if (cl.hasOption("plotXYR")) {

      // rotate but don't translate
      List<Vector3> xyr = new ArrayList<>();
      for (Vector3 p : globalPts) {
        Vector3 v = rotation.mxv(p);
        xyr.add(new Vector3(v.getElt(0), v.getElt(1), v.norm()));
      }

      BufferedImage image = pctp.makePlot(xyr, "R");
      PlotCanvas.writeImage(cl.getOptionValue("plotXYR"), image);
    }

    logger.info("statistics on full set");
    logger.info(vStats);

    Vector3 mean = MathConversions.toVector3(vStats.getMean());
    Vector3 std = MathConversions.toVector3(vStats.getStandardDeviation());
    double scale = 5;
    List<Double> minList = new ArrayList<>();
    List<Double> maxList = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      minList.add(mean.getElt(i) - scale * std.getElt(i));
      maxList.add(mean.getElt(i) + scale * std.getElt(i));
    }

    vStats = new VectorStatistics();
    for (Vector3 v : localPts) {
      boolean addThis = true;
      for (int i = 0; i < 3; i++) {
        if (v.getElt(i) < minList.get(i) || v.getElt(i) > maxList.get(i)) {
          addThis = false;
          break;
        }
      }
      if (addThis) vStats.add(v);
    }

    logger.info("statistics on set without points more than 5 standard deviations from the mean:");
    logger.info(vStats);

    if (cl.hasOption("outputFile")) pctp.writeOutput(cl.getOptionValue("outputFile"));
  }
}
