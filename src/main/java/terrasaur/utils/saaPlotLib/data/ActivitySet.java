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
