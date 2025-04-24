package terrasaur.utils.saaPlotLib.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.NavigableMap;
import java.util.NavigableSet;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisR;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisTheta;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotation;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.Point4D;
import terrasaur.utils.saaPlotLib.data.PointList;
import terrasaur.utils.saaPlotLib.util.Keyword;

/**
 * 0,0 is at top left
 * 
 * @author nairah1
 *
 */
public class PolarPlot extends PlotCanvas {

  protected final AxisR axisR;
  protected final AxisTheta axisTheta;
  final protected int radius;
  final protected Point2D origin;
  private double rotate;

  public PolarPlot(PlotConfig config, AxisR axisR, AxisTheta axisTheta) {
    super(config);

    this.axisR = axisR;
    this.axisTheta = axisTheta;

    radius = Math.min(getWidth(), getHeight()) / 2;
    origin = new Point2D.Double(config.leftMargin() + getWidth() / 2.,
        config.topMargin() + getHeight() / 2.);

    rotate = 0;
  }

  public PolarPlot setRotate(double rotate) {
    this.rotate = rotate;
    return this;
  }

  private Graphics2D getGraphics(boolean applyTransform) {
    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    if (applyTransform) {
      AffineTransform transform =
          AffineTransform.getRotateInstance(-rotate, origin.getX(), origin.getY());
      g.setTransform(transform);
    }
    return g;
  }

  /**
   * 
   * @param a Annotation
   * @param x pixel x coordinate
   * @param y pixel y coordinate
   * @param rotate rotation angle in radians
   */
  @Override
  public void addAnnotation(Annotation a, double x, double y, double rotate) {
    Graphics2D g = getGraphics(true);
    g.setColor(a.color());
    g.setFont(a.font());
    g.translate(x, y);
    g.rotate(rotate);
    addAnnotation(g, a.text(), 0, 0, a.verticalAlignment(), a.horizontalAlignment());
  }

  @Override
  public void setPixel(double x, double y, int brightness, boolean whiteBackground) {

    Point2D src = new Point2D.Double(x, y);
    Point2D dst = getGraphics(true).getTransform().transform(src, new Point2D.Double());

    super.setPixel(dst.getX(), dst.getY(), brightness, whiteBackground);
  }

  /**
   * Draw tick marks and labels
   */
  public void drawAxes() {
    Graphics2D g = getGraphics(true);
    g.setFont(config.axisFont());

    // draw theta
    g.setColor(axisTheta.getAxisColor());
    g.draw(getBoundary());

    double majorTickLength = 0.2 * (config.topMargin() + config.bottomMargin());
    double minorTickLength = 0.5 * majorTickLength;

    NavigableSet<Double> minorTicks = axisTheta.getMinorTicks();
    if (minorTicks != null) {
      for (double minorTick : minorTicks) {
        if (!axisTheta.getRange().closedContains(minorTick))
          continue;

        Path2D.Double path = new Path2D.Double();
        path.moveTo(origin.getX() + (radius + minorTickLength / 2) * Math.cos(minorTick),
            origin.getY() - (radius + minorTickLength / 2) * Math.sin(minorTick));
        path.lineTo(origin.getX() + (radius - minorTickLength / 2) * Math.cos(minorTick),
            origin.getY() - (radius - minorTickLength / 2) * Math.sin(minorTick));
        g.draw(path);

      }
    }

    NavigableMap<Double, String> tickLabels = axisTheta.getTickLabels();
    for (double majorTick : tickLabels.keySet()) {
      if (!axisTheta.getRange().closedContains(majorTick))
        continue;
      Path2D.Double path = new Path2D.Double();
      path.moveTo(origin.getX() + (radius + minorTickLength / 2) * Math.cos(majorTick),
          origin.getY() - (radius + minorTickLength / 2) * Math.sin(majorTick));
      path.lineTo(origin.getX() + (radius - minorTickLength / 2) * Math.cos(majorTick),
          origin.getY() - (radius - minorTickLength / 2) * Math.sin(majorTick));
      g.draw(path);

      if (!axisTheta.getTitle().isEmpty()) {
        boolean printThis = true;
        if (majorTick == tickLabels.navigableKeySet().last()) {

          if (Math
              .abs(Math.sin(tickLabels.navigableKeySet().first())
                  - Math.sin(tickLabels.navigableKeySet().last())) < 1e-8
              && Math.abs(Math.cos(tickLabels.navigableKeySet().first())
                  - Math.cos(tickLabels.navigableKeySet().last())) < 1e-8) {
            printThis = false;
          }
        }

        if (printThis) {
          double pointX = origin.getX() + (radius + majorTickLength) * Math.cos(majorTick);
          double pointY = origin.getY() - (radius + majorTickLength) * Math.sin(majorTick);
          addAnnotation(g, tickLabels.get(majorTick), pointX, pointY, rotate);
        }
      }
    }

    // draw R
    g.setColor(axisR.getAxisColor());
    Point2D axisRLow = dataToPixel(axisR, axisR.getRange().getBegin(), 0);
    Point2D axisRHigh = dataToPixel(axisR, axisR.getRange().getEnd(), 0);

    // Draw the R axis
    if (!axisR.getTitle().isEmpty()) {
      g.drawLine((int) Math.round(axisRLow.getX()), (int) Math.round(origin.getY()),
          (int) Math.round(axisRHigh.getX()), (int) Math.round(origin.getY()));
    }

    majorTickLength = 0.2 * (config.topMargin() + config.bottomMargin());
    minorTickLength = 0.5 * majorTickLength;
    minorTicks = axisR.getMinorTicks();
    if (minorTicks != null) {
      for (double minorTick : minorTicks) {
        if (!axisR.getRange().closedContains(minorTick))
          continue;

        Path2D.Double path = new Path2D.Double();
        Point2D point = dataToPixel(axisR, minorTick, 0);
        path.moveTo(point.getX(), point.getY() + minorTickLength / 2);
        path.lineTo(point.getX(), point.getY() - minorTickLength / 2);
        g.draw(path);
      }
    }

    tickLabels = axisR.getTickLabels();
    for (double majorTick : tickLabels.keySet()) {
      if (!axisR.getRange().closedContains(majorTick))
        continue;

      Path2D.Double path = new Path2D.Double();
      Point2D point = dataToPixel(axisR, majorTick, 0);
      path.moveTo(point.getX(), point.getY() + majorTickLength / 2);
      path.lineTo(point.getX(), point.getY() - majorTickLength / 2);

      g.draw(path);

      if (!axisR.getTitle().isEmpty()) {
        addAnnotation(g, tickLabels.get(majorTick), point.getX(), origin.getY() + majorTickLength,
            rotate);
      }
    }


  }

  /**
   * Draw contours for the major tick marks on the R and Theta axes
   */
  public void drawGrid() {
    Graphics2D g = getGraphics(true);
    g.setFont(config.axisFont());
    g.setColor(config.gridColor());

    drawRGrid(g);
    drawThetaGrid(g);
  }

  /*
   * Draw concentric circles at the major tick marks for the R axis
   */
  private void drawRGrid(Graphics2D g) {

    final double majorTickLength = 0.2 * (config.topMargin() + config.bottomMargin());
    final double minorTickLength = 0.5 * majorTickLength;
    NavigableSet<Double> minorTicks = axisR.getMinorTicks();
    if (minorTicks != null) {
      for (double minorTick : minorTicks) {
        if (!axisR.getRange().closedContains(minorTick))
          continue;

        Path2D.Double path = new Path2D.Double();
        Point2D point = dataToPixel(axisR, minorTick, 0);
        path.moveTo(point.getX(), point.getY() + minorTickLength / 2);
        path.lineTo(point.getX(), point.getY() - minorTickLength / 2);
        g.draw(path);
      }
    }

    NavigableMap<Double, String> tickLabels = axisR.getTickLabels();
    for (double majorTick : tickLabels.keySet()) {
      if (!axisR.getRange().closedContains(majorTick))
        continue;

      Path2D.Double path = new Path2D.Double();
      Point2D point = dataToPixel(axisR, majorTick, 0);
      path.moveTo(point.getX(), point.getY() + majorTickLength / 2);
      path.lineTo(point.getX(), point.getY() - majorTickLength / 2);

      g.draw(path);

      double radius = Math.abs(point.getX() - origin.getX());

      Ellipse2D.Double circle = new Ellipse2D.Double(origin.getX() - radius, origin.getY() - radius,
          2 * radius, 2 * radius);
      g.draw(circle);
    }
  }

  private Arc2D.Double getBoundary() {
    Point2D axisRLow = dataToPixel(axisR, axisR.getRange().getBegin(), 0);
    Point2D axisRHigh = dataToPixel(axisR, axisR.getRange().getEnd(), 0);

    // Draw the theta boundary
    double radius = Math.max(Math.abs(origin.getX() - axisRLow.getX()),
        Math.abs(origin.getX() - axisRHigh.getX()));
    return new Arc2D.Double(origin.getX() - radius, origin.getY() - radius, 2 * radius, 2 * radius,
        Math.toDegrees(axisTheta.getRange().getBegin()),
        Math.toDegrees(axisTheta.getRange().getEnd()), Arc2D.OPEN);
  }

  /**
   * Draw radial lines at each major tick mark on the theta axis
   * 
   * @param g graphics object
   */
  private void drawThetaGrid(Graphics2D g) {

    NavigableMap<Double, String> tickLabels = axisTheta.getTickLabels();
    for (double majorTick : tickLabels.keySet()) {
      if (!axisTheta.getRange().closedContains(majorTick))
        continue;
      Path2D.Double path = new Path2D.Double();
      path.moveTo(origin.getX(), origin.getY());
      path.lineTo(origin.getX() + radius * Math.cos(majorTick),
          origin.getY() - radius * Math.sin(majorTick));
      g.draw(path);
    }
  }

  public void drawTitle() {
    Graphics2D g = getGraphics(false);
    g.setFont(config.axisFont());
    g.setColor(Color.BLACK);

    if (!config.title().isEmpty()) {
      int x = config.leftMargin() + config.width() / 2;

      g.setFont(config.titleFont());
      g.setColor(Color.BLACK);
      addAnnotation(g, config.title(), x, config.topMargin() * 0.2, Keyword.ALIGN_CENTER,
          Keyword.ALIGN_CENTER);
    }
  }

  /**
   * Plots the DataSet ds using the supplied axes
   * 
   * @param ds data set to plot
   */
  public void plot(DiscreteDataSet ds) {
    switch (ds.getPlotType()) {
      case LINE:
        plotLine(ds);
        break;
      case SYMBOL:
        plotSymbol(ds);
        break;
      default:
    }
  }

  private void plotLine(DiscreteDataSet ds) {
    PointList xy = ds.getData();
    GeneralPath gp = new GeneralPath();
    Point4D prior = xy.getFirst();
    Point2D pixel = dataToPixel(axisR, prior.getX(), prior.getY());
    gp.moveTo(pixel.getX(), pixel.getY());
    for (int i = 1; i < xy.size() - 1; i++) {
      Point4D p = xy.get(i);
      pixel = dataToPixel(axisR, p.getX(), p.getY());
      gp.lineTo(pixel.getX(), pixel.getY());
    }
    Point4D next = xy.getLast();
    pixel = dataToPixel(axisR, next.getX(), next.getY());
    gp.lineTo(pixel.getX(), pixel.getY());

    Graphics2D g = getGraphics(true);
    g.setClip(getBoundary());
    if (ds.getStroke() != null)
      g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());
    g.draw(gp);
  }

  private void plotSymbol(DiscreteDataSet ds) {
    PointList xy = ds.getData();

    Graphics2D g = getGraphics(true);
    g.setClip(getBoundary());
    if (ds.getStroke() != null)
      g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());

    Point4D prior = xy.getFirst();
    Point2D pixel = dataToPixel(axisR, prior.getX(), prior.getY());

    if (ds.getColorRamp() != null) {
      if (!Double.isNaN(prior.getW()))
        g.setColor(ds.getColorRamp().getColor(prior.getW()));
    }
    ds.getSymbol().draw(g, pixel.getX(), pixel.getY());
    /*-
    		Point2D xError = prior.getXError();
    		xError = new Point2D.Double(dataXtoPixel(xAxis, prior.getX() - xError.getX()),
    				dataXtoPixel(xAxis, prior.getX() + xError.getY()));
    
    		Point2D yError = prior.getYError();
    		yError = new Point2D.Double(dataYtoPixel(yAxis, prior.getY() - yError.getX()),
    				dataYtoPixel(yAxis, prior.getY() + yError.getY()));
    
    		ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);
    */
    for (int i = 1; i < xy.size() - 1; i++) {
      Point4D p = xy.get(i);
      pixel = dataToPixel(axisR, p.getX(), p.getY());

      if (ds.getColorRamp() != null) {
        if (!Double.isNaN(p.getW()))
          g.setColor(ds.getColorRamp().getColor(p.getW()));
      }

      ds.getSymbol().draw(g, pixel.getX(), pixel.getY());
      /*-
      xError = p.getXError();
      xError = new Point2D.Double(dataXtoPixel(xAxis, p.getX() - xError.getX()),
      		dataXtoPixel(xAxis, p.getX() + xError.getY()));
      
      yError = p.getYError();
      yError = new Point2D.Double(dataYtoPixel(yAxis, p.getY() - yError.getX()),
      		dataYtoPixel(yAxis, p.getY() + yError.getY()));
      
      ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);
      */
    }

    Point4D next = xy.getLast();
    pixel = dataToPixel(axisR, next.getX(), next.getY());

    if (ds.getColorRamp() != null) {
      if (!Double.isNaN(next.getW()))
        g.setColor(ds.getColorRamp().getColor(next.getW()));
    }

    ds.getSymbol().draw(g, pixel.getX(), pixel.getY());
    /*-
    xError = next.getXError();
    if (xError != null) {
    	xError = new Point2D.Double(dataXtoPixel(xAxis, next.getX() - xError.getX()),
    			dataXtoPixel(xAxis, next.getX() + xError.getY()));
    }
    yError = next.getYError();
    if (yError != null) {
    	yError = new Point2D.Double(dataYtoPixel(yAxis, next.getY() - yError.getX()),
    			dataYtoPixel(yAxis, next.getY() + yError.getY()));
    }
    ds.getSymbol().drawError(g, pixelX, pixelY, xError, yError);
    */
  }

  /**
   * 
   * @param axis radial axis
   * @param r radius
   * @param theta in radians
   * @return (x,y) coordinates of data
   */
  public Point2D dataToPixel(AxisR axis, double r, double theta) {
    return new Point2D.Double(
        origin.getX() + radius * r / axis.getRange().getEnd() * Math.cos(theta),
        origin.getY() - radius * r / axis.getRange().getEnd() * Math.sin(theta));
  }

  /**
   * @param axis radial axis
   * @param pixel pixel location
   * @return r, theta (in radians) of (x,y) coordinates
   */
  public Point2D pixelToData(AxisR axis, Point2D pixel) {
    double x = pixel.getX() - origin.getX();
    double y = pixel.getY() - origin.getY();

    double r = Math.sqrt(x * x + y * y);
    double theta = Math.atan2(y, x);

    return new Point2D.Double(r, theta);
  }

}
