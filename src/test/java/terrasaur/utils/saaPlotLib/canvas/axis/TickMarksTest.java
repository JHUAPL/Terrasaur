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
