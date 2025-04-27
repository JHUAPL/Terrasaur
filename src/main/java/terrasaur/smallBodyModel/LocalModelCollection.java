package terrasaur.smallBodyModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.HashMultimap;
import picante.math.vectorspace.VectorIJK;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.PolyDataStatistics;
import terrasaur.utils.PolyDataUtil;
import terrasaur.utils.tessellation.FibonacciSphere;
import vtk.vtkPoints;
import vtk.vtkPolyData;

/**
 * Hold a collection of local shape models
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class LocalModelCollection {

  private final static Logger logger = LogManager.getLogger();

  static class LocalModel {
    final Vector3D center;
    final String filename;

    LocalModel(Vector3D center, String filename) {
      this.center = center;
      this.filename = filename;
    }

  }

  // key is tile index, value is collection of localModels
  private HashMultimap<Long, LocalModel> localModelMap;
  private FibonacciSphere tessellation;
  // key is filename, value is shape model
  private ThreadLocal<Map<String, SmallBodyModel>> localModels;

  private Double scale;
  private Rotation rotation;

  /**
   * 
   * @param numTiles total number of tiles to use for sorting local models
   */
  public LocalModelCollection(int numTiles, Double scale, Rotation rotation) {
    localModelMap = HashMultimap.create();
    tessellation = new FibonacciSphere(numTiles);
    localModels = new ThreadLocal<>();
    this.scale = scale;
    this.rotation = rotation;
  }

  /**
   * Add a shape model. Models are stored in a map with the center as the key, so an entry with the
   * same center as an existing entry will overwrite the existing one.
   * 
   * @param latInRadians
   * @param lonInRadians
   * @param filename
   */
  public void addModel(double latInRadians, double lonInRadians, String filename) {
    Vector3D center = new Vector3D(lonInRadians, latInRadians);
    long tileIndex = tessellation.getTileIndex(MathConversions.toVectorIJK(center));
    LocalModel lm = new LocalModel(center, filename);
    localModelMap.put(tileIndex, lm);
  }

  /**
   * Return a shape model containing the supplied point. This may not be the only shape model that
   * contains this point, just the first one found.
   * 
   * @param point
   * @return
   */
  public SmallBodyModel get(Vector3D point) {
    List<String> filenames = getFilenames(point);
    if (filenames.size() == 0)
      logger.error("No shape models cover {}", point.toString());
    double[] origin = new double[3];
    double[] intersectPoint = new double[3];
    for (String filename : filenames) {
      SmallBodyModel sbm = load(filename);
      long intersect =
          sbm.computeRayIntersection(origin, point.toArray(), 2 * point.getNorm(), intersectPoint);

      if (intersect != -1)
        return sbm;
    }
    logger.debug("Failed intersection for lon {}, lat {}", Math.toDegrees(point.getAlpha()),
        Math.toDegrees(point.getDelta()));
    return null;
  }

  /**
   * Load a shape model after applying any rotation or scaling
   * 
   * @param filename
   * @return
   */
  private SmallBodyModel load(String filename) {
    Map<String, SmallBodyModel> map = localModels.get();
    if (map == null) {
      map = new HashMap<>();
      localModels.set(map);
    }
    SmallBodyModel sbm = map.get(filename);
    if (sbm == null) {

      logger.debug("Thread {}: Loading {}", Thread.currentThread().getId(),
          FilenameUtils.getBaseName(filename));
      try {
        vtkPolyData model = PolyDataUtil.loadShapeModel(filename);
        if (scale != null || rotation != null) {
          PolyDataStatistics stats = new PolyDataStatistics(model);
          Vector3D center = new Vector3D(stats.getCentroid());

          vtkPoints points = model.GetPoints();
          for (int i = 0; i < points.GetNumberOfPoints(); i++) {
            Vector3D thisPoint = new Vector3D(points.GetPoint(i));
            if (scale != null)
              thisPoint = thisPoint.subtract(center).scalarMultiply(scale).add(center);
            if (rotation != null)
              thisPoint = rotation.applyTo(thisPoint.subtract(center)).add(center);
            points.SetPoint(i, thisPoint.toArray());
          }
        }

        sbm = new SmallBodyModel(model);
      } catch (Exception e) {
        logger.error(e.getLocalizedMessage());
      }

      map.put(filename, sbm);
    }
    return map.get(filename);
  }

  /**
   * Return the local model with the closest center to point
   * 
   * @param point
   * @return null if no models have been loaded
   */
  private List<String> getFilenames(Vector3D point) {
    VectorIJK ijk = MathConversions.toVectorIJK(point);

    // A sorted map of tiles by distance
    NavigableMap<Double, Integer> distanceMap = tessellation.getDistanceMap(ijk);

    List<String> smallBodyModels = new ArrayList<>();
    for (Double dist : distanceMap.keySet()) {

      // A set of local models with centers in this tile
      Set<LocalModel> localModelSet = localModelMap.get((long) distanceMap.get(dist));

      if (localModelSet.size() > 0) {
        NavigableMap<Double, LocalModel> localDistanceMap = new TreeMap<>();
        for (LocalModel localModel : localModelSet) {
          double thisDist = Vector3D.angle(localModel.center, point);
          localDistanceMap.put(thisDist, localModel);
        }
        // add all local models with centers within PI/4 of point
        for (double localDist : localDistanceMap.headMap(Math.PI / 4, true).keySet()) {
          smallBodyModels.add(localDistanceMap.get(localDist).filename);
        }
      }
    }

    return smallBodyModels;
  }

}
