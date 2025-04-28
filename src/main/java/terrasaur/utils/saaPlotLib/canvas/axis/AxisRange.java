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
package terrasaur.utils.saaPlotLib.canvas.axis;

import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Range to use for an Axis. Similar to {@link Interval} but the beginning value can be
 * greater than the end for decreasing axes.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class AxisRange {

  private final static Logger logger = LogManager.getLogger(AxisRange.class);

  private final double begin;
  private final double end;

  private final double min;
  private final double max;
  private final double length;

  private final boolean isDecreasing;

  /**
   * 
   * @return starting (left) value
   */
  public double getBegin() {
    return begin;
  }

  /**
   * 
   * @return ending (right) value
   */
  public double getEnd() {
    return end;
  }

  /**
   * 
   * @return the smaller endpoint
   */
  public double getMin() {
    return min;
  }

  /**
   * 
   * @return the larger endpoint
   */
  public double getMax() {
    return max;
  }

  /**
   *
   * @return the middle point in the interval: (this.begin + this.end)/2.0
   */
  public double getMiddle() {
    return (end + begin) / 2.0;
  }

  /**
   * 
   * @return the value of {@link #getMax()} - {@link #getMin()}. This is always a non-negative
   *         value.
   */
  public double getLength() {
    return length;
  }

  /**
   * 
   * @return true if {@link #getBegin()} > {@link #getEnd()}
   */
  public boolean isDecreasing() {
    return isDecreasing;
  }

  /**
   * Define a range where the beginning value may be greater than the ending value.
   * 
   * @param begin leftmost value
   * @param end rightmost value
   */
  public AxisRange(double begin, double end) {
    this.begin = begin;
    this.end = end;
    this.min = Math.min(begin, end);
    this.max = Math.max(begin, end);
    this.length = this.max - this.min;
    this.isDecreasing = begin > end;
  }

  /**
   * Is the supplied value contained within the interval in a [,] fashion?
   * 
   * @param value the value to consider for containment
   * 
   * @return true if value lies in [this.min,this.max]; false otherwise
   */
  public boolean closedContains(double value) {
    return (value >= min) && (value <= max);
  }

  /**
   * Clamps the supplied value into the range supported by the interval. This function returns:
   * <ul>
   * <li>( value &le; begin ) -> begin</li>
   * <li>( value &ge; end ) -> end</li>
   * <li>value, otherwise</li>
   * </ul>
   * 
   * @param value the value to clamp
   * 
   * @return a value in the range [this.begin, this.end]
   */
  public double clamp(double value) {
    return Math.min(Math.max(min, value), max);
  }

  /**
   * <p>
   * Examples:
   * <table border="1">
   * <tr>
   * <th>min</th>
   * <th>Axis begin</th>
   * <th>max</th>
   * <th>Axis end</th>
   * </tr>
   * <tr>
   * <td>1.33</td>
   * <td>1</td>
   * <td>4.55</td>
   * <td>10</td>
   * </tr>
   * <tr>
   * <td>1.33</td>
   * <td>1</td>
   * <td>45.5</td>
   * <td>100</td>
   * </tr>
   * <tr>
   * <td>0.0133</td>
   * <td>0.01</td>
   * <td>455</td>
   * <td>1000</td>
   * </tr>
   * </table>
   *
   * @param min minimum value.  Axis minimum will be the highest power of ten equal to or less than min.
   * @param max maximum value.  Axis maximum will be the lowest power of ten equal to or greater than max.
   * @return axis range with the lower and upper limits rounded to the next lower and higher
   *    powers of ten.
   */
  public static AxisRange getLogAxis(double min, double max) {
    if (min <= 0) {
      logger.error(
          new RuntimeException("Minimum " + min + " passed to getLogAxis()!  Returning null."));
      return null;
    }

    if (max <= 0) {
      logger.error(
          new RuntimeException("Maximum " + max + " passed to getLogAxis()!  Returning null."));
      return null;
    }

    int minExp = (int) Math.floor(Math.log10(min));
    int maxExp = (int) Math.ceil(Math.log10(max));
    double begin = Math.pow(10., minExp);
    double end = Math.pow(10., maxExp);
    return new AxisRange(begin, end);
  }

  /**
   * <p>
   * Examples for a min of 0.01234567 and max of 12.34567:
   * <table border="1">
   * <tr>
   * <th>Precision</th>
   * <th>Axis begin</th>
   * <th>Axis end</th>
   * </tr>
   * <tr>
   * <td>1</td>
   * <td>0.01</td>
   * <td>20</td>
   * </tr>
   * <tr>
   * <td>2</td>
   * <td>0.012</td>
   * <td>13</td>
   * </tr>
   * <tr>
   * <td>3</td>
   * <td>0.0123</td>
   * <td>12.4</td>
   * </tr>
   * <tr>
   * <td>4</td>
   * <td>0.01234</td>
   * <td>12.35</td>
   * </tr>
   * <tr>
   * <td>5</td>
   * <td>0.012345</td>
   * <td>12.346</td>
   * </tr>
   * <tr>
   * <td>6</td>
   * <td>0.0123456</td>
   * <td>12.3457</td>
   * </tr>
   * </table>
   *
   * @param min minimum value.  Axis minimum will be equal to this value using the desired number of significant figures
   * @param max maximum value.  Axis maximum will be equal to this value using the desired number of significant figures
   * @param precision number of significant figures to use
   * @return axis range with the lower and upper limits having the desired number of significant
   *     figures.
   */
  public static AxisRange getLinearAxis(double min, double max, int precision) {

    if (precision <= 0) {
      logger.error(new RuntimeException(
          "Precision " + precision + " passed to getLinearAxis()!  Returning null."));
      return null;
    }

    double scale = Math.pow(10, precision - 1);

    double roundedMax = 0;
    if (max != 0) {
      int exp = (int) Math.floor(Math.log10(Math.abs(max)));
      double mantissa = max / Math.pow(10, exp);
      roundedMax = Math.ceil(mantissa * scale) / scale * Math.pow(10, exp);
    }

    double roundedMin = 0;
    if (min != 0) {
      int exp = (int) Math.floor(Math.log10(Math.abs(min)));
      double mantissa = min / Math.pow(10, exp);
      roundedMin = Math.floor(mantissa * scale) / scale * Math.pow(10, exp);

    }

    return new AxisRange(roundedMin, roundedMax);
  }

}
