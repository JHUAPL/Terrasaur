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
