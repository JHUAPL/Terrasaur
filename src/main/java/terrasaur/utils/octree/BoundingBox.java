package terrasaur.utils.octree;


import picante.math.intervals.UnwritableInterval;
import picante.math.vectorspace.UnwritableVectorIJK;

/**
 * Define a 3D box using its minimum and maximum coordinates
 * 
 * @author nairah1
 *
 */
public class BoundingBox {

  private double[] minPt;
  private double[] maxPt;
  private UnwritableInterval xRange;
  private UnwritableInterval yRange;
  private UnwritableInterval zRange;

  public BoundingBox(UnwritableVectorIJK minPt, UnwritableVectorIJK maxPt) {
    this.minPt = new double[] {minPt.getI(), minPt.getJ(), minPt.getK()};
    this.maxPt = new double[] {maxPt.getI(), maxPt.getJ(), maxPt.getK()};

    this.xRange = new UnwritableInterval(this.minPt[0], this.maxPt[0]);
    this.yRange = new UnwritableInterval(this.minPt[1], this.maxPt[1]);
    this.zRange = new UnwritableInterval(this.minPt[2], this.maxPt[2]);
  }

  public UnwritableVectorIJK minPt() {
    return new UnwritableVectorIJK(minPt);
  }

  public UnwritableVectorIJK maxPt() {
    return new UnwritableVectorIJK(maxPt);
  }

  public UnwritableInterval getxRange() {
    return xRange;
  }

  public UnwritableInterval getyRange() {
    return yRange;
  }

  public UnwritableInterval getzRange() {
    return zRange;
  }

  public boolean closedContains(UnwritableVectorIJK point) {
    return (xRange.closedContains(point.getI()) && yRange.closedContains(point.getJ())
        && zRange.closedContains(point.getK()));
  }

  /**
   * 
   * Check if the ray intersects the bounding box. Algorithm is from
   * https://medium.com/@bromanz/another-view-on-the-classic-ray-aabb-intersection-algorithm-for-bvh-traversal-41125138b525
   * 
   * @param source observer position
   * @param ray look direction
   * 
   * @return
   */
  public boolean intersects(UnwritableVectorIJK source, UnwritableVectorIJK ray) {
    double[] origin = new double[] {source.getI(), source.getJ(), source.getK()};
    double[] invDir = new double[] {1. / ray.getI(), 1. / ray.getJ(), 1. / ray.getK()};
    double tmin = -Double.MAX_VALUE;
    double tmax = Double.MAX_VALUE;

    for (int i = 0; i < 3; i++) {
      double t0 = (minPt[i] - origin[i]) * invDir[i];
      double t1 = (maxPt[i] - origin[i]) * invDir[i];
      double tsmall = Math.min(t0, t1);
      double tlarge = Math.max(t0, t1);

      tmin = Math.max(tmin, tsmall);
      tmax = Math.min(tmax, tlarge);

      if (tmax < tmin) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return String.format("%s - %s", new UnwritableVectorIJK(minPt), new UnwritableVectorIJK(maxPt));
  }

  public static void main(String[] args) {
    BoundingBox bb =
        new BoundingBox(new UnwritableVectorIJK(-1, -1, 1), new UnwritableVectorIJK(1, 1, 2));
    System.out.println(
        bb.intersects(new UnwritableVectorIJK(0, 0, 0), new UnwritableVectorIJK(1, 1, 1.01)));

  }

}
