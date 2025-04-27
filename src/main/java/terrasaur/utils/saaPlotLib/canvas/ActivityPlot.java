package terrasaur.utils.saaPlotLib.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.symbol.Symbol;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Activity;
import terrasaur.utils.saaPlotLib.data.ActivitySet;
import terrasaur.utils.saaPlotLib.util.Keyword;

public class ActivityPlot extends RectangularPlotCanvas {

  boolean outline;

  public ActivityPlot(PlotConfig config) {
    super(config);
    outline = true;
  }

  public ActivityPlot setOutline(boolean outline) {
    this.outline = outline;
    return this;
  }

  /** draw the axes - note tick labels are suppressed if axis title length is 0. */
  @Override
  public void drawAxes() {
    // draw the y axes in plot(ActivitySet, Axis)
    drawLowerAxis();
    drawUpperAxis();

    if (!config.title().isEmpty()) {
      Graphics2D g = image.createGraphics();
      configureHintsForSubpixelQuality(g);

      int x = config.leftMargin() + config.width() / 2;

      g.setFont(config.titleFont());
      g.setColor(Color.BLACK);
      addAnnotation(
          g,
          config.title(),
          x,
          config.topMargin() * 0.2,
          Keyword.ALIGN_CENTER,
          Keyword.ALIGN_CENTER);
    }
  }

  public void plot(List<ActivitySet> activityList) {
    double rotateLabels = yLeftAxis.getRotateLabels();
    double rotateTitle = yLeftAxis.getRotateTitle();

    NavigableSet<Double> minorTicks = yLeftAxis.getMinorTicks();
    yLeftAxis = new AxisY(-0.5, activityList.size() - 0.5, yLeftAxis.getTitle());
    yLeftAxis.setMinorTicks(minorTicks);
    yLeftAxis.setRotateLabels(rotateLabels);
    yLeftAxis.setRotateTitle(rotateTitle);

    yRightAxis = new AxisY(yLeftAxis.getRange().getBegin(), yLeftAxis.getRange().getEnd(), "");
    yRightAxis.setMinorTicks(minorTicks);

    NavigableMap<Double, String> labels = new TreeMap<>();
    NavigableMap<Double, String> emptyLabels = new TreeMap<>();
    double barHalfHeight = 0.25;

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setClip(config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height());

    for (int i = 0; i < activityList.size(); i++) {
      ActivitySet as = activityList.get(activityList.size() - 1 - i);
      labels.put((double) i, String.format("%s", as.getName()));
      emptyLabels.put((double) i, "");

      Map<Interval, Activity> activityMap = as.getActivityMap();
      for (Interval interval : activityMap.keySet()) {
        Activity a = activityMap.get(interval);

        Color fillColor = a.color();
        g.setColor(fillColor);

        double bottomY = dataYtoPixel(yLeftAxis, i + barHalfHeight);
        double height = dataYtoPixel(yLeftAxis, i - barHalfHeight) - bottomY;

        double leftX = dataXtoPixel(xLowerAxis, interval.getInf());
        double rightX = dataXtoPixel(xLowerAxis, interval.getSup());

        if (a.symbol().isPresent()) {
          Symbol symbol = a.symbol().get();
          double y = dataYtoPixel(yLeftAxis, i);
          symbol.draw(g, leftX, y);
          symbol.draw(g, rightX, y);
        } else {
          double width = rightX - leftX;
          g.fillRect(
              (int) (leftX + 0.5),
              (int) (bottomY + 0.5),
              (int) (width + 0.5),
              (int) (height + 0.5));
          g.setColor(outline ? Color.BLACK : fillColor);
          g.drawRect(
              (int) (leftX + 0.5),
              (int) (bottomY + 0.5),
              (int) (width + 0.5),
              (int) (height + 0.5));
        }
        g.setColor(fillColor);
      }
    }

    yLeftAxis.setTickLabels(labels);
    yRightAxis.setTickLabels(emptyLabels);

    drawLeftAxis();
    drawRightAxis();
  }
}
