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
package terrasaur.utils.saaPlotLib.canvas;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.apache.commons.math3.analysis.MultivariateFunction;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisRange;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.config.PlotConfig;

public class AreaPlot extends DiscreteDataPlot {

  public AreaPlot(PlotConfig config) {
    super(config);
  }

  /**
   * Plot the 2D function using the ramp and supplied axes.
   *
   * @param func 2D function to plot
   * @param ramp color ramp
   * @param xAxis X axis
   * @param yAxis Y axis
   */
  public void plot(MultivariateFunction func, ColorRamp ramp, AxisX xAxis, AxisY yAxis) {
    plot(func, ramp, xAxis, yAxis, xAxis.getRange(), yAxis.getRange());
  }

  /**
   * Plot the 2D function using the ramp and supplied axes. Coordinates outside the supplied ranges
   * will not be plotted.
   *
   * @param func 2D function to plot
   * @param ramp color ramp
   * @param xAxis X axis
   * @param yAxis Y axis
   * @param xRange X axis range
   * @param yRange Y axis range
   */
  public void plot(
      MultivariateFunction func,
      ColorRamp ramp,
      AxisX xAxis,
      AxisY yAxis,
      AxisRange xRange,
      AxisRange yRange) {
    double[] point = new double[2];
    for (int i = 0; i < config.width(); i++) {
      point[0] = pixelXtoData(xAxis, config.leftMargin() + i);
      if (xRange.closedContains(point[0])) {
        for (int j = 0; j < config.height(); j++) {
          point[1] = pixelYtoData(yAxis, config.topMargin() + j);
          if (yRange.closedContains(point[1])) {
            double value = func.value(point);
            Color color = ramp.getColor(value);
            if (color.getAlpha() > 0)
              image.setRGB(config.leftMargin() + i, config.topMargin() + j, color.getRGB());
          }
        }
      }
    }
  }

  /**
   * Plot the 2D function using the ramp and supplied axes. Coordinates outside the supplied shape
   * will not be plotted.
   *
   * @param func 2D function to plot
   * @param ramp color ramp
   * @param xAxis X axis
   * @param yAxis Y axis
   * @param path shape in data coordinates
   */
  public void plot(
      MultivariateFunction func,
      ColorRamp ramp,
      AxisX xAxis,
      AxisY yAxis,
      List<Point2D.Double> path) {

    // create a shape in pixel coordinates
    GeneralPath gp = new GeneralPath();
    double x = dataXtoPixel(xAxis, path.get(0).getX());
    double y = dataYtoPixel(yAxis, path.get(0).getY());
    gp.moveTo(x, y);
    for (int i = 1; i < path.size(); i++) {
      Point2D.Double xy = path.get(i);
      x = dataXtoPixel(xAxis, xy.getX());
      y = dataYtoPixel(yAxis, xy.getY());
      gp.lineTo(x, y);
    }
    gp.closePath();

    Rectangle2D bounds = gp.getBounds();

    double[] point = new double[2];
    for (int i = (int) bounds.getMinX(); i < (int) bounds.getMaxX() + 1; i++) {
      for (int j = (int) bounds.getMinY(); j < (int) bounds.getMaxY() + 1; j++) {
        if (gp.contains(i, j)) {
          point[0] = pixelXtoData(xAxis, config.leftMargin() + i);
          point[1] = pixelYtoData(yAxis, config.topMargin() + j);
          double value = func.value(point);
          Color color = ramp.getColor(value);
          if (color.getAlpha() > 0)
            image.setRGB(config.leftMargin() + i, config.topMargin() + j, color.getRGB());
        }
      }
    }
  }
}
