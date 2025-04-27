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
