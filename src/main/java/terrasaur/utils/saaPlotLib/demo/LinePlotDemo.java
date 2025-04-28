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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisRange;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.symbol.Square;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotations;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.ImmutableAnnotation;
import terrasaur.utils.saaPlotLib.data.Point3D;
import terrasaur.utils.saaPlotLib.data.PointList.COORDINATE;
import terrasaur.utils.saaPlotLib.util.Keyword;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class LinePlotDemo {

  private static DiscreteDataSet createSinX(AxisRange range) {
    DiscreteDataSet ds = new DiscreteDataSet("sin");
    for (int i = 0; i < 100; i++) {
      double x = range.getMin() + range.getLength() * i / 99.;

      /*-
      Only the Y coordinate has error bars. The error bar limits are sin(x)-0.05 and sin(x)+0.1.
      The Z coordinate is ignored for a line plot.
      x is also given as the property value.  This is used for the point's color.
      */
      ds.add(
          new Point3D(x, Double.NaN, Double.NaN),
          new Point3D(Math.sin(x), 0.05, 0.1),
          new Point3D(Double.NaN, Double.NaN, Double.NaN),
          x);
    }
    return ds;
  }

  private static DiscreteDataSet createCosX(AxisRange range) {
    DiscreteDataSet ds = new DiscreteDataSet("cos");
    for (int i = 0; i < 100; i++) {

      // random X coordinates
      double x = new Random().nextDouble() * range.getLength() + range.getMin();
      double y = Math.cos(x);
      ds.add(x, y);
    }

    // sort X coordinates
    ds.getData().sort(COORDINATE.X);

    return ds;
  }

  private static DiscreteDataSet createExpX(AxisRange range) {
    DiscreteDataSet ds = new DiscreteDataSet("exp");
    for (int i = 0; i < 100; i++) {
      double x = range.getMin() + range.getLength() * i / 99.;
      double y = Math.exp(x);
      ds.add(x, y);
    }
    return ds;
  }

  public static BufferedImage makeSimplePlot() {
    AxisX xAxis = new AxisX(-1, 10, "X Axis");
    AxisY yAxis = new AxisY(0.1, 1e5, "Y Axis", "%.4g");
    yAxis.setLog(true);
    DiscreteDataSet cos = createExpX(xAxis.getRange());
    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).build();
    DiscreteDataPlot canvas = new DiscreteDataPlot(config);
    canvas.setAxes(xAxis, yAxis);
    canvas.drawAxes();
    canvas.plot(cos);
    return canvas.getImage();
  }

  public static BufferedImage makePlot() {
    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(800)
            .height(600)
            .topMargin(120)
            .rightMargin(120)
            .title("Line Plot Example")
            .build();

    config =
        ImmutablePlotConfig.builder()
            .from(config)
            .legendPosition(new Point2D.Double(config.leftMargin() + 50, config.topMargin() + 50))
            .legendFont(new Font("Helvetica", Font.BOLD, 36))
            .gridColor(Color.LIGHT_GRAY)
            .build();

    AxisX xLowerAxis = new AxisX(0, 2 * Math.PI, "X Axis");
    AxisX xUpperAxis = new AxisX(-Math.PI, Math.PI, "Top Title");
    xLowerAxis.setRotateLabels(-Math.PI / 4);

    AxisY yLeftAxis = new AxisY(-1, 1, "Y Axis");
    AxisY yRightAxis = new AxisY(-1, 1, "Right Title");
    yRightAxis.setRotateTitle(Math.PI / 2);

    DiscreteDataPlot canvas = new DiscreteDataPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis, xUpperAxis, yRightAxis);

    DiscreteDataSet sin = createSinX(xLowerAxis.getRange());
    DiscreteDataSet cos = createCosX(xUpperAxis.getRange());
    float[] dash = {10.0f};
    cos.setStroke(
        new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
    cos.setColor(Color.RED);

    xLowerAxis.setAxisColor(sin.getColor());
    xUpperAxis.setAxisColor(cos.getColor());

    canvas.drawAxes();
    canvas.drawGrid();
    canvas.plot(sin);
    sin.setSymbol(new Square().setFill(true).setSize(4).setRotate(45));

    // give each symbol a different color based on its x value
    ColorRamp ramp = ColorRamp.createHue(sin.getXStats().getMin(), sin.getXStats().getMax());
    sin.setColorRamp(ramp);

    canvas.plot(sin);
    canvas.plot(cos, xUpperAxis);
    canvas.addToLegend(sin.getLegendEntry());
    canvas.addToLegend(cos.getLegendEntry());
    canvas.drawLegend();

    Annotations a = new Annotations();
    a.addAnnotation(ImmutableAnnotation.builder().text("default").build(), Math.PI / 2, 0);
    a.addAnnotation(
        ImmutableAnnotation.builder().text("blue").color(Color.BLUE).build(), Math.PI / 2, 0.25);
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("small italic green")
            .color(Color.GREEN)
            .font(new Font("Helvetica", Font.ITALIC, 6))
            .build(),
        Math.PI / 2,
        -0.25);
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("left")
            .horizontalAlignment(Keyword.ALIGN_RIGHT)
            .verticalAlignment(Keyword.ALIGN_CENTER)
            .build(),
        Math.PI / 2,
        -0.5);
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("right")
            .horizontalAlignment(Keyword.ALIGN_LEFT)
            .verticalAlignment(Keyword.ALIGN_CENTER)
            .build(),
        Math.PI / 2,
        -0.5);

    canvas.addAnnotations(a);

    return canvas.getImage();
  }

  public static void main(String[] args) {
    BufferedImage image = makeSimplePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);

    image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
//     PlotCanvas.writeImage("doc/images/linePlotDemo.png", image);
  }
}
