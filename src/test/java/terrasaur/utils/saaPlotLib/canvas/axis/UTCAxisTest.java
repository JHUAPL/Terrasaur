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
import picante.time.TimeConversion;

public class UTCAxisTest {

  @Test
  public void test1() {
    TimeConversion tc = TimeConversion.createUsingInternalConstants();

    double min = tc.utcStringToTDB("2035-11-05T16:02:17.782");
    double max = tc.utcStringToTDB("2036-01-24T12:30:17.780");

    UTCAxisX axis = new UTCAxisX(min, max, "UTC", 9);

    NavigableSet<Double> majorTicks = axis.getMajorTicks();
    NavigableSet<Double> minorTicks = axis.getMinorTicks();

    System.out.println(majorTicks.size() + " major ticks");

    NavigableMap<Double, String> ticks = new TreeMap<>();
    int count = 0;
    for (Double majorTick : majorTicks) {
      ticks.put(majorTick, String.format("%d %f", count++, majorTick));
    }
    for (Double minorTick : minorTicks) {
      if (!ticks.containsKey(minorTick))
        ticks.put(minorTick, String.format("\t%f", minorTick));
    }

    for (Double tick : ticks.keySet()) {
      System.out.printf("%-20s %s\n", ticks.get(tick), tc.tdbToUTC(tick).toString());
    }
    
    NavigableMap<Double, String> tickLabels = axis.getTickLabels();
    for (Double tick : tickLabels.keySet())
      System.out.printf("%f %s\n", tick, tickLabels.get(tick));

  }

  @Test
  public void test2() {
    TimeConversion tc = TimeConversion.createUsingInternalConstants();

    double min = 0;
    double max = 50 * 365.25 * 86400.;

    UTCAxisX axis = new UTCAxisX(min, max, "UTC", 5);

    NavigableSet<Double> majorTicks = axis.getMajorTicks();
    NavigableSet<Double> minorTicks = axis.getMinorTicks();

    System.out.println(majorTicks.size() + " major ticks");

    NavigableMap<Double, String> ticks = new TreeMap<>();
    int count = 0;
    for (Double majorTick : majorTicks) {
      ticks.put(majorTick, String.format("%d %f", count++, majorTick));
    }
    for (Double minorTick : minorTicks) {
      if (!ticks.containsKey(minorTick))
        ticks.put(minorTick, String.format("\t%f", minorTick));
    }

    for (Double tick : ticks.keySet()) {
      System.out.printf("%-20s %s\n", ticks.get(tick), tc.tdbToUTC(tick).toString());
    }

  }

}
