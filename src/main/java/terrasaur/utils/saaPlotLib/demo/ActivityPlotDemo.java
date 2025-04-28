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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import terrasaur.utils.saaPlotLib.canvas.ActivityPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.symbol.Circle;
import terrasaur.utils.saaPlotLib.canvas.symbol.Square;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Activity;
import terrasaur.utils.saaPlotLib.data.ActivitySet;
import terrasaur.utils.saaPlotLib.data.ImmutableActivity;
import terrasaur.utils.saaPlotLib.util.LegendEntry;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class ActivityPlotDemo {

  public static BufferedImage makePlot() {

    List<ActivitySet> activities = new ArrayList<>();
    ActivitySet as = new ActivitySet("First");

    Activity red = ImmutableActivity.builder().name(as.getName() + " Red").color(Color.RED).build();
    red.addInterval(new Interval(0.05, 0.35));
    red.addInterval(new Interval(0.45, 0.55));
    red.addInterval(new Interval(0.75, 0.85));
    as.addActivity(red);

    Activity orange =
        ImmutableActivity.builder().name(as.getName() + " Orange").color(Color.ORANGE).build();
    orange.addInterval(new Interval(0.30, 0.40));
    orange.addInterval(new Interval(0.60, 0.70));
    as.addActivity(orange);
    activities.add(as);

    as = new ActivitySet("Second");

    // this is a different activity than orange above, hence new object
    orange = ImmutableActivity.builder().name(as.getName() + " Orange").color(Color.ORANGE).build();
    orange.addInterval(new Interval(0.50, 0.65));
    as.addActivity(orange);

    Activity blue =
        ImmutableActivity.builder().name(as.getName() + " Blue").color(Color.BLUE).build();
    blue.addInterval(new Interval(0.35, 0.44));
    blue.addInterval(new Interval(0.6, 0.95));
    as.addActivity(blue);

    Activity circle =
        ImmutableActivity.builder()
            .name(as.getName() + " Circle")
            .color(Color.BLUE)
            .symbol(new Circle().setFill(true).setSize(8))
            .build();
    circle.addInterval(new Interval(0.30, 0.30));
    as.addActivity(circle);
    Activity diamond =
        ImmutableActivity.builder()
            .name(as.getName() + " Diamond")
            .color(Color.MAGENTA)
            .symbol(new Square().setFill(true).setSize(8).setRotate(45))
            .build();
    diamond.addInterval(new Interval(0.15, 0.15001)); // draw two overlapping symbols
    diamond.addInterval(new Interval(0.22, 0.24)); // draw two symbols
    as.addActivity(diamond);
    activities.add(as);

    as = new ActivitySet("Green");
    Activity green =
        ImmutableActivity.builder().name(as.getName() + " Green").color(Color.GREEN).build();
    green.addInterval(new Interval(-1, 1));
    as.addActivity(green);
    activities.add(as);

    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(800)
            .height(600)
            .rightMargin(220)
            .title("Activity Plot Example")
            .build();

    config =
        ImmutablePlotConfig.builder()
            .from(config)
            .legendPosition(
                new Point2D.Double(config.getRightPlotEdge() + 20, config.topMargin() + 50))
            .legendFont(new Font("Helvetica", Font.BOLD, 18))
            .legendColor(true)
            .build();

    AxisX xLowerAxis = new AxisX(0, 1, "X Axis");
    xLowerAxis.setMinorTicks(null);

    AxisY yLeftAxis = new AxisY(-1, 1, " ");
    yLeftAxis.setMinorTicks(null);

    ActivityPlot canvas = new ActivityPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();
    canvas.plot(activities);

    Set<LegendEntry> legendEntries = new LinkedHashSet<>();
    for (ActivitySet activitySet : activities) {
      for (Activity a : activitySet.getActivityMap().values()) {
        legendEntries.add(a.getLegendEntry());
      }
    }
    for (LegendEntry le : legendEntries) canvas.addToLegend(le);
    canvas.drawLegend();

    return canvas.getImage();
  }

  public static void main(String[] args) {
    BufferedImage image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
//    PlotCanvas.writeImage("doc/images/activityPlotDemo.png", image);
  }
}
