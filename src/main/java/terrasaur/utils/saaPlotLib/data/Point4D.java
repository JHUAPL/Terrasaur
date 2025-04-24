package terrasaur.utils.saaPlotLib.data;

import java.awt.geom.Point2D;
import java.util.Comparator;

public class Point4D {

  public static final Comparator<Point4D> SORT_ON_I =
          Comparator.comparingInt(Point4D::getIndex);

  public static final Comparator<Point4D> SORT_ON_X =
          Comparator.comparingDouble(Point4D::getX);

  public static final Comparator<Point4D> SORT_ON_Y =
          Comparator.comparingDouble(Point4D::getY);

  public static final Comparator<Point4D> SORT_ON_Z =
          Comparator.comparingDouble(Point4D::getZ);

  public static final Comparator<Point4D> SORT_ON_W =
          Comparator.comparingDouble(Point4D::getW);
  protected final double x;
  protected final double y;
  protected final double z; // 3d coordinate
  protected final Point2D xError;
  protected final Point2D yError;
  protected final Point2D zError;
  protected final double w; // property
  protected int index;

  public Point4D(int i, double x, double y) {
    this(i, x, y, Double.NaN, Double.NaN);
  }

  public Point4D(int i, Point2D x, Point2D y) {
    this(i, x, y, new Point2D.Double(Double.NaN, Double.NaN), Double.NaN);
  }

  public Point4D(int i, Point3D x, Point3D y) {
    this(i, x, y, new Point3D(Double.NaN, Double.NaN, Double.NaN), Double.NaN);
  }

  public Point4D(int i, double x, double y, double z) {
    this(i, x, y, z, Double.NaN);
  }

  public Point4D(int i, Point2D x, Point2D y, Point2D z) {
    this(i, x, y, z, Double.NaN);
  }

  public Point4D(int i, Point3D x, Point3D y, Point3D z) {
    this(i, x, y, z, Double.NaN);
  }

  public Point4D(int i, double x, double y, double z, double w) {
    this(
        i,
        new Point3D(x, Double.NaN, Double.NaN),
        new Point3D(y, Double.NaN, Double.NaN),
        new Point3D(z, Double.NaN, Double.NaN),
        w);
  }

  /**
   * Add a new point with coordinates (x, y, z). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getY()) and similarly for y and z.
   *
   * @param i index
   * @param x x
   * @param y y
   * @param z z
   * @param w property value
   */
  public Point4D(int i, Point2D x, Point2D y, Point2D z, double w) {
    this(
        i,
        new Point3D(x.getX(), x.getY(), x.getY()),
        new Point3D(y.getX(), y.getY(), y.getY()),
        new Point3D(z.getX(), z.getY(), z.getY()),
        w);
  }

  /**
   * Add a new point with coordinates (x, y, z). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getZ()) and similarly for y and z.
   *
   * @param index index
   * @param x x
   * @param y y
   * @param z z
   * @param w property value
   */
  public Point4D(int index, Point3D x, Point3D y, Point3D z, double w) {
    this.index = index;
    this.x = x.x();
    this.y = y.x();
    this.z = z.x();
    this.xError = new Point2D.Double(x.y(), x.z());
    this.yError = new Point2D.Double(y.y(), y.z());
    this.zError = new Point2D.Double(z.y(), z.z());
    this.w = w;
  }

  /**
   * Create a deep copy of the supplied point.
   *
   * @param other point to copy
   */
  public Point4D(Point4D other) {
    this.index = other.index;
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
    this.w = other.w;
    this.xError = new Point2D.Double(other.xError.getX(), other.xError.getY());
    this.yError = new Point2D.Double(other.yError.getX(), other.yError.getY());
    this.zError = new Point2D.Double(other.zError.getX(), other.zError.getY());
  }

  public int getIndex() {
    return index;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public Point2D getXError() {
    return xError;
  }

  public Point2D getYError() {
    return yError;
  }

  public Point2D getZError() {
    return zError;
  }

  public double getW() {
    return w;
  }
}
