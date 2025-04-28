/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
