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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.symbol.Square;
import terrasaur.utils.saaPlotLib.colorMaps.ColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.util.PlotUtils;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class ScatterPlotDemo {

  public static BufferedImage makePlot() {
    DiscreteDataSet scatter = new DiscreteDataSet("data");
    int npts = 1000;
    Random r = new Random();
    ColorRamp ramp = ColorRamp.createLinear(0, npts);
    scatter.setColorRamp(ramp);
    for (int i = 0; i < npts; i++) scatter.add(r.nextGaussian(), 3 * r.nextGaussian(), 0, i);

    scatter.setSymbol(new Square().setFill(true).setRotate(45));

    AxisX xAxis = new AxisX(10, -10, "X Axis");
    AxisY yAxis = new AxisY(-10, 10, "Y Axis");
    PlotConfig config = ImmutablePlotConfig.builder().build();
    DiscreteDataPlot canvas = new DiscreteDataPlot(config);
    canvas.setAxes(xAxis, yAxis);
    canvas.drawAxes();
    canvas.plot(scatter);

    ColorBar colorBar = ImmutableColorBar.builder()
            .rect(new Rectangle(config.leftMargin(), 40, config.width(), 10)).ramp(ramp).numTicks(5)
            .tickFunction(StringFunctions.fixedFormat("%.0f")).build();
    canvas.drawColorBar(colorBar);

    return canvas.getImage();
  }

  public static void main(String[] args) {
    BufferedImage image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
    PlotCanvas.writeImage("doc/images/scatterPlotDemo.png", image);
  }
}
