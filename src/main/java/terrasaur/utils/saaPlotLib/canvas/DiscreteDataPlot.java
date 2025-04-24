package terrasaur.utils.saaPlotLib.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.Point4D;
import terrasaur.utils.saaPlotLib.data.PointList;

public class DiscreteDataPlot extends RectangularPlotCanvas {

  // used for sand plot
  private PointList baseline;

  public DiscreteDataPlot(PlotConfig config) {
    super(config);
  }

  /**
   * Plots the DataSet ds using the X lower axis and Y left axis
   *
   * @param ds DiscreteDataSet to plot
   */
  public void plot(DiscreteDataSet ds) {
    plot(ds, xLowerAxis, yLeftAxis);
  }

  /**
   * Plots the DataSet ds using the supplied X axis and the Y left axis
   *
   * @param ds DiscreteDataSet to plot
   * @param xAxis lower X axis
   */
  public void plot(DiscreteDataSet ds, AxisX xAxis) {
    plot(ds, xAxis, yLeftAxis);
  }

  /**
   * Plots the DataSet ds using the lower X axis and the supplied Y axis
   *
   * @param ds DiscreteDataSet to plot
   * @param yAxis left Y axis
   */
  public void plot(DiscreteDataSet ds, AxisY yAxis) {
    plot(ds, xLowerAxis, yAxis);
  }

  /**
   * Plots the DataSet ds using the supplied axes
   *
   * @param ds DiscreteDataSet to plot
   * @param xAxis lower X axis
   * @param yAxis left Y axis
   */
  public void plot(DiscreteDataSet ds, AxisX xAxis, AxisY yAxis) {
    switch (ds.getPlotType()) {
      case LINE:
        plotLine(ds, xAxis, yAxis);
        break;
      case BAR:
        plotBar(ds, xAxis);
        break;
      case SAND:
        plotSand(ds, xAxis, yAxis);
        break;
      case SYMBOL:
        plotSymbol(ds, xAxis, yAxis);
        break;
      default:
    }
  }

  /**
   * Draw a filled shape on the plot
   *
   * @param color fill color
   * @param outline outline to fill
   */
  public void plot(Color color, List<Point2D> outline) {
    plot(color, outline, true);
  }

  /**
   * Draw a shape on the plot
   *
   * @param color shape color
   * @param outline outline to draw
   * @param fill if true, fill outline
   */
  public void plot(Color color, List<Point2D> outline, boolean fill) {
    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(color);
    g.setClip(
        new Rectangle(config.leftMargin(), config.topMargin(), config.width(), config.height()));

    GeneralPath gp = new GeneralPath();
    Point2D xy;
    for (Point2D point2D : outline) {
      xy = point2D;
      if (xy == null) continue;

      double pixelX =
          xLowerAxis.dataToPixel(config.getLeftPlotEdge(), config.getRightPlotEdge(), xy.getX());
      double pixelY =
          yLeftAxis.dataToPixel(config.getBottomPlotEdge(), config.getTopPlotEdge(), xy.getY());

      if (gp.getCurrentPoint() == null) gp.moveTo(pixelX, pixelY);
      else gp.lineTo(pixelX, pixelY);
    }

    if (gp.getCurrentPoint() != null) {
      gp.closePath();
      if (fill) g.fill(gp);
      g.draw(gp);
    }
  }

  private double roundToNearestTenth(double x) {
    return Math.round(x * 10) / 10.;
  }

  private void plotLine(DiscreteDataSet ds, AxisX xAxis, AxisY yAxis) {
    PointList xy = ds.getData();
    if (xy.size() == 0) return;

    Point4D prior = xy.getFirst();

    // Store points to plot if they differ from the previous point by more than a tenth of a pixel.
    // This can save a lot of memory if there are a lot of points.
    List<Point2D> pointMap = new ArrayList<>();
    Point2D lastPoint =
        new Point2D.Double(
            roundToNearestTenth(dataXtoPixel(xAxis, prior.getX())),
            roundToNearestTenth(dataYtoPixel(yAxis, prior.getY())));
    pointMap.add(lastPoint);
    for (int i = 1; i < xy.size(); i++) {
      Point4D p = xy.get(i);
      Point2D nextPoint =
          new Point2D.Double(
              roundToNearestTenth(dataXtoPixel(xAxis, p.getX())),
              roundToNearestTenth(dataYtoPixel(yAxis, p.getY())));
      if (lastPoint.getX() != nextPoint.getX() || lastPoint.getY() != nextPoint.getY()) {
        pointMap.add(nextPoint);
        lastPoint = nextPoint;
      }
    }

    GeneralPath gp = new GeneralPath();
    gp.moveTo(pointMap.get(0).getX(), pointMap.get(0).getY());
    for (int i = 1; i < pointMap.size(); i++) {
      gp.lineTo(pointMap.get(i).getX(), pointMap.get(i).getY());
    }

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setClip(
        new Rectangle2D.Double(
            config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height()));
    g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());
    g.draw(gp);
  }

  private void plotBar(DiscreteDataSet ds, AxisX xAxis) {
    PointList xy = ds.getData().subSetX(xAxis.getRange().getMin(), xAxis.getRange().getMax());
    if (xy.size() == 0) return;

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setClip(
        new Rectangle2D.Double(
            config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height()));

    List<Double> xValues = xy.getX();
    // need a prior and next for each point
    if (xValues.size() < 3) return;

    for (int i = 0; i < xValues.size(); i++) {
      double priorX;
      double thisX = xValues.get(i);
      double nextX;
      if (i == 0) {
        nextX = xValues.get(i + 1);
        priorX = thisX - (nextX - thisX);
      } else if (i == xValues.size() - 1) {
        priorX = xValues.get(i - 1);
        nextX = thisX - priorX + thisX;
      } else {
        priorX = xValues.get(i - 1);
        nextX = xValues.get(i + 1);
      }

      double leftX =
          dataXtoPixel(
              xLowerAxis,
              Math.max((xValues.get(i) + priorX) / 2, xLowerAxis.getRange().getBegin()));
      double rectWidth =
          dataXtoPixel(
                  xLowerAxis,
                  Math.min((xValues.get(i) + nextX) / 2, xLowerAxis.getRange().getEnd()))
              - leftX;

      double topY = dataYtoPixel(yLeftAxis, xy.get(i).getY());
      double botY = dataYtoPixel(yLeftAxis, 0);

      // origin is at the upper left, Y increases downward
      if (topY > botY) {
        double tmp = topY;
        topY = botY;
        botY = tmp;
      }

      double rectHeight = Math.abs(topY - botY);
      g.setColor(ds.getColor());
      g.fillRect(
          (int) (leftX + 0.5),
          (int) (topY + 0.5),
          (int) (rectWidth + 0.5),
          (int) (rectHeight + 0.5));
      g.setColor(Color.BLACK);
      g.drawRect(
          (int) (leftX + 0.5),
          (int) (topY + 0.5),
          (int) (rectWidth + 0.5),
          (int) (rectHeight + 0.5));
    }
  }

  private void plotSand(DiscreteDataSet ds, AxisX xAxis, AxisY yAxis) {
    PointList xy = ds.getData().subSetX(xAxis.getRange().getMin(), xAxis.getRange().getMax());

    if (xy.size() == 0) return;

    if (baseline == null) {
      baseline = new PointList();
      for (double x : xy.getX()) {
        baseline.add(x, Double.MIN_VALUE);
      }
    }

    PointList newBaseline = new PointList();

    GeneralPath gp = new GeneralPath();
    Point4D prior = xy.getFirst();
    double priorX = prior.getX();
    double y = prior.getY() + baseline.getY(priorX);
    newBaseline.add(priorX, y);
    gp.moveTo(dataXtoPixel(xAxis, priorX), dataYtoPixel(yAxis, y));
    for (int i = 1; i < xy.size() - 1; i++) {
      Point4D p = xy.get(i);
      double x = p.getX();
      y = p.getY() + baseline.getY(x);
      newBaseline.add(x, y);
      gp.lineTo(dataXtoPixel(xAxis, x), dataYtoPixel(yAxis, y));
    }
    Point4D next = xy.getLast();
    double nextX = next.getX();
    y = next.getY() + baseline.getY(nextX);
    newBaseline.add(nextX, y);
    gp.lineTo(dataXtoPixel(xAxis, nextX), dataYtoPixel(yAxis, y));

    gp.lineTo(dataXtoPixel(xAxis, nextX), dataYtoPixel(yAxis, baseline.getY(nextX)));
    for (int i = 1; i < xy.size() - 1; i++) {
      Point4D p = xy.get(xy.size() - 1 - i);
      gp.lineTo(dataXtoPixel(xAxis, p.getX()), dataYtoPixel(yAxis, baseline.getY(p.getX())));
    }

    gp.lineTo(dataXtoPixel(xAxis, priorX), dataYtoPixel(yAxis, baseline.getY(priorX)));
    gp.lineTo(
        dataXtoPixel(xAxis, priorX), dataYtoPixel(yAxis, prior.getY() + baseline.getY(priorX)));

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setClip(
        new Rectangle2D.Double(
            config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height()));
    g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());
    g.fill(gp);

    baseline = newBaseline;
  }

  private void plotSymbol(DiscreteDataSet ds, AxisX xAxis, AxisY yAxis) {
    PointList xy = ds.getData().subSetX(xAxis.getRange().getMin(), xAxis.getRange().getMax());
    if (xy.size() == 0) return;

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setClip(
        new Rectangle2D.Double(
            config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height()));
    if (ds.getStroke() != null) g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());

    Point4D prior = xy.getFirst();
    double pixelX = dataXtoPixel(xAxis, prior.getX());
    double pixelY = dataYtoPixel(yAxis, prior.getY());

    if (ds.getColorRamp() != null) {
      if (!Double.isNaN(prior.getW())) g.setColor(ds.getColorRamp().getColor(prior.getW()));
    }
    ds.getSymbol().draw(g, pixelX, pixelY);

    Point2D xError = prior.getXError();
    xError =
        new Point2D.Double(
            dataXtoPixel(xAxis, prior.getX() - xError.getX()),
            dataXtoPixel(xAxis, prior.getX() + xError.getY()));

    Point2D yError = prior.getYError();
    yError =
        new Point2D.Double(
            dataYtoPixel(yAxis, prior.getY() - yError.getX()),
            dataYtoPixel(yAxis, prior.getY() + yError.getY()));

    ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);

    for (int i = 1; i < xy.size() - 1; i++) {
      Point4D p = xy.get(i);
      pixelX = dataXtoPixel(xAxis, p.getX());
      pixelY = dataYtoPixel(yAxis, p.getY());

      if (ds.getColorRamp() != null) {
        if (!Double.isNaN(p.getW())) g.setColor(ds.getColorRamp().getColor(p.getW()));
      }

      ds.getSymbol().draw(g, pixelX, pixelY);
      xError = p.getXError();
      xError =
          new Point2D.Double(
              dataXtoPixel(xAxis, p.getX() - xError.getX()),
              dataXtoPixel(xAxis, p.getX() + xError.getY()));

      yError = p.getYError();
      yError =
          new Point2D.Double(
              dataYtoPixel(yAxis, p.getY() - yError.getX()),
              dataYtoPixel(yAxis, p.getY() + yError.getY()));

      ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);
    }

    Point4D next = xy.getLast();
    pixelX = dataXtoPixel(xAxis, next.getX());
    pixelY = dataYtoPixel(yAxis, next.getY());

    if (ds.getColorRamp() != null) {
      if (!Double.isNaN(next.getW())) g.setColor(ds.getColorRamp().getColor(next.getW()));
    }

    ds.getSymbol().draw(g, pixelX, pixelY);
    xError = next.getXError();
    if (xError != null) {
      xError =
          new Point2D.Double(
              dataXtoPixel(xAxis, next.getX() - xError.getX()),
              dataXtoPixel(xAxis, next.getX() + xError.getY()));
    }
    yError = next.getYError();
    if (yError != null) {
      yError =
          new Point2D.Double(
              dataYtoPixel(yAxis, next.getY() - yError.getX()),
              dataYtoPixel(yAxis, next.getY() + yError.getY()));
    }
    ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);
  }
}
