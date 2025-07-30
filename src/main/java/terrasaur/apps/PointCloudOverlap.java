/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package terrasaur.apps;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import nom.tam.fits.FitsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.MonotoneChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.LatitudinalVector;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import terrasaur.enums.FORMATS;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.VectorStatistics;
import terrasaur.utils.tessellation.StereographicProjection;
import vtk.vtkPoints;

public class PointCloudOverlap implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
      return "Find points in a point cloud which overlap a reference point cloud.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer =
        "\nThis program finds all points in the input point cloud which overlap the points in a reference point cloud.\n\n"
            + "Supported input formats are ASCII, BINARY, L2, OBJ, and VTK.  Supported output formats are ASCII, BINARY, L2, and VTK.  "
            + "ASCII format is white spaced delimited x y z coordinates.  BINARY files must contain double precision x y z coordinates.\n\n"
            + "A plane is fit to the reference point cloud and all points in each cloud are projected onto this plane.  Any point in the "
            + "projected input cloud which falls within the outline of the projected reference cloud is considered to be overlapping.";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private Path2D.Double polygon;

  private StereographicProjection proj;

  /*- Useful for debugging
  private vtkPoints ref2DPoints;
  private vtkPoints input2DPoints;
  private vtkPolyData polygonPolyData;
  private vtkCellArray polygonCells;
  private vtkPoints polygonPoints;
  private vtkDoubleArray polygonSuccessArray;
  */

  public PointCloudOverlap(Collection<double[]> refPoints) {
    if (refPoints != null) {
      VectorStatistics vStats = new VectorStatistics();
      for (double[] pt : refPoints) vStats.add(new Vector3(pt));

      Vector3D centerXYZ = vStats.getMean();

      proj =
          new StereographicProjection(
              new LatitudinalVector(1, centerXYZ.getDelta(), centerXYZ.getAlpha()));

      createRefPolygon(refPoints);
    }
  }

  public StereographicProjection getProjection() {
    return proj;
  }

  private void createRefPolygon(Collection<double[]> refPoints) {
    List<Vector2D> stereographicPoints = new ArrayList<>();
    for (double[] refPt : refPoints) {
      Vector3D point3D = new Vector3D(refPt);
      Point2D point = proj.forward(point3D.getDelta(), point3D.getAlpha());
      stereographicPoints.add(new Vector2D(point.getX(), point.getY()));
    }

    /*-
    ref2DPoints = new vtkPoints();
    input2DPoints = new vtkPoints();
    polygonPolyData = new vtkPolyData();
    polygonCells = new vtkCellArray();
    polygonPoints = new vtkPoints();
    polygonSuccessArray = new vtkDoubleArray();
    polygonSuccessArray.SetName("success")
    polygonPolyData.SetPoints(polygonPoints);
    polygonPolyData.SetLines(polygonCells);
    polygonPolyData.GetCellData().AddArray(polygonSuccessArray);

    for (Vector2D refPoint : refPoints)
    	ref2DPoints.InsertNextPoint(refPoint.getX(), refPoint.getY(), 0);
    */

    MonotoneChain mc = new MonotoneChain();
    List<Vector2D> vertices = new ArrayList<>(mc.findHullVertices(stereographicPoints));
    /*-
    for (int i = 1; i < vertices.size(); i++) {
    	Vector2D lastPt = vertices.get(i - 1);
    	Vector2D thisPt = vertices.get(i);
    	System.out.printf("%f %f to %f %f distance %f\n", lastPt.getX(), lastPt.getY(), thisPt.getX(),
    			thisPt.getY(), lastPt.distance(thisPt));
    }
    Vector2D lastPt = vertices.get(vertices.size() - 1);
    Vector2D thisPt = vertices.get(0);
    System.out.printf("%f %f to %f %f distance %f\n", lastPt.getX(), lastPt.getY(), thisPt.getX(),
    		thisPt.getY(), lastPt.distance(thisPt));
     */
    // int id0 = 0;
    for (Vector2D vertex : vertices) {
      // int id1 = polygonPoints.InsertNextPoint(vertex.getX(), vertex.getY(), 0);

      if (polygon == null) {
        polygon = new Path2D.Double();
        polygon.moveTo(vertex.getX(), vertex.getY());
      } else {
        polygon.lineTo(vertex.getX(), vertex.getY());
        /*-
        vtkLine line = new vtkLine();
        line.GetPointIds().SetId(0, id0);
        line.GetPointIds().SetId(1, id1);
        polygonCells.InsertNextCell(line);
        */
      }
      // id0 = id1;
    }
    polygon.closePath();

    /*-
    vtkPolyDataWriter writer = new vtkPolyDataWriter();
    writer.SetInputData(polygonPolyData);
    writer.SetFileName("polygon2D.vtk");
    writer.SetFileTypeToBinary();
    writer.Update();

    writer = new vtkPolyDataWriter();
    polygonPolyData = new vtkPolyData();
    polygonPolyData.SetPoints(ref2DPoints);
    writer.SetInputData(polygonPolyData);
    writer.SetFileName("refPoints.vtk");
    writer.SetFileTypeToBinary();
    writer.Update();
    */

  }

  public boolean isEnclosed(double[] xyz) {
    Vector3D point = new Vector3D(xyz);
    Point2D projected = proj.forward(point.getDelta(), point.getAlpha());
    return polygon.contains(projected.getX(), projected.getY());
  }

  /**
   * @param inputPoints points to consider
   * @param scale scale factor
   * @return indices of all points inside the scaled polygon
   */
  public List<Integer> scalePoints(List<double[]> inputPoints, double scale) {

    List<Vector2D> projected = new ArrayList<>();
    for (double[] inputPoint : inputPoints) {
      Vector3D point = new Vector3D(inputPoint);
      Point2D projectedPoint = proj.forward(point.getDelta(), point.getAlpha());
      projected.add(new Vector2D(projectedPoint.getX(), projectedPoint.getY()));
    }

    Vector2D center = new Vector2D(0, 0);
    for (Vector2D inputPoint : projected) center = center.add(inputPoint);

    center = center.scalarMultiply(1. / inputPoints.size());

    List<Vector2D> translatedPoints = new ArrayList<>();
    for (Vector2D inputPoint : projected) translatedPoints.add(inputPoint.subtract(center));

    Path2D.Double thisPolygon = null;
    MonotoneChain mc = new MonotoneChain();
    Collection<Vector2D> vertices = mc.findHullVertices(translatedPoints);
    for (Vector2D vertex : vertices) {
      if (thisPolygon == null) {
        thisPolygon = new Path2D.Double();
        thisPolygon.moveTo(scale * vertex.getX(), scale * vertex.getY());
      } else {
        thisPolygon.lineTo(scale * vertex.getX(), scale * vertex.getY());
      }
    }
    thisPolygon.closePath();

    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < projected.size(); i++) {
      Vector2D inputPoint = projected.get(i);
      if (thisPolygon.contains(
          inputPoint.getX() - center.getX(), inputPoint.getY() - center.getY())) indices.add(i);
    }
    return indices;
  }

  private static Options defineOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder("inputFormat")
            .hasArg()
            .desc(
                "Format of input file.  If not present format will be inferred from file extension.")
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
        Option.builder("referenceFormat")
            .hasArg()
            .desc(
                "Format of reference file.  If not present format will be inferred from file extension.")
            .build());
    options.addOption(
        Option.builder("referenceFile")
            .required()
            .hasArg()
            .desc("Required.  Name of reference file.")
            .build());
    options.addOption(
        Option.builder("refllr")
            .desc(
                "If present, reference values are assumed to be lon, lat, rad.  Default is x, y, z.  Only used with ASCII or BINARY formats.")
            .build());
    options.addOption(
        Option.builder("outputFormat")
            .hasArg()
            .desc(
                "Format of output file.  If not present format will be inferred from file extension.")
            .build());
    options.addOption(
        Option.builder("outputFile")
            .required()
            .hasArg()
            .desc("Required.  Name of output file.")
            .build());
    options.addOption(
        Option.builder("outllr")
            .desc(
                "If present, output values will be lon, lat, rad.  Default is x, y, z.  Only used with ASCII or BINARY formats.")
            .build());
    options.addOption(
        Option.builder("scale")
            .hasArg()
            .desc("Value to scale bounding box containing intersect region.  Default is 1.0.")
            .build());
    return options;
  }

  public static void main(String[] args)
      throws SpiceException, IOException, InterruptedException, FitsException {

    NativeLibraryLoader.loadVtkLibraries();
    NativeLibraryLoader.loadSpiceLibraries();

    TerrasaurTool defaultOBJ = new PointCloudOverlap(null);

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    // Read the reference file
    FORMATS refFormat =
        cl.hasOption("referenceFormat")
            ? FORMATS.valueOf(cl.getOptionValue("referenceFormat").toUpperCase())
            : FORMATS.formatFromExtension(cl.getOptionValue("referenceFile"));
    String refFile = cl.getOptionValue("referenceFile");
    boolean refLLR = cl.hasOption("refllr");

    PointCloudFormatConverter pcfc = new PointCloudFormatConverter(refFormat, FORMATS.VTK);
    pcfc.read(refFile, refLLR);
    vtkPoints referencePoints = pcfc.getPoints();
    logger.info("{} points read from {}", referencePoints.GetNumberOfPoints(), refFile);

    List<double[]> refPts = new ArrayList<>();
    for (int i = 0; i < referencePoints.GetNumberOfPoints(); i++) {
      refPts.add(referencePoints.GetPoint(i));
    }

    // create the overlap object and set the enclosing polygon
    PointCloudOverlap pco = new PointCloudOverlap(refPts);

    // Read the input point cloud
    FORMATS inFormat =
        cl.hasOption("inputFormat")
            ? FORMATS.valueOf(cl.getOptionValue("inputFormat").toUpperCase())
            : FORMATS.formatFromExtension(cl.getOptionValue("inputFile"));
    String inFile = cl.getOptionValue("inputFile");
    boolean inLLR = cl.hasOption("inllr");

    pcfc = new PointCloudFormatConverter(inFormat, FORMATS.VTK);
    pcfc.read(inFile, inLLR);
    vtkPoints inputPoints = pcfc.getPoints();
    logger.info("{} points read from {}", inputPoints.GetNumberOfPoints(), inFile);

    List<Integer> enclosedIndices = new ArrayList<>();
    for (int i = 0; i < inputPoints.GetNumberOfPoints(); i++) {
      double[] pt = inputPoints.GetPoint(i);
      if (pco.isEnclosed(pt)) enclosedIndices.add(i);
    }

    if (cl.hasOption("scale")) {
      List<double[]> pts = new ArrayList<>();
      for (Integer i : enclosedIndices) pts.add(inputPoints.GetPoint(i));

      // this list includes which of the enclosed points are inside the scaled polygon
      List<Integer> theseIndices = pco.scalePoints(pts, Double.parseDouble(cl.getOptionValue("scale")));

      // now relate this list back to the original list of points
      List<Integer> newIndices = new ArrayList<>();
      for (Integer i : theseIndices) newIndices.add(enclosedIndices.get(i));
      enclosedIndices = newIndices;
    }

    VectorStatistics xyzStats = new VectorStatistics();
    VectorStatistics xyStats = new VectorStatistics();
    for (Integer i : enclosedIndices) {
      double[] thisPt = inputPoints.GetPoint(i);
      Vector3D thisPt3D = new Vector3D(thisPt);
      xyzStats.add(thisPt3D);
      Point2D projectedPt = pco.getProjection().forward(thisPt3D.getDelta(), thisPt3D.getAlpha());
      xyStats.add(new Vector3(projectedPt.getX(), projectedPt.getY(), 0));
    }

    logger.info(
        "Center XYZ: {}, {}, {}",
        xyzStats.getMean().getX(), xyzStats.getMean().getY(), xyzStats.getMean().getZ());
    Vector3D centerXYZ = xyzStats.getMean();
    logger.info(
        "Center lon, lat: {}, {}\n",
        Math.toDegrees(centerXYZ.getAlpha()), Math.toDegrees(centerXYZ.getDelta()));
    logger.info(
        "xmin/xmax/extent: {}/{}/{}\n",
        xyzStats.getMin().getX(),
        xyzStats.getMax().getX(),
        xyzStats.getMax().getX() - xyzStats.getMin().getX());
    logger.info(
        "ymin/ymax/extent: {}/{}/{}\n",
        xyzStats.getMin().getY(),
        xyzStats.getMax().getY(),
        xyzStats.getMax().getY() - xyzStats.getMin().getY());
    logger.info(
        "zmin/zmax/extent: {}/{}/{}\n",
        xyzStats.getMin().getZ(),
        xyzStats.getMax().getZ(),
        xyzStats.getMax().getZ() - xyzStats.getMin().getZ());

    FORMATS outFormat =
        cl.hasOption("outputFormat")
            ? FORMATS.valueOf(cl.getOptionValue("outputFormat").toUpperCase())
            : FORMATS.formatFromExtension(cl.getOptionValue("outputFile"));

    vtkPoints pointsToWrite = new vtkPoints();
    for (Integer i : enclosedIndices) pointsToWrite.InsertNextPoint(inputPoints.GetPoint(i));
    pcfc = new PointCloudFormatConverter(FORMATS.VTK, outFormat);
    pcfc.setPoints(pointsToWrite);
    String outputFilename = cl.getOptionValue("outputFile");
    pcfc.write(outputFilename, cl.hasOption("outllr"));
    if (new File(outputFilename).exists()) {
      logger.info(
          "{} points written to {}",
          pointsToWrite.GetNumberOfPoints(), outputFilename);
    } else {
      logger.error("Could not write {}", outputFilename);
    }

    logger.info("Finished");
  }
}

// TODO write out center of output pointcloud
