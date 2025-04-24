package terrasaur.utils.saaPlotLib.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.immutables.value.Value;
import picante.math.intervals.IntervalSet;
import picante.math.intervals.UnwritableInterval;
import terrasaur.utils.saaPlotLib.canvas.symbol.Symbol;
import terrasaur.utils.saaPlotLib.util.ImmutableLegendEntry;
import terrasaur.utils.saaPlotLib.util.LegendEntry;

@Value.Immutable
public abstract class Activity {

  public abstract String name();

  public abstract Color color();

  public abstract Optional<Symbol> symbol();

  private final IntervalSet.Builder intervals = IntervalSet.builder();

  public List<Interval> getIntervals() {
    IntervalSet intervalSet= intervals.build();
    List<Interval> intervalList = new ArrayList<>();
    for (UnwritableInterval i : intervalSet) intervalList.add(new Interval(i.getBegin(), i.getEnd()));
    return intervalList;
  }

  public void addInterval(Interval interval) {
    intervals.add(new UnwritableInterval(interval.getInf(), interval.getSup()));
  }

  public LegendEntry getLegendEntry() {
    return ImmutableLegendEntry.builder().name(name()).color(color()).symbol(symbol()).build();
  }
}
