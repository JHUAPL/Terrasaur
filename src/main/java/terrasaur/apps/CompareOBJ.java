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

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.Plane;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import terrasaur.enums.FORMATS;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.lidar.LidarTransformation;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkIdList;
import vtk.vtkOctreePointLocator;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkUnstructuredGrid;

public class CompareOBJ implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  private CompareOBJ() {}

  @Override
  public String shortDescription() {
    return "Report the differences between two OBJ shape files.";
  }

  @Override
  public String fullDescription(Options options) {
    HelpFormatter formatter = new HelpFormatter();

    String header = "";
    StringBuffer p1 = new StringBuffer("This program takes a point cloud or shape model and ");
    p1.append("compares to a reference shape model.");
    StringBuffer p2 = new StringBuffer("It iterates over each point or facet center and ");
    p2.append("finds the closest point on the surface defined by the reference shape.  ");
    p2.append("The program then outputs the overall mean distance, mean squared distance ");
    p2.append("and RMS distance between the corresponding points and prints them out to ");
    p2.append("the terminal. The models do not need to be the same size. All units shown ");
    p2.append("are in terms of the original units employed in the shape models (both must ");
    p2.append("be the same). When comparing global models, use --fit-plane-radius for best ");
    p2.append("results.");

    StringBuffer footer = new StringBuffer("\n");
    footer.append(WordUtils.wrap(p1.toString(), formatter.getWidth()));
    footer.append("\n");
    footer.append("\n");
    footer.append(WordUtils.wrap(p2.toString(), formatter.getWidth()));
    return TerrasaurTool.super.fullDescription(options, header, footer.toString());
  }

  private String referenceModelName;
  private vtkPolyData polyDataModel;
  private FORMATS inputFormat;
  private vtkPolyData polyDataTruth;

  private LidarTransformation transform = LidarTransformation.defaultTransform();

  private String tmpdir;

  private static class DistanceContainer implements Comparable<DistanceContainer> {
    double closestDistance;
    double normalDistance;

    public DistanceContainer(double closestDistance) {
      super();
      this.closestDistance = closestDistance;
      this.normalDistance = -1;
    }

    public void setNormalDistance(double normalDistance) {
      this.normalDistance = normalDistance;
    }

    public double getClosestDistance() {
      return closestDistance;
    }

    public double getNormalDistance() {
      return normalDistance;
    }

    @Override
    public int compareTo(DistanceContainer o) {
      return Double.compare(closestDistance, o.closestDistance);
    }
  }

  public CompareOBJ(String modelName, String referenceModelName) {
    this.referenceModelName = referenceModelName;
    try {
      inputFormat = FORMATS.formatFromExtension(modelName);
      if (inputFormat.pointsOnly) {
        vtkPoints points = PointCloudFormatConverter.readPointCloud(modelName);
        polyDataModel = new vtkPolyData();
        polyDataModel.SetPoints(points);
      } else polyDataModel = PolyDataUtil.loadShapeModel(modelName);
      polyDataTruth = PolyDataUtil.loadShapeModel(referenceModelName);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      System.exit(0);
    }

    this.tmpdir = ".";
  }

  public void setTmpdir(String tmpdir) {
    this.tmpdir = tmpdir;
  }

  /**
   * Save out polydata to track format for use as input to lidar-optimize.
   *
   * @param polydata polydata to convert
   * @param filename filename to write
   * @param useOverlappingPoints if true, only points overlapping the reference model will be added
   *     to the track file.
   */
  private void convertPolyDataToTrackFormat(
      vtkPolyData polydata, String filename, boolean useOverlappingPoints) {
    vtkPoints polyDataPoints = polydata.GetPoints();
    vtkPoints points = polyDataPoints;

    if (useOverlappingPoints) {
      vtkPoints modelPoints = polyDataTruth.GetPoints();
      vtkUnstructuredGrid overlap = PolyDataUtil.intersectingPoints(modelPoints, polyDataPoints);
      points = overlap.GetPoints();
    }

    try (PrintWriter pw = new PrintWriter(filename)) {
      double[] p = new double[3];
      for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
        points.GetPoint(i, p);
        pw.write(
            String.format(
                "2010-11-11T00:00:00.000 "
                    + p[0]
                    + " "
                    + p[1]
                    + " "
                    + p[2]
                    + " "
                    + p[0]
                    + " "
                    + p[1]
                    + " "
                    + p[2]
                    + "\n"));
      }
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Run the C++ lidar-optimize code and store the derived transformation.
   *
   * @param computeOptimalTranslation true if translation should be computed
   * @param computeOptimalRotation true if rotation should be computed
   * @param maxNumberOfControlPoints maximum number of control points
   * @param useOverlappingPoints if true, only points overlapping the reference model will be used
   * @param transformationFile JSON file to write
   */
  public void computeOptimalTransformationToTarget(
      boolean computeOptimalTranslation,
      boolean computeOptimalRotation,
      int maxNumberOfControlPoints,
      boolean useOverlappingPoints,
      String transformationFile) {
    File tmpDir =
        new File(
            String.format("%s%sCompareOBJ-%d", tmpdir, File.separator, System.currentTimeMillis()));
    if (!tmpDir.exists()) tmpDir.mkdirs();

    String trackFile = tmpDir + File.separator + "shapemodel-as-track.txt";
    convertPolyDataToTrackFormat(polyDataModel, trackFile, useOverlappingPoints);

    String tmpTransformationFile = tmpDir + File.separator + "transformationfile.txt";
    String transformationType = null;
    if (computeOptimalTranslation && computeOptimalRotation) transformationType = "";
    else if (computeOptimalTranslation) transformationType = "--translation-only ";
    else if (computeOptimalRotation) transformationType = "--rotation-only ";

    File lsk = ResourceUtils.writeResourceToFile("/resources/kernels/lsk/naif0012.tls");
    String command =
        "lidar-optimize --max-number-control-points "
            + maxNumberOfControlPoints
            + " "
            + transformationType
            + "--load-from-file "
            + lsk.getPath()
            + " "
            + referenceModelName
            + " "
            + trackFile
            + " 0 0 "
            + tmpTransformationFile;

    long startTime = System.currentTimeMillis();
    try {
      ProcessUtils.runProgramAndWait(command, null, false);
    } catch (IOException | InterruptedException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
    long duration = System.currentTimeMillis() - startTime;
    logger.info(String.format("Execution time %.3f seconds", duration / 1e3));

    // save a copy of the JSON transformation file
    if (transformationFile != null) {
      try (PrintWriter pw = new PrintWriter(transformationFile)) {
        List<String> lines =
            FileUtils.readLines(new File(tmpTransformationFile), Charset.defaultCharset());
        for (String line : lines) pw.println(line);
      } catch (IOException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }

    transform = LidarTransformation.fromJSON(new File(tmpTransformationFile));
  }

  /**
   * Transform the model polydata with the lidar transform.
   *
   * @param saveOptimalShape if true, write out transformed shape
   * @param optimalShapeFile name of shape file to write
   */
  private void transformPolyData(boolean saveOptimalShape, String optimalShapeFile) {
    // Transform all points in inpolydata1
    long numPoints = polyDataModel.GetNumberOfPoints();
    vtkPoints points = polyDataModel.GetPoints();
    for (int i = 0; i < numPoints; ++i) {
      Vector3D transformedPoint = transform.transformPoint(new Vector3D(points.GetPoint(i)));
      points.SetPoint(i, transformedPoint.toArray());
    }

    if (saveOptimalShape) {
      if (inputFormat.pointsOnly) {
        FORMATS outputFormat = FORMATS.formatFromExtension(optimalShapeFile);
        PointCloudFormatConverter pcfc = new PointCloudFormatConverter(inputFormat, outputFormat);
        pcfc.setPoints(polyDataModel.GetPoints());
        pcfc.write(optimalShapeFile, false);
      } else {
        try {
          PolyDataUtil.saveShapeModelAsOBJ(polyDataModel, optimalShapeFile);
        } catch (IOException e) {
          logger.error(e.getLocalizedMessage(), e);
        }
      }
    }
  }

  /**
   * return distances between each pair of points projected onto a plane
   *
   * @param points List of points
   * @param fitPlane plane to project points
   * @return distances on the plane
   */
  private List<Double> findDistances(List<Vector3> points, Plane fitPlane) {
    List<Vector3> projectedPoints = new ArrayList<>();
    for (Vector3 point : points) {
      try {
        projectedPoints.add(fitPlane.project(point));
      } catch (SpiceException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }

    List<Double> distances = new ArrayList<>();
    for (int i = 0; i < projectedPoints.size(); i++) {
      Vector3 point0 = projectedPoints.get(i);
      for (int j = i + 1; j < projectedPoints.size(); j++) {
        Vector3 point1 = projectedPoints.get(j);
        distances.add(point1.sub(point0).norm());
      }
    }
    return distances;
  }

  /**
   *
   *
   * <ol>
   *   <li>Have the code randomly select npts region across the model and find a distinguishing
   *       feature (I was thinking of a local maximum or minimum in this region). Then find the
   *       corresponding best fit point (i.e. the local minima or maxima) in the truth model.
   *   <li>Determine the horizontal distance in the plane of the model between each one of these
   *       distinguishing features. Repeat the same exercise across the truth, but first fit a plane
   *       over the region of the truth that more or less match the extent of the model.
   *   <li>Subtract from the distance measured in the plane of the model between the npts regions
   *       from the distance between the same npts regions in the truth model, and normalize the
   *       result by the distance between the npts regions in the truth. Multiply this number by the
   *       typical extent of maplet which should be input as an option; the default should be 5 m.
   *   <li>
   * </ol>
   *
   * @param npts number of regions to select over the shape model
   * @param radius size of maplet
   */
  private void assessHorizontalAccuracy(int npts, double radius) throws SpiceException {
    // find npts random facets in the shape model
    long numVertices = polyDataModel.GetNumberOfPoints();
    List<Integer> vertexIDs = new ArrayList<>();
    do {
      int nextID = (int) (numVertices * Math.random());
      if (!vertexIDs.contains(nextID)) vertexIDs.add(nextID);
    } while (vertexIDs.size() < npts);

    // now find the distance between each pair of points in the plane of the model
    Plane fitPlaneModel = PolyDataUtil.fitPlaneToPolyData(polyDataModel);

    double[] pt = new double[3];
    List<Vector3> points = new ArrayList<>();
    for (int vertexID : vertexIDs) {
      polyDataModel.GetPoint(vertexID, pt);
      points.add(new Vector3(pt));
    }

    List<Double> distancesModel = findDistances(points, fitPlaneModel);

    // now find each of these points in the truth model
    double resolution = .1; // meters per pixel
    int offset = 5; // number of pixels to slide model in each direction to get best match with
    // truth

    // 2D array of heights of the reference shape model above the reference plane
    Plane fitPlaneReference = PolyDataUtil.fitPlaneToPolyData(polyDataTruth);

    XYGrid xyGridModel = new XYGrid(fitPlaneModel, resolution, radius, polyDataModel);
    XYGrid xyGridReference = new XYGrid(fitPlaneReference, resolution, 2 * radius, polyDataTruth);

    int gridShift = (xyGridReference.getNx() - xyGridModel.getNx()) / 2;

    Vector<Vector3> adjustedPoints = new Vector<>();

    for (Vector3 point : points) {
      // both have odd number of points in each dimension, reference grid is 2x-1 model grid,
      // centered on same point
      xyGridModel.buildHeightGrid(point);
      xyGridReference.buildHeightGrid(point);
      double[][] heightModel = xyGridModel.getHeightGrid();
      double[][] heightReference = xyGridReference.getHeightGrid();

      double minSum = Double.MAX_VALUE;
      int bestX = 0;
      int bestY = 0;
      for (int yOffset = -offset; yOffset <= offset; yOffset++) {
        for (int xOffset = -offset; xOffset <= offset; xOffset++) {
          double sum = 0;
          for (int j = 0; j < xyGridModel.getNy(); j++) {
            int iyModel = j;
            int iyReference = iyModel + yOffset + gridShift;
            for (int i = 0; i < xyGridModel.getNy(); i++) {
              int ixModel = i;
              int ixReference = ixModel + xOffset + gridShift;
              double modelHeight = heightModel[iyModel][ixModel];
              double refHeight = heightReference[iyReference][ixReference];
              if (!Double.isNaN(modelHeight) && !Double.isNaN(refHeight)) {
                sum += Math.abs(modelHeight - refHeight);
                // if (Math.abs(xOffset) < 2 && Math.abs(yOffset) < 2)
                // {
                // System.out.printf("%d %d %d %d %d %d %f %f\n",
                // xOffset, yOffset,
                // ixModel, iyModel,
                // ixReference, iyReference,
                // modelHeight, refHeight);
                // }
              }
            }
          }
          if (sum < minSum) {
            minSum = sum;
            bestX = xOffset;
            bestY = yOffset;
          }
          // System.out.printf("%d %d %6.2f\n", xOffset, yOffset, sum);
        }
      } // for (int yOffset = -offset; yOffset <= offset; yOffset++)

      // System.out.printf("%d %d %f\n", bestX, bestY, minSum);

      Vector3 adjustedPoint = xyGridReference.shift(fitPlaneReference.project(point), bestX, bestY);
      adjustedPoints.add(adjustedPoint);
    }

    // find the distance between each pair of points in the plane of the truth model

    List<Double> distancesTruth = findDistances(adjustedPoints, fitPlaneModel);

    double sumDiff = 0;
    for (int i = 0; i < distancesModel.size(); i++) {
      sumDiff += (Math.abs(distancesModel.get(i) - distancesTruth.get(i))) / distancesTruth.get(i);
    }
    sumDiff *= radius;

    System.out.printf(
        "Sum normalized difference in horizontal distances (%d points used): %f meters\n",
        npts, sumDiff);
  }

  /**
   * Write out difference files.
   *
   * @param closestDiffFile argument to -savePlateDiff
   * @param closestIndexFile argument to -savePlateIndex
   * @param verticalDiffFile argument to -computeVerticalError
   * @param limitClosestPoints argument to -limitClosestPoints
   * @param radius argument to -fitPlaneRadius
   */
  private void computeDifferences(
      String closestDiffFile,
      String closestIndexFile,
      String verticalDiffFile,
      double limitClosestPoints,
      double radius)
      throws IOException {
    final boolean saveDiff = (closestDiffFile != null);
    // only valid for shape models with facet information
    final boolean saveIndex = !inputFormat.pointsOnly && (closestIndexFile != null);
    final boolean computeVerticalError = (verticalDiffFile != null);
    final boolean fitLocalPlane = (radius > 0);

    BufferedWriter outClosest = null;
    if (saveDiff) {
      FileWriter fstream = new FileWriter(closestDiffFile);
      outClosest = new BufferedWriter(fstream);
    }
    BufferedWriter outClosestIndices = null;
    if (saveIndex) {
      FileWriter fstream = new FileWriter(closestIndexFile);
      outClosestIndices = new BufferedWriter(fstream);
      outClosestIndices.write("# plate index, closest reference plate index, distance\n");
    }
    BufferedWriter outVertical = null;
    if (computeVerticalError) {
      FileWriter fstream = new FileWriter(verticalDiffFile);
      outVertical = new BufferedWriter(fstream);
    }

    // fit a plane to the entire shape model - no good for global shape models
    Pair<Rotation, Vector3D> pair = PolyDataUtil.findLocalFrame(polyDataTruth);
    Vector3D normal = pair.getKey().applyInverseTo(Vector3D.PLUS_K);

    SmallBodyModel smallBodyModel = null;
    SmallBodyModel smallBodyTruth = new SmallBodyModel(polyDataTruth);

    long numPoints;
    vtkIdList idList = new vtkIdList();
    vtkOctreePointLocator pointLocator = new vtkOctreePointLocator();
    if (inputFormat.pointsOnly) {
      if (fitLocalPlane) {
        pointLocator.FreeSearchStructure();
        vtkPolyData pointSet = new vtkPolyData();
        pointSet.SetPoints(polyDataModel.GetPoints());
        pointLocator.SetDataSet(pointSet);
        pointLocator.BuildLocator();
      }
      numPoints = polyDataModel.GetNumberOfPoints();
    } else {
      smallBodyModel = new SmallBodyModel(polyDataModel);
      numPoints = polyDataModel.GetNumberOfCells();
    }

    List<DistanceContainer> distanceContainerVector = new ArrayList<>();
    int numPlatesActuallyUsed = 0;

    // loop through each cell in the model and find the closest point in the reference model
    for (int i = 0; i < numPoints; ++i) {

      Vector3D p;
      if (inputFormat.pointsOnly) {

        p = new Vector3D(polyDataModel.GetPoint(i));

        if (fitLocalPlane) {

          // fit a plane to all point cloud points within radius of p
          pointLocator.FindPointsWithinRadius(radius, p.toArray(), idList);

          if (idList.GetNumberOfIds() < 3) {
            logger.error(
                String.format(
                    "point %d (%f %f %f): %d points within %f, using radial vector to find intersection",
                    i, p.getX(), p.getY(), p.getZ(), idList.GetNumberOfIds(), radius));
            normal = p.normalize();
          } else {
            vtkPoints tmpPoints = new vtkPoints();
            for (int j = 0; j < idList.GetNumberOfIds(); j++) {
              tmpPoints.InsertNextPoint(polyDataModel.GetPoint(idList.GetId(j)));
            }

            PointCloudToPlane pctp = new PointCloudToPlane(tmpPoints);
            normal = new Vector3D(pctp.getGMU().getPlaneNormal());
          }
        }

      } else {

        TriangularFacet facet = PolyDataUtil.getFacet(polyDataModel, i);
        p = MathConversions.toVector3D(facet.getCenter());
        if (fitLocalPlane) {
          normal = new Vector3D(smallBodyModel.getNormalAtPoint(p.toArray(), radius));
        } else {
          normal = MathConversions.toVector3D(facet.getNormal());
        }
      }

      Optional<Vector3D> normalPoint =
          findIntersectPointInNormalDirection(p, normal, smallBodyTruth);
      DistanceContainer dc = null;

      // Skip this plate in the error calculation if there is no intersection
      if (normalPoint.isPresent()) {
        Vector3D closestPoint = new Vector3D(smallBodyTruth.findClosestPoint(p.toArray()));
        double closestDistance = p.distance(closestPoint);
        dc = new DistanceContainer(closestDistance);
        distanceContainerVector.add(dc);

        if (saveDiff || saveIndex) {
          // Determining if p from model 1 is inside or outside of ref surface
          // model 2. Negative distance means p is inside the ref model which
          // corresponds to a positive dot product
          Vector3D pdiff = closestPoint.subtract(p);
          if (pdiff.dotProduct(p) > 0) closestDistance *= -1.0;

          if (saveDiff) outClosest.write(closestDistance + "\n");

          if (saveIndex) {
            long closestFacet = smallBodyTruth.findClosestCell(p.toArray());
            outClosestIndices.write(
                String.format("%d, %d, %f\n", i, closestFacet, closestDistance));
          }
        }

      } else {
        if (saveDiff) outClosest.write("no-intersection\n");
      }

      if (computeVerticalError) {
        if (normalPoint.isPresent()) {
          double normalDistance = p.distance(normalPoint.get());
          dc.setNormalDistance(normalDistance);

          Vector3D pdiff = normalPoint.get().subtract(p);
          if (pdiff.dotProduct(p) > 0) normalDistance *= -1.0;

          outVertical.write(normalDistance + "\n");
        } else {
          outVertical.write("no-intersection\n");
        }
      }

      if (normalPoint.isPresent()) ++numPlatesActuallyUsed;
    } // for (int i = 0; i < numPlates; ++i)

    if (computeVerticalError) outVertical.close();

    if (saveDiff) outClosest.close();

    if (saveIndex) outClosestIndices.close();

    numPoints = (int) (limitClosestPoints * distanceContainerVector.size() + 0.5);
    double closestDistanceError = 0.0;
    double closestDistance2Error = 0.0;
    double normalDistanceError = 0.0;
    double normalDistance2Error = 0.0;
    double minDist = 0.0;
    double maxDist = 0.0;
    if (!distanceContainerVector.isEmpty()) {
      Collections.sort(distanceContainerVector);
      closestDistanceError = 0;
      closestDistance2Error = 0;
      for (int i = 0; i < numPoints; i++) {
        double distance = distanceContainerVector.get(i).getClosestDistance();
        closestDistanceError += distance;
        closestDistance2Error += distance * distance;
      }
      closestDistanceError /= numPoints;
      closestDistance2Error /= numPoints;
      if (computeVerticalError) {
        for (int i = 0; i < numPoints; i++) {
          double distance = distanceContainerVector.get(i).getNormalDistance();
          normalDistanceError += distance;
          normalDistance2Error += distance * distance;
        }
        normalDistanceError /= numPoints;
        normalDistance2Error /= numPoints;
      }

      minDist = distanceContainerVector.get(0).getClosestDistance();
      maxDist =
          distanceContainerVector.get(distanceContainerVector.size() - 1).getClosestDistance();
    }

    Vector3D translation = transform.getTranslation();
    String tmpString =
        String.format(
            "%16.8e,%16.8e,%16.8e", translation.getX(), translation.getY(), translation.getZ());
    System.out.println("Translation: " + tmpString.replaceAll("\\s+", ""));

    Rotation rotation = transform.getRotation();
    Rotation inverse =
        rotation.composeInverse(Rotation.IDENTITY, RotationConvention.FRAME_TRANSFORM);
    Quaternion q =
        new Quaternion(inverse.getQ0(), inverse.getQ1(), inverse.getQ2(), inverse.getQ3())
            .getPositivePolarForm();

    tmpString =
        String.format("%16.8e,%16.8e,%16.8e,%16.8e", q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3());
    System.out.println("Rotation quaternion: " + tmpString.replaceAll("\\s+", ""));

    Vector3D axis = inverse.getAxis(RotationConvention.FRAME_TRANSFORM);
    tmpString =
        String.format(
            "%16.8e,%16.8e,%16.8e,%16.8e",
            Math.toDegrees(inverse.getAngle()), axis.getX(), axis.getY(), axis.getZ());
    System.out.println("Rotation angle (degrees) and axis: " + tmpString.replaceAll("\\s+", ""));

    Vector3D centerOfRotation = transform.getCenterOfRotation();
    tmpString =
        String.format(
            "%16.8e,%16.8e,%16.8e",
            centerOfRotation.getX(), centerOfRotation.getY(), centerOfRotation.getZ());
    System.out.println("Center of rotation: " + tmpString.replaceAll("\\s+", ""));

    double[][] rotMatrix = inverse.getMatrix();
    double[] translationArray = translation.toArray();
    System.out.println("4x4 Transformation matrix:");
    for (int j = 0; j < 3; j++) {
      for (int i = 0; i < 3; i++) {
        // SPICE defines its matrices with the row as the first index and Apache Commons Rotation
        // uses the column as the first index. We want to write out each row of the matrix.
        System.out.printf("%16.8e ", rotMatrix[i][j]);
      }
      System.out.printf("%16.8e\n", translationArray[j]);
    }
    System.out.printf("%16.8e %16.8e %16.8e %16.8e\n", 0., 0., 0., 1.);

    System.out.printf(
        "Using %d of %d points (excluding %.1f%% largest distances)\n",
        numPoints,
        distanceContainerVector.size(),
        100 * (1 - ((double) numPoints) / distanceContainerVector.size()));
    System.out.println("Min Distance:              " + minDist);
    System.out.println("Max Distance:              " + maxDist);
    System.out.println("Mean Distance:             " + closestDistanceError);
    System.out.println("Mean Square Distance:      " + closestDistance2Error);
    System.out.println("Root Mean Square Distance: " + Math.sqrt(closestDistance2Error));

    if (computeVerticalError) {
      System.out.println("Mean Vertical Distance:             " + normalDistanceError);
      System.out.println("Mean Square Vertical Distance:      " + normalDistance2Error);
      System.out.println("Root Mean Square Vertical Distance: " + Math.sqrt(normalDistance2Error));

      if (!fitLocalPlane) {

        // plane containing the origin
        Vector3D unitNormal = normal.normalize();

        org.apache.commons.math3.geometry.euclidean.threed.Plane p =
            new org.apache.commons.math3.geometry.euclidean.threed.Plane(
                Vector3D.ZERO, unitNormal, 1e-6);
        Vector3D parallelVector = p.toSpace(p.toSubSpace(translation));

        System.out.println(
            "Direction perpendicular to plane: "
                + unitNormal.getX()
                + " "
                + unitNormal.getY()
                + " "
                + unitNormal.getZ());
        System.out.println(
            "Magnitude of projection perpendicular to plane: "
                + translation.dotProduct(unitNormal));
        System.out.println(
            "Projection vector of translation parallel to plane: "
                + parallelVector.getX()
                + " "
                + parallelVector.getY()
                + " "
                + parallelVector.getZ());
        System.out.println(
            "Magnitude of projection vector of translation parallel to plane: "
                + parallelVector.getNorm());

        /*- SPICE

        // plane containing the origin
        Vector3 normalVector = VectorUtils.toVector3(normal).hat();
        Vector3 translationVector = VectorUtils.toVector3(translation);
        try {
          Plane p = new Plane(VectorUtils.toVector3(normal), new Vector3());
          Vector3 parallelVector = p.project(translationVector);
          normalVector = normalVector.scale(translationVector.dot(normalVector));

          System.out.println("Direction perpendicular to plane: " + normalVector.getElt(0) + " "
              + normalVector.getElt(1) + " " + normalVector.getElt(2));
          System.out
              .println("Magnitude of projection perpendicular to plane: " + normalVector.norm());
          System.out.println(
              "Projection vector of translation parallel to plane: " + parallelVector.getElt(0)
                  + " " + parallelVector.getElt(1) + " " + parallelVector.getElt(2));
          System.out.println("Magnitude of projection vector of translation parallel to plane: "
              + parallelVector.norm());
        } catch (SpiceException e) {
          SimpleLogger.getInstance().warn(e.getLocalizedMessage());
        }

        */

      }

      System.out.println(
          numPlatesActuallyUsed
              + " plates used in error calculation out of "
              + polyDataModel.GetNumberOfCells()
              + " total in the shape model");
    }
  }

  /**
   * Shoot 2 rays: one from "on top" and a second from "below". Ideally, both should be identical.
   * But if not, then return the closest one.
   *
   * @param in
   * @param normal
   * @param smallBodyModel
   * @return
   */
  private static Optional<Vector3D> findIntersectPointInNormalDirection(
      Vector3D in, Vector3D normal, SmallBodyModel smallBodyModel) {

    double size = Math.max(smallBodyModel.getBoundingBoxDiagonalLength(), in.getNorm());

    // First do the intersection from "below"
    Vector3D startBottom = in.subtract(normal.scalarMultiply(size));
    double[] out1 = new double[3];
    long cellId1 =
        smallBodyModel.computeRayIntersection(startBottom.toArray(), normal.toArray(), out1);

    // Then do the intersection from on "top"
    Vector3D startTop = in.add(normal.scalarMultiply(size));
    double[] out2 = new double[3];
    long cellId2 =
        smallBodyModel.computeRayIntersection(startTop.toArray(), normal.negate().toArray(), out2);

    if (cellId1 >= 0 && cellId2 >= 0) {
      Vector3D out1V = new Vector3D(out1);
      Vector3D out2V = new Vector3D(out2);

      Vector3D inSubOut1 = in.subtract(out1V);
      Vector3D inSubOut2 = in.subtract(out2V);
      // If both intersected, take the closest
      if (inSubOut1.dotProduct(inSubOut1) < inSubOut2.dotProduct(inSubOut2))
        return Optional.of(out1V);
      else return Optional.of(out2V);
    }
    if (cellId1 >= 0) return Optional.of(new Vector3D(out1));

    if (cellId2 >= 0) return Optional.of(new Vector3D(out2));

    return Optional.empty();
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("logFile")
            .hasArg()
            .argName("path")
            .desc("If present, save screen output to <path>.")
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

    sb = new StringBuilder();
    sb.append("If specified, the program first computes the optimal rotation of ");
    sb.append("-model so that it best matches -reference prior ");
    sb.append("to computing the error between them.  Prior to computing the errors, ");
    sb.append("-model is transformed to -reference using this optimal ");
    sb.append("rotation. This results in an error unbiased by a possible rotation between ");
    sb.append("the two models.");
    options.addOption(Option.builder("computeOptimalRotation").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("If specified, the program first computes the optimal translation of ");
    sb.append("-model so that it best matches -reference prior to ");
    sb.append("computing the error between them. Prior to computing the errors, -model ");
    sb.append("is transformed to the -reference using this optimal translation.  ");
    sb.append("This results in an error unbiased by a possible translation offset between ");
    sb.append("the two models.");
    options.addOption(Option.builder("computeOptimalTranslation").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("If specified, the program first computes the optimal translation and ");
    sb.append("rotation of -model so that it best matches -reference  ");
    sb.append("prior to computing the error between them. Prior to computing the errors, ");
    sb.append("-model is transformed to -reference using this optimal ");
    sb.append("translation and rotation. This results in an error unbiased by a possible ");
    sb.append("translation offset or rotation between the two models.");
    options.addOption(
        Option.builder("computeOptimalRotationAndTranslation").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("If specified, the program computes the error in the vertical direction ");
    sb.append("(by fitting a plane to -model) and saves it to <path>. This option ");
    sb.append("only produces meaningful results for digital terrain models to which a plane ");
    sb.append("can be fit.");
    options.addOption(
        Option.builder("computeVerticalError")
            .hasArg()
            .argName("path")
            .desc(sb.toString())
            .build());

    sb = new StringBuilder();
    sb.append("Use the normal to a plane fit to the model when computing distances to the ");
    sb.append("reference.  If this option is absent, a plane is fit to the entire model.  If ");
    sb.append("present, only points within a distance of radius will be used to construct the ");
    sb.append("plane.  Recommended value is ~5% of the body radius for a global model.  Units ");
    sb.append("are units of the shape model.");
    options.addOption(Option.builder("fitPlaneRadius").hasArg().desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Limit the distances (described in -savePlateDiff) used in calculating the mean ");
    sb.append("distance and RMS distance to the closest fraction of all distances.");
    options.addOption(Option.builder("limitClosestPoints").hasArg().desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Max number of control points to use in optimization. Default is 2000.");
    options.addOption(
        Option.builder("maxNumberControlPoints").hasArg().desc(sb.toString()).build());

    options.addOption(
        Option.builder("model")
            .required()
            .hasArg()
            .argName("path")
            .desc(
                "Required.  Point cloud/shape file to compare to reference shape.  Valid formats are "
                    + "anything that can be read by the PointCloudFormatConverter.")
            .build());
    options.addOption(
        Option.builder("reference")
            .required()
            .hasArg()
            .argName("path")
            .desc(
                "Required.  Reference shape file.  Valid formats are FITS, ICQ, OBJ, PLT, PLY, or VTK.")
            .build());

    sb = new StringBuilder();
    sb.append("Save the rotated and/or translated -model to <path>.  ");
    sb.append("This option requires one of -computeOptimalRotation, -computeOptimalTranslation ");
    sb.append("or -computeOptimalRotationAndTranslation to be specified.");
    options.addOption(
        Option.builder("saveOptimalShape").hasArg().argName("path").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Save to a file specified by <path> the distances (in same units as the shape ");
    sb.append("models) between each plate center of -model to the closest point in ");
    sb.append("-reference. The number of lines in the file equals the number of plates in ");
    sb.append("the first shape model with each line containing the distance of that plate ");
    sb.append("to the closest point in the second shape model.");
    options.addOption(
        Option.builder("savePlateDiff").hasArg().argName("path").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Save to a file specified by <path> the index of the closest plate in ");
    sb.append("second (reference) shape model to the center of each plate in the first model. ");
    sb.append("The format of each line is ");
    sb.append("\n\tplate index, closest reference plate index, distance\n");
    sb.append("Only valid when -model is a shape model with facet information.");
    options.addOption(
        Option.builder("savePlateIndex").hasArg().argName("path").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Save output of lidar-optimize to <path> (JSON format file)");
    options.addOption(
        Option.builder("saveTransformationFile")
            .hasArg()
            .argName("path")
            .desc(sb.toString())
            .build());

    sb = new StringBuilder();
    sb.append("Directory to store temporary files.  It will be created if it does not exist.  ");
    sb.append("Default is the current working directory.");
    options.addOption(
        Option.builder("tmpDir").hasArg().argName("path").desc(sb.toString()).build());

    sb = new StringBuilder();
    sb.append("Use all points in -model when attempting to fit to ");
    sb.append("-reference.  The default behavior is to use only points which overlap the ");
    sb.append("reference model.");
    options.addOption(Option.builder("useAllPoints").desc(sb.toString()).build());
    return options;
  }

  public static void main(String[] args) {

    TerrasaurTool defaultOBJ = new CompareOBJ();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    double limitClosestPoints =
        cl.hasOption("limitClosestPoints")
            ? Double.parseDouble(cl.getOptionValue("limitClosestPoints"))
            : 1.0;
    limitClosestPoints = Math.max(0, Math.min(1, limitClosestPoints));

    boolean saveOptimalShape = cl.hasOption("saveOptimalShape");
    boolean computeOptimalTranslation =
        cl.hasOption("computeOptimalRotationAndTranslation")
            || cl.hasOption("computeOptimalTranslation");
    boolean computeOptimalRotation =
        cl.hasOption("computeOptimalRotationAndTranslation")
            || cl.hasOption("computeOptimalRotation");
    final boolean computeHorizontalError = false;
    boolean useOverlappingPoints = !cl.hasOption("useAllPoints");
    double planeRadius =
        cl.hasOption("fitPlaneRadius")
            ? Double.parseDouble(cl.getOptionValue("fitPlaneRadius"))
            : 0;
    int maxNumberOfControlPoints =
        cl.hasOption("maxNumberControlPoints")
            ? Integer.parseInt(cl.getOptionValue("maxNumberControlPoints"))
            : 2000;
    int npoints = -1;
    double size = 0;
    String closestDiffFile =
        cl.hasOption("savePlateDiff") ? cl.getOptionValue("savePlateDiff") : null;
    String closestIndexFile =
        cl.hasOption("savePlateIndex") ? cl.getOptionValue("savePlateIndex") : null;
    String optimalShapeFile = saveOptimalShape ? cl.getOptionValue("saveOptimalShape") : null;
    String verticalDiffFile =
        cl.hasOption("computeVerticalError") ? cl.getOptionValue("computeVerticalError") : null;
    String transformationFile =
        cl.hasOption("saveTransformationFile") ? cl.getOptionValue("saveTransformationFile") : null;
    String tmpdir = cl.hasOption("tmpDir") ? cl.getOptionValue("tmpDir") : ".";

    String infile1 = cl.getOptionValue("model");
    String infile2 = cl.getOptionValue("reference");

    NativeLibraryLoader.loadSpiceLibraries();
    NativeLibraryLoader.loadVtkLibraries();

    CompareOBJ compareOBJ = new CompareOBJ(infile1, infile2);
    compareOBJ.setTmpdir(tmpdir);

    if (computeOptimalTranslation || computeOptimalRotation) {
      compareOBJ.computeOptimalTransformationToTarget(
          computeOptimalTranslation,
          computeOptimalRotation,
          maxNumberOfControlPoints,
          useOverlappingPoints,
          transformationFile);

      compareOBJ.transformPolyData(saveOptimalShape, optimalShapeFile);
    }

    // note: this will never be called
    if (computeHorizontalError) {
      try {
        compareOBJ.assessHorizontalAccuracy(npoints, size);
      } catch (SpiceException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }

    try {
      compareOBJ.computeDifferences(
          closestDiffFile, closestIndexFile, verticalDiffFile, limitClosestPoints, planeRadius);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }
}
