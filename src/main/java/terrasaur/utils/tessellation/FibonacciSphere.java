package terrasaur.utils.tessellation;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.FastMath;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.MatrixIJ;
import picante.math.vectorspace.VectorIJ;
import picante.math.vectorspace.VectorIJK;
import picante.math.vectorspace.UnwritableVectorIJK;

import picante.math.intervals.UnwritableInterval;

/**
 * Implements fibonacci tiling and reverse lookup as described in
 * <p>
 * Keinert, B., Innmann, M., Sänger, M., Stamminger, M. 2015. Spherical Fibonacci Mapping. ACM
 * Trans. Graph. 34, 6, Article 193 (November 2015), 7 pages. <br>
 * DOI = <a href= "http://doi.acm.org/10.1145/2816795.2818131">10.1145/2816795.2818131</a> .
 * 
 */
public class FibonacciSphere implements SphericalTessellation {

  private static final double GOLDEN_RATIO = (Math.sqrt(5) + 1) / 2;

  private final List<LatitudinalVector> lvList;
  private final List<UnwritableVectorIJK> ijkList;
  private final ThreadLocal<List<Double>> threadLocalDistance;
  private final ThreadLocal<DescriptiveStatistics> threadLocalStats;

  /**
   * Create a set of points approximately uniformly distributed about the unit sphere
   * 
   * @param npts Number of points to use
   */
  public FibonacciSphere(int npts) {
    lvList = new ArrayList<>();
    ijkList = new ArrayList<>();
    threadLocalStats = new ThreadLocal<>();

    for (int i = 0; i < npts; i++) {
      double phi = 2 * Math.PI * madfrac(i, GOLDEN_RATIO - 1);
      double z = 1 - (2 * i + 1.) / npts;

      LatitudinalVector lv = new LatitudinalVector(1, Math.asin(z), phi);

      lvList.add(lv);
      ijkList.add(CoordConverters.convert(lv));
    }

    threadLocalDistance = new ThreadLocal<>();
  }

  /**
   * 
   * @return statistics on the distances between each point and its closest neighbor
   */
  public DescriptiveStatistics getDistanceStats() {
    if (threadLocalStats.get() == null) {
      DescriptiveStatistics distanceStats = new DescriptiveStatistics();
      for (Double dist : getClosestNeighborDistance()) {
        distanceStats.addValue(Math.toDegrees(dist));
      }
      threadLocalStats.set(distanceStats);
    }
    return threadLocalStats.get();
  }

  private List<Double> getClosestNeighborDistance() {
    if (threadLocalDistance.get() == null) {
      List<Double> closestNeighborDistance = new ArrayList<>();
      for (int i = 0; i < lvList.size(); i++) {

        double minDist = Double.MAX_VALUE;

        for (UnwritableVectorIJK ijk : ijkList) {
          double dist = ijk.getSeparation(ijkList.get(i));
          if (dist > 0 && dist < minDist) {
            minDist = dist;
          }
        }
        closestNeighborDistance.add(minDist);
      }
      threadLocalDistance.set(closestNeighborDistance);
    }
    return threadLocalDistance.get();
  }

  @Override
  public long getNumTiles() {
    return lvList.size();
  }

  /**
   * @param i tile index
   * @return position of this tile center as a LatitudinalVector
   */
  @Override
  public LatitudinalVector getTileCenter(long i) {
    return lvList.get((int) i);
  }

  /**
   *
   * @param i tile index
   * @return position of this tile center as a VectorIJK
   */
  public UnwritableVectorIJK getTileCenterIJK(long i) {
    return ijkList.get((int) i);
  }

  @Override
  public long getTileIndex(LatitudinalVector lv) {
    return getTileIndex(CoordConverters.convert(lv));
  }

  @Override
  public long getTileIndex(UnwritableVectorIJK ijk) {
    return getNearest(ijk).getValue();
    /*-
    final long n = getNumTiles();
    final double rcpN = 1. / n;
    
    double phi = Math.min(Math.atan2(p.getJ(), p.getI()), Math.PI);
    double cosTheta = p.getK(); // theta is the colatitude, cosTheta is the sine of the latitude
    
    // global coordinates of the input point. (0,0) maps
    // to first point in the Fibonacci set.
    VectorIJ uv = new VectorIJ(phi, cosTheta - (1 - rcpN));
    
    MatrixIJ B = getLocalToGlobalTransform(p);
    MatrixIJ invB = B.createInverse();
    
    // coordinates on the local grid
    VectorIJ c = invB.mxv(uv);
    VectorIJ corner = new VectorIJ(Math.floor(c.getI()), Math.floor(c.getJ()));
    
    UnwritableInterval cosThetaRange = new UnwritableInterval(-1, 1);
    
    // global coordinates of the corner
    VectorIJ thisUV = B.mxv(corner);
    cosTheta = thisUV.getJ() + (1 - rcpN);
    cosTheta = cosThetaRange.clamp(cosTheta) * 2 - cosTheta;
    
    // index of the point with the closest latitude
    long i = (long) Math.floor(n * 0.5 * (1 - cosTheta));
    return i;
    */
  }

  private double madfrac(double a, double b) {
    return a * b - FastMath.floor(a * b);
  }

  /**
   *
   * @param lv input location
   * @return key is distance to tile center in radians, value is tile index
   */
  public Map.Entry<Double, Long> getNearest(LatitudinalVector lv) {
    return getNearest(CoordConverters.convert(lv));
  }

  /**
   * Get the nearest point from the input location. Do the inverse mapping using the method of
   * <p>
   * Keinert, B., Innmann, M., Sänger, M., Stamminger, M. 2015. Spherical Fibonacci Mapping. ACM
   * Trans. Graph. 34, 6, Article 193 (November 2015), 7 pages. <br>
   * DOI = <a href= "http://doi.acm.org/10.1145/2816795.2818131">10.1145/2816795.2818131</a> .
   * 
   * 
   * @param ijk cartesian coordinates
   * @return key is distance to tile center in radians, value is tile index
   */
  public Map.Entry<Double, Long> getNearest(UnwritableVectorIJK ijk) {
    final long n = getNumTiles();
    final double rcpN = 1. / n;

    double phi = Math.min(Math.atan2(ijk.getJ(), ijk.getI()), Math.PI);
    double cosTheta = ijk.getK(); // theta is the colatitude, cosTheta is the sine of the latitude

    // global coordinates of the input point. (0,0) maps
    // to first point in the Fibonacci set.
    VectorIJ uv = new VectorIJ(phi, cosTheta - (1 - rcpN));

    MatrixIJ B = getLocalToGlobalTransform(ijk);
    MatrixIJ invB = B.createInverse();

    // coordinates on the local grid
    VectorIJ c = invB.mxv(uv);
    c = new VectorIJ(Math.floor(c.getI()), Math.floor(c.getJ()));

    double d = Double.MAX_VALUE;
    long j = 0;

    UnwritableInterval cosThetaRange = new UnwritableInterval(-1, 1);
    for (int s = 0; s < 4; s++) {
      VectorIJ corner = VectorIJ.add(new VectorIJ(s % 2, s / 2), c);

      VectorIJ thisUV = B.mxv(corner);
      cosTheta = thisUV.getJ() + (1 - rcpN);
      cosTheta = cosThetaRange.clamp(cosTheta) * 2 - cosTheta;

      // index of the point with the closest latitude
      long i = (long) Math.floor(n * 0.5 * (1 - cosTheta));

      phi = 2 * Math.PI * madfrac(i, GOLDEN_RATIO - 1);
      cosTheta = 1 - (2 * i + 1) * rcpN;
      double sinTheta = Math.sqrt(1 - cosTheta * cosTheta); // theta is the colatitude (90 -
                                                            // latitude)

      VectorIJK q = new VectorIJK(Math.cos(phi) * sinTheta, Math.sin(phi) * sinTheta, cosTheta);
      VectorIJK qMp = VectorIJK.subtract(q, ijk);

      double dist2 = qMp.getDot(qMp);

      if (dist2 < d) {
        d = dist2;
        j = i;
      }
    }

    return new AbstractMap.SimpleEntry<>(Math.sqrt(d), j);
  }

  private MatrixIJ getLocalToGlobalTransform(UnwritableVectorIJK p) {
    final long n = getNumTiles();

    double cosTheta = p.getK(); // theta is the colatitude, cosTheta is the sine of the latitude

    // k is the zone number (Equation 5)
    int k = (int) Math.max(2,
        Math.floor(Math.log(n * Math.PI * Math.sqrt(5) * (1 - cosTheta * cosTheta))
            / Math.log(GOLDEN_RATIO + 1)));

    // estimate of the fibonacci number for this zone
    double Fk = Math.pow(GOLDEN_RATIO, k) / Math.sqrt(5);

    // F0 and F1 are bounds on the fibonacci number for this zone
    double F0 = Math.round(Fk);
    double F1 = Math.round(Fk * GOLDEN_RATIO);

    // basis vectors for the local grid
    VectorIJ bk = new VectorIJ(
        2 * Math.PI * (madfrac(F0 + 1, GOLDEN_RATIO - 1) - (GOLDEN_RATIO - 1)), -2 * F0 / n);
    VectorIJ bkp = new VectorIJ(
        2 * Math.PI * (madfrac(F1 + 1, GOLDEN_RATIO - 1) - (GOLDEN_RATIO - 1)), -2 * F1 / n);

    // local to global transformation matrix
    MatrixIJ B = new MatrixIJ(bk, bkp);

    return B;
  }

  /**
   *
   * @param i tile index
   * @return distance to this tile's closest neighbor center
   */
  public Double getDist(int i) {
    return getClosestNeighborDistance().get(i);
  }

  /**
   *
   * @param lv input point
   * @return map of tile indices sorted by distance from the input point
   */
  public NavigableMap<Double, Integer> getDistanceMap(LatitudinalVector lv) {
    UnwritableVectorIJK ijk = CoordConverters.convert(lv);
    return getDistanceMap(ijk);
  }

  /**
   *
   * @param point input point
   * @return map of tile indices sorted by distance from the input point
   */
  public NavigableMap<Double, Integer> getDistanceMap(UnwritableVectorIJK point) {
    NavigableMap<Double, Integer> distanceMap = new TreeMap<>();
    for (int i = 0; i < ijkList.size(); i++) {
      UnwritableVectorIJK ijk = ijkList.get(i);
      double dist = ijk.getSeparation(point);
      distanceMap.put(dist, i);
    }
    return distanceMap;
  }

}
