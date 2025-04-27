package terrasaur.utils.saaPlotLib.data;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;

public class PointList implements Iterable<Point4D> {

  protected COORDINATE sortedOn;
  protected final List<Point4D> pointList;
  private ColorRamp colorRamp;

  public PointList() {
    sortedOn = COORDINATE.I;
    pointList = new ArrayList<>();
    colorRamp = null;
  }

  public ColorRamp getColorRamp() {
    return colorRamp;
  }

  public void setColorRamp(ColorRamp colorRamp) {
    this.colorRamp = colorRamp;
  }

  public void add(Point4D p) {
    p.index = pointList.size();
    pointList.add(p);
  }

  /**
   * Add a new point with coordinates (x, y).
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void add(double x, double y) {
    pointList.add(new Point4D(pointList.size(), x, y));
  }

  /**
   * Replace point at index i with coordinates (x, y).
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void set(int i, double x, double y) {
    pointList.set(i, new Point4D(i, x, y));
  }

  /**
   * Add a new point with coordinates (x, y). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getY()) and similarly for y.
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void add(Point2D x, Point2D y) {
    pointList.add(new Point4D(pointList.size(), x, y));
  }

  /**
   * Add a new point with coordinates (x, y). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getZ()) and similarly for y.
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void add(Point3D x, Point3D y) {
    pointList.add(new Point4D(pointList.size(), x, y));
  }

  /**
   * Add a new point with coordinates (x, y, z).
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   */
  public void add(double x, double y, double z) {
    pointList.add(new Point4D(pointList.size(), x, y, z));
  }

  /**
   * Add a new point with coordinates (x, y, z). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getY()) and similarly for y and z.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   */
  public void add(Point2D x, Point2D y, Point2D z) {
    pointList.add(new Point4D(pointList.size(), x, y, z));
  }

  /**
   * Add a new point with coordinates (x, y, z). The error bounds on x are (x.getX() - x.getY(),
   * x.getX() + x.getZ()) and similarly for y and z.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   */
  public void add(Point3D x, Point3D y, Point3D z) {
    pointList.add(new Point4D(pointList.size(), x, y, z));
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @param w property value
   */
  public void add(double x, double y, double z, double w) {
    pointList.add(new Point4D(pointList.size(), x, y, z, w));
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w. The error bounds on x are (x.getX()
   * - x.getY(), x.getX() + x.getY()) and similarly for y and z.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @param w property value
   */
  public void add(Point2D x, Point2D y, Point2D z, double w) {
    pointList.add(new Point4D(pointList.size(), x, y, z, w));
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w. The error bounds on x are (x.getX()
   * - x.getY(), x.getX() + x.getZ()) and similarly for y and z.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @param w property value
   */
  public void add(Point3D x, Point3D y, Point3D z, double w) {
    pointList.add(new Point4D(pointList.size(), x, y, z, w));
  }

  /**
   * Sort the dataset on the desired {@link COORDINATE}.
   *
   * @param c coordinate to use for sorting
   */
  public void sort(COORDINATE c) {
    switch (c) {
      case I:
        sortOnIndex();
        break;
      case W:
        sortOnW();
        break;
      case X:
        sortOnX();
        break;
      case Y:
        sortOnY();
        break;
      case Z:
        sortOnZ();
        break;
    }
  }

  /** Sort the data points by insertion order. */
  private void sortOnIndex() {
    if (sortedOn != COORDINATE.I) {
      pointList.sort(Point4D.SORT_ON_I);
      sortedOn = COORDINATE.I;
    }
  }

  /** Sort the data points by X value. */
  private void sortOnX() {
    if (sortedOn != COORDINATE.X) {
      pointList.sort(Point4D.SORT_ON_X);
      sortedOn = COORDINATE.X;
    }
  }

  /** Sort the data points by Y value. */
  private void sortOnY() {
    if (sortedOn != COORDINATE.Y) {
      pointList.sort(Point4D.SORT_ON_Y);
      sortedOn = COORDINATE.Y;
    }
  }

  /** Sort the data points by Z value. */
  private void sortOnZ() {
    if (sortedOn != COORDINATE.Z) {
      pointList.sort(Point4D.SORT_ON_Z);
      sortedOn = COORDINATE.Z;
    }
  }

  /** Sort the data points by W value. */
  private void sortOnW() {
    if (sortedOn != COORDINATE.W) {
      pointList.sort(Point4D.SORT_ON_W);
      sortedOn = COORDINATE.W;
    }
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @see List#get(int)
   * @param index index of the element to return
   * @return the element at the specified position in this list
   */
  public Point4D get(int index) {
    return pointList.get(index);
  }

  /**
   *
   * @return the first element in the list. Be sure to sort the list on the desired coordinate before
   *    calling this function.
   */
  public Point4D getFirst() {
    return pointList.get(0);
  }

  /**
   *
   * @return the last element in the list. Be sure to sort the list on the desired coordinate before
   *    calling this function.
   */
  public Point4D getLast() {
    return pointList.get(pointList.size() - 1);
  }

  /**
   *
   * @return a list of the X coordinates, which may not be sorted
   */
  public List<Double> getX() {
    return pointList.stream().map(Point4D::getX).collect(Collectors.toList());
  }

  /**
   *
   * @return a list of the Y coordinates, which may not be sorted
   */
  public List<Double> getY() {
    return pointList.stream().map(Point4D::getY).collect(Collectors.toList());
  }

  /**
   *
   * @param x x value
   * @return the linearly interpolated y value at x
   */
  public double getY(double x) {
    PointList pl = subSetX(x, x);

    if (pl.size() == 1) return pl.getFirst().getY();

    double frac = (x - pl.getFirst().getX()) / (pl.getLast().getX() - pl.getFirst().getX());
    return pl.getFirst().getY() + frac * (pl.getLast().getY() - pl.getFirst().getY());
  }

  /**
   *
   * @return the number of elements in this list. If this list contains more than Integer.MAX_VALUE
   *    elements, returns Integer.MAX_VALUE.
   */
  public int size() {
    return pointList.size();
  }

  /**
   *
   * @param xMin minimum X value
   * @param xMax maximum X value
   * @return PointList of all points with X coordinates bounding xMin and xMax. Points are sorted on
   *    X.
   */
  public PointList subSetX(double xMin, double xMax) {
    PointList sortedOnX = new PointList();
    for (Point4D p : this) sortedOnX.add(p);
    sortedOnX.sort(COORDINATE.X);

    List<Double> x = sortedOnX.getX();

    int minIndex = Collections.binarySearch(x, xMin);
    if (minIndex < 0) minIndex = Math.abs(minIndex) - 1;
    int maxIndex = Collections.binarySearch(x, xMax);
    if (maxIndex < 0) maxIndex = Math.abs(maxIndex) - 1;

    minIndex = Math.max(0, minIndex - 1);
    maxIndex = Math.min(sortedOnX.size(), maxIndex + 1);
    PointList pl = new PointList();
    for (int i = minIndex; i < maxIndex; i++) {
      pl.add(sortedOnX.get(i));
    }
    return pl;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Point4D p : this) {
      sb.append(
          String.format(
              "%d %f %f %f %f %s\n",
              p.getIndex(),
              p.getX(),
              p.getY(),
              p.getZ(),
              p.getW(),
              colorRamp == null || Double.isNaN(p.getW()) ? "" : colorRamp.getColor(p.getW())));
    }
    return sb.toString();
  }

  @Override
  public Iterator<Point4D> iterator() {
    return pointList.iterator();
  }

  public enum COORDINATE {
    X,
    Y,
    Z,
    W,
    I
  }
}
