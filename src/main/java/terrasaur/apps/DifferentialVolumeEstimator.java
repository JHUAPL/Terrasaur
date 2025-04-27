package terrasaur.apps;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.vectorspace.RotationMatrixIJK;
import terrasaur.smallBodyModel.SBMTStructure;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

/**
 * Given a reference surface (either from a shape model or a set of points), find a best fit
 * reference plane. Find the height above this plane for the reference surface and an input shape
 * model. Report the differential height and volume between these two surfaces on a uniform grid.
 * <p>
 * This class uses three coordinate systems:
 * <ul>
 * <li>The global coordinate system. This is the coordinate system of the input data.</li>
 * <li>The local coordinate system. This has the X and Y axes in the best fit plane to the reference
 * data. The origin is optionally set by the user.</li>
 * <li>The native coordinate system. This is not seen by the user. The XY plane is the same as the
 * local coordinate system, but the origin may be translated and there may be a rotation applied
 * about the Z axis.</li>
 * </ul>
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class DifferentialVolumeEstimator implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  private DifferentialVolumeEstimator() {}


  @Override
  public String shortDescription() {
      return "Find volume difference between two shape models.";
  }

  // degree of polynomial used to fit surface
  private final int POLYNOMIAL_DEGREE = 2;

  @Override
  public String fullDescription(Options options) {
		String header = "";
		String footer = "\nThis program finds the volume difference between a shape model and a reference surface.  "
		    +"The reference surface can either be another shape model or a degree "+POLYNOMIAL_DEGREE+" fit to a set of supplied points.  "+
		    "A local coordinate system is derived from the reference surface.  The heights of the shape and reference at "+
		    "each grid point are reported. ";
    return TerrasaurTool.super.fullDescription(options, header, footer);

  }



  /** input shape model */
  private vtkPolyData globalPolyData;
  /** shape model in native coordinates */
  private SmallBodyModel nativeSBM;
  /** number of radial profiles */
  private Integer numProfiles;

  public void setNumProfiles(Integer numProfiles) {
    this.numProfiles = numProfiles;
  }

  /** true if local +Z is aligned with the center radial direction */
  private boolean radialUp;

  public void setRadialUp(boolean radialUp) {
    this.radialUp = radialUp;
  }

  /** reference points in global coordinates */
  private List<Vector3D> referencePoints;

  public void setReferencePoints(List<Vector3D> referencePoints) {
    this.referencePoints = referencePoints;
  }

  /** reference model in global coordinates */
  private vtkPolyData referencePolyData;

  public void setReferencePolyData(vtkPolyData referencePolyData) {
    this.referencePolyData = referencePolyData;
  }

  /** Reference shape in native coordinates */
  private SmallBodyModel referenceSBM;
  /** reference surface in native coordinates */
  private FitSurface referenceSurface;

  private double gridSpacing;
  private double gridHalfExtent;

  /** global coordinates of the highest point of the shape model */
  private Vector3D highPoint;
  /** global coordinates of the lowest point of the shape model */
  private Vector3D lowPoint;

  /** this plane converts coordinates from native to global and back */
  private FitPlane plane;

  /** Inner edge of the ROI */
  private Path2D.Double roiInner;
  /** Outer edge of the ROI */
  private Path2D.Double roiOuter;

  // local grid is in the same plane as native grid but is translated and rotated
  private Entry<Rotation, Vector3D> nativeToLocal;

  // the origin of the local coordinate system, in global coordinates
  private enum ORIGIN {
    MIN_HEIGHT, MAX_HEIGHT, CUSTOM, DEFAULT
  }

  public Vector3D nativeToLocal(Vector3D nativeIJK) {
      return nativeToLocal.getKey().applyTo(nativeIJK.subtract(nativeToLocal.getValue()));
  }

  public Vector3D localToNative(Vector3D local) {
      return nativeToLocal.getKey().applyInverseTo(local).add(nativeToLocal.getValue());
  }

  /**
   * Set the inner boundary of the ROI
   * 
   * @param filename file containing points in global coordinates
   */
  public void setInnerROI(String filename) {
    List<Vector3D> points = readPointsFromFile(filename);
    roiInner = createOutline(points);
  }

  /**
   * Set the outer boundary of the ROI
   * 
   * @param filename file containing points in global coordinates
   */
  public void setOuterROI(String filename) {
    List<Vector3D> points = readPointsFromFile(filename);
    roiOuter = createOutline(points);
  }

  /**
   * Construct an outline on the local grid from a list of points in global coordinates.
   * 
   * @param points points in global coordinates
   * @return outline on local grid
   */
  private Path2D.Double createOutline(List<Vector3D> points) {

    Path2D.Double outline = new Path2D.Double();
    for (int i = 0; i < points.size(); i++) {
      Vector3D nativeIJK = plane.globalToLocal(points.get(i));
      Vector3D localIJK =
          nativeToLocal.getKey().applyTo(nativeIJK.subtract(nativeToLocal.getValue()));
      if (i == 0) {
        outline.moveTo(localIJK.getX(), localIJK.getY());
      } else {
        outline.lineTo(localIJK.getX(), localIJK.getY());
      }
    }
    outline.closePath();

    return outline;
  }

  public DifferentialVolumeEstimator(vtkPolyData polyData) {
    this.globalPolyData = polyData;
    this.referencePolyData = null;
    this.referencePoints = null;
    this.numProfiles = 0;
    this.radialUp = false;
  }

  /**
   * Get the height of the shape model above the reference plane
   * 
   * @param x in native coordinates
   * @param y in native coordinates
   * @return height, or {@link Double#NaN} if no intersection found
   */
  public double getHeight(double x, double y) {

    double height = Double.NaN;
    double[] origin = {x, y, 0};
    double[] direction = {0, 0, 1};
    double[] intersect = new double[3];

    long cellID = nativeSBM.computeRayIntersection(origin, direction, intersect);
    if (cellID < 0) {
      direction[2] = -1;
      cellID = nativeSBM.computeRayIntersection(origin, direction, intersect);
    }
    if (cellID >= 0)
      height = direction[2] * new Vector3D(origin).distance(new Vector3D(intersect));

    if (Double.isNaN(height))
      return Double.NaN;

    return height;
  }

  /**
   * Get the height of the reference surface above the reference plane.
   * 
   * @param x in native coordinates
   * @param y in native coordinates
   * @return height of surface at (x,y) above reference plane
   */
  public double getRefHeight(double x, double y) {

    double[] origin = {x, y, 0};
    double[] direction = {0, 0, 1};
    double[] intersect;

    double refHeight = Double.NaN;
    if (referenceSBM != null) {
      intersect = new double[3];

      long cellID = referenceSBM.computeRayIntersection(origin, direction, intersect);
      if (cellID < 0) {
        direction[2] = -1;
        cellID = referenceSBM.computeRayIntersection(origin, direction, intersect);
      }
      if (cellID >= 0) {
        refHeight = direction[2] * new Vector3D(origin).distance(new Vector3D(intersect));
      }
    } else {
      refHeight = referenceSurface.value(x, y);
    }

    if (Double.isNaN(refHeight))
      return Double.NaN;

    return refHeight;
  }

  /**
   * Create an array of grid points with heights
   * 
   * @param gridHalfExtent half-size of grid
   * @param gridSpacing spacing between points
   * @return grid points sorted by x coordinate, then y
   */
  private NavigableSet<GridPoint> createGrid(double gridHalfExtent, double gridSpacing) {
    Set<Vector3D> localGrid = new HashSet<>();

    for (double x = 0; x <= gridHalfExtent; x += gridSpacing) {
      for (double y = 0; y <= gridHalfExtent; y += gridSpacing) {
        localGrid.add(new Vector3D(x, y, 0));
        if (y != 0)
          localGrid.add(new Vector3D(x, -y, 0));
        if (x != 0)
          localGrid.add(new Vector3D(-x, y, 0));
        if (x != 0 && y != 0)
          localGrid.add(new Vector3D(-x, -y, 0));
      }
    }
    this.gridSpacing = gridSpacing;
    this.gridHalfExtent=gridHalfExtent;

    GridPoint highGridPoint = null;
    GridPoint lowGridPoint = null;
    NavigableSet<GridPoint> gridPoints = new TreeSet<>();
    for (Vector3D localPoint : localGrid) {
      GridPoint gp = new GridPoint(localPoint);
      gridPoints.add(gp);
      if (Double.isFinite(gp.height)) {
        if (highGridPoint == null || highGridPoint.height < gp.height)
          highGridPoint = gp;
        if (lowGridPoint == null || lowGridPoint.height > gp.height)
          lowGridPoint = gp;
      }
    }

    if (highGridPoint != null)
      highPoint = highGridPoint.globalIJK;
    if (lowGridPoint != null)
      lowPoint = lowGridPoint.globalIJK;

    return gridPoints;
  }

  /**
   * Create the reference surface, either from a fit to a set of points or from an input shape
   * model.
   * 
   * @param localOriginInGlobalCoordinates local origin in global coordinates
   */
  public void createReference(Vector3D localOriginInGlobalCoordinates) {

    double[] pt = new double[3];

    if (referencePolyData != null) {
      if (referencePoints != null) {
        logger.warn(
            "Both -referenceList and -referenceShape were specified.  Reference surface will be set to argument of -referenceShape.");
      }
      referencePoints = new ArrayList<>();
      for (int i = 0; i < referencePolyData.GetNumberOfPoints(); i++) {
        referencePolyData.GetPoint(i, pt);
        referencePoints.add(new Vector3D(pt));
      }
    }

    // this is the best fit plane to the reference points. It can convert points in input
    // (global) coordinates to native coordinates and vice versa
    plane = new FitPlane(referencePoints);

    // set the +Z direction for the local plane
    Vector3D referenceNormal = radialUp ? plane.getTransform().getValue() : Vector3D.PLUS_K;

    // check if the plane normal is pointing in the same direction as the reference normal. If not,
    // flip the plane
    Pair<Rotation, Vector3D> transform = plane.getTransform();
    Vector3D planeNormal = transform.getKey().applyInverseTo(referenceNormal);
    if (planeNormal.dotProduct(referenceNormal) < 0)
      plane = plane.reverseNormal();

    // create the SmallBodyModel for the shape to evaluate
    vtkPolyData nativePolyData = new vtkPolyData();
    nativePolyData.DeepCopy(globalPolyData);
    vtkPoints points = nativePolyData.GetPoints();
    for (int i = 0; i < points.GetNumberOfPoints(); i++) {
      points.GetPoint(i, pt);
      Vector3D nativePoint = plane.globalToLocal(new Vector3D(pt));
      double[] data = nativePoint.toArray();
      points.SetPoint(i, data);
    }
    nativeSBM = new SmallBodyModel(nativePolyData);

    // now define the reference shape/surface
    if (referencePolyData != null) {
      // create the SmallBodyModel for the reference shape
      nativePolyData = new vtkPolyData();
      nativePolyData.DeepCopy(referencePolyData);
      points = nativePolyData.GetPoints();
      for (int i = 0; i < points.GetNumberOfPoints(); i++) {
        points.GetPoint(i, pt);
        Vector3D nativePoint = plane.globalToLocal(new Vector3D(pt));
        double[] data = nativePoint.toArray();
        points.SetPoint(i, data);
      }

      referenceSBM = new SmallBodyModel(nativePolyData);
    } else {
      // create the reference surface
      List<Vector3D> nativePoints = new ArrayList<>();
      for (Vector3D v : referencePoints) {
        Vector3D nativePoint = plane.globalToLocal(v);
        nativePoints.add(nativePoint);
      }

      referenceSurface = new FitSurface(nativePoints, POLYNOMIAL_DEGREE);
    }

    // create a rotation matrix to go from native to local (where the Z axis is the same for both
    // and the X axis is aligned in the same direction as the global X axis)
    Pair<Rotation, Vector3D> globalToLocalTransform = plane.getTransform();
    Vector3D kRow = Vector3D.PLUS_K;
    Vector3D iRow = globalToLocalTransform.getKey().applyTo(Vector3D.PLUS_I);
    Vector3D jRow = Vector3D.crossProduct(kRow, iRow).normalize();
    kRow = Vector3D.crossProduct(iRow, jRow).normalize();
    iRow = iRow.normalize();

    Vector3D translateNativeToLocal = Vector3D.ZERO;
    if (localOriginInGlobalCoordinates.getNorm() > 0) {
      // translation to go from native to local (where localOriginInGlobalCoordinates defines 0,0 in
      // the local frame)
      Vector3D nativeOriginInGlobalCoordinates = plane.localToGlobal(Vector3D.ZERO);
      Vector3D translateNativeToLocalInGlobalCoordinates =
          localOriginInGlobalCoordinates.subtract(nativeOriginInGlobalCoordinates);
      // TODO: check that the Z component is zero (it should be?)
      translateNativeToLocal =
          globalToLocalTransform.getKey().applyTo(translateNativeToLocalInGlobalCoordinates);
    }

    Rotation rotateNativeToLocal =
        MathConversions.toRotation(new RotationMatrixIJK(iRow.getX(), jRow.getX(), kRow.getX(),
            iRow.getY(), jRow.getY(), kRow.getY(), iRow.getZ(), jRow.getZ(), kRow.getZ()));

    this.nativeToLocal = new AbstractMap.SimpleEntry<>(rotateNativeToLocal, translateNativeToLocal);
  }

  /**
   * The header for grid and profile CSV files. Each line begins with a #
   * 
   * @param header string at beginning of header
   * @return complete header
   */
  public static String getHeader(String header) {
    StringBuffer sb = new StringBuffer();
    sb.append(header);
    sb.append("# Local X and Y are grid coordinates in the local reference frame\n");
    sb.append("# Angle is measured from the local X axis, in degrees\n");
    sb.append("# ROI flag is 1 if point is in the region of interest, 0 if not\n");
    sb.append("# Global X, Y, and Z are the local grid points in the global "
        + " (input) reference system\n");
    sb.append("# Reference Height is the height of the reference model (or fit surface) above "
        + "the local grid plane\n");
    sb.append("# Model Height is the height of the shape model above the local grid plane.  "
        + "NaN means there is no model intersection at this grid point.\n");
    sb.append("# Bin volume is the grid cell area times the model - reference height\n");
    sb.append("#\n");
    sb.append(String.format("%s, ", "Local X"));
    sb.append(String.format("%s, ", "Local Y"));
    sb.append(String.format("%s, ", "Angle"));
    sb.append(String.format("%s, ", "ROI Flag"));
    sb.append(String.format("%s, ", "Global X"));
    sb.append(String.format("%s, ", "Global Y"));
    sb.append(String.format("%s, ", "Global Z"));
    sb.append(String.format("%s, ", "Reference Height"));
    sb.append(String.format("%s, ", "Model Height"));
    sb.append(String.format("%s, ", "Model - Reference"));
    sb.append(String.format("%s", "Bin Volume"));
    return sb.toString();
  }


  /**
   * The header for the sector CSV file. Each line begins with a #
   *
   * @param header string at beginning of header
   * @return complete header
   */
  public static String getSectorHeader(String header) {
    StringBuilder sb = new StringBuilder();
    sb.append(header);
    sb.append("# Angle is measured from the local X axis, in degrees\n");
    sb.append(
        "# Sector volume is the grid cell area times the model - reference height summed over all grid cells in the ROI.\n");
    sb.append("#\n");
    sb.append(String.format("%s, ", "Index"));
    sb.append(String.format("%s, ", "Start angle (degrees)"));
    sb.append(String.format("%s, ", "Stop angle (degrees)"));
    sb.append(String.format("%s, ", "Sector Volume above reference surface"));
    sb.append(String.format("%s, ", "Sector Volume below reference surface"));
    sb.append(String.format("%s", "Total Sector Volume"));

    return sb.toString();
  }

  private String toCSV(GridPoint gp) {
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("%f, ", gp.localIJK.getX()));
    sb.append(String.format("%f, ", gp.localIJK.getY()));

    double angle = Math.toDegrees(Math.atan2(gp.localIJK.getY(), gp.localIJK.getX()));
    if (angle < 0)
      angle += 360;
    sb.append(String.format("%f, ", angle));
    sb.append(String.format("%d, ", isInsideROI(gp.localIJK) ? 1 : 0));

    sb.append(String.format("%f, ", gp.globalIJK.getX()));
    sb.append(String.format("%f, ", gp.globalIJK.getY()));
    sb.append(String.format("%f, ", gp.globalIJK.getZ()));

    sb.append(String.format("%g, ", gp.referenceHeight));
    sb.append(String.format("%g, ", gp.height));
    sb.append(String.format("%g, ", gp.differentialHeight));
    sb.append(String.format("%g", gridSpacing * gridSpacing * gp.differentialHeight));

    return sb.toString();
  }

  /**
   *
   * @param localIJ Point on the local grid.  The Z coordinate is ignored
   * @return true if the point is inside the outer boundary and outside the inner boundary. If the
   *     outer boundary is null then all points are considered to be inside the outer boundary. If the
   *     inner boundary is null all points are considered to be outside the inner boundary.
   */
  private boolean isInsideROI(Vector3D localIJ) {
    Point2D thisPoint = new Point2D.Double(localIJ.getX(), localIJ.getY());
    boolean insideROI = roiOuter == null || roiOuter.contains(thisPoint);
    if (roiInner != null) {
      if (insideROI && roiInner.contains(thisPoint))
        insideROI = false;
    }
    return insideROI;
  }

  /**
   * Write out a VTK file with the local grid points. Useful for a sanity check.
   * 
   * @param gridPointsList grid points
   * @param profilesMap grid points along each profile
   * @param sectorsMap grid points within each sector
   * @param vtkFile file to write
   */
  private void writeReferenceVTK(Collection<GridPoint> gridPointsList,
      Map<Integer, Collection<GridPoint>> profilesMap,
      Map<Integer, Collection<GridPoint>> sectorsMap, String vtkFile) {

    Map<Vector3D, Boolean> roiMap = new HashMap<>();
    Map<Vector3D, Integer> profileMap = new HashMap<>();
    Map<Vector3D, Integer> sectorMap = new HashMap<>();

    for (GridPoint gp : gridPointsList) {
      Vector3D localIJK = gp.localIJK;
      Vector3D nativeIJK =
          nativeToLocal.getKey().applyInverseTo(localIJK).add(nativeToLocal.getValue());
      Vector3D globalIJK = plane.localToGlobal(nativeIJK);
      roiMap.put(globalIJK, isInsideROI(gp.localIJK));
      profileMap.put(globalIJK, 0);
      sectorMap.put(globalIJK, 0);
    }

    for (int i : profilesMap.keySet()) {
      for (GridPoint gp : profilesMap.get(i)) {
        Vector3D localIJK = gp.localIJK;
        Vector3D nativeIJK =
            nativeToLocal.getKey().applyInverseTo(localIJK).add(nativeToLocal.getValue());
        Vector3D globalIJK = plane.localToGlobal(nativeIJK);
        profileMap.put(globalIJK, i + 1);
      }
    }

    for (int i : sectorsMap.keySet()) {
      for (GridPoint gp : sectorsMap.get(i)) {
        Vector3D localIJK = gp.localIJK;
        Vector3D nativeIJK =
            nativeToLocal.getKey().applyInverseTo(localIJK).add(nativeToLocal.getValue());
        Vector3D globalIJK = plane.localToGlobal(nativeIJK);
        sectorMap.put(globalIJK, i + 1);
      }
    }

    vtkDoubleArray insideROI = new vtkDoubleArray();
    insideROI.SetName("Inside ROI");

    vtkDoubleArray profiles = new vtkDoubleArray();
    profiles.SetName("Profiles");

    vtkDoubleArray sectors = new vtkDoubleArray();
    sectors.SetName("Sectors");

    vtkPoints pointsXYZ = new vtkPoints();
    for (Vector3D point : roiMap.keySet()) {
      double[] array = point.toArray();
      pointsXYZ.InsertNextPoint(array);
      insideROI.InsertNextValue(roiMap.get(point) ? 1 : 0);
      profiles.InsertNextValue(profileMap.get(point));
      sectors.InsertNextValue(sectorMap.get(point));
    }

    vtkPolyData polyData = new vtkPolyData();
    polyData.SetPoints(pointsXYZ);
    polyData.GetPointData().AddArray(insideROI);
    polyData.GetPointData().AddArray(profiles);
    polyData.GetPointData().AddArray(sectors);

    vtkCellArray cells = new vtkCellArray();
    polyData.SetPolys(cells);

    for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
      vtkIdList idList = new vtkIdList();
      idList.InsertNextId(i);
      cells.InsertNextCell(idList);
    }

    vtkPolyDataWriter writer = new vtkPolyDataWriter();
    writer.SetInputData(polyData);
    writer.SetFileName(vtkFile);
    writer.SetFileTypeToBinary();
    writer.Update();
  }

  /**
   * Write the local grid out to a file
   * 
   * @param gridPoints grid points
   * @param header file header
   * @param outputBasename CSV file to write
   */
  private void writeGridCSV(Collection<GridPoint> gridPoints, String header, String outputBasename) {
    String csvFile = outputBasename + "_grid.csv";

    try (PrintWriter pw = new PrintWriter(csvFile)) {
      pw.println(getHeader(header));
      for (GridPoint gp : gridPoints)
        pw.println(toCSV(gp));
    } catch (FileNotFoundException e) {
      logger.warn("Can't write " + csvFile);
      logger.warn(e.getLocalizedMessage());
    }
  }

  /**
   * Write profiles to file
   * 
   * @param gridPoints grid points
   * @param header file header
   * @param outputBasename CSV file to write
   */
  private Map<Integer, Collection<GridPoint>> writeProfileCSV(Collection<GridPoint> gridPoints,
      String header, String outputBasename) {
    Map<Integer, Collection<GridPoint>> profileMap = new HashMap<>();

    if (numProfiles == 0)
      return profileMap;

    // sort grid points into radial bins
    NavigableMap<Integer, Set<GridPoint>> radialMap = new TreeMap<>();
    for (GridPoint gp : gridPoints) {
      double radius = gp.localIJK.getNorm() / gridSpacing;
      int key = (int) radius;
      Set<GridPoint> set = radialMap.computeIfAbsent(key, k -> new HashSet<>());
        set.add(gp);
    }

    final double deltaAngle = 2 * Math.PI / numProfiles;
    for (int i = 0; i < numProfiles; i++) {

      Collection<GridPoint> profileGridPoints = new HashSet<>();
      profileMap.put(i, profileGridPoints);

      double angle = deltaAngle * i;
      String csvFile = String.format("%s_profile_%03d.csv", outputBasename,
          (int) Math.round(Math.toDegrees(angle)));

      try (PrintWriter pw = new PrintWriter(csvFile)) {
        pw.println(getHeader(header));
        for (int bin : radialMap.keySet()) {

          // stop profile at grid edge
          double thisX = Math.abs(Math.cos(angle) * bin) * gridSpacing;
          if (thisX > gridHalfExtent) continue;
          double thisY = Math.abs(Math.sin(angle) * bin) * gridSpacing;
          if (thisY > gridHalfExtent) continue;

          // sort points in this radial bin by angular distance from profile angle
          NavigableSet<GridPoint> sortedByAngle = new TreeSet<>((o1, o2) -> {
            double angle1 = Math.atan2(o1.localIJK.getY(), o1.localIJK.getX());
            if (angle1 < 0)
              angle1 += 2 * Math.PI;
            double angle2 = Math.atan2(o2.localIJK.getY(), o2.localIJK.getX());
            if (angle2 < 0)
              angle2 += 2 * Math.PI;
            return Double.compare(Math.abs(angle1 - angle), Math.abs(angle2 - angle));
          });

            sortedByAngle.addAll(radialMap.get(bin));

          pw.println(toCSV(sortedByAngle.first()));
          GridPoint thisPoint = sortedByAngle.first();
          if (Double.isFinite(thisPoint.differentialHeight))
            profileGridPoints.add(thisPoint);
        }
      } catch (FileNotFoundException e) {
        logger.warn("Can't write {}", csvFile);
        logger.warn(e.getLocalizedMessage());
      }

    }

    return profileMap;
  }

  /**
   * Write sector volumes to a file
   *
   * @param gridPoints grid points
   * @param header file header
   * @param outputBasename CSV file to write
   */
  private Map<Integer, Collection<GridPoint>> writeSectorCSV(Collection<GridPoint> gridPoints,
      String header, String outputBasename) {

    // grid points in each sector
    Map<Integer, Collection<GridPoint>> sectorMap = new HashMap<>();

    if (numProfiles == 0)
      return sectorMap;

    String csvFile = outputBasename + "_sector.csv";

    NavigableMap<Double, Double> aboveMap = new TreeMap<>();
    NavigableMap<Double, Double> belowMap = new TreeMap<>();
    final double deltaAngle = 2 * Math.PI / numProfiles;
    for (int i = 0; i < numProfiles; i++) {
      aboveMap.put(i * deltaAngle, 0.);
      belowMap.put(i * deltaAngle, 0.);
    }

    // run through all the grid points and put them in the appropriate sector
    double gridCellArea = gridSpacing * gridSpacing;
    for (GridPoint gp : gridPoints) {
      Vector3D localIJK = gp.localIJK;
      double azimuth = Math.atan2(localIJK.getY(), localIJK.getX());
      if (azimuth < 0)
        azimuth += 2 * Math.PI;
      double key = aboveMap.floorKey(azimuth);

      int sector = (int) (key / deltaAngle);
      Collection<GridPoint> sectorGridPoints = sectorMap.computeIfAbsent(sector, k -> new HashSet<>());

        if (isInsideROI(gp.localIJK)) {
        double dv = gridCellArea * gp.differentialHeight;
        if (Double.isFinite(dv)) {
          if (dv > 0) {
              aboveMap.compute(key, (k, value) -> value + dv);
          } else {
              belowMap.compute(key, (k, value) -> value + dv);
          }
          sectorGridPoints.add(gp);
        }
      }
    }

    try (PrintWriter pw = new PrintWriter(csvFile)) {
      pw.println(getSectorHeader(header));
      for (double azimuth : aboveMap.keySet()) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%d, ", (int) (azimuth / deltaAngle)));
        sb.append(String.format("%.2f, ", Math.toDegrees(azimuth)));
        sb.append(String.format("%.2f, ", Math.toDegrees(azimuth + deltaAngle)));
        sb.append(String.format("%e, ", aboveMap.get(azimuth)));
        sb.append(String.format("%e, ", belowMap.get(azimuth)));
        sb.append(String.format("%e", aboveMap.get(azimuth) + belowMap.get(azimuth)));
        pw.println(sb);
      }

    } catch (FileNotFoundException e) {
      logger.warn("Can't write " + csvFile);
      logger.warn(e.getLocalizedMessage());
    }

    return sectorMap;

  }

  private class GridPoint implements Comparable<GridPoint> {
    Vector3D localIJK;
    Vector3D globalIJK;
    double referenceHeight;
    double height;
    double differentialHeight;

    /**
     * Create a grid point from an input location in local coordinates
     * 
     * @param xy point in local coordinates.  Z value is ignored.
     */
    public GridPoint(Vector3D xy) {
      this.localIJK = xy;
      Vector3D nativeIJK =
          nativeToLocal.getKey().applyInverseTo(localIJK).add(nativeToLocal.getValue());
      globalIJK = plane.localToGlobal(nativeIJK);
      referenceHeight = getRefHeight(nativeIJK.getX(), nativeIJK.getY());
      height = getHeight(nativeIJK.getX(), nativeIJK.getY());
      differentialHeight = height - referenceHeight;
    }

    /**
     * sort by the x coordinate on the local grid, then by the y coordinate.
     */
    @Override
    public int compareTo(GridPoint o) {
      int compare = Double.compare(localIJK.getX(), o.localIJK.getX());
      if (compare == 0)
        compare = Double.compare(localIJK.getY(), o.localIJK.getY());
      return compare;
    }
  }

  private static List<Vector3D> readPointsFromFile(String filename) {
    List<Vector3D> points = new ArrayList<>();

    try {
      if (FilenameUtils.getExtension(filename).equalsIgnoreCase("vtk")) {

        vtkPolyData polydata = PolyDataUtil.loadShapeModel(filename);
        double[] pt = new double[3];
        for (int i = 0; i < polydata.GetNumberOfPoints(); i++) {
          polydata.GetPoint(i, pt);
          points.add(new Vector3D(pt));
        }
      } else {
        List<String> lines = FileUtils.readLines(new File(filename), Charset.defaultCharset());
        for (String line : lines) {
          if (line.trim().isEmpty() || line.trim().startsWith("#"))
            continue;
          SBMTStructure structure = SBMTStructure.fromString(line);
          points.add(structure.centerXYZ());
        }
      }
    } catch (Exception e) {
      logger.warn(e.getLocalizedMessage());
    }
    return points;
  }
  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("gridExtent").required().hasArg().desc(
                    "Required.  Size of local grid, in same units as shape model and reference surface.  Grid is assumed to be square.")
            .build());
    options.addOption(Option.builder("gridSpacing").required().hasArg()
            .desc(
                    "Required.  Spacing of local grid, in same units as shape model and reference surface.")
            .build());
    options.addOption(Option.builder("logFile").hasArg()
            .desc("If present, save screen output to log file.").build());
    options.addOption(Option.builder("logLevel").hasArg()
            .desc("If present, print messages above selected priority.  Valid values are "
                    + "ALL, OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, or FINEST.  Default is INFO.")
            .build());
    options.addOption(Option.builder("numProfiles").hasArg().desc(
                    "Number of radial profiles to create.  Profiles are evenly spaced in degrees and evaluated "
                            + "at intervals of gridSpacing in the radial direction.")
            .build());
    options.addOption(Option.builder("origin").hasArg()
            .desc("If present, set origin of local coordinate system.  "
                    + "Options are MAX_HEIGHT (set to maximum elevation of the shape model), "
                    + "MIN_HEIGHT (set to minimum elevation of the shape model), "
                    + "or a three element vector specifying the desired origin, comma separated, no spaces (e.g. 11.45,-45.34,0.932).")
            .build());
    options.addOption(Option.builder("output").hasArg().required().desc(
                    "Basename of output files.  Files will be named ${output}_grid.csv for the grid, ${output}_sector.csv for the sectors, "
                            + "and ${output}_profile_${degrees}.csv for profiles.")
            .build());
    options.addOption(Option.builder("radialUp")
            .desc("Specify +Z direction of local coordinate system to be in the radial "
                    + "direction.  Default is to align local +Z along global +Z.")
            .build());
    options.addOption(Option.builder("referenceList").hasArg().desc(
                    "File containing reference points.  If the file extension is .vtk it is read as a VTK file, "
                            + "otherwise it is assumed to be an SBMT structure file.")
            .build());
    options.addOption(Option.builder("referenceShape").hasArg().desc("Reference shape.").build());
    options.addOption(Option.builder("referenceVTK").hasArg()
            .desc("If present, write out a VTK file with the reference surface at each grid point.  "
                    + "If an ROI is defined color points inside/outside the boundaries.")
            .build());
    options.addOption(Option.builder("roiInner").hasArg().desc(
                    "Flag points closer to the origin than this as outside the ROI. Supported formats are the same as referenceList.")
            .build());
    options.addOption(Option.builder("roiOuter").hasArg().desc(
                    "Flag points closer to the origin than this as outside the ROI. Supported formats are the same as referenceList.")
            .build());
    options.addOption(Option.builder("shapeModel").hasArg().required()
            .desc("Shape model for volume computation.").build());    return options;
  }


  public static void main(String[] args)  {
    TerrasaurTool defaultOBJ = new DifferentialVolumeEstimator();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    StringBuilder header = new StringBuilder();
    header.append("# ").append(new Date()).append("\n");
    header.append("# ").append(defaultOBJ.getClass().getSimpleName()).append(" [").append(AppVersion.getVersionString()).append("]\n");
    header.append("# ").append(startupMessages.get(MessageLabel.ARGUMENTS)).append("\n");

    NativeLibraryLoader.loadVtkLibraries();

    double gridHalfExtent = Double.parseDouble(cl.getOptionValue("gridExtent")) / 2;
    double gridSpacing = Double.parseDouble(cl.getOptionValue("gridSpacing"));

    String outputBasename = cl.getOptionValue("output");
    String dirName = FilenameUtils.getFullPath(outputBasename);
    if (!dirName.trim().isEmpty()) {
      File dir = new File(dirName);
      if (!dir.exists())
        dir.mkdirs();
    }

      vtkPolyData polyData = null;
      try {
          polyData = PolyDataUtil.loadShapeModel(cl.getOptionValue("shapeModel"));
      } catch (Exception e) {
          logger.error("Cannot load shape model!");
          logger.error(e.getLocalizedMessage(), e);
      }

      ORIGIN originType = ORIGIN.DEFAULT;
    Vector3D localOrigin = Vector3D.ZERO;
    if (cl.hasOption("origin")) {
      String originString = cl.getOptionValue("origin");
      if (originString.contains(",")) {
        String[] parts = originString.split(",");
        if (parts.length == 3) {
          localOrigin = new Vector3D(Double.parseDouble(parts[0].trim()),
              Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()));
          originType = ORIGIN.CUSTOM;
        }
      } else {
        originType = ORIGIN.valueOf(originString.toUpperCase());
      }
    }
    DifferentialVolumeEstimator app = new DifferentialVolumeEstimator(polyData);

    if (cl.hasOption("numProfiles"))
      app.setNumProfiles(Integer.parseInt(cl.getOptionValue("numProfiles")));

    if (cl.hasOption("radialUp"))
      app.setRadialUp(true);

    if (cl.hasOption("referenceShape")) {
        try {
            app.setReferencePolyData(PolyDataUtil.loadShapeModel(cl.getOptionValue("referenceShape")));
        } catch (Exception e) {
            logger.error("Cannot load reference shape model!");
            logger.error(e.getLocalizedMessage(), e);
        }
    }
    if (cl.hasOption("referenceList"))
      app.setReferencePoints(readPointsFromFile(cl.getOptionValue("referenceList")));

    app.createReference(localOrigin);

    // Shift the origin if needed
    switch (originType) {
      case CUSTOM:
      case DEFAULT:
        break;
      case MAX_HEIGHT:
        app.createGrid(gridHalfExtent, 0.1 * gridSpacing);
        localOrigin = app.highPoint;
        app.createReference(localOrigin);
        break;
      case MIN_HEIGHT:
        app.createGrid(gridHalfExtent, 0.1 * gridSpacing);
        localOrigin = app.lowPoint;
        app.createReference(localOrigin);
        break;
    }

    if (cl.hasOption("roiInner"))
      app.setInnerROI(cl.getOptionValue("roiInner"));

    if (cl.hasOption("roiOuter"))
      app.setOuterROI(cl.getOptionValue("roiOuter"));

    NavigableSet<GridPoint> gridPoints = app.createGrid(gridHalfExtent, gridSpacing);

    app.writeGridCSV(gridPoints, header.toString(), outputBasename);
    Map<Integer, Collection<GridPoint>> profileMap =
        app.writeProfileCSV(gridPoints, header.toString(), outputBasename);
    Map<Integer, Collection<GridPoint>> sectorMap =
        app.writeSectorCSV(gridPoints, header.toString(), outputBasename);

    if (cl.hasOption("referenceVTK")) {
      app.writeReferenceVTK(gridPoints, profileMap, sectorMap, cl.getOptionValue("referenceVTK"));
    }

    logger.info("Finished.");
  }
}
