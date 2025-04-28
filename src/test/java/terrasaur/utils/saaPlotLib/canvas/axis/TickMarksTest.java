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

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import org.junit.Test;

public class TickMarksTest {

  @Test
  public void test1() {
    double begin = 0.1;
    double end = 1e5;

    boolean isLog = true;

    TickMarks tm = new TickMarks(new AxisRange(begin, end), isLog);

    NavigableSet<Double> majorTicks = tm.getMajorTicks();
    NavigableSet<Double> minorTicks = tm.getMinorTicks();

    NavigableMap<Double, String> ticks = new TreeMap<>();
    for (Double majorTick : majorTicks) {
      ticks.put(majorTick, String.format("Major tick: %f", majorTick));
    }
    for (Double minorTick : minorTicks) {
      if (!ticks.containsKey(minorTick))
        ticks.put(minorTick, String.format("Minor tick:\t %f", minorTick));
    }

    for (String tick : ticks.values()) {
      System.out.println(tick);
    }
  }

}
