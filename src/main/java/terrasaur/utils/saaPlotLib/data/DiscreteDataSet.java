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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.symbol.Symbol;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.util.ImmutableLegendEntry;
import terrasaur.utils.saaPlotLib.util.LegendEntry;

public class DiscreteDataSet {

  protected Color color;
  protected ColorRamp colorRamp;
  protected PointList data;
  protected Stroke stroke;
  protected DescriptiveStatistics xStats;
  protected DescriptiveStatistics yStats;
  protected String name;
  protected PLOTTYPE plotType;
  protected Symbol symbol;

  public DiscreteDataSet(String name) {
    this.name = name;
    color = Color.blue;
    colorRamp = null;
    data = new PointList();
    stroke = new BasicStroke(1.5f);
    plotType = PLOTTYPE.LINE;
    symbol = null;
  }

  public void setTo(DiscreteDataSet other) {
    this.color = other.color;
    this.colorRamp = other.colorRamp;
    this.data = other.data;
    this.stroke = other.stroke;
    this.xStats = other.xStats;
    this.yStats = other.yStats;
    this.name = other.name;
  }

  /**
   * @param x X value
   * @param y Y value
   */
  public void add(double x, double y) {
    data.add(x, y);
  }

  /**
   * @param x X value
   * @param y Y value
   * @param z Z value
   */
  public void add(double x, double y, double z) {
    data.add(x, y, z);
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w.
   *
   * @param x X value
   * @param y Y value
   * @param z Z value
   * @param w W value (this is often a property of the 3D point)
   */
  public void add(double x, double y, double z, double w) {
    data.add(x, y, z, w);
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w.
   *
   * @param xyz 3D coordinates
   * @param w W value (this is often a property of the 3D point)
   */
  public void add(Point3D xyz, double w) {
    data.add(xyz.x(), xyz.y(), xyz.z(), w);
  }

  /**
   * Add a new point with coordinates (x, y, z) and property w. The error bounds on x are (x.getX()
   * - x.getY(), x.getX() + x.getZ()) and similarly for y and z.
   *
   * @param x x value with error bars
   * @param y x value with error bars
   * @param z x value with error bars
   * @param w W value (this is often a property of the 3D point)
   */
  public void add(Point3D x, Point3D y, Point3D z, double w) {
    data.add(x, y, z, w);
  }

  public void add(List<Double> x, List<Double> y) {
    for (int i = 0; i < x.size(); i++) data.add(x.get(i), y.get(i));
  }

  public void add(Map<Double, Double> map) {
    for (double key : map.keySet()) {
      data.add(key, map.get(key));
    }
  }

  public LegendEntry getLegendEntry() {
    ImmutableLegendEntry.Builder builder = ImmutableLegendEntry.builder().name(name).color(color);
    if (stroke != null) builder.stroke(stroke);
    if (symbol != null) builder.symbol(symbol);
    return builder.build();
  }

  public Color getColor() {
    return color;
  }

  /**
   * @param color color for this data set
   */
  public void setColor(Color color) {
    this.color = color;
  }

  public ColorRamp getColorRamp() {
    return colorRamp;
  }

  /**
   * @param colorRamp color ramp. Presently only implemented for plotting symbols.
   */
  public void setColorRamp(ColorRamp colorRamp) {
    this.colorRamp = colorRamp;
  }

  public PointList getData() {
    return data;
  }

  /** clear the data array */
  public void clear() {
    data = new PointList();
  }

  public AxisX defaultXAxis(String title) {
    DescriptiveStatistics xStats = getXStats();
    return new AxisX(xStats.getMin(), xStats.getMax(), title);
  }

  public AxisY defaultYAxis(String title) {
    DescriptiveStatistics yStats = getYStats();
    return new AxisY(yStats.getMin(), yStats.getMax(), title);
  }

  public DescriptiveStatistics getXStats() {

    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (double x : data.getX()) stats.addValue(x);

    return stats;
  }

  public DescriptiveStatistics getYStats() {

    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (double y : data.getY()) stats.addValue(y);

    return stats;
  }

  public Stroke getStroke() {
    return stroke;
  }

  public void setStroke(Stroke stroke) {
    this.stroke = stroke;
  }

  public String getName() {
    return name;
  }

  public PLOTTYPE getPlotType() {
    return plotType;
  }

  public void setPlotType(PLOTTYPE plotType) {
    this.plotType = plotType;
  }

  public Symbol getSymbol() {
    return symbol;
  }

  public void setSymbol(Symbol symbol) {
    this.symbol = symbol;
    setPlotType(PLOTTYPE.SYMBOL);
  }

  public enum PLOTTYPE {
    BAR,
    LINE,
    SAND,
    SYMBOL
  }
}
