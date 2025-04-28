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

import java.util.HashMap;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import picante.math.vectorspace.VectorIJK;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkCellDataToPointData;
import vtk.vtkDataArray;
import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkPointDataToCellData;
import vtk.vtkPolyData;

/**
 * Container class that holds attributes associated with a single cell in a vtkPolyData object
 * associated with ALTWG processing. Also contains static methods for dealing with cell data. The
 * attributes are specific to ALTWG classes which precludes this class from being generic enough to
 * reside in saavtk.
 * 
 * @author espirrc1
 *
 */
@Value.Immutable
public abstract class CellInfo {

  private final static Logger logger = LogManager.getLogger(CellInfo.class);

  abstract Vector3D pt0();

  abstract Vector3D pt1();

  abstract Vector3D pt2();

  /**
   * returns the unitized cross product of (v3-v2)x(v1-v2). Vertices are in counterclockwise order.
   */
  public abstract Vector3D normal();

  public abstract Vector3D center();

  abstract double area();

  /** latitude in degrees */
  abstract double latitude();

  /** longitude in degrees */
  abstract double longitude();

  abstract double radius();

  /** angle between normal and radial, or its supplement if they are more than 90 degrees apart */
  abstract double tiltDeg();

  /**
   * Basic tilt direction definition: Project plate normal into the plate facet. Determine angle in
   * CW of projected facet assuming up or N is 0deg. Take longitude and generate a quaternion
   * representing rotation about the Z-axis. Rotate normal vector by quaternion such that new
   * coordinate system x' is now along R. Call this n'.
   * 
   * Tilt direction B = 90 - atan2(n'[3],n'[2]) if B &lt; 0 then B = 360 + B
   */
  abstract double tiltDirDeg();

  /**
   * Radius of a circle enclosing an equilateral triangle with an area equal to this facet's area.
   */
  abstract double circumscribingRadius();

  private final static HashMap<vtkPolyData, HashMap<Long, CellInfo>> completeMap =
      new HashMap<>();

  public static synchronized void removeKey(vtkPolyData polydata) {
    completeMap.remove(polydata);
  }

  /**
   * Find the cell with index cellId.
   * 
   * @param polydata
   * @param cellId
   * @param idList will contain vertex indices on return
   * @param pt0 will contain first vertex on return
   * @param pt1 will contain second vertex on return
   * @param pt2 will contain third vertex on return
   */
  public static void getCellPoints(vtkPolyData polydata, long cellId, vtkIdList idList, double[] pt0,
      double[] pt1, double[] pt2) {
    polydata.GetCellPoints(cellId, idList);

    long numberOfCells = idList.GetNumberOfIds();
    if (numberOfCells != 3) {
      logger.warn("CellInfo.getCellPoints(): cell {} must have 3 vertices but {} in list", cellId,
          idList.GetNumberOfIds());
      for (int i = 0; i < idList.GetNumberOfIds(); i++)
        logger.warn("cellIdList[{}] = {}", i, idList.GetId(i));
      return;
    }

    polydata.GetPoint(idList.GetId(0), pt0);
    polydata.GetPoint(idList.GetId(1), pt1);
    polydata.GetPoint(idList.GetId(2), pt2);
  }

  private static CellInfo fromPoints(double[] pt0, double[] pt1, double[] pt2) {

    ImmutableCellInfo.Builder builder = ImmutableCellInfo.builder();
    builder.pt0(new Vector3D(pt0));
    builder.pt1(new Vector3D(pt1));
    builder.pt2(new Vector3D(pt2));

    TriangularFacet tf =
        new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
    Vector3D normal = MathConversions.toVector3D(tf.getNormal());
    builder.normal(normal);

    Vector3D center = MathConversions.toVector3D(tf.getCenter());
    builder.center(center);

    double area = tf.getArea();
    builder.area(area);
    builder.circumscribingRadius(Math.sqrt(4 / (3 * Math.sqrt(3)) * area));

    double latitude = Math.toDegrees(center.getDelta());
    double longitude = Math.toDegrees(center.getAlpha());

    if (longitude < 0)
      longitude += 360;

    builder.latitude(latitude);
    builder.longitude(longitude);
    builder.radius(center.getNorm());

    builder.tiltDeg(tiltDeg(center, normal));
    builder.tiltDirDeg(tiltDirDeg(longitude, normal));

    return builder.build();
  }

  /**
   * Return the angle between the radial and normal vectors in degrees. The angle is constrained to
   * be between 0 and 90 degrees.
   * 
   * @param radial
   * @param normal
   * @return
   */
  public static double tiltDeg(double[] radial, double[] normal) {
    return tiltDeg(new Vector3D(radial), new Vector3D(normal));
  }

  /**
   * Return the angle between the radial and normal vectors in degrees. The angle is constrained to
   * be between 0 and 90 degrees.
   * 
   * @param radial
   * @param normal
   * @return
   */
  public static double tiltDeg(Vector3D radial, Vector3D normal) {
    double tiltDeg = Math.toDegrees(Vector3D.angle(radial, normal));
    if (tiltDeg > 90)
      tiltDeg = 180 - tiltDeg;
    return tiltDeg;
  }

  public static double tiltDirDeg(double lonDeg, double[] normal) {
    return tiltDirDeg(lonDeg, new Vector3D(normal));
  }

  public static double tiltDirDeg(double lonDeg, Vector3D normal) {
    Rotation r =
        new Rotation(Vector3D.PLUS_K, Math.toRadians(lonDeg), RotationConvention.FRAME_TRANSFORM);
    Vector3D newVector = r.applyTo(normal);

    double atan2D = newVector.getAlpha();
    double tiltDirDeg = 90 - Math.toDegrees(atan2D);
    if (tiltDirDeg < 0)
      tiltDirDeg = 360D + tiltDirDeg;
    return tiltDirDeg;
  }

  /**
   * Find the cell with index cellId.
   * 
   * @param polydata
   * @param cellId
   * @param idList
   * @return
   */
  public static CellInfo getCellInfo(vtkPolyData polydata, long cellId, vtkIdList idList) {
    return getCellInfo(polydata, cellId, idList, false);
  }

  /**
   * Find the cell with index cellId.
   * 
   * @param polydata
   * @param cellId
   * @param idList
   * @param CACHE_CELLINFO add this cell to the cached map
   * @return
   */
  public static synchronized CellInfo getCellInfo(vtkPolyData polydata, long cellId,
      vtkIdList idList, boolean CACHE_CELLINFO) {
    HashMap<Long, CellInfo> ciMap = completeMap.get(polydata);
    if (ciMap == null) {
      ciMap = new HashMap<>();
      if (CACHE_CELLINFO)
        completeMap.put(polydata, ciMap);
    }

    CellInfo ci = ciMap.get(cellId);
    if (ci == null) {
      double[] pt0 = new double[3];
      double[] pt1 = new double[3];
      double[] pt2 = new double[3];
      getCellPoints(polydata, cellId, idList, pt0, pt1, pt2);
      ci = fromPoints(pt0, pt1, pt2);

      if (CACHE_CELLINFO)
        ciMap.put(cellId, ci);
    } else {
      // need to populate idList as it is not cached
      polydata.GetCellPoints(cellId, idList);
    }
    return ci;
  }

  /**
   * Given a polydata model and vtkFloatArrays containing data at each cell in the polydata model,
   * calculate the data values at points in the polydata model.
   * 
   * @param dem
   * @param cellData
   * @param pointData
   */
  public static void convertCellDataToPointData(vtkPolyData dem,
      HashMap<String, vtkFloatArray> cellData, HashMap<String, vtkFloatArray> pointData) {

    vtkCellDataToPointData cellToPoint = new vtkCellDataToPointData();
    cellToPoint.SetInputData(dem);

    for (String arrayName : cellData.keySet()) {
      vtkFloatArray array = cellData.get(arrayName);
      dem.GetCellData().SetScalars(array);
      cellToPoint.Update();
      vtkFloatArray arrayPoint = new vtkFloatArray();
      vtkDataArray outputScalars =
          ((vtkPolyData) cellToPoint.GetOutput()).GetPointData().GetScalars();
      arrayPoint.DeepCopy(outputScalars);
      pointData.put(arrayName, arrayPoint);
    }

    dem.GetPointData().SetScalars(null);

    cellToPoint.Delete();
  }

  /**
   * Converts a vtkFloat array of data values at the facet center into a vtkFloat array of data
   * values at the vertices. Assumes input vtkFloatArray contains data that are in the same order as
   * the cells in the dem, otherwise this doesn't work.
   * 
   * @param dem
   * @param cellData
   * @return
   */
  public static vtkFloatArray convertvtkCellToPoint(vtkPolyData dem, vtkFloatArray cellData) {
    vtkCellDataToPointData cellToPoint = new vtkCellDataToPointData();
    cellToPoint.SetInputData(dem);

    dem.GetCellData().SetScalars(cellData);
    cellToPoint.Update();
    vtkFloatArray pointData = new vtkFloatArray();
    vtkDataArray outputScalars =
        ((vtkPolyData) cellToPoint.GetOutput()).GetPointData().GetScalars();
    pointData.DeepCopy(outputScalars);

    dem.GetPointData().SetScalars(null);
    cellToPoint.Delete();

    return pointData;
  }

  /**
   * Converts a vtkFloat array of data values at the vertices into a vtkFloat array of data values
   * at the facet centers. Assumes vertex data are in the same order as the vertices for each facet
   * in the dem, otherwise this doesn't work.
   * 
   * @param dem
   * @param pointData
   * @return
   */
  public static vtkFloatArray convertvtkPointToCell(vtkPolyData dem, vtkFloatArray pointData) {
    vtkPointDataToCellData pointToCell = new vtkPointDataToCellData();
    pointToCell.SetInputData(dem);
    dem.GetPointData().SetScalars(pointData);
    pointToCell.Update();
    vtkFloatArray arrayCell = new vtkFloatArray();
    vtkDataArray outputScalars = ((vtkPolyData) pointToCell.GetOutput()).GetCellData().GetScalars();
    arrayCell.DeepCopy(outputScalars);
    dem.GetPointData().SetScalars(null);
    pointToCell.Delete();

    return arrayCell;

  }

}
