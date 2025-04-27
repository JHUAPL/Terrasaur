package terrasaur.utils.saaPlotLib.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class for annotations along with their coordinates. This is just a {@link PointList}
 * with an added Map of point index to annotation.
 *
 * @author nairah1
 */
public class Annotations extends PointList {

  private final Map<Integer, Annotation> map;

  public Annotations() {
    map = new HashMap<>();
  }

  public void addAnnotation(Annotation annotation, double x, double y) {
    map.put(size(), annotation);
    add(x, y);
  }

  /**
   *
   * @param i index in the PointList
   * @return the annotation on point at index i
   */
  public Annotation getAnnotation(int i) {
    Point4D p = get(i);
    return getAnnotation(p);
  }

  /**
   * Return an annotation given a point p. Note that this point object MUST have been previously
   * supplied to the Annotations class via {@link #addAnnotation(Annotation, double, double)}.
   *
   * <p>Example:
   *
   * <pre>
   * for (Point4D p : annotations) {
   *   Annotation a = annotations.getAnnotation(p);
   *   // do something
   * }
   * </pre>
   *
   * @param p point
   * @return annotation, or null if not defined
   */
  public Annotation getAnnotation(Point4D p) {
    return map.get(p.getIndex());
  }
}
