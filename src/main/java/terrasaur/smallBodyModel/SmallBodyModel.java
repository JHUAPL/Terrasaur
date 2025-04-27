package terrasaur.smallBodyModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import terrasaur.utils.PolyDataUtil;
import terrasaur.utils.math.RotationUtils;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkAbstractPointLocator;
import vtk.vtkCell;
import vtk.vtkCellData;
import vtk.vtkFloatArray;
import vtk.vtkGenericCell;
import vtk.vtkIdList;
import vtk.vtkOctreePointLocator;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtksbCellLocator;
import vtk.vtksbModifiedBSPTree;

/**
 * The SmallBodyModel class represents a shape model of a small body such as Bennu or Eros. It
 * contains methods for common operations on a shape model such as searching for closest points or
 * cells, cutting out an elliptical region or boundary, or exporting to different formats.
 * 
 * @author kahneg1
 * @version 1.0
 * 
 */
public class SmallBodyModel {

  private final static Logger logger = LogManager.getLogger(SmallBodyModel.class);

  public enum ColoringValueType {
    POINT_DATA, CELLDATA
  }

  private vtkPolyData smallBodyPolyData;
  private vtkPolyData lowResSmallBodyPolyData;
  private vtksbCellLocator cellLocator;
  private vtksbModifiedBSPTree bspLocator;
  private vtkOctreePointLocator pointLocator;
  private vtkOctreePointLocator lowResPointLocator;
  private SmallBodyCubes smallBodyCubes;
  private File defaultModelFile;
  private int resolutionLevel = 0;
  private vtkGenericCell genericCell;
  private String[] modelNames;
  private BoundingBox boundingBox = null;

  private vtkFloatArray cellNormals;
  private vtkIdList idList; // to avoid repeated allocations
  private vtkIdList idList2; // to avoid repeated allocations

  /**
   * Default constructor. Must be followed by a call to setSmallBodyPolyData.
   */
  public SmallBodyModel() {
    smallBodyPolyData = new vtkPolyData();
    genericCell = new vtkGenericCell();
    idList = new vtkIdList();
    idList2 = new vtkIdList();
  }

  /**
   * Convenience method for initializing a SmallBodyModel with just a vtkPolyData.
   * 
   * @param polyData
   */
  public SmallBodyModel(vtkPolyData polyData) {
    this();

    vtkFloatArray[] coloringValues = {};
    String[] coloringNames = {};
    String[] coloringUnits = {};
    ColoringValueType coloringValueType = ColoringValueType.CELLDATA;

    setSmallBodyPolyData(polyData, coloringValues, coloringNames, coloringUnits, coloringValueType);
  }

  public void setSmallBodyPolyData(vtkPolyData polydata, vtkFloatArray[] coloringValues,
      String[] coloringNames, String[] coloringUnits, ColoringValueType coloringValueType) {
    smallBodyPolyData.DeepCopy(polydata);

    smallBodyPolyData.BuildLinks(0);

    initializeLocators();

    lowResSmallBodyPolyData = smallBodyPolyData;
    lowResPointLocator = pointLocator;
  }

  public boolean isBuiltIn() {
    return true;
  }

  private void initializeLocators() {
    if (cellLocator == null) {
      cellLocator = new vtksbCellLocator();
      bspLocator = new vtksbModifiedBSPTree();
      pointLocator = new vtkOctreePointLocator();
    }

    // Initialize the cell locator
    cellLocator.FreeSearchStructure();
    cellLocator.SetDataSet(smallBodyPolyData);
    cellLocator.CacheCellBoundsOn();
    cellLocator.AutomaticOn();
    // cellLocator.SetMaxLevel(10);
    // cellLocator.SetNumberOfCellsPerNode(5);
    cellLocator.BuildLocator();

    // Initialize the BSP locator
    bspLocator.FreeSearchStructure();
    bspLocator.SetDataSet(smallBodyPolyData);
    bspLocator.CacheCellBoundsOn();
    bspLocator.AutomaticOn();
    // bspLocator.SetMaxLevel(10);
    // bspLocator.SetNumberOfCellsPerNode(5);
    bspLocator.BuildLocator();

    pointLocator.FreeSearchStructure();
    pointLocator.SetDataSet(smallBodyPolyData);
    pointLocator.BuildLocator();
  }

  private void initializeLowResData() {
    if (lowResPointLocator == null) {
      lowResSmallBodyPolyData = new vtkPolyData();

      try {
        lowResSmallBodyPolyData.ShallowCopy(
            PolyDataUtil.loadShapeModelAndComputeNormals(defaultModelFile.getAbsolutePath()));
      } catch (Exception e) {
        e.printStackTrace();
      }

      lowResPointLocator = new vtkOctreePointLocator();
      lowResPointLocator.SetDataSet(lowResSmallBodyPolyData);
      lowResPointLocator.BuildLocator();
    }
  }

  public vtkPolyData getSmallBodyPolyData() {
    return smallBodyPolyData;
  }

  public vtkPolyData getLowResSmallBodyPolyData() {
    initializeLowResData();

    return lowResSmallBodyPolyData;
  }

  public vtksbCellLocator getCellLocator() {
    return cellLocator;
  }

  public vtkAbstractPointLocator getPointLocator() {
    return pointLocator;
  }

  public SmallBodyCubes getSmallBodyCubes() {
    if (smallBodyCubes == null) {
      // The number 38.66056033363347 is used here so that the cube size
      // comes out to 1 km for Eros.

      // Compute bounding box diagonal length of lowest res shape model
      double diagonalLength =
          new BoundingBox(getLowResSmallBodyPolyData().GetBounds()).getDiagonalLength();
      double cubeSize = diagonalLength / 38.66056033363347;
      smallBodyCubes =
          new SmallBodyCubes(getLowResSmallBodyPolyData(), cubeSize, 0.01 * cubeSize, true);
    }

    return smallBodyCubes;
  }

  public TreeSet<Integer> getIntersectingCubes(vtkPolyData polydata) {
    return getSmallBodyCubes().getIntersectingCubes(polydata);
  }

  public TreeSet<Integer> getIntersectingCubes(BoundingBox bb) {
    return getSmallBodyCubes().getIntersectingCubes(bb);
  }

  public int getCubeId(double[] point) {
    return getSmallBodyCubes().getCubeId(point);
  }

  public vtkFloatArray getCellNormals() {
    // Compute the normals of necessary. For now don't add the normals to
    // the cell
    // data of the small body model since doing so might create problems.
    // TODO consider adding normals to cell data without creating problems
    if (cellNormals == null) {
      vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
      normalsFilter.SetInputData(smallBodyPolyData);
      normalsFilter.SetComputeCellNormals(1);
      normalsFilter.SetComputePointNormals(0);
      normalsFilter.SplittingOff();
      normalsFilter.ConsistencyOn();
      normalsFilter.AutoOrientNormalsOff();
      normalsFilter.Update();

      vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
      vtkCellData normalsFilterOutputCellData = normalsFilterOutput.GetCellData();
      vtkFloatArray normals = (vtkFloatArray) normalsFilterOutputCellData.GetNormals();

      cellNormals = new vtkFloatArray();
      cellNormals.DeepCopy(normals);

      normals.Delete();
      normalsFilterOutputCellData.Delete();
      normalsFilterOutput.Delete();
      normalsFilter.Delete();
    }

    return cellNormals;
  }

  /**
   * Get the normal at a point. The normal vector at several vertices near the current point are
   * averaged to compute the normal.
   * 
   * @param point
   * @return
   */
  public double[] getNormalAtPoint(double[] point) {
    return PolyDataUtil.getPolyDataNormalAtPoint(point, smallBodyPolyData, pointLocator);
  }

  /**
   * Get the normal at a point. Unlike the other function with the same name, this averages the
   * normals to all vertices within radius distance of point.
   * 
   * @param point
   * @param radius
   * @return
   */
  public double[] getNormalAtPoint(double[] point, double radius) {
    return PolyDataUtil.getPolyDataNormalAtPointWithinRadius(point, smallBodyPolyData, pointLocator,
        radius);
  }

  public double[] getClosestNormal(double[] point) {
    long closestCell = findClosestCell(point);
    return getCellNormals().GetTuple3(closestCell);
  }

  /**
   * Get the normal at a point. Unlike the other function with the same name, this averages the
   * normals to all vertices within radius distance of point.
   * 
   * @param point
   * @param radius
   * @return
   */
  public vtkIdList getIDsNearPoint(double[] point, double radius) {
    return PolyDataUtil.getIDsAtPointWithinRadius(point, smallBodyPolyData, pointLocator, radius);
  }

  /**
   * This returns the closest point to the model to pt. Note the returned point need not be a vertex
   * of the model and can lie anywhere on a plate.
   * 
   * @param pt
   * @return
   */
  public double[] findClosestPoint(double[] pt) {
    double[] closestPoint = new double[3];
    long[] cellId = new long[1];
    int[] subId = new int[1];
    double[] dist2 = new double[1];

    cellLocator.FindClosestPoint(pt, closestPoint, genericCell, cellId, subId, dist2);

    return closestPoint;
  }

  /**
   * This returns the closest vertex in the shape model to pt. Unlike findClosestPoin this functions
   * only returns one of the vertices of the shape model not an arbitrary point lying on a cell.
   * 
   * @param pt
   * @return
   */
  public double[] findClosestVertex(double[] pt) {
    long id = pointLocator.FindClosestPoint(pt);
    double[] returnPt = new double[3];
    smallBodyPolyData.GetPoint(id, returnPt);
    return returnPt;
  }

  /**
   * This returns the index of the closest cell in the model to pt. The closest point within the
   * cell is returned in closestPoint
   * 
   * @param pt
   * @param closestPoint the closest point within the cell is returned here
   * @return
   */
  public long findClosestCell(double[] pt, double[] closestPoint) {
    long[] cellId = new long[1];
    int[] subId = new int[1];
    double[] dist2 = new double[1];

    // Use FindClosestPoint rather the FindCell since not sure what
    // tolerance to use in the latter.
    cellLocator.FindClosestPoint(pt, closestPoint, genericCell, cellId, subId, dist2);

    return cellId[0];
  }

  /**
   * This returns the index of the closest cell in the model to pt.
   * 
   * @param pt
   * @return
   */
  public long findClosestCell(double[] pt) {
    double[] closestPoint = new double[3];
    return findClosestCell(pt, closestPoint);
  }

  public Set<Long> findClosestCellsWithinRadius(double[] pt, double radius) {
    Set<Long> cells = new HashSet<>();
    pointLocator.FindPointsWithinRadius(radius, pt, idList);
    long size = idList.GetNumberOfIds();
    for (int i = 0; i < size; ++i) {
      long id = idList.GetId(i);
      smallBodyPolyData.GetPointCells(id, idList2);
      long numCells = idList2.GetNumberOfIds();
      for (int j = 0; j < numCells; ++j) {
        long id2 = idList2.GetId(j);
        cells.add(id2);
      }
    }
    return cells;
  }

  public ArrayList<Long> findClosestVerticesWithinRadius(double[] pt, double radius) {
    ArrayList<Long> vertices = new ArrayList<>();
    pointLocator.FindPointsWithinRadius(radius, pt, idList);
    long size = idList.GetNumberOfIds();
    for (long i = 0; i < size; ++i) {
      long id = idList.GetId(i);
      vertices.add(id);
    }
    return vertices;
  }

  /**
   * Compute the point on the asteroid that has the specified latitude and longitude. Returns the
   * cell id of the cell containing that point. This is done by shooting a ray from the origin in
   * the specified direction.
   * 
   * @param lat - in radians
   * @param lon - in radians
   * @param intersectPoint
   * @return the cellId of the cell containing the intersect point
   */
  public long getPointAndCellIdFromLatLon(double lat, double lon, double[] intersectPoint) {
    LatitudinalVector lla = new LatitudinalVector(1.0, lat, lon);
    UnwritableVectorIJK rect = CoordConverters.convert(lla);

    double[] origin = {0.0, 0.0, 0.0};
    double[] lookPt = {rect.getI(), rect.getJ(), rect.getK()};

    return computeRayIntersection(origin, lookPt, 10 * getBoundingBoxDiagonalLength(),
        intersectPoint);
  }

  /**
   * Write shape out to regular rectangular lat/lon grid. Grid is 180 degrees in lat, 360 in lon.
   * 
   * @param pixelsPerDegree
   * @return
   */
  public double[][][] polyDataToLatLonRadGrid(double pixelsPerDegree) {
    int numRows = (int) Math.round(180.0 * pixelsPerDegree) + 1;
    int numCols = (int) Math.round(360.0 * pixelsPerDegree) + 1;
    double[][][] data = new double[6][numRows][numCols];

    double[] intersectPoint = new double[3];

    double incr = 1.0 / pixelsPerDegree;
    for (int m = 0; m < numRows; ++m) {
      for (int n = 0; n < numCols; ++n) {
        double lat = m * incr - 90.0;
        double lon = n * incr - 180.0;

        data[0][m][n] = lat;
        data[1][m][n] = lon;

        long cellId =
            getPointAndCellIdFromLatLon(Math.toRadians(lat), Math.toRadians(lon), intersectPoint);
        double rad = -1.0e32;
        if (cellId >= 0)
          rad = new VectorIJK(intersectPoint).getLength();
        else {
          logger.info(String.format("Warning: no intersection at lat:%.5f, lon:%.5f", lat, lon));
        }
        data[2][m][n] = rad;
        data[3][m][n] = intersectPoint[0];
        data[4][m][n] = intersectPoint[1];
        data[5][m][n] = intersectPoint[2];
      }
    }

    return data;
  }

  /**
   * Compute the intersection of a line segment with the asteroid. Returns the cell id of the cell
   * containing that point. This is done by shooting a ray from the specified origin in the
   * specified direction.
   * 
   * @param origin one end of line segment
   * @param direction direction of line segment, assumed to be a unit vector
   * @param intersectPoint (returned)
   * @return the cellId of the cell containing the intersect point or -1 if no intersection
   */
  public long computeRayIntersection(double[] origin, double[] direction, double[] intersectPoint) {
    double distance = new VectorIJK(origin).getLength() + 10 * getBoundingBoxDiagonalLength();
    return computeRayIntersection(origin, direction, distance, intersectPoint);
  }

  /**
   * Compute the intersection of a line segment with the asteroid. Returns the cell id of the cell
   * containing that point. This is done by shooting a ray from the specified origin in the
   * specified direction.
   * 
   * @param origin one end of line segment
   * @param direction direction of line segment, assumed to be a unit vector
   * @param distance length of line segment
   * @param intersectPoint (returned)
   * @return the cellId of the cell containing the intersect point or -1 if no intersection
   */
  public long computeRayIntersection(double[] origin, double[] direction, double distance,
      double[] intersectPoint) {

    double[] lookPt = new double[3];
    lookPt[0] = origin[0] + 2.0 * distance * direction[0];
    lookPt[1] = origin[1] + 2.0 * distance * direction[1];
    lookPt[2] = origin[2] + 2.0 * distance * direction[2];

    double tol = 1e-6;
    double[] t = new double[1];
    double[] x = new double[3];
    double[] pcoords = new double[3];
    int[] subId = new int[1];
    long[] cellId = new long[1];

    int result = cellLocator.IntersectWithLine(origin, lookPt, tol, t, x, pcoords, subId, cellId,
        genericCell);

    intersectPoint[0] = x[0];
    intersectPoint[1] = x[1];
    intersectPoint[2] = x[2];

    if (result > 0)
      return cellId[0];
    else
      return -1;
  }

  /**
   * Return a unit vector that points east
   * 
   * @param pt direction of the surface point from the origin. Does not need to be a unit vector or
   *        lie on the surface.
   * @return unit vector that points east
   */
  public Vector3D findEastVector(double[] pt) {
    // define a topographic frame where the Z axis points up and the Y axis points north. The X axis
    // will point east.
    Rotation bodyFixedToTopo =
        RotationUtils.KprimaryJsecondary(new Vector3D(pt), Vector3D.PLUS_K);
    return bodyFixedToTopo.applyTo(Vector3D.PLUS_I);
  }

  /**
   * Return a unit vector that points west
   * 
   * @param pt direction of the surface point from the origin. Does not need to be a unit vector or
   *        lie on the surface.
   * @return unit vector that points west
   */
  public Vector3D findWestVector(double[] pt) {
    // define a topographic frame where the Z axis points up and the Y axis points north. The X axis
    // will point east.
    Rotation bodyFixedToTopo =
            RotationUtils.KprimaryJsecondary(new Vector3D(pt), Vector3D.PLUS_K);
    return bodyFixedToTopo.applyTo(Vector3D.MINUS_I);
  }

  /** @return {@link BoundingBox} which encloses this shape */
  public BoundingBox getBoundingBox() {
    if (boundingBox == null) {
      smallBodyPolyData.ComputeBounds();
      boundingBox = new BoundingBox(smallBodyPolyData.GetBounds());
    }

    return boundingBox;
  }

  /** @return diagonal length of the enclosing @{link BoundingBox} */
  public double getBoundingBoxDiagonalLength() {
    return getBoundingBox().getDiagonalLength();
  }

  /** @return statistics on the edge lengths of each cell of the shape model */
  public DescriptiveStatistics computeLargestSmallestMeanEdgeLength() {
    long numberOfCells = smallBodyPolyData.GetNumberOfCells();

    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (int i = 0; i < numberOfCells; ++i) {
      vtkCell cell = smallBodyPolyData.GetCell(i);
      vtkPoints points = cell.GetPoints();
      double[] pt0 = points.GetPoint(0);
      double[] pt1 = points.GetPoint(1);
      double[] pt2 = points.GetPoint(2);

      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));

      stats.addValue(VectorIJK.subtract(facet.getVertex1(), facet.getVertex2()).getLength());
      stats.addValue(VectorIJK.subtract(facet.getVertex2(), facet.getVertex3()).getLength());
      stats.addValue(VectorIJK.subtract(facet.getVertex3(), facet.getVertex1()).getLength());

      points.Delete();
      cell.Delete();
    }

    return stats;
  }

  public String getModelName() {
    if (resolutionLevel >= 0 && resolutionLevel < modelNames.length)
      return modelNames[resolutionLevel];
    else
      return null;
  }

  /** clean up VTK allocated internal objects */
  public void delete() {
    if (cellLocator != null)
      cellLocator.Delete();
    if (bspLocator != null)
      bspLocator.Delete();
    if (pointLocator != null)
      pointLocator.Delete();
    if (genericCell != null)
      genericCell.Delete();
    if (smallBodyPolyData != null)
      smallBodyPolyData.Delete();
  }

  public void saveAsPLT(File file) throws IOException {
    PolyDataUtil.saveShapeModelAsPLT(smallBodyPolyData, file.getAbsolutePath());
  }

  public void saveAsOBJ(File file) throws IOException {
    PolyDataUtil.saveShapeModelAsOBJ(smallBodyPolyData, file.getAbsolutePath());
  }

  public void saveAsVTK(File file) throws IOException {
    PolyDataUtil.saveShapeModelAsVTK(smallBodyPolyData, file.getAbsolutePath());
  }

  public void saveAsSTL(File file) throws IOException {
    PolyDataUtil.saveShapeModelAsSTL(smallBodyPolyData, file.getAbsolutePath());
  }

}
