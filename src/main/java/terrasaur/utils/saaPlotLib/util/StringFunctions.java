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
package terrasaur.utils.saaPlotLib.util;

import java.util.function.Function;

public class StringFunctions {

  public static Function<Double, String> fixedFormat(String format) {
    return (t) -> String.format(format, t);
  }

  public static Function<Double, String> scale(String format, double scale) {
    return (t) -> String.format(format, t * scale);
  }

  public static Function<Double, String> toDegrees(String format) {
    return (t) -> String.format(format, Math.toDegrees(t));
  }

  public static Function<Double, String> toDegreesLat(String format) {
    return (t) -> String.format(format + (t > 0 ? "N" : "S"), Math.abs(Math.toDegrees(t)));
  }

  public static Function<Double, String> toDegreesLon(String format) {
    return (t) -> String.format(format + (t > 0 ? "E" : "W"), Math.abs(Math.toDegrees(t)));
  }

  public static Function<Double, String> toDegreesELon(String format) {
    return t -> {
      t = Math.toDegrees(t);
      if (t < 0) t += 360;
      if (t >= 360) t -= 360;
      return String.format(format + "E", t);
    };
  }

  public static Function<Double, String> toDegreesWLon(String format) {
    return t -> {
      t = -Math.toDegrees(t);
      if (t < 0) t += 360;
      if (t > 360) t -= 360;
      return String.format(format + "W", t);
    };
  }

  public static Function<Double, String> toRadians(String format) {
    return (t) -> String.format(format, Math.toRadians(t));
  }
}
