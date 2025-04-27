package terrasaur.smallBodyModel;

import java.util.ArrayList;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.intervals.Interval;
import vtk.vtkPolyData;

/**
 * This class is used to subdivide the bounding box of a shape model into a contiguous grid of 3D
 * cubes (sort of like voxels).
 * 
 * @author kahneg1
 * @version 1.0
 *
 */
public class SmallBodyCubes {

  private final static Logger logger = LogManager.getLogger(SmallBodyCubes.class);

  private BoundingBox boundingBox;
  private ArrayList<BoundingBox> allCubes = new ArrayList<BoundingBox>();
  private final double cubeSize;
  private final double buffer;

  /**
   * Create a cube set structure for the given model, where each cube has side <tt>cubeSize</tt> and
   * <tt>buffer</tt> is added to all sides of the bounding box of the model. Cubes that do not
   * intersect the asteroid at all are removed.
   * 
   * @param smallBodyPolyData
   * @param cubeSize
   * @param buffer
   */
  public SmallBodyCubes(vtkPolyData smallBodyPolyData, double cubeSize, double buffer,
      boolean removeEmptyCubes) {
    this.cubeSize = cubeSize;
    this.buffer = buffer;

    initialize(smallBodyPolyData);

    if (removeEmptyCubes)
      removeEmptyCubes(smallBodyPolyData);
  }

  private void initialize(vtkPolyData smallBodyPolyData) {
    smallBodyPolyData.ComputeBounds();
    double[] bounds = smallBodyPolyData.GetBounds();
    boundingBox = new BoundingBox(new Interval(bounds[0] - buffer, bounds[1] + buffer),
        new Interval(bounds[2] - buffer, bounds[3] + buffer),
        new Interval(bounds[4] - buffer, bounds[5] + buffer));

    int numCubesX = (int) Math.ceil(boundingBox.getXRange().getLength() / cubeSize);
    int numCubesY = (int) Math.ceil(boundingBox.getYRange().getLength() / cubeSize);
    int numCubesZ = (int) Math.ceil(boundingBox.getZRange().getLength() / cubeSize);

    for (int k = 0; k < numCubesZ; ++k) {
      double zmin = boundingBox.getZRange().getBegin() + k * cubeSize;
      double zmax = boundingBox.getZRange().getBegin() + (k + 1) * cubeSize;
      for (int j = 0; j < numCubesY; ++j) {
        double ymin = boundingBox.getYRange().getBegin() + j * cubeSize;
        double ymax = boundingBox.getYRange().getBegin() + (j + 1) * cubeSize;
        for (int i = 0; i < numCubesX; ++i) {
          double xmin = boundingBox.getXRange().getBegin() + i * cubeSize;
          double xmax = boundingBox.getXRange().getBegin() + (i + 1) * cubeSize;
          BoundingBox bb = new BoundingBox(new Interval(xmin, xmax), new Interval(ymin, ymax),
              new Interval(zmin, zmax));
          allCubes.add(bb);
        }
      }
    }
  }

  private void removeEmptyCubes(vtkPolyData smallBodyPolyData) {
    logger.info("total cubes before reduction = {}", allCubes.size());

    // Remove from allCubes all cubes that do not intersect the asteroid
    // long t0 = System.currentTimeMillis();
    TreeSet<Integer> intersectingCubes = getIntersectingCubes(smallBodyPolyData);
    // System.out.println("Time elapsed: " +
    // ((double)System.currentTimeMillis()-t0)/1000.0);

    ArrayList<BoundingBox> tmpCubes = new ArrayList<BoundingBox>();
    for (Integer i : intersectingCubes) {
      tmpCubes.add(allCubes.get(i));
    }

    allCubes = tmpCubes;

    logger.info("finished initializing cubes, total = {}", allCubes.size());
  }

  public BoundingBox getCube(int cubeId) {
    return allCubes.get(cubeId);
  }

  /**
   * Get all the cubes that intersect with <tt>polydata</tt>
   * 
   * @param polydata
   * @return
   */
  public TreeSet<Integer> getIntersectingCubes(vtkPolyData polydata) {
    TreeSet<Integer> cubeIds = new TreeSet<Integer>();

    // Iterate through each cube and check if it intersects
    // with the bounding box of any of the polygons of the polydata
    BoundingBox polydataBB = new BoundingBox(polydata.GetBounds());

    long numberPolygons = polydata.GetNumberOfCells();

    // Store all the bounding boxes of all the individual polygons in an
    // array first
    // since the call to GetCellBounds is very slow.
    double[] cellBounds = new double[6];
    ArrayList<BoundingBox> polyCellsBB = new ArrayList<BoundingBox>();
    for (int j = 0; j < numberPolygons; ++j) {
      polydata.GetCellBounds(j, cellBounds);
      polyCellsBB.add(new BoundingBox(cellBounds));
    }

    int numberCubes = allCubes.size();
    for (int i = 0; i < numberCubes; ++i) {
      // Before checking each polygon individually, first see if the
      // polydata as a whole intersects the cube
      BoundingBox cube = getCube(i);
      if (cube.intersects(polydataBB)) {
        for (int j = 0; j < numberPolygons; ++j) {
          BoundingBox bb = polyCellsBB.get(j);
          if (cube.intersects(bb)) {
            cubeIds.add(i);
            break;
          }
        }
      }
    }

    return cubeIds;
  }

  /**
   * Get all the cubes that intersect with BoundingBox <tt>bb</tt>
   * 
   * @param bb
   * @return
   */
  public TreeSet<Integer> getIntersectingCubes(BoundingBox bb) {
    TreeSet<Integer> cubeIds = new TreeSet<Integer>();

    int numberCubes = allCubes.size();
    for (int i = 0; i < numberCubes; ++i) {
      BoundingBox cube = getCube(i);
      if (cube.intersects(bb)) {
        cubeIds.add(i);
      }
    }

    return cubeIds;
  }

  /**
   * Get the id of the cube containing <tt>point</tt>
   * 
   * @param point
   * @return
   */
  public int getCubeId(double[] point) {
    if (!boundingBox.contains(point))
      return -1;

    int numberCubes = allCubes.size();
    for (int i = 0; i < numberCubes; ++i) {
      BoundingBox cube = getCube(i);
      if (cube.contains(point))
        return i;
    }

    // If we reach here something is wrong
    System.err.println("Error: could not find cube");

    return -1;
  }
}
