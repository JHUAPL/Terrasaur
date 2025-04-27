package terrasaur.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.VectorIJK;
import picante.math.vectorspace.UnwritableVectorIJK;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.math.RotationUtils;
import terrasaur.utils.mesh.TriangularFacet;
import spice.basic.Plane;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import vtk.vtkAbstractPointLocator;
import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkDecimatePro;
import vtk.vtkDelaunay3D;
import vtk.vtkFeatureEdges;
import vtk.vtkFloatArray;
import vtk.vtkGenericCell;
import vtk.vtkGeometryFilter;
import vtk.vtkIdList;
import vtk.vtkOBJReader;
import vtk.vtkPLYReader;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtkPolyDataReader;
import vtk.vtkPolyDataWriter;
import vtk.vtkSTLReader;
import vtk.vtkSTLWriter;
import vtk.vtkUnstructuredGrid;
import vtk.vtksbCellLocator;

/** Utilities for working with a {@link vtkPolyData} object */
public class PolyDataUtil {

  private static final Logger logger = LogManager.getLogger();

  public static final float INVALID_VALUE = -1.0e38f;

  /** add point normals if this polydata does not already contain them */
  public static void addPointNormalsToShapeModel(vtkPolyData polyData) {
    if (polyData.GetPointData().GetNormals() == null) {
      // Add normal vectors
      vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
      normalsFilter.SetInputData(polyData);
      normalsFilter.SetComputeCellNormals(0);
      normalsFilter.SetComputePointNormals(1);
      normalsFilter.SplittingOff();
      normalsFilter.AutoOrientNormalsOn();
      normalsFilter.ConsistencyOn();
      normalsFilter.Update();

      vtkPolyData normalsOutput = normalsFilter.GetOutput();
      polyData.ShallowCopy(normalsOutput);

      normalsFilter.Delete();
    }
  }

  /**
   * Compute the mean normal vector over the entire vtkPolyData by averaging all the normal vectors
   * of all cells.
   */
  public static Vector3D computeMeanPolyDataNormal(vtkPolyData polyData) {

    // Average the normals
    double[] normal = {0.0, 0.0, 0.0};

    long numCells = polyData.GetNumberOfCells();
    for (int i = 0; i < numCells; ++i) {
      TriangularFacet tf = getFacet(polyData, i);
      UnwritableVectorIJK n = tf.getNormal();
      normal[0] += n.getI();
      normal[1] += n.getJ();
      normal[2] += n.getK();
    }

    normal[0] /= numCells;
    normal[1] /= numCells;
    normal[2] /= numCells;

    return new Vector3D(normal);
  }

  /**
   * @return the centroid of all the points in the polydata.
   */
  public static Vector3D computePolyDataCentroid(vtkPolyData polyData) {
    // Average the normals
    double[] centroid = {0.0, 0.0, 0.0};

    long numPoints = polyData.GetNumberOfPoints();
    double[] p = new double[3];
    for (int i = 0; i < numPoints; ++i) {
      polyData.GetPoint(i, p);
      centroid[0] += p[0];
      centroid[1] += p[1];
      centroid[2] += p[2];
    }

    centroid[0] /= numPoints;
    centroid[1] /= numPoints;
    centroid[2] /= numPoints;

    return new Vector3D(centroid);
  }

  /**
   * Reduce the number of cells in a mesh by targetReduction. For example, if the mesh contains 100
   * triangles and targetReduction is .90, after the decimation there will be approximately 10
   * triangles - a 90% reduction.
   *
   * @param polydata
   * @param targetReduction fraction between zero and one
   */
  public static void decimatePolyData(vtkPolyData polydata, double targetReduction) {
    vtkDecimatePro dec = new vtkDecimatePro();
    dec.SetInputData(polydata);
    dec.SetTargetReduction(targetReduction);
    dec.PreserveTopologyOn();
    dec.SplittingOff();
    dec.BoundaryVertexDeletionOff();
    dec.SetMaximumError(Double.MAX_VALUE);
    dec.AccumulateErrorOn();
    dec.PreSplitMeshOn();
    dec.Update();
    vtkPolyData decOutput = dec.GetOutput();

    polydata.DeepCopy(decOutput);

    dec.Delete();
  }

  /**
   * Fit a {@link Plane} to a polyData.
   *
   * @param polyData
   * @return
   */
  public static Plane fitPlaneToPolyData(vtkPolyData polyData) {
    Pair<Rotation, Vector3D> entry = findLocalFrame(polyData);
    Vector3 normal = MathConversions.toVector3(entry.getKey().applyInverseTo(Vector3D.PLUS_K));
    Vector3 pointInPlane = MathConversions.toVector3(entry.getValue());
    Plane p = null;
    try {
      p = new Plane(normal, pointInPlane);
    } catch (SpiceException e) {
      logger.warn(e.getLocalizedMessage());
    }
    return p;
  }

  /**
   * Return a rotation matrix where the Z axis points along the average normal or radial, and the
   * centroid of all of the points. This is only meaningful with local shape models.
   *
   * @return
   */
  public static Pair<Rotation, Vector3D> findLocalFrame(vtkPolyData polyData) {
    try {
      Vector3D centroid = computePolyDataCentroid(polyData);

      // subtract out the centroid from the points
      int numPoints = (int) polyData.GetNumberOfPoints();
      double[][] points = new double[3][numPoints];
      double[] p = new double[3];
      for (int i = 0; i < numPoints; ++i) {
        polyData.GetPoint(i, p);
        points[0][i] = p[0] - centroid.getX();
        points[1][i] = p[1] - centroid.getY();
        points[2][i] = p[2] - centroid.getZ();
      }
      RealMatrix pointMatrix = new Array2DRowRealMatrix(points, false);

      // Now do SVD on this matrix
      SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
      RealMatrix u = svd.getU();

      // uz points normal to the plane and equals the eigenvector
      // corresponding to the smallest eigenvalue of the V matrix
      Vector3D uz = new Vector3D(u.getColumn(2)).normalize();

      // Make sure uz points away from the asteroid rather than towards it
      // by looking at the dot product of uz and a point normal to the
      // data. If dot product is negative, reverse uz.
      Vector3D normal = computeMeanPolyDataNormal(polyData);

      normal = normal.normalize();
      if (normal.dotProduct(uz) < 0) uz = uz.scalarMultiply(-1);

      // make ux and uy perpendicular to uz
      Vector3D ux = uz.crossProduct(Vector3D.PLUS_K);
      if (ux.getNorm() == 0) ux = Vector3D.PLUS_I;

      Rotation rot = RotationUtils.KprimaryIsecondary(uz, ux);
      return new Pair<Rotation, Vector3D>(rot, centroid);
    } catch (Exception e) {
      logger.warn(e.getLocalizedMessage());
      return null;
    }
  }

  public static double[] getPolyDataNormalAtPoint(
      double[] pt, vtkPolyData polyData, vtkAbstractPointLocator pointLocator) {
    vtkIdList idList = new vtkIdList();

    pointLocator.FindClosestNPoints(20, pt, idList);

    // Average the normals
    double[] normal = {0.0, 0.0, 0.0};

    long N = idList.GetNumberOfIds();
    if (N < 1) return null;

    vtkDataArray normals = polyData.GetPointData().GetNormals();
    for (int i = 0; i < N; ++i) {
      double[] tmp = normals.GetTuple3(idList.GetId(i));
      normal[0] += tmp[0];
      normal[1] += tmp[1];
      normal[2] += tmp[2];
    }

    normal[0] /= N;
    normal[1] /= N;
    normal[2] /= N;

    idList.Delete();

    return normal;
  }

  public static double[] getPolyDataNormalAtPointWithinRadius(
      double[] pt, vtkPolyData polyData, vtkAbstractPointLocator pointLocator, double radius) {
    vtkIdList idList = new vtkIdList();

    pointLocator.FindPointsWithinRadius(radius, pt, idList);

    // Average the normals
    double[] normal = {0.0, 0.0, 0.0};

    long N = idList.GetNumberOfIds();
    if (N < 1) return null;

    vtkDataArray normals = polyData.GetPointData().GetNormals();
    for (int i = 0; i < N; ++i) {
      double[] tmp = normals.GetTuple3(idList.GetId(i));
      normal[0] += tmp[0];
      normal[1] += tmp[1];
      normal[2] += tmp[2];
    }

    normal[0] /= N;
    normal[1] /= N;
    normal[2] /= N;

    idList.Delete();

    return normal;
  }

  /**
   * @param polydata
   */
  public static vtkPolyData getBoundary(vtkPolyData polydata) {
    // Compute the bounding edges of this surface
    vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
    edgeExtracter.SetInputData(polydata);
    edgeExtracter.BoundaryEdgesOn();
    edgeExtracter.FeatureEdgesOff();
    edgeExtracter.NonManifoldEdgesOff();
    edgeExtracter.ManifoldEdgesOff();
    edgeExtracter.Update();

    vtkPolyData edgeExtracterOutput = edgeExtracter.GetOutput();

    vtkPolyData boundary = new vtkPolyData();
    boundary.DeepCopy(edgeExtracterOutput);

    edgeExtracter.Delete();
    return boundary;
  }

  /**
   * @param polyData shape model
   * @param i facet index
   * @return {@link TriangularFacet} with index i
   */
  public static TriangularFacet getFacet(vtkPolyData polyData, long i) {
    vtkIdList idList = new vtkIdList();
    polyData.GetCellPoints(i, idList);

    double[] pt = new double[3];
    polyData.GetPoint(idList.GetId(0), pt);
    VectorIJK v1 = new VectorIJK(pt);
    polyData.GetPoint(idList.GetId(1), pt);
    VectorIJK v2 = new VectorIJK(pt);
    polyData.GetPoint(idList.GetId(2), pt);
    VectorIJK v3 = new VectorIJK(pt);

    return new TriangularFacet(v1, v2, v3);
  }

  /**
   * Return the vtkIDList for points found within radius of point pt.
   *
   * @param pt
   * @param polyData
   * @param pointLocator
   * @param radius
   * @return
   */
  public static vtkIdList getIDsAtPointWithinRadius(
      double[] pt, vtkPolyData polyData, vtkAbstractPointLocator pointLocator, double radius) {

    vtkIdList idList = new vtkIdList();
    pointLocator.FindPointsWithinRadius(radius, pt, idList);
    return idList;
  }

  /**
   * A 3D Delaunay triangulation is constructed from the points in region0. Any points from region1
   * contained within this mesh are considered to be in the overlapping region.
   *
   * @param region0 Reference point set
   * @param region1 Test point set
   * @return points in region1 contained within region0
   */
  public static vtkUnstructuredGrid intersectingPoints(vtkPoints region0, vtkPoints region1) {
    vtkUnstructuredGrid region0Grid = new vtkUnstructuredGrid();
    region0Grid.SetPoints(region0);

    vtkDelaunay3D vtkd = new vtkDelaunay3D();
    vtkd.SetInputData(region0Grid);
    vtkd.Update();

    vtkGeometryFilter vtkgf = new vtkGeometryFilter();
    vtkgf.SetInputData(vtkd.GetOutput());
    vtkgf.Update();

    vtksbCellLocator cellLoc = new vtksbCellLocator();
    cellLoc.SetDataSet(vtkgf.GetOutput());
    cellLoc.BuildLocator();

    // vtkPolyDataWriter pdw = new vtkPolyDataWriter();
    // pdw.SetInputData(vtkgf.GetOutput());
    // pdw.SetFileName("./fixedHull.vtk");
    // pdw.SetFileTypeToBinary();
    // pdw.Update();

    double[] origin = {0., 0., 0.};
    double tol = 1e-6;
    double[] t = new double[1];
    double[] x = new double[3];
    double[] pcoords = new double[3];
    int[] subId = new int[1];
    long[] cellId = new long[1];
    vtkGenericCell genericCell = new vtkGenericCell();
    vtkPoints intersected = new vtkPoints();
    for (int i = 0; i < region1.GetNumberOfPoints(); i++) {
      double[] p = region1.GetPoint(i);
      double[] endPt = new double[3];
      for (int j = 0; j < 3; j++) endPt[j] = p[j] * 10;
      int code =
          cellLoc.IntersectWithLine(origin, endPt, tol, t, x, pcoords, subId, cellId, genericCell);
      if (code == 0) continue;
      intersected.InsertNextPoint(p);
    }

    // System.out.printf("intersectingPoints: %d intersecting points\n",
    // intersected.GetNumberOfPoints());

    vtkUnstructuredGrid returnGrid = new vtkUnstructuredGrid();
    returnGrid.SetPoints(intersected);

    return returnGrid;
  }

  /**
   * Read in a shape model in ICQ format and convert to PLT format.
   *
   * @param icqFile
   * @return
   */
  private static List<String> icq2plt(String icqFile) {
    try {
      List<String> lines = FileUtils.readLines(new File(icqFile), Charset.defaultCharset());
      return icq2plt(lines);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage());
      return null;
    }
  }

  /**
   * Read in a shape model in ICQ format and convert to PLT format. Based on SHAPE2PLATES from SPC.
   *
   * @param icqLines
   * @return
   */
  private static List<String> icq2plt(List<String> icqLines) {

    List<String> pltLines = new ArrayList<>();

    String[] parts = icqLines.get(0).strip().split("\\s+");
    int q = Integer.parseInt(parts[0].strip());

    pltLines.add(String.format("%d", (int) (6 * Math.pow(q + 1, 2))));

    double[][][][] vec = new double[3][q + 1][q + 1][6];
    int[][][] n = new int[q + 1][q + 1][6];

    int n0 = 0;
    for (int f = 0; f < 6; f++) {
      for (int j = 0; j <= q; j++) {
        for (int i = 0; i <= q; i++) {
          parts = icqLines.get(++n0).strip().split("\\s+");
          for (int k = 0; k < 3; k++)
            vec[k][i][j][f] = Double.parseDouble(parts[k].strip().replaceAll("D", "E"));
          pltLines.add(
              String.format(
                  "%10d %.12f %.12f %.12f", n0, vec[0][i][j][f], vec[1][i][j][f], vec[2][i][j][f]));
          n[i][j][f] = n0;
        }
      }
    }

    for (int i = 1; i < q; i++) {
      n[i][q][5] = n[q - i][q][3];
      n[i][0][5] = n[i][q][1];
      n[i][0][4] = n[q][q - i][0];
      n[i][0][3] = n[q - i][0][0];
      n[i][0][2] = n[0][i][0];
      n[i][0][1] = n[i][q][0];
    }

    for (int j = 1; j < q; j++) {
      n[q][j][5] = n[j][q][4];
      n[q][j][4] = n[0][j][3];
      n[q][j][3] = n[0][j][2];
      n[q][j][2] = n[0][j][1];
      n[0][j][5] = n[q - j][q][2];
      n[0][j][4] = n[q][j][1];
    }

    n[0][0][2] = n[0][0][0];
    n[q][0][3] = n[0][0][0];
    n[0][0][1] = n[0][q][0];
    n[q][0][2] = n[0][q][0];
    n[0][0][3] = n[q][0][0];
    n[q][0][4] = n[q][0][0];
    n[0][0][4] = n[q][q][0];
    n[q][0][1] = n[q][q][0];
    n[0][0][5] = n[0][q][1];
    n[q][q][2] = n[0][q][1];
    n[0][q][4] = n[q][q][1];
    n[q][0][5] = n[q][q][1];
    n[q][q][3] = n[0][q][2];
    n[0][q][5] = n[0][q][2];
    n[q][q][4] = n[0][q][3];
    n[q][q][5] = n[0][q][3];

    try (PrintWriter pw = new PrintWriter("java.txt")) {
      for (int f = 0; f < 6; f++) {
        for (int i = 0; i < q; i++) {
          for (int j = 0; j < q; j++) {
            pw.printf("%3d%3d%3d%6d\n", i, j, f + 1, n[i][j][f]);
          }
        }
      }
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage());
    }

    pltLines.add(String.format("%d", 12 * q * q));
    n0 = 0;
    for (int f = 0; f < 6; f++) {
      for (int i = 0; i < q; i++) {
        for (int j = 0; j < q; j++) {

          // @formatter:off
          double w1x =
              vec[1][i][j][f] * vec[2][i + 1][j + 1][f] - vec[2][i][j][f] * vec[1][i + 1][j + 1][f];
          double w1y =
              vec[2][i][j][f] * vec[0][i + 1][j + 1][f] - vec[0][i][j][f] * vec[2][i + 1][j + 1][f];
          double w1z =
              vec[0][i][j][f] * vec[1][i + 1][j + 1][f] - vec[1][i][j][f] * vec[0][i + 1][j + 1][f];
          double w2x =
              vec[1][i + 1][j][f] * vec[2][i][j + 1][f] - vec[2][i + 1][j][f] * vec[1][i][j + 1][f];
          double w2y =
              vec[2][i + 1][j][f] * vec[0][i][j + 1][f] - vec[0][i + 1][j][f] * vec[2][i][j + 1][f];
          double w2z =
              vec[0][i + 1][j][f] * vec[1][i][j + 1][f] - vec[1][i + 1][j][f] * vec[0][i][j + 1][f];
          // @formatter:on

          double z1 = w1x * w1x + w1y * w1y + w1z * w1z;
          double z2 = w2x * w2x + w2y * w2y + w2z * w2z;

          if (z1 <= z2) {

            n0++;
            pltLines.add(
                String.format(
                    "%10d %10d %10d %10d", n0, n[i][j][f], n[i + 1][j + 1][f], n[i + 1][j][f]));

            n0++;
            pltLines.add(
                String.format(
                    "%10d %10d %10d %10d", n0, n[i][j][f], n[i][j + 1][f], n[i + 1][j + 1][f]));
          } else {
            n0++;
            pltLines.add(
                String.format(
                    "%10d %10d %10d %10d", n0, n[i][j][f], n[i][j + 1][f], n[i + 1][j][f]));

            n0++;
            pltLines.add(
                String.format(
                    "%10d %10d %10d %10d", n0, n[i + 1][j][f], n[i][j + 1][f], n[i + 1][j + 1][f]));
          }
        }
      }
    }

    return pltLines;
  }

  /**
   * Read in PDS vertex file format. There are 2 variants of this file. In one the first line
   * contains the number of points and the number of cells and then follows the points and vertices.
   * In the other variant the first line only contains the number of points, then follows the
   * points, then follows a line listing the number of cells followed by the cells. Support both
   * variants here.
   *
   * @param lines
   * @return
   */
  private static vtkPolyData loadPDSShapeModel(List<String> lines) {
    vtkPolyData polydata = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray cells = new vtkCellArray();
    polydata.SetPoints(points);
    polydata.SetPolys(cells);

    // Read in the first line which list the number of points and plates
    int count = 0;
    String val = lines.get(count++).strip();
    String[] vals = val.split("\\s+");
    int numPoints = -1;
    int numCells = -1;
    if (vals.length == 1) {
      numPoints = Integer.parseInt(vals[0]);
    } else if (vals.length == 2) {
      numPoints = Integer.parseInt(vals[0]);
      numCells = Integer.parseInt(vals[1]);
    } else {
      logger.error("Invalid format");
      return polydata;
    }

    for (int j = 0; j < numPoints; ++j) {
      vals = lines.get(count++).strip().split("\\s+");
      double x = Double.parseDouble(vals[1]);
      double y = Double.parseDouble(vals[2]);
      double z = Double.parseDouble(vals[3]);
      points.InsertNextPoint(x, y, z);
    }

    if (numCells == -1) {
      val = lines.get(count++).strip();
      numCells = Integer.parseInt(val);
    }

    vtkIdList idList = new vtkIdList();
    idList.SetNumberOfIds(3);
    for (int j = 0; j < numCells; ++j) {
      vals = lines.get(count++).strip().split("\\s+");
      int idx1 = Integer.parseInt(vals[1]) - 1;
      int idx2 = Integer.parseInt(vals[2]) - 1;
      int idx3 = Integer.parseInt(vals[3]) - 1;
      idList.SetId(0, idx1);
      idList.SetId(1, idx2);
      idList.SetId(2, idx3);
      cells.InsertNextCell(idList);
    }

    idList.Delete();

    return polydata;
  }

  /**
   * Read in PDS vertex file format. There are 2 variants of this file. In one the first line
   * contains the number of points and the number of cells and then follows the points and vertices.
   * In the other variant the first line only contains the number of points, then follows the
   * points, then follows a line listing the number of cells followed by the cells. Support both
   * variants here.
   *
   * @param filename
   * @return
   * @throws IOException
   */
  private static vtkPolyData loadPDSShapeModel(String filename) {
    try {
      List<String> lines = FileUtils.readLines(new File(filename), Charset.defaultCharset());
      return loadPDSShapeModel(lines);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage());
      return null;
    }
  }

  /**
   * Several PDS shape models are in special format similar to standard Gaskell vertex shape models
   * but are zero based and don't have a first column listing the id.
   *
   * @param filename
   * @param inMeters If true, vertices are assumed to be in meters. If false, assumed to be
   *     kilometers.
   * @return
   * @throws IOException
   */
  private static vtkPolyData loadTempel1AndWild2ShapeModel(String filename, boolean inMeters)
      throws Exception {
    vtkPolyData polydata = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray cells = new vtkCellArray();
    polydata.SetPoints(points);
    polydata.SetPolys(cells);

    InputStream fs = new FileInputStream(filename);
    InputStreamReader isr = new InputStreamReader(fs);
    BufferedReader in = new BufferedReader(isr);

    // Read in the first line which lists the number of points and plates
    String val = in.readLine().trim();
    String[] vals = val.split("\\s+");
    int numPoints = -1;
    int numCells = -1;
    if (vals.length == 2) {
      numPoints = Integer.parseInt(vals[0]);
      numCells = Integer.parseInt(vals[1]);
    } else {
      in.close();
      throw new IOException("Format not valid");
    }

    for (int j = 0; j < numPoints; ++j) {
      vals = in.readLine().trim().split("\\s+");
      double x = Double.parseDouble(vals[0]);
      double y = Double.parseDouble(vals[1]);
      double z = Double.parseDouble(vals[2]);

      if (inMeters) {
        x /= 1000.0;
        y /= 1000.0;
        z /= 1000.0;
      }

      points.InsertNextPoint(x, y, z);
    }

    vtkIdList idList = new vtkIdList();
    idList.SetNumberOfIds(3);
    for (int j = 0; j < numCells; ++j) {
      vals = in.readLine().trim().split("\\s+");
      int idx1 = Integer.parseInt(vals[0]);
      int idx2 = Integer.parseInt(vals[1]);
      int idx3 = Integer.parseInt(vals[2]);
      idList.SetId(0, idx1);
      idList.SetId(1, idx2);
      idList.SetId(2, idx3);
      cells.InsertNextCell(idList);
    }

    idList.Delete();

    in.close();

    return polydata;
  }

  public static vtkPolyData loadLocalFitsLLRModelN(double[][][] data) throws Exception {
    return loadLocalFitsLLRModelN(data, null, null, 0);
  }

  /**
   * Read in a FITS local shape model (i.e. a surface patch such as a maplet) with format where the
   * first 6 planes are lat, lon, radius, x, y, z and return the result as a vtkPolyData. Note only
   * the x, y, z planes are read to get the point data. The lat, lon, radius planes are ignored.
   *
   * <p>The user must also define the number of planes to skip before parsing the ancillary data and
   * storing it in List&lt;vtkFloatArray&gt;.
   *
   * <p>For example, if planesToSkip = 6 then any planes after the 6th plane will be stored and
   * returned in ancillaryData. The user may decide to set planesToSkip = 0 in which case ALL the
   * planes in the fits file will be returned in ancillaryData, including the first 6 planes (lat,
   * lon, radius, x, y, z).
   *
   * @param data
   * @param ancillaryData - list of vtkFloatArray found by parsing planes in the fits file. NOTE:
   *     Any prior content in ancillaryData will be cleared by the method and repopulated with
   *     results from the fits file!
   * @param planeNames
   * @param planesToSkip - integer number of the planes to skip before putting planes in
   *     ancillaryData
   * @return
   * @throws Exception
   */
  public static vtkPolyData loadLocalFitsLLRModelN(
      double[][][] data,
      List<vtkFloatArray> ancillaryData,
      List<String> planeNames,
      int planesToSkip)
      throws Exception {
    int xIndex = 3;
    int yIndex = 4;
    int zIndex = 5;

    vtkIdList idList = new vtkIdList();
    vtkPolyData dem = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    dem.SetPoints(points);
    dem.SetPolys(polys);

    int numExtraPlanes = data.length - planesToSkip;
    if (ancillaryData != null) {
      ancillaryData.clear();
      for (int i = 0; i < numExtraPlanes; ++i) {
        vtkFloatArray array = new vtkFloatArray();
        array.SetName(planeNames.get(i + planesToSkip));
        ancillaryData.add(array);
      }
    }

    int liveSizeX = data[0].length;
    int liveSizeY = data[0][0].length;

    int[][] indices = new int[liveSizeX][liveSizeY];
    int c = 0;
    int i0, i1, i2, i3;

    // First add points to the vtkPoints array
    for (int m = 0; m < liveSizeX; ++m)
      for (int n = 0; n < liveSizeY; ++n) {
        indices[m][n] = -1;

        double x = data[xIndex][m][n];
        double y = data[yIndex][m][n];
        double z = data[zIndex][m][n];

        boolean valid = (x != INVALID_VALUE && y != INVALID_VALUE && z != INVALID_VALUE);

        if (valid) {

          double[] pt = {x, y, z};
          points.InsertNextPoint(pt);

          if (ancillaryData != null) {
            for (int k = 0; k < numExtraPlanes; ++k)
              ancillaryData.get(k).InsertNextTuple1(data[planesToSkip + k][m][n]);
          }

          indices[m][n] = c;

          ++c;
        }
      }

    idList.SetNumberOfIds(3);

    // Now add connectivity information
    for (int m = 1; m < liveSizeX; ++m)
      for (int n = 1; n < liveSizeY; ++n) {
        // Get the indices of the 4 corners of the rectangle to the
        // upper left
        i0 = indices[m - 1][n - 1];
        i1 = indices[m][n - 1];
        i2 = indices[m - 1][n];
        i3 = indices[m][n];

        // Add upper left triangle
        if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
          idList.SetId(0, i0);
          idList.SetId(1, i1);
          idList.SetId(2, i2);
          polys.InsertNextCell(idList);
        }
        // Add bottom right triangle
        if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
          idList.SetId(0, i2);
          idList.SetId(1, i1);
          idList.SetId(2, i3);
          polys.InsertNextCell(idList);
        }
      }

    vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
    normalsFilter.SetInputData(dem);
    normalsFilter.SetComputeCellNormals(0);
    normalsFilter.SetComputePointNormals(1);
    normalsFilter.SplittingOff();
    normalsFilter.ConsistencyOn();
    normalsFilter.AutoOrientNormalsOff();
    if (needToFlipMapletNormalVectors(dem)) normalsFilter.FlipNormalsOn();
    else normalsFilter.FlipNormalsOff();
    normalsFilter.Update();

    vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
    dem.DeepCopy(normalsFilterOutput);

    return dem;
  }

  /**
   * Read in a shape model with format where each line in file consists of lat, lon, and radius, or
   * lon, lat, and radius. Note that most of the shape models of Thomas and Stooke in this format
   * use west longtude. The only exception is Thomas's Ida model which uses east longitude.
   *
   * @param filename
   * @param westLongitude if true, assume longitude is west, if false assume east
   * @return
   * @throws Exception
   */
  private static vtkPolyData loadLLRShapeModel(String filename, boolean westLongitude)
      throws Exception {
    // We need to load the file in 2 passes. In the first pass
    // we figure out the latitude/longitude spacing (both assumed same),
    // which column is latitude, and which column is longitude.
    //
    // It is assumed the following:
    // If 0 is the first field of the first column,
    // then longitude is the first column.
    // If -90 is the first field of the first column,
    // then latitude is the first column.
    // If 90 is the first field of the first column,
    // then latitude is the first column.
    //
    // These assumptions ensure that the shape models of Thomas, Stooke, and Hudson
    // are loaded in correctly. However, other shape models in some other lat, lon
    // scheme may not be loaded correctly with this function.
    //
    // In the second pass, we load the file using the values
    // determined in the first pass.

    // First pass
    double latLonSpacing = 0.0;
    int latIndex = 0;
    int lonIndex = 1;

    InputStream fs = new FileInputStream(filename);
    InputStreamReader isr = new InputStreamReader(fs);
    BufferedReader in = new BufferedReader(isr);

    {
      // We only need to look at the first 2 lines of the file
      // in the first pass to determine everything we need.
      String[] vals = in.readLine().trim().split("\\s+");
      double a1 = Double.parseDouble(vals[0]);
      double b1 = Double.parseDouble(vals[1]);
      vals = in.readLine().trim().split("\\s+");
      double a2 = Double.parseDouble(vals[0]);
      double b2 = Double.parseDouble(vals[1]);

      if (a1 == 0.0) {
        latIndex = 1;
        lonIndex = 0;
      } else if (a1 == -90.0 || a1 == 90.0) {
        latIndex = 0;
        lonIndex = 1;
      } else {
        System.err.println("loadLLRShapeModel: Incorrect format for input file");
      }

      if (a1 != a2) latLonSpacing = Math.abs(a2 - a1);
      else if (b1 != b2) latLonSpacing = Math.abs(b2 - b1);
      else System.err.println("loadLLRShapeModel: Incorrect format for input file");

      in.close();
    }

    // Second pass
    fs = new FileInputStream(filename);
    isr = new InputStreamReader(fs);
    in = new BufferedReader(isr);

    vtkPolyData body = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    body.SetPoints(points);
    body.SetPolys(polys);

    int numRows = (int) Math.round(180.0 / latLonSpacing) + 1;
    int numCols = (int) Math.round(360.0 / latLonSpacing) + 1;

    int count = 0;
    int[][] indices = new int[numRows][numCols];
    String line;
    while ((line = in.readLine()) != null) {
      String[] vals = line.trim().split("\\s+");
      double lat = Double.parseDouble(vals[latIndex]);
      double lon = Double.parseDouble(vals[lonIndex]);
      double rad = Double.parseDouble(vals[2]);

      int row = (int) Math.round((lat + 90.0) / latLonSpacing);
      int col = (int) Math.round(lon / latLonSpacing);

      // Only include 1 point at each pole and don't include any points
      // at longitude 360 since it's the same as longitude 0
      if ((lat == -90.0 && lon > 0.0) || (lat == 90.0 && lon > 0.0) || lon == 360.0) {
        indices[row][col] = -1;
      } else {
        if (westLongitude) lon = -lon;

        indices[row][col] = count++;
        UnwritableVectorIJK v =
            CoordConverters.convert(
                new LatitudinalVector(rad, Math.toRadians(lat), Math.toRadians(lon)));
        double[] pt = {v.getI(), v.getJ(), v.getK()};
        points.InsertNextPoint(pt);
      }
    }

    in.close();

    // Now add connectivity information
    int i0, i1, i2, i3;
    vtkIdList idList = new vtkIdList();
    idList.SetNumberOfIds(3);
    for (int m = 0; m <= numRows - 2; ++m)
      for (int n = 0; n <= numCols - 2; ++n) {
        // Add triangles touching south pole
        if (m == 0) {
          i0 = indices[m][0]; // index of south pole point
          i1 = indices[m + 1][n];
          if (n == numCols - 2) i2 = indices[m + 1][0];
          else i2 = indices[m + 1][n + 1];

          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building south pole facets.");
          }

        }
        // Add triangles touching north pole
        else if (m == numRows - 2) {
          i0 = indices[m + 1][0]; // index of north pole point
          i1 = indices[m][n];
          if (n == numCols - 2) i2 = indices[m][0];
          else i2 = indices[m][n + 1];

          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building north pole facets.");
          }
        }
        // Add middle triangles that do not touch either pole
        else {
          // Get the indices of the 4 corners of the rectangle to the upper right
          i0 = indices[m][n];
          i1 = indices[m + 1][n];
          if (n == numCols - 2) {
            i2 = indices[m][0];
            i3 = indices[m + 1][0];
          } else {
            i2 = indices[m][n + 1];
            i3 = indices[m + 1][n + 1];
          }

          // Add upper left triangle
          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building facets.");
          }

          // Add bottom right triangle
          if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
            idList.SetId(0, i2);
            idList.SetId(1, i1);
            idList.SetId(2, i3);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building facets.");
          }
        }
      }

    // vtkPolyDataWriter writer = new vtkPolyDataWriter();
    // writer.SetInput(body);
    // writer.SetFileName("/tmp/coneeros.vtk");
    //// writer.SetFileTypeToBinary();
    // writer.Write();

    return body;
  }

  /**
   * This function is used to load the Eros model based on NLR data available from
   * http://sbn.psi.edu/pds/resource/nearbrowse.html. It is very similar to the previous function
   * but with several subtle differences.
   *
   * @param filename
   * @param westLongitude
   * @return
   * @throws Exception
   */
  private static vtkPolyData loadLLR2ShapeModel(String filename, boolean westLongitude)
      throws Exception {
    double latLonSpacing = 1.0;
    int latIndex = 1;
    int lonIndex = 0;

    InputStream fs = new FileInputStream(filename);
    InputStreamReader isr = new InputStreamReader(fs);
    BufferedReader in = new BufferedReader(isr);

    vtkPolyData body = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    body.SetPoints(points);
    body.SetPolys(polys);

    int numRows = (int) Math.round(180.0 / latLonSpacing) + 2;
    int numCols = (int) Math.round(360.0 / latLonSpacing);

    int count = 0;
    int[][] indices = new int[numRows][numCols];
    String line;
    double[] northPole = {0.0, 0.0, 0.0};
    double[] southPole = {0.0, 0.0, 0.0};

    indices[0][0] = count++;
    points.InsertNextPoint(southPole); // placeholder for south pole

    while ((line = in.readLine()) != null) {
      String[] vals = line.trim().split("\\s+");
      double lat = Double.parseDouble(vals[latIndex]);
      double lon = Double.parseDouble(vals[lonIndex]);
      double rad = Double.parseDouble(vals[2]) / 1000.0;

      int row = (int) Math.round((lat + 89.5) / latLonSpacing) + 1;
      int col = (int) Math.round((lon - 0.5) / latLonSpacing);

      if (westLongitude) lon = -lon;

      indices[row][col] = count++;
      UnwritableVectorIJK v =
          CoordConverters.convert(
              new LatitudinalVector(rad, Math.toRadians(lat), Math.toRadians(lon)));
      double[] pt = {v.getI(), v.getJ(), v.getK()};
      points.InsertNextPoint(pt);

      // We need to compute the pole points (not included in the file)
      // by averaging the points at latitudes 89.5 and -89.5
      if (lat == -89.5) {
        southPole[0] += pt[0];
        southPole[1] += pt[1];
        southPole[2] += pt[2];
      } else if (lat == 89.5) {
        northPole[0] += pt[0];
        northPole[1] += pt[1];
        northPole[2] += pt[2];
      }
    }

    in.close();

    for (int i = 0; i < 3; ++i) {
      southPole[i] /= 360.0;
      northPole[i] /= 360.0;
    }

    points.SetPoint(0, southPole);

    indices[numRows - 1][0] = count++;
    points.InsertNextPoint(northPole); // north pole

    // Now add connectivity information
    int i0, i1, i2, i3;
    vtkIdList idList = new vtkIdList();
    idList.SetNumberOfIds(3);
    for (int m = 0; m <= numRows - 2; ++m)
      for (int n = 0; n <= numCols - 1; ++n) {
        // Add triangles touching south pole
        if (m == 0) {
          i0 = indices[m][0]; // index of south pole point
          i1 = indices[m + 1][n];
          if (n == numCols - 1) i2 = indices[m + 1][0];
          else i2 = indices[m + 1][n + 1];

          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building south pole facets.");
          }

        }
        // Add triangles touching north pole
        else if (m == numRows - 2) {
          i0 = indices[m + 1][0]; // index of north pole point
          i1 = indices[m][n];
          if (n == numCols - 1) i2 = indices[m][0];
          else i2 = indices[m][n + 1];

          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building north pole facets.");
          }
        }
        // Add middle triangles that do not touch either pole
        else {
          // Get the indices of the 4 corners of the rectangle to the upper right
          i0 = indices[m][n];
          i1 = indices[m + 1][n];
          if (n == numCols - 1) {
            i2 = indices[m][0];
            i3 = indices[m + 1][0];
          } else {
            i2 = indices[m][n + 1];
            i3 = indices[m + 1][n + 1];
          }

          // Add upper left triangle
          if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
            idList.SetId(0, i0);
            idList.SetId(1, i1);
            idList.SetId(2, i2);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building facets.");
          }

          // Add bottom right triangle
          if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
            idList.SetId(0, i2);
            idList.SetId(1, i1);
            idList.SetId(2, i3);
            polys.InsertNextCell(idList);
          } else {
            System.err.println("loadLLRShapeModel: Error building facets.");
          }
        }
      }

    return body;
  }

  private static vtkPolyData loadVTKShapeModel(String filename) throws Exception {
    vtkPolyDataReader smallBodyReader = new vtkPolyDataReader();
    smallBodyReader.SetFileName(filename);
    smallBodyReader.Update();

    vtkPolyData output = smallBodyReader.GetOutput();

    vtkPolyData shapeModel = new vtkPolyData();
    shapeModel.ShallowCopy(output);

    smallBodyReader.Delete();

    return shapeModel;
  }

  private static vtkPolyData loadOBJShapeModel(String filename) throws Exception {
    vtkOBJReader smallBodyReader = new vtkOBJReader();
    smallBodyReader.SetFileName(filename);
    smallBodyReader.Update();

    vtkPolyData output = smallBodyReader.GetOutput();

    vtkPolyData shapeModel = new vtkPolyData();
    shapeModel.ShallowCopy(output);

    smallBodyReader.Delete();

    return shapeModel;
  }

  private static vtkPolyData loadPLYShapeModel(String filename) throws Exception {
    vtkPLYReader smallBodyReader = new vtkPLYReader();
    smallBodyReader.SetFileName(filename);
    smallBodyReader.Update();

    vtkPolyData output = smallBodyReader.GetOutput();

    vtkPolyData shapeModel = new vtkPolyData();
    shapeModel.ShallowCopy(output);

    smallBodyReader.Delete();

    return shapeModel;
  }

  private static vtkPolyData loadSTLShapeModel(String filename) throws Exception {
    vtkSTLReader smallBodyReader = new vtkSTLReader();
    smallBodyReader.SetFileName(filename);
    smallBodyReader.Update();

    vtkPolyData output = smallBodyReader.GetOutput();

    vtkPolyData shapeModel = new vtkPolyData();
    shapeModel.ShallowCopy(output);

    smallBodyReader.Delete();

    return shapeModel;
  }

  public static vtkPolyData loadFITShapeModel(String filename) throws Exception {
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    vtkPolyData shapeModel = new vtkPolyData();
    vtkIdList idList = new vtkIdList();
    shapeModel.SetPoints(points);
    shapeModel.SetPolys(polys);

    Fits f = new Fits(filename);
    BasicHDU hdu = f.getHDU(0);

    // First pass, figure out number of planes and grab size and scale information
    Header header = hdu.getHeader();
    HeaderCard headerCard;
    int xIdx = -1;
    int yIdx = -1;
    int zIdx = -1;
    int planeCount = 0;
    while ((headerCard = header.nextCard()) != null) {
      String headerKey = headerCard.getKey();
      String headerValue = headerCard.getValue();

      if (headerKey.startsWith("PLANE")) {
        // Determine if we are looking at a coordinate or a backplane
        if (headerValue.startsWith("X")) {
          // This plane is the X coordinate, save the index
          xIdx = planeCount;
        } else if (headerValue.startsWith("Y")) {
          // This plane is the Y coordinate, save the index
          yIdx = planeCount;
        } else if (headerValue.startsWith("Z")) {
          // This plane is the Z coordinate, save the index
          zIdx = planeCount;
        }

        // Increment plane count
        planeCount++;
      }
    }

    // Check to see if x,y,z planes were all defined
    if (xIdx < 0) {
      throw new IOException("FITS file does not contain plane for X coordinate");
    } else if (yIdx < 0) {
      throw new IOException("FITS file does not contain plane for Y coordinate");
    } else if (zIdx < 0) {
      throw new IOException("FITS file does not contain plane for Z coordinate");
    }

    // Check dimensions of actual data
    int[] axes = hdu.getAxes();
    if (axes.length != 3 || axes[1] != axes[2]) {
      throw new IOException("FITS file has incorrect dimensions");
    }

    int liveSize = axes[1];

    float[][][] data = (float[][][]) hdu.getData().getData();
    f.getStream().close();

    int[][] indices = new int[liveSize][liveSize];
    int c = 0;
    float x, y, z;
    float INVALID_VALUE = -1.0e38f;

    // First add points to the vtkPoints array
    for (int m = 0; m < liveSize; ++m)
      for (int n = 0; n < liveSize; ++n) {
        indices[m][n] = -1;

        // A pixel value of -1.0e38 means that pixel is invalid and should be skipped
        x = data[xIdx][m][n];
        y = data[yIdx][m][n];
        z = data[zIdx][m][n];

        // Check to see if x,y,z values are all valid
        boolean valid = x != INVALID_VALUE && y != INVALID_VALUE && z != INVALID_VALUE;

        // Only add point if everything is valid
        if (valid) {
          points.InsertNextPoint(x, y, z);
          indices[m][n] = c;
          ++c;
        }
      }

    idList.SetNumberOfIds(3);

    // Now add connectivity information
    int i0, i1, i2, i3;
    for (int m = 1; m < liveSize; ++m)
      for (int n = 1; n < liveSize; ++n) {
        // Get the indices of the 4 corners of the rectangle to the upper left
        i0 = indices[m - 1][n - 1];
        i1 = indices[m][n - 1];
        i2 = indices[m - 1][n];
        i3 = indices[m][n];

        // Add upper left triangle
        if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
          idList.SetId(0, i0);
          idList.SetId(1, i2);
          idList.SetId(2, i1);
          polys.InsertNextCell(idList);
        }
        // Add bottom right triangle
        if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
          idList.SetId(0, i2);
          idList.SetId(1, i3);
          idList.SetId(2, i1);
          polys.InsertNextCell(idList);
        }
      }

    vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
    normalsFilter.SetInputData(shapeModel);
    normalsFilter.SetComputeCellNormals(0);
    normalsFilter.SetComputePointNormals(1);
    normalsFilter.SplittingOff();
    normalsFilter.FlipNormalsOn();
    normalsFilter.Update();

    vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
    shapeModel.DeepCopy(normalsFilterOutput);

    return shapeModel;
  }

  /**
   * This function loads a shape model in a variety of formats. It looks at the file extension to
   * determine its format. It supports these formats:
   *
   * <ul>
   *   <li>VTK (.vtk extension)
   *   <li>OBJ (.obj extension)
   *   <li>ICQ (.icq) Bob Gaskell's ICQ format
   *   <li>PDS vertex style shape models (.pds, .plt, or .tab extension)
   *   <li>Lat, lon, radius format also used in PDS shape models (.llr extension)
   *   <li>PLY (.ply extension)
   *   <li>STL (.stl extension)
   * </ul>
   *
   * <p>If you want VTK to compute the normals (this may cause the vertex indices per facet to be
   * reordered), use {@link #loadShapeModelAndComputeNormals(String)} instead.
   *
   * @param filename
   * @return
   * @throws Exception
   */
  public static vtkPolyData loadShapeModel(String filename) throws Exception {
    if (!new File(filename).exists()) {
      logger.warn("loadShapeModel: cannot load " + filename);
      return null;
    }

    String ext = FilenameUtils.getExtension(filename).toLowerCase();
    return loadShapeModel(filename, ext);
  }

  /**
   * This function loads a shape model in a variety of formats.
   *
   * <ul>
   *   <li>VTK (.vtk extension) *
   *   <li>OBJ (.obj extension) *
   *   <li>ICQ (.icq) Bob Gaskell's ICQ format *
   *   <li>PDS vertex style shape models (.pds, .plt, or .tab extension) *
   *   <li>Lat, lon, radius format also used in PDS shape models (.llr extension) *
   *   <li>PLY (.ply extension) *
   *   <li>STL (.stl extension) *
   * </ul>
   *
   * @param filename
   * @param ext
   * @return
   * @throws Exception
   */
  public static vtkPolyData loadShapeModel(String filename, String ext) throws Exception {
    if (!new File(filename).exists()) {
      logger.warn("loadShapeModel: cannot load " + filename);
      return null;
    }

    ext = ext.toLowerCase();

    vtkPolyData shapeModel = new vtkPolyData();
    if (ext.equals("vtk")) {
      shapeModel = loadVTKShapeModel(filename);
    } else if (ext.equals("icq")) {
      shapeModel = loadPDSShapeModel(icq2plt(filename));
    } else if (ext.equals("obj") || ext.equals("wf")) {
      shapeModel = loadOBJShapeModel(filename);
    } else if (ext.equals("pds") || ext.equals("plt") || ext.equals("tab")) {
      shapeModel = loadPDSShapeModel(filename);
    } else if (ext.equals("llr")) {
      boolean westLongitude = true;
      // Thomas's Ida shape model uses east longitude. All the others use west longitude.
      // TODO rather than hard coding this check in, need better way to decide if model
      // uses west or east longitude.
      if (filename.toLowerCase().contains("thomas") && filename.toLowerCase().contains("243ida"))
        westLongitude = false;
      shapeModel = loadLLRShapeModel(filename, westLongitude);
    } else if (ext.equals("llr2")) {
      shapeModel = loadLLR2ShapeModel(filename, false);
    } else if (ext.equals("t1")) {
      shapeModel = loadTempel1AndWild2ShapeModel(filename, false);
    } else if (ext.equals("w2")) {
      shapeModel = loadTempel1AndWild2ShapeModel(filename, true);
    } else if (ext.equals("ply")) {
      shapeModel = loadPLYShapeModel(filename);
    } else if (ext.equals("stl")) {
      shapeModel = loadSTLShapeModel(filename);
    } else {
      logger.warn("Unknown extension: " + FilenameUtils.getExtension(filename));
      return null;
    }
    return shapeModel;
  }

  /**
   * This function loads a shape model in a variety of formats. It looks at the file extension to
   * determine its format. It supports these formats:
   *
   * <ul>
   *   <li>VTK (.vtk extension)
   *   <li>OBJ (.obj extension)
   *   <li>PDS vertex style shape models (.pds, .plt, or .tab extension)
   *   <li>Lat, lon, radius format also used in PDS shape models (.llr extension)
   *   <li>PLY (.ply extension)
   *   <li>STL (.stl extension)
   * </ul>
   *
   * This function also adds normal vectors to the returned polydata, if not available in the file.
   *
   * <p>ONLY TO BE USED WITH CLOSED SHAPE MODELS! Otherwise, "all bets are off" according to the VTK
   * documentation. VTK will reorder the vertex indices for a facet if it thinks the normal is
   * pointing in the "wrong" direction, which may not be what you want to happen. For a local shape
   * model, or if you don't care about the VTK computed normals (this is probably most of the time),
   * use {@link #loadShapeModel(String)}.
   *
   * @param filename
   * @return
   * @throws Exception
   */
  public static vtkPolyData loadShapeModelAndComputeNormals(String filename) throws Exception {
    vtkPolyData shapeModel = loadShapeModel(filename);
    addPointNormalsToShapeModel(shapeModel);
    return shapeModel;
  }

  /**
   * True if the mean normal and centroid vectors point in opposite directions. Only meaningful for
   * local shape models.
   *
   * @param polydata
   * @return
   */
  public static boolean needToFlipMapletNormalVectors(vtkPolyData polydata) {
    Vector3D normal = computeMeanPolyDataNormal(polydata);
    Vector3D centroid = computePolyDataCentroid(polydata);
    return normal.dotProduct(centroid) < 0;
  }

  /**
   * This method removes any duplicate vertices and renumbers the facet index map. The order of
   * vertices is preserved.
   *
   * @param polyDataIn
   * @throws Exception
   */
  public static vtkPolyData removeDuplicatePoints(vtkPolyData polyDataIn) throws Exception {

    Map<Integer, List<UnwritableVectorIJK>> cellPointMap = new HashMap<>();
    vtkIdList idList = new vtkIdList();
    double[] pt = new double[3];
    for (int i = 0; i < polyDataIn.GetNumberOfCells(); i++) {
      polyDataIn.GetCellPoints(i, idList);
      List<UnwritableVectorIJK> cellPoints = new ArrayList<>();
      for (int j = 0; j < 3; j++) {
        polyDataIn.GetPoint(idList.GetId(j), pt);
        cellPoints.add(new UnwritableVectorIJK(pt));
      }
      cellPointMap.put(i, cellPoints);
    }

    // lookup table from vertex to index
    LinkedHashMap<UnwritableVectorIJK, Integer> pointMap = new LinkedHashMap<>();
    for (Integer key : cellPointMap.keySet()) {
      List<UnwritableVectorIJK> cellPoints = cellPointMap.get(key);
      for (UnwritableVectorIJK point : cellPoints) {
        if (!pointMap.containsKey(point)) {
          pointMap.put(point, pointMap.size());
        }
      }
    }

    vtkPolyData body = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    body.SetPoints(points);
    body.SetPolys(polys);

    List<UnwritableVectorIJK> pointList = new ArrayList<>(pointMap.keySet());
    for (int i = 0; i < pointList.size(); i++) {
      UnwritableVectorIJK point = pointList.get(i);
      points.InsertNextPoint(point.getI(), point.getJ(), point.getK());
    }

    for (Integer i : cellPointMap.keySet()) {
      List<UnwritableVectorIJK> cellPoints = cellPointMap.get(i);
      idList = new vtkIdList();
      for (UnwritableVectorIJK cellPoint : cellPoints) idList.InsertNextId(pointMap.get(cellPoint));
      polys.InsertNextCell(idList);
    }

    return body;
  }

  /**
   * Take the input polydata and create a new polydata with only vertices that are part of facets.
   *
   * @param polyDataIn
   * @return
   */
  public static vtkPolyData removeUnreferencedPoints(vtkPolyData polyDataIn) {

    vtkIdList idList = new vtkIdList();

    // build the set of vertices referenced by any facet
    NavigableSet<Long> vertexIndices = new TreeSet<>();
    for (int i = 0; i < polyDataIn.GetNumberOfCells(); i++) {
      polyDataIn.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);

      vertexIndices.add(id0);
      vertexIndices.add(id1);
      vertexIndices.add(id2);
    }

    vtkPolyData body = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    body.SetPoints(points);
    body.SetPolys(polys);

    // create list of points that are referenced by at least one facet, and
    // a map from new to old vertex index
    NavigableMap<Long, Integer> indexMap = new TreeMap<>();
    double[] pt = new double[3];
    for (Long index : vertexIndices) {
      polyDataIn.GetPoint(index, pt);
      points.InsertNextPoint(pt);
      indexMap.put(index, indexMap.size());
    }

    for (int i = 0; i < polyDataIn.GetNumberOfCells(); i++) {
      polyDataIn.GetCellPoints(i, idList);
      idList.SetId(0, indexMap.get(idList.GetId(0)));
      idList.SetId(1, indexMap.get(idList.GetId(1)));
      idList.SetId(2, indexMap.get(idList.GetId(2)));
      polys.InsertNextCell(idList);
    }

    return body;
  }

  public static vtkPolyData removeZeroAreaFacets(vtkPolyData polyDataIn) {
    vtkPolyData body = new vtkPolyData();
    vtkPoints points = new vtkPoints();
    vtkCellArray polys = new vtkCellArray();
    body.SetPoints(points);
    body.SetPolys(polys);

    double[] pt = new double[3];
    for (int i = 0; i < polyDataIn.GetNumberOfPoints(); i++) {
      polyDataIn.GetPoint(i, pt);
      points.InsertNextPoint(pt);
    }

    vtkIdList idList = new vtkIdList();
    double[] pt0 = new double[3];
    double[] pt1 = new double[3];
    double[] pt2 = new double[3];

    for (int i = 0; i < polyDataIn.GetNumberOfCells(); ++i) {
      polyDataIn.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      polyDataIn.GetPoint(id0, pt0);
      polyDataIn.GetPoint(id1, pt1);
      polyDataIn.GetPoint(id2, pt2);

      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
      double area = facet.getArea();
      if (area > 0) polys.InsertNextCell(idList);
    }

    return body;
  }

  public static void saveShapeModelAsPLT(vtkPolyData polyData, String filename) throws IOException {
    // This saves it out in exactly the same format as Bob Gaskell's shape
    // models including precision and field width. That's why there's
    // extra space padded at the end to make all lines the same length.

    FileWriter fstream = new FileWriter(filename);
    BufferedWriter out = new BufferedWriter(fstream);

    vtkPoints points = polyData.GetPoints();

    long numberPoints = polyData.GetNumberOfPoints();
    long numberCells = polyData.GetNumberOfCells();
    out.write(
        String.format("%12d %12d                              \r\n", numberPoints, numberCells));

    double[] p = new double[3];
    for (int i = 0; i < numberPoints; ++i) {
      points.GetPoint(i, p);
      out.write(String.format("%10d%15.5f%15.5f%15.5f\r\n", (i + 1), p[0], p[1], p[2]));
    }

    polyData.BuildCells();
    vtkIdList idList = new vtkIdList();
    for (int i = 0; i < numberCells; ++i) {
      polyData.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      out.write(
          String.format(
              "%10d%10d%10d%10d               \r\n", (i + 1), (id0 + 1), (id1 + 1), (id2 + 1)));
    }

    idList.Delete();
    out.close();
  }

  public static void saveShapeModelAsOBJ(vtkPolyData polyData, String filename)
      throws FileNotFoundException, IOException {
    saveShapeModelAsOBJ(polyData, filename, null);
  }

  public static void saveShapeModelAsOBJ(vtkPolyData polyData, String filename, String header)
      throws FileNotFoundException, IOException {
    try (FileOutputStream fos = new FileOutputStream(new File(filename))) {
      saveShapeModelAsOBJ(polyData, fos, header);
    }
  }

  public static void saveShapeModelAsOBJ(vtkPolyData polyData, OutputStream stream)
      throws IOException {
    saveShapeModelAsOBJ(polyData, stream, null);
  }

  public static void saveShapeModelAsOBJ(vtkPolyData polyData, OutputStream stream, String header)
      throws IOException {
    OutputStreamWriter fstream = new OutputStreamWriter(stream);
    BufferedWriter out = new BufferedWriter(fstream);

    if (header != null) out.write(header);

    vtkPoints points = polyData.GetPoints();

    long numberPoints = polyData.GetNumberOfPoints();
    long numberCells = polyData.GetNumberOfCells();

    double[] p = new double[3];
    for (int i = 0; i < numberPoints; ++i) {
      points.GetPoint(i, p);
      out.write(String.format("v %20.16f %20.16f %20.16f\r\n", p[0], p[1], p[2]));
    }

    polyData.BuildCells();
    vtkIdList idList = new vtkIdList();
    for (int i = 0; i < numberCells; ++i) {
      polyData.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      out.write(String.format("%-10s%10d%10d%10d\r\n", "f", id0 + 1, id1 + 1, id2 + 1));
    }

    idList.Delete();
    out.close();
  }

  public static void saveShapeModelAsVTK(vtkPolyData polyData, String filename) throws IOException {
    // First make a copy of polydata and remove all cell and point data since we don't want to save
    // that out
    vtkPolyData newpolydata = new vtkPolyData();
    newpolydata.DeepCopy(polyData);
    newpolydata.GetPointData().Reset();
    newpolydata.GetCellData().Reset();

    // regenerate point normals
    vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
    normalsFilter.SetInputData(newpolydata);
    normalsFilter.SetComputeCellNormals(0);
    normalsFilter.SetComputePointNormals(1);
    normalsFilter.AutoOrientNormalsOn();
    normalsFilter.SplittingOff();
    normalsFilter.Update();

    vtkPolyDataWriter writer = new vtkPolyDataWriter();
    writer.SetInputConnection(normalsFilter.GetOutputPort());
    writer.SetFileName(filename);
    writer.SetFileTypeToBinary();
    writer.Write();
  }

  public static void saveShapeModelAsSTL(vtkPolyData polyData, String filename) throws IOException {
    // First make a copy of polydata and remove all cell and point data since we don't want to save
    // that out
    vtkPolyData newpolydata = new vtkPolyData();
    newpolydata.DeepCopy(polyData);
    newpolydata.GetPointData().Reset();
    newpolydata.GetCellData().Reset();

    vtkSTLWriter writer = new vtkSTLWriter();
    writer.SetInputData(newpolydata);
    writer.SetFileName(filename);
    writer.SetFileTypeToBinary();
    writer.Write();
  }

  public static void writeVTKPoints(String outFile, List<Vector3D> points) {

    vtkPoints pointsXYZ = new vtkPoints();
    for (Vector3D point : points) {
      double[] array = new double[] {point.getX(), point.getY(), point.getZ()};
      pointsXYZ.InsertNextPoint(array);
    }

    vtkPolyData polyData = new vtkPolyData();
    polyData.SetPoints(pointsXYZ);

    vtkCellArray cells = new vtkCellArray();
    polyData.SetPolys(cells);

    for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
      vtkIdList idList = new vtkIdList();
      idList.InsertNextId(i);
      cells.InsertNextCell(idList);
    }

    vtkPolyDataWriter writer = new vtkPolyDataWriter();
    writer.SetInputData(polyData);
    writer.SetFileName(outFile);
    writer.SetFileTypeToBinary();
    writer.Update();
  }
}
