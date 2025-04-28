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
package terrasaur.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.vectorspace.VectorIJK;
import terrasaur.apps.PointCloudToPlane;
import terrasaur.apps.ValidateOBJ;
import terrasaur.smallBodyModel.BoundingBox;
import terrasaur.utils.mesh.TriangularFacet;
import spice.basic.LatitudinalCoordinates;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import vtk.vtkIdList;
import vtk.vtkPolyData;

public class PolyDataStatistics {

  private final static Logger logger = LogManager.getLogger(PolyDataStatistics.class);

  private long numberPlates;
  private long numberVertices;
  private int numberEdges;
  private int enumberDuplicateVertices;
  private double surfaceArea;
  private double meanCellArea;
  private double minCellArea;
  private double maxCellArea;
  private double stdCellArea;
  private double varCellArea;
  private double meanEdgeLength;
  private double minEdgeLength;
  private double maxEdgeLength;
  private double stdEdgeLength;
  private double varEdgeLength;
  private boolean isClosed;
  private BoundingBox boundingBox = new BoundingBox();
  private long eulerPolyhedronFormula;
  private int unusedVertexCount;
  private StringBuilder vertexErrorMessage;
  private double[][] inertiaCOM = new double[3][3];
  private double[][] inertiaWorld = new double[3][3];
  private double[] centroid = new double[3];
  private double volume;

  private ArrayList<double[]> principalAxes;

  private vtkPolyData polydata;

  public PolyDataStatistics(vtkPolyData polydata) {
    this.polydata = polydata;
    getPolyDataStatistics();
  }

  private void getPolyDataStatistics() {

    // First determine if the shape model is closed
    vtkPolyData boundary = PolyDataUtil.getBoundary(polydata);
    isClosed = (boundary.GetNumberOfCells() == 0);

    polydata.BuildCells();
    vtkIdList idList = new vtkIdList();

    long numberOfCells = polydata.GetNumberOfCells();

    double[] pt0 = new double[3];
    double[] pt1 = new double[3];
    double[] pt2 = new double[3];
    Set<Pair<Long, Long>> edges = new LinkedHashSet<>();

    DescriptiveStatistics areaStatistics = new DescriptiveStatistics();
    for (int i = 0; i < numberOfCells; ++i) {
      polydata.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      polydata.GetPoint(id0, pt0);
      polydata.GetPoint(id1, pt1);
      polydata.GetPoint(id2, pt2);

      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
      double area = facet.getArea();

      areaStatistics.addValue(area);

      edges.add(id0 < id1 ? new Pair<>(id0, id1) : new Pair<>(id1, id0));
      edges.add(id1 < id2 ? new Pair<>(id1, id2) : new Pair<>(id2, id1));
      edges.add(id2 < id0 ? new Pair<>(id2, id0) : new Pair<>(id0, id1));
    }

    DescriptiveStatistics edgeStatistics = new DescriptiveStatistics();
    for (Pair<Long, Long> edge : edges) {
      polydata.GetPoint(edge.getKey(), pt0);
      polydata.GetPoint(edge.getValue(), pt1);
      double length = new Vector3D(pt0).distance(new Vector3D(pt1));
      edgeStatistics.addValue(length);
    }

    if (isClosed()) {
      volume = getMassProperties();

      // For debugging, print out this information to make sure it agrees
      // with our implementation
      /*-
      vtkMassProperties massProp = new vtkMassProperties();
      massProp.SetInputData(polydata);
      massProp.Update();
      
      System.out.println("Surface Area = " +
      		massProp.GetSurfaceArea());
      System.out.println("Volume = " + massProp.GetVolume());
      System.out.println("Mean Plate Area = " +
      		massProp.GetSurfaceArea() / polydata.GetNumberOfCells());
      System.out.println("Min Plate Area = " +
      		massProp.GetMinCellArea());
      System.out.println("Max Plate Area = " +
      		massProp.GetMaxCellArea());
      		*/
    }

    eulerPolyhedronFormula = polydata.GetNumberOfPoints() - edges.size() + numberOfCells;
    if (isClosed() && eulerPolyhedronFormula != 2) {
      vertexErrorMessage = new StringBuilder();
      vertexErrorMessage
          .append("Warning: The polyhedron is closed, but the Euler polyhedron formula, V-E+F, "
              + "(see https://en.wikipedia.org/wiki/Euler_characteristic) does not equal 2, "
              + "as would be expected. This is usually caused by duplicate vertices in the "
              + "shape model. Please contact the creator of the shape model to see if this "
              + "can be corrected.");
    }

    polydata.ComputeBounds();
    boundingBox = new BoundingBox(polydata.GetBounds());

    numberPlates = numberOfCells;
    numberVertices = polydata.GetNumberOfPoints();
    numberEdges = edges.size();
    surfaceArea = areaStatistics.getSum();
    meanCellArea = areaStatistics.getMean();
    minCellArea = areaStatistics.getMin();
    maxCellArea = areaStatistics.getMax();
    stdCellArea = areaStatistics.getStandardDeviation();
    varCellArea = areaStatistics.getVariance();
    meanEdgeLength = edgeStatistics.getMean();
    minEdgeLength = edgeStatistics.getMin();
    maxEdgeLength = edgeStatistics.getMax();
    stdEdgeLength = edgeStatistics.getStandardDeviation();
    varEdgeLength = edgeStatistics.getVariance();

  }

  private List<String> evaluateOpenModel() throws SpiceException {
    NativeLibraryLoader.loadSpiceLibraries();

    PointCloudToPlane pctp = new PointCloudToPlane(polydata.GetPoints());
    List<Vector3> globalPoints = new ArrayList<>();
    VectorStatistics vStats = new VectorStatistics();
    double[] pt = new double[3];
    for (int i = 0; i < polydata.GetNumberOfPoints(); i++) {
      polydata.GetPoint(i, pt);
      Vector3 v = new Vector3(pt);
      globalPoints.add(v);
      vStats.add(v);
    }

    List<Vector3> localPoints = pctp.getGMU().globalToLocal(globalPoints);
    NavigableMap<Integer, Vector3> cornerMap = new TreeMap<>();
    for (int i = 0; i < 4; i++)
      cornerMap.put(i, new Vector3());
    for (Vector3 localPoint : localPoints) {
      double x = localPoint.getElt(0);
      double y = localPoint.getElt(1);
      int quadrant;
      if (x < 0)
        quadrant = (y > 0 ? 1 : 2);
      else
        quadrant = (y > 0 ? 0 : 3);

      Vector3 v = cornerMap.get(quadrant);
      if (localPoint.norm() > v.norm())
        cornerMap.put(quadrant, localPoint);
    }

    List<Vector3> localCorners = new ArrayList<>(cornerMap.values());
    List<Vector3> globalCorners = pctp.getGMU().localToGlobal(localCorners);

    List<String> list = new ArrayList<>();

    Vector3D center = vStats.getMean();
    double lon = Math.toDegrees(center.getAlpha());
    if (lon < 0)
      lon += 360;
    list.add(String.format("%-26s = %f", "Center Longitude (deg)", lon));
    list.add(
        String.format("%-26s = %f", "Center Latitude (deg)", Math.toDegrees(center.getDelta())));
    for (int i = 0; i < globalCorners.size(); i++) {
      LatitudinalCoordinates lc = new LatitudinalCoordinates(globalCorners.get(i));
      lon = Math.toDegrees(lc.getLongitude());
      if (lon < 0)
        lon += 360;
      list.add(String.format("%-26s = %f", "Corner " + i + " Longitude (deg)", lon));
      list.add(String.format("%-26s = %f", "Corner " + i + " Latitude (deg)",
          Math.toDegrees(lc.getLatitude())));
    }

    return list;
  }

  /**
   * The following function was adapted from from the file Wm5PolyhedralMassProperties.cpp in the
   * Geometric Tools source code (http://www.geometrictools.com)
   * 
   * @return mass
   */
  private double getMassProperties() {
    long numberOfCells = polydata.GetNumberOfCells();
    vtkIdList idList = new vtkIdList();

    double[] v0 = new double[3];
    double[] v1 = new double[3];
    double[] v2 = new double[3];

    final double oneDiv6 = 1.0 / 6.0;
    final double oneDiv24 = 1.0 / 24.0;
    final double oneDiv60 = 1.0 / 60.0;
    final double oneDiv120 = 1.0 / 120.0;

    // order: 1, x, y, z, x^2, y^2, z^2, xy, yz, zx
    double[] integral = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    for (int i = 0; i < numberOfCells; ++i) {
      polydata.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      polydata.GetPoint(id0, v0);
      polydata.GetPoint(id1, v1);
      polydata.GetPoint(id2, v2);

      // Get cross product of edges and normal vector.
      Vector3D V1mV0 = new Vector3D(v1).subtract(new Vector3D(v0));
      Vector3D V2mV0 = new Vector3D(v2).subtract(new Vector3D(v0));
      double[] N = V1mV0.crossProduct(V2mV0).toArray();
      // Vector3<Real> V1mV0 = v1 - v0;
      // Vector3<Real> V2mV0 = v2 - v0;
      // Vector3<Real> N = V1mV0.Cross(V2mV0);

      // Compute integral terms.
      double tmp0, tmp1, tmp2;
      double f1x, f2x, f3x, g0x, g1x, g2x;
      tmp0 = v0[0] + v1[0];
      f1x = tmp0 + v2[0];
      tmp1 = v0[0] * v0[0];
      tmp2 = tmp1 + v1[0] * tmp0;
      f2x = tmp2 + v2[0] * f1x;
      f3x = v0[0] * tmp1 + v1[0] * tmp2 + v2[0] * f2x;
      g0x = f2x + v0[0] * (f1x + v0[0]);
      g1x = f2x + v1[0] * (f1x + v1[0]);
      g2x = f2x + v2[0] * (f1x + v2[0]);

      double f1y, f2y, f3y, g0y, g1y, g2y;
      tmp0 = v0[1] + v1[1];
      f1y = tmp0 + v2[1];
      tmp1 = v0[1] * v0[1];
      tmp2 = tmp1 + v1[1] * tmp0;
      f2y = tmp2 + v2[1] * f1y;
      f3y = v0[1] * tmp1 + v1[1] * tmp2 + v2[1] * f2y;
      g0y = f2y + v0[1] * (f1y + v0[1]);
      g1y = f2y + v1[1] * (f1y + v1[1]);
      g2y = f2y + v2[1] * (f1y + v2[1]);

      double f1z, f2z, f3z, g0z, g1z, g2z;
      tmp0 = v0[2] + v1[2];
      f1z = tmp0 + v2[2];
      tmp1 = v0[2] * v0[2];
      tmp2 = tmp1 + v1[2] * tmp0;
      f2z = tmp2 + v2[2] * f1z;
      f3z = v0[2] * tmp1 + v1[2] * tmp2 + v2[2] * f2z;
      g0z = f2z + v0[2] * (f1z + v0[2]);
      g1z = f2z + v1[2] * (f1z + v1[2]);
      g2z = f2z + v2[2] * (f1z + v2[2]);

      // Update integrals.
      integral[0] += N[0] * f1x;
      integral[1] += N[0] * f2x;
      integral[2] += N[1] * f2y;
      integral[3] += N[2] * f2z;
      integral[4] += N[0] * f3x;
      integral[5] += N[1] * f3y;
      integral[6] += N[2] * f3z;
      integral[7] += N[0] * (v0[1] * g0x + v1[1] * g1x + v2[1] * g2x);
      integral[8] += N[1] * (v0[2] * g0y + v1[2] * g1y + v2[2] * g2y);
      integral[9] += N[2] * (v0[0] * g0z + v1[0] * g1z + v2[0] * g2z);
    }

    integral[0] *= oneDiv6;
    integral[1] *= oneDiv24;
    integral[2] *= oneDiv24;
    integral[3] *= oneDiv24;
    integral[4] *= oneDiv60;
    integral[5] *= oneDiv60;
    integral[6] *= oneDiv60;
    integral[7] *= oneDiv120;
    integral[8] *= oneDiv120;
    integral[9] *= oneDiv120;

    // mass
    double mass = integral[0];

    // center of mass
    centroid[0] = integral[1] / mass;
    centroid[1] = integral[2] / mass;
    centroid[2] = integral[3] / mass;

    // inertia relative to world origin
    inertiaWorld[0][0] = integral[5] + integral[6];
    inertiaWorld[0][1] = -integral[7];
    inertiaWorld[0][2] = -integral[9];
    inertiaWorld[1][0] = inertiaWorld[0][1];
    inertiaWorld[1][1] = integral[4] + integral[6];
    inertiaWorld[1][2] = -integral[8];
    inertiaWorld[2][0] = inertiaWorld[0][2];
    inertiaWorld[2][1] = inertiaWorld[1][2];
    inertiaWorld[2][2] = integral[4] + integral[5];

    // inertia relative to center of mass
    for (int i = 0; i < 3; ++i)
      for (int j = 0; j < 3; ++j)
        inertiaCOM[i][j] = inertiaWorld[i][j];
    inertiaCOM[0][0] -= mass * (centroid[1] * centroid[1] + centroid[2] * centroid[2]);
    inertiaCOM[0][1] += mass * centroid[0] * centroid[1];
    inertiaCOM[0][2] += mass * centroid[2] * centroid[0];
    inertiaCOM[1][0] = inertiaCOM[0][1];
    inertiaCOM[1][1] -= mass * (centroid[2] * centroid[2] + centroid[0] * centroid[0]);
    inertiaCOM[1][2] += mass * centroid[1] * centroid[2];
    inertiaCOM[2][0] = inertiaCOM[0][2];
    inertiaCOM[2][1] = inertiaCOM[1][2];
    inertiaCOM[2][2] -= mass * (centroid[0] * centroid[0] + centroid[1] * centroid[1]);

    RealMatrix inertiaTensor = new Array2DRowRealMatrix(inertiaWorld);
    EigenDecomposition ed = new EigenDecomposition(inertiaTensor);
    principalAxes = new ArrayList<>();
    for (int i = 0; i < 3; i++)
      principalAxes.add(ed.getEigenvector(i).toArray());

    return mass;
  }

  public Map<String, String> getShapeModelStatsMap() {
    Map<String, String> stats = new TreeMap<>();

    stats.put("Number of Plates", Long.toString(numberPlates));
    stats.put("Number of Vertices", Long.toString(numberVertices));
    stats.put("Number of Edges", Integer.toString(numberEdges));
    stats.put("Euler Polyhedron Formula", Long.toString(eulerPolyhedronFormula));
    stats.put("Surface Area", String.format("%-21.16g km^2", surfaceArea));
    stats.put("Plate Area Mean", String.format("%-21.16g km^2", meanCellArea));
    stats.put("Plate Area Min", String.format("%-21.16g km^2", minCellArea));
    stats.put("Plate Area Standard Dev", String.format("%-21.16g km^2", stdCellArea));
    stats.put("Edge Length Mean", String.format("%-21.16g km", meanEdgeLength));
    stats.put("Edge Length Max", String.format("%-21.16g km", maxEdgeLength));
    stats.put("Edge Length Variance", String.format("%-21.16g km", varEdgeLength));
    stats.put("Surface Closed?", String.format("%s", isClosed ? "Yes" : "No"));
    if (isClosed()) {
      stats.put("Volume", String.format("%-21.16g km^3", getVolume()));
      stats.put("Centroid", Arrays.toString(centroid) + " km");
      stats.put("Moment of Inertia Tensor Relative To Origin", Arrays.toString(inertiaWorld[0])
          + "  " + Arrays.toString(inertiaWorld[1]) + "  " + Arrays.toString(inertiaWorld[2]));
      stats.put("Moment of Inertia Tensor Relative To Centroid", Arrays.toString(inertiaCOM[0])
          + "  " + Arrays.toString(inertiaCOM[1]) + "  " + Arrays.toString(inertiaCOM[2]));
      for (int i = 0; i < principalAxes.size(); i++) {
        stats.put("Principal Axis " + i, Arrays.toString(principalAxes.get(i)));
      }
    }
    stats.put("Extent X", "[" + boundingBox.getXRange().getBegin() + ", "
        + boundingBox.getXRange().getEnd() + "] km");
    stats.put("Extent Y", "[" + boundingBox.getYRange().getBegin() + ", "
        + boundingBox.getYRange().getEnd() + "] km");
    stats.put("Extent Z", "[" + boundingBox.getZRange().getBegin() + ", "
        + boundingBox.getZRange().getEnd() + "] km");
    if (isClosed() && eulerPolyhedronFormula != 2) {
      logger.warn(vertexErrorMessage.toString());
    }
    return stats;
  }

  /**
   * return an array of strings to be used to add comments to an OBJ file as defined in the SIS.
   * 
   * @return
   * @throws Exception
   */
  public ArrayList<String> getShapeModelStats() throws Exception {
    ArrayList<String> stats = new ArrayList<String>();

    stats.add(String.format("%-26s = %d", "Number of Plates", numberPlates));
    stats.add(String.format("%-26s = %d", "Number of Vertices", numberVertices));
    stats.add(String.format("%-26s = %d", "Number of Edges", numberEdges));
    stats.add(String.format("%-26s = %d", "Euler Polyhedron Formula", eulerPolyhedronFormula));
    stats.add(String.format("%-26s = %-21.16g km^2", "Surface Area", surfaceArea));
    stats.add(String.format("%-26s = %-21.16g km^2", "Plate Area Mean", meanCellArea));
    stats.add(String.format("%-26s = %-21.16g km^2", "Plate Area Min", minCellArea));
    stats.add(String.format("%-26s = %-21.16g km^2", "Plate Area Standard Dev", stdCellArea));
    stats.add(String.format("%-26s = %-21.16g km", "Edge Length Mean", meanEdgeLength));
    stats.add(String.format("%-26s = %-21.16g km", "Edge Length Max", maxEdgeLength));
    stats.add(String.format("%-26s = %-21.16g km^2", "Edge Length Variance", varEdgeLength));
    stats.add(String.format("%-26s = %s", "Surface Closed? ", isClosed ? "Yes" : "No"));
    if (isClosed()) {
      stats.add(String.format("%-26s = %-21.16g km^3", "Volume", getVolume()));
      stats.add("Centroid [km]:");
      stats.add("  " + Arrays.toString(centroid));
      stats.add("Moment of Inertia Tensor Relative To Origin [kg km^2]:");
      stats.add("  " + Arrays.toString(inertiaWorld[0]));
      stats.add("  " + Arrays.toString(inertiaWorld[1]));
      stats.add("  " + Arrays.toString(inertiaWorld[2]));
      stats.add("Moment of Inertia Tensor Relative To Centroid [kg km^2]:");
      stats.add("  " + Arrays.toString(inertiaCOM[0]));
      stats.add("  " + Arrays.toString(inertiaCOM[1]));
      stats.add("  " + Arrays.toString(inertiaCOM[2]));
      for (int i = 0; i < principalAxes.size(); i++) {
        stats.add("Principal Axis " + i);
        stats.add("  " + Arrays.toString(principalAxes.get(i)));
      }
    } else {
      stats.addAll(evaluateOpenModel());
    }
    stats.add("Extent [km]:");
    stats.add("  X: [" + boundingBox.getXRange().getBegin() + ", "
        + boundingBox.getXRange().getEnd() + "]");
    stats.add("  Y: [" + boundingBox.getYRange().getBegin() + ", "
        + boundingBox.getYRange().getEnd() + "]");
    stats.add("  Z: [" + boundingBox.getZRange().getBegin() + ", "
        + boundingBox.getZRange().getEnd() + "]");
    if (isClosed() && eulerPolyhedronFormula != 2) {
      for (String s : WordUtils.wrap(vertexErrorMessage.toString(), 80, "\n", true).split("\n"))
        stats.add(s);
    }

    ValidateOBJ vo = new ValidateOBJ(polydata);
    if (isClosed()) {
      vo.testFacets();
      stats.add(vo.getMessage());
      vo.testVertices();
      stats.add(vo.getMessage());
    }
    vo.findDuplicateVertices();
    stats.add(vo.getMessage());
    vo.findUnreferencedVertices();
    stats.add(vo.getMessage());
    vo.findZeroAreaFacets();
    stats.add(vo.getMessage());

    return stats;

  }

  public double[] getCentroid() {
    return centroid;
  }

  public double getMeanEdgeLength() {
    return meanEdgeLength;
  }

  public ArrayList<double[]> getPrincipalAxes() {
    return principalAxes;
  }

  public double getVolume() {
    return volume;
  }

  public boolean isClosed() {
    return isClosed;
  }
}
