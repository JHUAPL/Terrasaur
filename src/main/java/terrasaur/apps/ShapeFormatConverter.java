package terrasaur.apps;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spice.basic.Matrix33;
import spice.basic.Vector3;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.ImmutableSBMTEllipseRecord.Builder;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.math.RotationUtils;
import vtk.vtkPoints;
import vtk.vtkPolyData;

public class ShapeFormatConverter implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Transform a shape model to a new coordinate system.";
  }

  @Override
  public String fullDescription(Options options) {

    String header = "";
    String footer =
        "This program will rotate, translate, and/or scale a shape model.  It can additionally transform a "
            + "single point, a sum file, or an SBMT ellipse file.  For a sum file, the SCOBJ vector is "
            + "transformed and the cx, cy, cz, and sz vectors are rotated.  For SBMT ellipse files, only "
            + "center of the ellipse is transformed.  The size, orientation, and all other fields in the "
            + "file are unchanged.";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private enum COORDTYPE {
    LATLON,
    XYZ,
    POLYDATA
  }

  private enum FORMATS {
    ICQ,
    LLR,
    OBJ,
    PDS,
    PLT,
    PLY,
    STL,
    VTK,
    FITS,
    SUM,
    SBMT
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("centerOfRotation")
            .hasArg()
            .desc(
                "Subtract this point before applying rotation matrix, add back after.  "
                    + "Specify by three floating point numbers separated by commas.  If not present default is (0,0,0).")
            .build());
    options.addOption(
        Option.builder("decimate")
            .hasArg()
            .desc(
                "Reduce the number of facets in a shape model.  The argument should be between 0 and 1.  "
                    + "For example, if a model has 100 facets and the argument to -decimate is 0.90, "
                    + "there will be approximately 10 facets after the decimation.")
            .build());
    options.addOption(
        Option.builder("input")
            .required()
            .hasArg()
            .desc(
                "Required.  Name of shape model to transform. Extension must be icq, fits, llr, obj, pds, plt, ply, sbmt, stl, sum, or vtk.  "
                    + "Alternately transform a single point using three floating point numbers separated "
                    + "by commas to specify XYZ coordinates, or latitude, longitude in degrees separated by commas.  "
                    + "Transformed point will be written to stdout in the same format as the input string.")
            .build());
    options.addOption(
        Option.builder("inputFormat")
            .hasArg()
            .desc(
                "Format of input file.  If not present format will be inferred from inputFile extension.")
            .build());
    options.addOption(
        Option.builder("output")
            .hasArg()
            .desc(
                "Required for all but single point input.  Name of transformed file.  "
                    + "Extension must be obj, plt, sbmt, stl, sum, or vtk.")
            .build());
    options.addOption(
        Option.builder("outputFormat")
            .hasArg()
            .desc(
                "Format of output file.  If not present format will be inferred from outputFile extension.")
            .build());
    options.addOption(
        Option.builder("register")
            .hasArg()
            .desc("Use SVD to transform input file to best align with register file.")
            .build());
    options.addOption(
        Option.builder("rotate")
            .hasArg()
            .desc(
                "Rotate surface points and spacecraft position.  "
                    + "Specify by an angle (degrees) and a 3 element rotation axis vector (XYZ)  "
                    + "separated by commas.")
            .build());
    options.addOption(
        Option.builder("rotateToPrincipalAxes")
            .desc("Rotate body to align along its principal axes of inertia.")
            .build());
    options.addOption(
        Option.builder("scale")
            .hasArg()
            .desc(
                "Scale the shape model by <arg>.  This can either be one value or three "
                    + "separated by commas.  One value scales all three axes uniformly, "
                    + "three values scale the x, y, and z axes respectively.  For example, "
                    + "-scale 0.5,0.25,1.5 scales the model in the x dimension by 0.5, the "
                    + "y dimension by 0.25, the z dimension by 1.5.")
            .build());
    options.addOption(
        Option.builder("translate")
            .hasArg()
            .desc(
                "Translate surface points and spacecraft position.  "
                    + "Specify by three floating point numbers separated by commas.")
            .build());
    options.addOption(
        Option.builder("translateToCenter")
            .desc("Translate body so that its center of mass is at the origin.")
            .build());
    options.addOption(
        Option.builder("transform")
            .hasArg()
            .desc(
                "Translate and rotate surface points and spacecraft position.  "
                    + "Specify a file containing a 4x4 combined translation/rotation matrix.  The top left 3x3 matrix "
                    + "is the rotation matrix.  The top three entries in the right hand column are the translation "
                    + "vector.  The bottom row is always 0 0 0 1.")
            .build());
    return options;
  }

  public static void main(String[] args) throws Exception {

    TerrasaurTool defaultOBJ = new ShapeFormatConverter();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadSpiceLibraries();
    NativeLibraryLoader.loadVtkLibraries();

    String filename = cl.getOptionValue("input");
    COORDTYPE coordType = COORDTYPE.POLYDATA;
    vtkPolyData polydata = null;
    SumFile sumFile = null;
    List<SBMTEllipseRecord> sbmtEllipse = null;

    String extension = null;
    if (cl.hasOption("inputFormat")) {
      try {
        extension =
            FORMATS.valueOf(cl.getOptionValue("inputFormat").toUpperCase()).name().toLowerCase();
      } catch (IllegalArgumentException e) {
        logger.warn("Unsupported -inputFormat {}", cl.getOptionValue("inputFormat"));
      }
    }
    if (extension == null) extension = FilenameUtils.getExtension(filename).toLowerCase();
    switch (extension) {
      case "icq", "llr", "obj", "pds", "plt", "ply", "stl", "vtk" ->
          polydata = PolyDataUtil.loadShapeModel(filename, extension);
      case "fits" -> polydata = PolyDataUtil.loadFITShapeModel(filename);
      case "sum" -> {
        List<String> lines = FileUtils.readLines(new File(filename), Charset.defaultCharset());
        sumFile = SumFile.fromLines(lines);
      }
      case "sbmt" -> {
        sbmtEllipse = new ArrayList<>();
        vtkPoints points = new vtkPoints();
        polydata = new vtkPolyData();
        polydata.SetPoints(points);
        for (String line : FileUtils.readLines(new File(filename), Charset.defaultCharset())) {
          SBMTEllipseRecord record = SBMTEllipseRecord.fromString(line);
          sbmtEllipse.add(record);
          points.InsertNextPoint(record.x(), record.y(), record.z());
        }
      }
      default -> {
        // Single point
        String[] params = filename.split(",");
        vtkPoints points = new vtkPoints();
        polydata = new vtkPolyData();
        polydata.SetPoints(points);
        if (params.length == 2) {
          double[] array =
              new Vector3D(
                      Math.toRadians(Double.parseDouble(params[0].trim())),
                      Math.toRadians(Double.parseDouble(params[1].trim())))
                  .toArray();
          points.InsertNextPoint(array);
          coordType = COORDTYPE.LATLON;
        } else if (params.length == 3) {
          double[] array = new double[3];
          for (int i = 0; i < 3; i++) array[i] = Double.parseDouble(params[i].trim());
          points.InsertNextPoint(array);
          coordType = COORDTYPE.XYZ;
        } else {
          logger.error(
              "Can't read input shape model {} with format {}", filename, extension.toUpperCase());
          System.exit(0);
        }
      }
    }

    if (cl.hasOption("decimate") && polydata != null) {
      double reduction = Double.parseDouble(cl.getOptionValue("decimate"));
      if (reduction < 0) {
        logger.printf(Level.WARN, "Argument to -decimate is %.f!  Setting to zero.", reduction);
        reduction = 0;
      }
      if (reduction > 1) {
        logger.printf(Level.WARN, "Argument to -decimate is %.f!  Setting to one.", reduction);
        reduction = 1;
      }
      PolyDataUtil.decimatePolyData(polydata, reduction);
    }

    if (coordType == COORDTYPE.POLYDATA && !cl.hasOption("output")) {
      logger.error(String.format("No output file specified for input file %s", filename));
      System.exit(0);
    }

    Vector3 centerOfRotation = null;
    Matrix33 rotation = null;
    Vector3 translation = null;
    Vector3 scale = new Vector3(1., 1., 1.);
    for (Option option : cl.getOptions()) {
      if (option.getOpt().equals("centerOfRotation"))
        centerOfRotation =
            MathConversions.toVector3(
                VectorUtils.stringToVector3D(cl.getOptionValue("centerOfRotation")));

      if (option.getOpt().equals("rotate"))
        rotation =
            MathConversions.toMatrix33(RotationUtils.stringToRotation(cl.getOptionValue("rotate")));

      if (option.getOpt().equals("scale")) {
        String scaleString = cl.getOptionValue("scale");
        if (scaleString.contains(",")) {
          scale = MathConversions.toVector3(VectorUtils.stringToVector3D(scaleString));
        } else {
          scale = scale.scale(Double.parseDouble(scaleString));
        }
      }

      if (option.getOpt().equals("translate"))
        translation =
            MathConversions.toVector3(VectorUtils.stringToVector3D(cl.getOptionValue("translate")));

      if (option.getOpt().equals("transform")) {
        List<String> lines =
            FileUtils.readLines(new File(cl.getOptionValue("transform")), Charset.defaultCharset());
        Pair<Vector3D, Rotation> pair = RotationUtils.stringToTransform(lines);
        translation = MathConversions.toVector3(pair.getKey());
        rotation = MathConversions.toMatrix33(pair.getValue());
      }
    }

    if (cl.hasOption("rotateToPrincipalAxes")) {
      if (polydata != null) {
        PolyDataStatistics stats = new PolyDataStatistics(polydata);
        if (stats.isClosed()) {
          ArrayList<double[]> axes = stats.getPrincipalAxes();
          // make X primary, Y secondary
          rotation = new Matrix33(new Vector3(axes.get(0)), 1, new Vector3(axes.get(1)), 2);
        } else {
          logger.warn("Shape is not closed, cannot determine principal axes.");
        }
      }
    }

    if (cl.hasOption("register")) {
      String register = cl.getOptionValue("register");
      vtkPolyData registeredPolydata = null;
      extension = FilenameUtils.getExtension(register).toLowerCase();
      if (extension.equals("llr")
          || extension.equals("obj")
          || extension.equals("pds")
          || extension.equals("plt")
          || extension.equals("ply")
          || extension.equals("stl")
          || extension.equals("vtk")) {
        registeredPolydata = PolyDataUtil.loadShapeModelAndComputeNormals(register);
      } else {
        logger.error(String.format("Can't read input shape model for registration: %s", register));
        System.exit(0);
      }

      if (registeredPolydata != null) {
        Vector3D centerA = PolyDataUtil.computePolyDataCentroid(polydata);
        Vector3D centerB = PolyDataUtil.computePolyDataCentroid(registeredPolydata);

        vtkPoints points = polydata.GetPoints();
        double[][] pointsA = new double[(int) points.GetNumberOfPoints()][3];
        for (int i = 0; i < points.GetNumberOfPoints(); i++)
          pointsA[i] = new Vector3D(points.GetPoint(i)).subtract(centerA).toArray();
        points = registeredPolydata.GetPoints();

        if (points.GetNumberOfPoints() != polydata.GetPoints().GetNumberOfPoints()) {
          logger.error("registered polydata does not have the same number of points as input.");
          System.exit(0);
        }

        double[][] pointsB = new double[(int) points.GetNumberOfPoints()][3];
        for (int i = 0; i < points.GetNumberOfPoints(); i++)
          pointsB[i] = new Vector3D(points.GetPoint(i)).subtract(centerB).toArray();

        double[][] H = new double[3][3];
        for (int ii = 0; ii < points.GetNumberOfPoints(); ii++) {
          for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
              H[i][j] += pointsA[ii][i] * pointsB[ii][j];
            }
          }
        }

        RealMatrix pointMatrix = new Array2DRowRealMatrix(H);

        SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
        RealMatrix uT = svd.getUT();
        RealMatrix v = svd.getV();
        RealMatrix R = v.multiply(uT);

        if (new LUDecomposition(R).getDeterminant() < 0) {
          for (int i = 0; i < 3; i++) {
            R.multiplyEntry(i, 2, -1);
          }
        }
        rotation = new Matrix33(R.getData());
        translation = MathConversions.toVector3(centerB);
        translation = translation.sub(rotation.mxv(MathConversions.toVector3(centerA)));
      }
    }

    if (sumFile != null) {
      if (rotation != null && translation != null)
        sumFile.transform(
            MathConversions.toVector3D(translation), MathConversions.toRotation(rotation));
    } else {

      Vector3 center;
      if (polydata.GetNumberOfPoints() > 1) {
        PolyDataStatistics stats = new PolyDataStatistics(polydata);
        center = new Vector3(stats.getCentroid());
      } else {
        center = new Vector3(polydata.GetPoint(0));
      }
      if (cl.hasOption("translateToCenter")) translation = center.negate();

      double[] values = new double[3];
      for (int j = 0; j < 3; j++) values[j] = center.getElt(j) * scale.getElt(j);
      Vector3 scaledCenter = new Vector3(values);

      vtkPoints points = polydata.GetPoints();
      for (int i = 0; i < points.GetNumberOfPoints(); i++) {
        Vector3 thisPoint = new Vector3(points.GetPoint(i));
        thisPoint = thisPoint.sub(center);
        for (int j = 0; j < 3; j++) values[j] = thisPoint.getElt(j) * scale.getElt(j);
        thisPoint = new Vector3(values);
        thisPoint = thisPoint.add(scaledCenter);

        if (rotation != null) {
          if (centerOfRotation == null) centerOfRotation = new Vector3();
          /*-
          else {
          	System.out.printf("Center of rotation:\n%s\n", centerOfRotation.toString());
          	System.out.printf("-centerOfRotation %f,%f,%f\n", centerOfRotation.getElt(0),
          			centerOfRotation.getElt(1), centerOfRotation.getElt(2));
          }
          */
          thisPoint = rotation.mxv(thisPoint.sub(centerOfRotation)).add(centerOfRotation);
        }
        if (translation != null) thisPoint = thisPoint.add(translation);
        points.SetPoint(i, thisPoint.toArray());
      }
    }

    /*-
    if (rotation != null) {
    	AxisAndAngle aaa = new AxisAndAngle(rotation);
    	System.out.printf("Rotation:\n%s\n", rotation.toString());
    	System.out.printf("-rotate %.5e,%.5e,%.5e,%.5e\n", Math.toDegrees(aaa.getAngle()),
    			aaa.getAxis().getElt(0), aaa.getAxis().getElt(1), aaa.getAxis().getElt(2));
    }

    if (translation != null) {
    	System.out.printf("Translation:\n%s\n", translation.toString());
    	System.out.printf("-translate %.5e,%.5e,%.5e\n", translation.getElt(0), translation.getElt(1),
    			translation.getElt(2));
    }
    */

    if (coordType == COORDTYPE.LATLON) {
      double[] pt = new double[3];
      polydata.GetPoint(0, pt);
      Vector3D point = new Vector3D(pt);
      double lon = Math.toDegrees(point.getAlpha());
      if (lon < 0) lon += 360;
      System.out.printf("%.16f,%.16f\n", Math.toDegrees(point.getDelta()), lon);
    } else if (coordType == COORDTYPE.XYZ) {
      double[] pt = new double[3];
      polydata.GetPoint(0, pt);
      System.out.printf("%.16f,%.16f,%.16f\n", pt[0], pt[1], pt[2]);
    } else {
      filename = cl.getOptionValue("output");
      extension = null;
      if (cl.hasOption("outputFormat")) {
        try {
          extension =
              FORMATS.valueOf(cl.getOptionValue("outputFormat").toUpperCase()).name().toLowerCase();
        } catch (IllegalArgumentException e) {
          logger.warn("Unsupported -outputFormat {}", cl.getOptionValue("outputFormat"));
        }
      }
      if (extension == null) extension = FilenameUtils.getExtension(filename).toLowerCase();

      switch (extension) {
        case "vtk" -> PolyDataUtil.saveShapeModelAsVTK(polydata, filename);
        case "obj" -> PolyDataUtil.saveShapeModelAsOBJ(polydata, filename);
        case "plt" -> PolyDataUtil.saveShapeModelAsPLT(polydata, filename);
        case "stl" -> PolyDataUtil.saveShapeModelAsSTL(polydata, filename);
        case "sum" -> {
          try (PrintWriter pw = new PrintWriter(filename)) {
            pw.print(sumFile.toString());
          }
        }
        case "sbmt" -> {
          if (sbmtEllipse == null) {
            logger.error("No input SBMT ellipse specified!");
            System.exit(0);
          }
          double[] pt = new double[3];
          List<SBMTEllipseRecord> transformedRecords = new ArrayList<>();
          for (SBMTEllipseRecord record : sbmtEllipse) {
            polydata.GetPoint(0, pt);
            Vector3D point = new Vector3D(pt);
            double lon = Math.toDegrees(point.getAlpha());
            if (lon < 0) lon += 360;
            Builder builder = ImmutableSBMTEllipseRecord.builder().from(record);
            builder.x(point.getX());
            builder.y(point.getY());
            builder.z(point.getZ());
            builder.lat(Math.toDegrees(point.getDelta()));
            builder.lon(lon);
            builder.radius(point.getNorm());

            transformedRecords.add(builder.build());
          }
          try (PrintWriter pw = new PrintWriter(filename)) {
            for (SBMTEllipseRecord record : transformedRecords) pw.println(record.toString());
          }
        }
        default -> {
          logger.error(
              "Can't write output shape model {} with format {}",
              filename,
              extension.toUpperCase());
          System.exit(0);
        }
      }
      logger.info("Wrote {}", filename);
    }
  }
}
