package terrasaur.utils.saaPlotLib.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import picante.math.intervals.UnwritableInterval;
import picante.timeline.StateTimeline;

public class ActivitySet {

  private final StateTimeline.Builder<Optional<Activity>> builder;
  private String setName;

  public ActivitySet(String setName) {
    this.setName = setName;
    builder =
        StateTimeline.create(
            new UnwritableInterval(-Double.MAX_VALUE, Double.MAX_VALUE), Optional.empty());
  }

  public String getName() {
    return setName;
  }

  public void setName(String setName) {
    this.setName = setName;
  }

  /**
   * @param a activity to add. Overlapping activities will be overwritten.
   */
  public void addActivity(Activity a) {
    for (Interval interval : a.getIntervals()) {
      if (interval.getSize() > 0) builder.add(new UnwritableInterval(interval.getInf(), interval.getSup()), Optional.of(a));
    }
  }

  /**
   * @return a sorted list of intervals and activities.
   */
  public Map<Interval, Activity> getActivityMap() {
    StateTimeline<Optional<Activity>> timeline = getTimeline();

    Map<Interval, Activity> activityMap = new LinkedHashMap<>();
    for (Entry<UnwritableInterval, Optional<Activity>> entry : timeline.getEntries()) {
      UnwritableInterval interval = entry.getKey();
      Optional<Activity> activity = entry.getValue();
      activity.ifPresent(value -> activityMap.put(new Interval(interval.getBegin(), interval.getEnd()), value));
    }
    return activityMap;
  }

  /**
   * @return {@link StateTimeline} of activities.
   */
  private StateTimeline<Optional<Activity>> getTimeline() {
    return builder.build();
  }
}
