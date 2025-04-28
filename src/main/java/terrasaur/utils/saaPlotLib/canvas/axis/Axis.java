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

import java.awt.Color;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public abstract class Axis {

  private static final Logger logger = LogManager.getLogger(Axis.class);

  protected TickMarks tickMarks;
  protected Color axisColor;
  protected AxisRange range;
  protected boolean isLog;
  protected AxisRange logRange;
  protected NavigableMap<Double, String> tickLabels;
  protected String title;
  protected Function<Double, String> tickFunc;
  protected double rotateTitle;
  protected double rotateLabels;

  protected Axis(Axis other) {
    this(other.getRange().getBegin(), other.getRange().getEnd(), other.getTitle());
    setTo(other);
  }

  protected Axis(double begin, double end, String title) {
    this(begin, end, title, StringFunctions.fixedFormat("%.2f"));
  }

  protected Axis(double begin, double end, String title, Function<Double, String> tickFunc) {
    axisColor = Color.black;
    range = new AxisRange(begin, end);
    rotateTitle = 0;
    rotateLabels = 0;
    isLog = false;
    logRange = null;

    if (range.getLength() < 2e-15) {
      range = new AxisRange(range.getMiddle() - 1e-15, range.getMiddle() + 1e-15);
      logger.warn(
          String.format(
              "%s: Axis length is %g! Setting to [%e, %e].",
              title, end - begin, range.getBegin(), range.getEnd()));
    }

    this.tickMarks = new TickMarks(range, isLog);

    this.title = title;
    if (title == null) this.title = "";

    setTickLabelFunction(tickFunc);
  }

  protected void setTo(Axis other) {
    this.tickMarks = new TickMarks(other.tickMarks);
    this.axisColor = other.axisColor;
    this.range = other.range;
    this.isLog = other.isLog;
    this.logRange = other.logRange;
    this.tickLabels = new TreeMap<>(other.tickLabels);
    this.title = other.title;
    this.tickFunc = other.tickFunc;
    this.rotateTitle = other.rotateTitle;
    this.rotateLabels = other.rotateLabels;
  }

  public boolean isLog() {
    return isLog;
  }

  public void setLog(boolean isLog) {
    this.isLog = isLog;
    if (isLog) {
      if (range.getBegin() <= 0 || range.getEnd() <= 0) {
        throw new RuntimeException(String.format("Cannot create log axis with range %s\n", range));
      }

      tickMarks = new TickMarks(range, true);
      logRange = new AxisRange(Math.log10(range.getBegin()), Math.log10(range.getEnd()));

      setTickLabelFunction(tickFunc);
    }
  }

  public void setNMinorTicks(int nMinorTicks) {
    tickMarks.setNMinorTicks(nMinorTicks);
  }

  public void setTickLabels(NavigableMap<Double, String> labels, int nMinorTicks) {
    tickLabels = new TreeMap<>();
    for (Double tick : labels.keySet()) {
      tickLabels.put(tick, labels.get(tick));
    }
    tickMarks.setMajorTicks(tickLabels.navigableKeySet());
    setNMinorTicks(nMinorTicks);
  }

  public Function<Double, String> getTickLabelFunction() {
    return tickFunc;
  }

  public void setTickLabelFunction(Function<Double, String> tickFunc) {
    this.tickFunc = tickFunc;
    tickLabels = new TreeMap<>();
    for (double tick : tickMarks.getMajorTicks()) {
      tickLabels.put(tick, tickFunc.apply(tick));
    }
  }

  public double dataToPixel(int pixelMin, int pixelMax, double value) {
    double frac = (value - range.getBegin()) / (range.getEnd() - range.getBegin());
    if (isLog) {
      frac = (Math.log10(value) - logRange.getBegin()) / (logRange.getEnd() - logRange.getBegin());
    }
    return (pixelMin + frac * (pixelMax - pixelMin));
  }

  public double pixelToData(int pixelMin, int pixelMax, double value) {
    double frac = (value - pixelMin) / (pixelMax - pixelMin);
    if (isLog) return (logRange.getBegin() + frac * (logRange.getEnd() - logRange.getBegin()));
    else return (range.getBegin() + frac * (range.getEnd() - range.getBegin()));
  }

  public Color getAxisColor() {
    return axisColor;
  }

  public void setAxisColor(Color axisColor) {
    this.axisColor = axisColor;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public AxisRange getRange() {
    return range;
  }

  public NavigableSet<Double> getMajorTicks() {
    return tickMarks.getMajorTicks();
  }

  public NavigableMap<Double, String> getTickLabels() {
    return tickLabels;
  }

  public void setTickLabels(NavigableMap<Double, String> labels) {
    setTickLabels(labels, 0);
  }

  public NavigableSet<Double> getMinorTicks() {
    return tickMarks.getMinorTicks();
  }

  public void setMinorTicks(NavigableSet<Double> minorTicks) {
    tickMarks.setMinorTicks(minorTicks);
  }

  public double getRotateTitle() {
    return rotateTitle;
  }

  public void setRotateTitle(double rotateTitle) {
    this.rotateTitle = rotateTitle;
  }

  public double getRotateLabels() {
    return rotateLabels;
  }

  /**
   * @param rotateLabels Angle to rotate label clockwise, in radians
   */
  public void setRotateLabels(double rotateLabels) {
    this.rotateLabels = rotateLabels;
  }
}
