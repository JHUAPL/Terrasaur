package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;

// package private, called from Axis
class TickMarks {

  private double tickSpacing;

  private final boolean isLog;

  protected final AxisRange range;

  public AxisRange getRange() {
    return range;
  }

  private NavigableSet<Double> majorTicks;

  public NavigableSet<Double> getMajorTicks() {
    return majorTicks;
  }

  private NavigableSet<Double> minorTicks;

  public NavigableSet<Double> getMinorTicks() {
    return minorTicks;
  }

  TickMarks(TickMarks other) {
    this.tickSpacing = other.tickSpacing;
    this.isLog = other.isLog;
    this.range = new AxisRange(other.getRange().getBegin(), other.getRange().getEnd());
    majorTicks = Collections.unmodifiableNavigableSet(other.majorTicks);
    minorTicks = Collections.unmodifiableNavigableSet(other.minorTicks);
  }

  TickMarks(AxisRange range, boolean isLog) {

    this.range = range;
    this.isLog = isLog;

    if (isLog) {
      if (range.getBegin() <= 0 || range.getEnd() <= 0 || range.getLength() == 0) {
        throw new RuntimeException(String.format("Cannot create log axis with range %s\n", range));
      }

      range = new AxisRange(Math.log10(range.getBegin()), Math.log10(range.getEnd()));
    }

    tickSpacing = Math.pow(10, Math.floor(Math.log10(range.getLength())));
    int nTicks = (int) Math.ceil(range.getLength() / tickSpacing);

    while (nTicks < 5) {
      tickSpacing /= 2;
      nTicks = (int) Math.ceil(range.getLength() / tickSpacing);

      if (nTicks == 0) {
        nTicks = 5;
        tickSpacing = range.getLength() / nTicks;
        break;
      }
    }

    double begin = tickSpacing * Math.ceil(range.getMin() / tickSpacing);
    double end = nTicks * tickSpacing + begin;
    setNTicks(nTicks, new AxisRange(begin, end));
    setNMinorTicks(isLog ? 9 : 4);
  }

  protected void setNTicks(int nTicks, AxisRange range) {
    tickSpacing = range.getLength() / nTicks;
    NavigableSet<Double> majorTicks = new TreeSet<>();
    if (isLog) {
      double tick = Math.pow(10, range.getMin() - tickSpacing);
      majorTicks.add(tick);
      while (tick < Math.pow(10, range.getMax() + tickSpacing)) {
        tick *= Math.pow(10, tickSpacing);
        majorTicks.add(tick);
      }
    } else {
      double tick = range.getMin() - tickSpacing;
      while (tick < range.getMax() + tickSpacing) {
        majorTicks.add(tick);
        tick += tickSpacing;
      }
    }
    setMajorTicks(majorTicks);
  }

  /**
   * Divide the axis range into nTicks intervals. There will be nTicks-1 ticks between the axis
   * begin and end.
   *
   * @param nTicks number of intervals
   */
  public void setNTicks(int nTicks) {
    setNTicks(nTicks, range);
  }

  /**
   * Divide each major tick interval into nMinorTicks. There will be nMinorTicks-1 minor ticks drawn
   * in each interval.
   *
   * @param nMinorTicks number of intervals
   */
  public void setNMinorTicks(int nMinorTicks) {
    NavigableSet<Double> minorTicks = new TreeSet<>();
    for (Double majorTick : majorTicks) {
      if (nMinorTicks > 0) {
        Double nextTick = majorTicks.higher(majorTick);
        if (nextTick != null) {
          for (int i = 1; i < nMinorTicks; i++) {
            double minorTick = i * (nextTick - majorTick) / nMinorTicks + majorTick;
            minorTicks.add(minorTick);
          }
        }
      }
    }
    setMinorTicks(minorTicks);
  }

  public void setMajorTicks(NavigableSet<Double> majorTicks) {
    if (majorTicks != null) this.majorTicks = Collections.unmodifiableNavigableSet(majorTicks);
  }

  public void setMinorTicks(NavigableSet<Double> minorTicks) {
    if (minorTicks != null) this.minorTicks = Collections.unmodifiableNavigableSet(minorTicks);
  }
}
