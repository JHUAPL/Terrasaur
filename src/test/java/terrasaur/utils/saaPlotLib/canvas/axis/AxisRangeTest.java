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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AxisRangeTest {

  @Test
  public void testLinear() {

    double begin = 0.01234567;
    double end = 12.34567;

    double tolerance = 1e-12;

    AxisRange range = AxisRange.getLinearAxis(begin, end, 1);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.01, tolerance);
    assertEquals(range.getEnd(), 20, tolerance);

    range = AxisRange.getLinearAxis(begin, end, 2);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.012, tolerance);
    assertEquals(range.getEnd(), 13, tolerance);

    range = AxisRange.getLinearAxis(begin, end, 3);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.0123, tolerance);
    assertEquals(range.getEnd(), 12.4, tolerance);

    range = AxisRange.getLinearAxis(begin, end, 4);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.01234, tolerance);
    assertEquals(range.getEnd(), 12.35, tolerance);

    range = AxisRange.getLinearAxis(begin, end, 5);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.012345, tolerance);
    assertEquals(range.getEnd(), 12.346, tolerance);

    range = AxisRange.getLinearAxis(begin, end, 6);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.0123456, tolerance);
    assertEquals(range.getEnd(), 12.3457, tolerance);
  }

  @Test
  public void testLog() {
    double tolerance = 1e-12;

    AxisRange range = AxisRange.getLogAxis(1.33, 4.55);
    assertNotNull(range);
    assertEquals(range.getBegin(), 1, tolerance);
    assertEquals(range.getEnd(), 10, tolerance);

    range = AxisRange.getLogAxis(1.33, 45.5);
    assertNotNull(range);
    assertEquals(range.getBegin(), 1, tolerance);
    assertEquals(range.getEnd(), 100, tolerance);

    range = AxisRange.getLogAxis(.0133, 455);
    assertNotNull(range);
    assertEquals(range.getBegin(), 0.01, tolerance);
    assertEquals(range.getEnd(), 1000, tolerance);
  }

}
