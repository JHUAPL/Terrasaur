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
