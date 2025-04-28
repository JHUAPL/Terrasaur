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
package terrasaur.utils.saaPlotLib.demo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import terrasaur.utils.saaPlotLib.canvas.AreaPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.colorMaps.ColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.util.PlotUtils;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class AreaPlotDemo {

  public static BufferedImage mandelbrot(Interval xRange, Interval yRange) {

    MultivariateFunction func = point -> {
      double iter = 0;

      double re = point[0];
      double im = point[1];

      while (iter < 256) {

        double sqre = re * re - im * im + point[0];
        double sqim = 2 * re * im + point[1];

        re = sqre;
        im = sqim;

        if (Math.sqrt(re * re + im * im) > 2)
          break;

        iter++;

      }
      return iter;
    };

    PlotConfig config = ImmutablePlotConfig.builder().height(1200).build();
    config = ImmutablePlotConfig.builder().from(config).width(3 * config.height() / 2)
        .title(String.format("Center %f %f", xRange.getBarycenter(), yRange.getBarycenter())).build();

    AxisX xLowerAxis = new AxisX(xRange.getInf(), xRange.getSup(), "Re(c)");
    AxisY yLeftAxis = new AxisY(yRange.getInf(), yRange.getSup(), "Im(c)");

    ColorRamp ramp = ColorRamp.createLinear(0, 256).addLimitColors().createReverse();

    AreaPlot canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();

    canvas.plot(func, ramp, xLowerAxis, yLeftAxis);

    return canvas.getImage();
  }

  public static BufferedImage makePlot() {
    PlotConfig config =
        ImmutablePlotConfig.builder().width(800).height(600).title("cos(X) * cos(Y)").build();

    AxisX xLowerAxis = new AxisX(-180, 180, "X (degrees)");
    AxisY yLeftAxis = new AxisY(-180, 180, "Y (degrees)");

    MultivariateFunction func = point -> {
      // if (point[0] * point[1] > 0)
      // return Double.NaN;
      return Math.cos(Math.toRadians(point[0])) * Math.cos(Math.toRadians(point[1]));
    };

    ColorRamp ramp = ColorRamp.createLinear(-1, 1);
    AreaPlot canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();

    canvas.plot(func, ramp, xLowerAxis, yLeftAxis);

    // Horizontal, across the top
    canvas.drawColorBar(
        ImmutableColorBar.builder().rect(new Rectangle(config.leftMargin(), 40, config.width(), 10))
            .ramp(ramp).numTicks(5).tickFunction(StringFunctions.fixedFormat("%.1f")).build());

    // Vertical, along right side
    canvas.drawColorBar(ImmutableColorBar.builder()
        .rect(new Rectangle(canvas.getPageWidth() - 60, config.topMargin(), 10, config.height()))
        .ramp(ramp).numTicks(9).tickFunction(StringFunctions.fixedFormat("%.2f")).build());

    return canvas.getImage();
  }

  public static BufferedImage plotArray(boolean log) {

    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).title("X * Y").build();

    double[][] array = new double[101][101];
    int nX = array[0].length;
    int nY = array.length;
    for (int row = 0; row < nY; row++) {
      for (int col = 0; col < nX; col++) {
        array[row][col] = row * col;
      }
    }

    AxisX xLowerAxis = new AxisX(0, nX - 1, "X (pixel)", "%.2f");
    AxisY yLeftAxis = new AxisY(0, nY - 1, "Y (pixel)", "%.2f");

    MultivariateFunction func = point -> log ? Math.log10(array[(int) point[1]][(int) point[0]])
        : array[(int) point[1]][(int) point[0]];
    ColorRamp ramp =
        ColorRamp.createLinear(0, log ? Math.log10(array[nX - 1][nY - 1]) : array[nX - 1][nY - 1]);
    ColorBar colorBar = ImmutableColorBar.builder()
        .rect(new Rectangle(config.leftMargin(), 40, config.width(), 10)).ramp(ramp).numTicks(5)
        .tickFunction(StringFunctions.fixedFormat("%.0f")).log(true).build();

    AreaPlot canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();

    canvas.plot(func, ramp, xLowerAxis, yLeftAxis);

    canvas.drawColorBar(colorBar);

    return canvas.getImage();
  }

  public static void main(String[] args) {

    final double xCenter = -0.5;
    final double yCenter = 0;
    double width = 3;
    double height = 2 * width / 3;

    Interval xRange = new Interval(xCenter - width / 2, xCenter + width / 2);
    Interval yRange = new Interval(yCenter - height / 2, yCenter + height / 2);

    BufferedImage image = mandelbrot(xRange, yRange);
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);

    image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);

    image = plotArray(true);
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
    PlotCanvas.writeImage("doc/images/areaPlotDemo.png", image);
  }

}
