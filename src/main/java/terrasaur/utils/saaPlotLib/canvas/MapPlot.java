package terrasaur.utils.saaPlotLib.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.util.MathUtils;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.RotationMatrixIJK;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import picante.mechanics.rotations.AxisAndAngle;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisRange;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.projection.Projection;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionRectangular;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotation;
import terrasaur.utils.saaPlotLib.data.Annotations;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.Point4D;
import terrasaur.utils.saaPlotLib.data.PointList;
import terrasaur.utils.saaPlotLib.util.Keyword;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class MapPlot extends AreaPlot {

  private final Projection proj;
  private ProjectionRectangular sourceMapProj;
  private final List<Integer> offsets;
  public MapPlot(PlotConfig config, Projection p) {
    super(config);
    proj = p;
    sourceMapProj = null;

    offsets = new ArrayList<>();
    offsets.add(0);
    if (proj.isWrapAround()) {
      offsets.add(-width);
      offsets.add(width);
    }
  }

  /**
   * Set background using a rectangular map
   *
   * @param sourceMap image file
   * @param lv center point of the map
   */
  public void setBackgroundMap(BufferedImage sourceMap, LatitudinalVector lv) {

    sourceMapProj = new ProjectionRectangular(sourceMap.getWidth(), sourceMap.getHeight(), lv);

    for (int i = 0; i < proj.getWidth(); i++) {
      for (int j = 0; j < proj.getHeight(); j++) {
        LatitudinalVector ll = proj.pixelToSpherical(i, j);
        if (ll != null) {
          Point2D.Double xy = sourceMapProj.sphericalToPixel(ll);
          int argb = sourceMap.getRGB((int) xy.getX(), (int) xy.getY());
          image.setRGB(config.leftMargin() + i, config.topMargin() + j, argb);
        }
      }
    }
  }

  /**
   * @param latSpacing spacing between latitude lines, in radians
   * @param lonSpacing spacing between longitude lines, in radians
   * @param annotate mark lat/lon lines
   */
  public void drawLatLonGrid(double latSpacing, double lonSpacing, boolean annotate) {

    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(config.gridColor());
    g.setClip(
        new Rectangle(config.leftMargin(), config.topMargin(), config.width(), config.height()));

    NavigableSet<Double> lats = new TreeSet<>();
    NavigableSet<Double> lons = new TreeSet<>();

    for (double lat = -Math.PI / 2; lat < Math.PI / 2; lat += latSpacing) lats.add(lat);

    for (double lon = 0; lon < 2 * Math.PI; lon += lonSpacing) lons.add(lon);

    NavigableMap<Double, Double> latLengthMap = new TreeMap<>();
    for (double lat : lats) {
      double length = drawLatitudeLine(lat);
      latLengthMap.put(length, lat);
    }

    NavigableMap<Double, Double> lonLengthMap = new TreeMap<>();
    for (double lon : lons) {
      double length = drawLongitudeLine(lon);
      lonLengthMap.put(length, lon);
    }

    if (annotate) {
      g.setFont(config.gridFont());

      for (double lat : lats) {

        // along each latitude line, draw the label on the longest longitude line
        double lon = lonLengthMap.lastEntry().getValue();

        NavigableSet<Point2D> lonCrossings =
            new TreeSet<>(Comparator.comparingDouble(Point2D::getX));
        Point2D xy = proj.sphericalToPixel(lat, lon + lonSpacing / 2);
        if (xy != null) lonCrossings.add(xy);
        xy = proj.sphericalToPixel(lat, lon - lonSpacing / 2);
        if (xy != null) lonCrossings.add(xy);

        if (!lonCrossings.isEmpty()) {
          Point2D p = lonCrossings.first();
          addAnnotation(
              g,
              StringFunctions.toDegreesLat("%.0f").apply(lat),
              p.getX() + config.leftMargin(),
              p.getY() + config.topMargin(),
              Keyword.ALIGN_BOTTOM,
              Keyword.ALIGN_CENTER);
        }
      }

      for (double lon : lons) {

        // along each longitude line, draw the label on the longest latitude line
        double lat = latLengthMap.lastEntry().getValue();

        NavigableSet<Point2D> latCrossings =
            new TreeSet<>(Comparator.comparingDouble(Point2D::getY));

        // draw the label closer to the equator
        double labelLat = (lat < 0) ? lat + latSpacing / 2 : lat - latSpacing / 2;
        Point2D xy = proj.sphericalToPixel(labelLat, lon);
        if (xy != null) latCrossings.add(xy);

        if (!latCrossings.isEmpty()) {
          Point2D p = latCrossings.last();
          addAnnotation(
              g,
              StringFunctions.toDegreesELon("%.0f").apply(lon),
              p.getX() + config.leftMargin(),
              p.getY() + config.topMargin(),
              Keyword.ALIGN_BOTTOM,
              Keyword.ALIGN_CENTER);
        }
      }
    }
  }

  /**
   * @param lat in radians
   * @return length of the longest segment drawn, in pixels (line may be broken at 0 longitude)
   */
  public double drawLatitudeLine(double lat) {
    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(config.gridColor());
    g.setClip(
        new Rectangle(config.leftMargin(), config.topMargin(), config.width(), config.height()));

    double longestLength = 0;
    double length = 0;
    Point2D lastPoint = null;
    GeneralPath gp = new GeneralPath();
    final double spacing = proj.radiansPerPixel();
    for (double lon = 0; lon < 2 * Math.PI; lon += spacing) {
      Point2D xy = proj.sphericalToPixel(lat, lon);
      if (xy == null) {
        if (gp.getCurrentPoint() != null) {
          g.draw(gp);
          gp = new GeneralPath();
          if (length > longestLength) {
            longestLength = length;
          }
        }
      } else {
        if (gp.getCurrentPoint() == null) {
          gp.moveTo(xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
        } else {
          if (lastPoint != null) {
            gp.lineTo(xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
            double dx = xy.getX() - lastPoint.getX();
            double dy = xy.getY() - lastPoint.getY();
            length += Math.sqrt(dx * dx + dy * dy);
          }
        }
        lastPoint = xy;
      }
    }
    if (gp.getCurrentPoint() != null) g.draw(gp);
    if (length > longestLength) {
      longestLength = length;
    }
    return longestLength;
  }

  /**
   * @param lon in radians
   * @return length of line drawn
   */
  public double drawLongitudeLine(double lon) {
    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(config.gridColor());
    g.setClip(
        new Rectangle(config.leftMargin(), config.topMargin(), config.width(), config.height()));

    double length = 0;
    Point2D lastPoint = null;
    GeneralPath gp = new GeneralPath();
    final double spacing = proj.radiansPerPixel();
    for (double lat = -Math.PI / 2; lat < Math.PI / 2; lat += spacing) {
      Point2D xy = proj.sphericalToPixel(lat, lon);
      if (xy != null) {
        if (gp.getCurrentPoint() == null)
          gp.moveTo(xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
        else {
          if (lastPoint != null) {
            gp.lineTo(xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
            double dx = xy.getX() - lastPoint.getX();
            double dy = xy.getY() - lastPoint.getY();
            length += Math.sqrt(dx * dx + dy * dy);
          }
        }
        lastPoint = xy;
      }
    }
    if (gp.getCurrentPoint() != null) g.draw(gp);

    return length;
  }

  /**
   * @param center center
   * @param radius radius, measured as an angle from the center of the body
   * @param step angular step along the circumference. e.g. if step is pi/180, there will be one
   *     point per degree.
   * @param color color
   */
  public void drawCircle(LatitudinalVector center, double radius, double step, Color color) {
    List<Point2D> list = getCircle(center, radius, step);
    DiscreteDataSet outline = new DiscreteDataSet("");
    outline.setColor(color);
    for (Point2D p : list) outline.add(p.getX(), p.getY());
    plot(outline);
  }

  /**
   * @param center center
   * @param radius radius, measured as an angle from the center of the body
   * @param step angular step along the circumference. e.g. if step is pi/180, there will be one
   *     point per degree.
   * @param color color
   */
  public void drawFilledCircle(LatitudinalVector center, double radius, double step, Color color) {
    List<Point2D> list = getCircle(center, radius, step);
    plot(color, list, true);
  }

  /**
   * @param center center
   * @param radius radius, measured as an angle from the center of the body
   * @param step angular step along the circumference. e.g. if step is pi/180, there will be one
   *     point per degree.
   * @return return a list of points along the circumference.
   */
  private List<Point2D> getCircle(LatitudinalVector center, double radius, double step) {
    UnwritableVectorIJK centerIJK = CoordConverters.convert(center);

    // find a vector perpendicular to center
    UnwritableVectorIJK axis = VectorIJK.cross(centerIJK, VectorIJK.K);
    if (Math.abs(Math.sin(centerIJK.getSeparation(VectorIJK.K))) < 0.1)
      axis = VectorIJK.cross(centerIJK, VectorIJK.J);

    // find any point separated by radius from the center
    AxisAndAngle aaa = new AxisAndAngle(axis, radius);
    UnwritableVectorIJK border = aaa.getRotation(new RotationMatrixIJK()).mxv(centerIJK);
    LatitudinalVector lv = CoordConverters.convertToLatitudinal(border);
    aaa = new AxisAndAngle(centerIJK, step);

    List<Point2D> list = new ArrayList<>();
    list.add(new Point2D.Double(lv.getLongitude(), lv.getLatitude()));
    for (double angle = 0; angle <= 2 * Math.PI; angle += step) {
      border = aaa.getRotation(new RotationMatrixIJK()).mxv(border);
      lv = CoordConverters.convertToLatitudinal(border);
      list.add(
          new Point2D.Double(
              MathUtils.normalizeAngle(lv.getLongitude(), center.getLongitude()),
              lv.getLatitude()));
    }

    return list;
  }

  /**
   * Map is assumed to be in rectangular projection and have its upper left corner at lon -180, lat
   * 90 and lower right corner at lon 180, lat -90
   *
   * @param sourceMap image
   */
  public void setBackgroundMap(BufferedImage sourceMap) {
    setBackgroundMap(sourceMap, new LatitudinalVector(1, 0, 0));
  }

  /**
   * Plot the 2D function using the ramp and supplied axes. Coordinates outside the supplied ranges
   * will not be plotted.
   *
   * @param func input coordinates to func are longitude and latitude in radians
   * @param ramp color ramp
   * @param xRange allowed longitude range in radians
   * @param yRange allowed latitude range in radians
   */
  public void plot(MultivariateFunction func, ColorRamp ramp, AxisRange xRange, AxisRange yRange) {
    for (int i = 0; i < config.width(); i++) {
      for (int j = 0; j < config.height(); j++) {
        LatitudinalVector ll = proj.pixelToSpherical(i, j);
        if (ll == null) continue;
        if (xRange.closedContains(ll.getLongitude())) {
          if (yRange.closedContains(ll.getLatitude())) {
            double value = func.value(new double[] {ll.getLongitude(), ll.getLatitude()});
            Color color = ramp.getColor(value);
            if (color.getAlpha() > 0)
              image.setRGB(config.leftMargin() + i, config.topMargin() + j, color.getRGB());
          }
        }
      }
    }
  }

  @Override
  public void plot(
      MultivariateFunction func,
      ColorRamp ramp,
      AxisX xAxis,
      AxisY yAxis,
      AxisRange xRange,
      AxisRange yRange) {
    plot(func, ramp, xRange, yRange);
  }

  @Override
  public void plot(Color color, List<Point2D> outline) {
    plot(color, outline, true);
  }

  @Override
  public void plot(Color color, List<Point2D> outline, boolean fill) {

    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(color);
    g.setClip(config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height());

    for (double offset : offsets) {
      GeneralPath gp = new GeneralPath();
      double lastX = 0;
      for (Point2D point2D : outline) {
        Point2D xy = proj.sphericalToPixel(point2D.getY(), point2D.getX());
        if (xy == null) continue;

        double nextX = xy.getX() + config.leftMargin() + offset;
        double nextY = xy.getY() + config.topMargin();
        if (gp.getCurrentPoint() == null) {
          gp.moveTo(nextX, nextY);
        } else {
          if (proj.isWrapAround()) {
            if (Math.abs(lastX - nextX) > Math.abs(lastX - (nextX + width))) nextX += width;
            if (Math.abs(lastX - nextX) > Math.abs(lastX - (nextX - width))) nextX -= width;
          }
          gp.lineTo(nextX, nextY);
        }
        lastX = nextX;
      }

      if (gp.getCurrentPoint() != null) {
        gp.closePath();
        Rectangle2D bounds = gp.getBounds2D();
        if (bounds.getWidth() < config.width() / 2.) {
          if (fill) g.fill(gp);
          g.draw(gp);
        }
      }
    }
  }

  @Override
  public void addAnnotations(Annotations annotations) {

    for (double offset : offsets) {
      for (Point4D p : annotations) {
        Annotation a = annotations.getAnnotation(p);

        Point2D xy = proj.sphericalToPixel(p.getY(), p.getX());
        if (xy != null) {
          Graphics2D g = image.createGraphics();
          configureHintsForSubpixelQuality(g);

          g.setColor(a.color());
          g.setFont(a.font());
          g.setClip(
              config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height());

          addAnnotation(
              g,
              a.text(),
              xy.getX() + config.leftMargin() + offset,
              xy.getY() + config.topMargin(),
              a.verticalAlignment(),
              a.horizontalAlignment());
        }
      }
    }
  }

  /**
   * @param ds dataset to plot
   */
  @Override
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

    PointList points = ds.getData();
    if (points.size() == 0) return;

    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());
    g.setClip(config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height());

    for (double offset : offsets) {
      GeneralPath gp = new GeneralPath();
      double lastX = 0;
      for (int i = 0; i < points.size(); i++) {
        Point2D xy = proj.sphericalToPixel(points.get(i).getY(), points.get(i).getX());
        if (xy == null) continue;
        double nextX = xy.getX() + config.leftMargin() + offset;
        double nextY = xy.getY() + config.topMargin();
        if (gp.getCurrentPoint() == null) {
          gp.moveTo(nextX, nextY);
        } else {
          if (proj.isWrapAround()) {
            if (Math.abs(lastX - nextX) > Math.abs(lastX - (nextX + width))) nextX += width;
            if (Math.abs(lastX - nextX) > Math.abs(lastX - (nextX - width))) nextX -= width;
          }
          gp.lineTo(nextX, nextY);
        }
        lastX = nextX;
      }
      g.draw(gp);
    }
  }

  private void plotSymbol(DiscreteDataSet ds) {
    PointList points = ds.getData();
    if (points.size() == 0) return;

    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setStroke(ds.getStroke());
    g.setColor(ds.getColor());
    g.setClip(config.getLeftPlotEdge(), config.getTopPlotEdge(), config.width(), config.height());

    Point4D prior = points.getFirst();
    Point2D xy = proj.sphericalToPixel(prior.getY(), prior.getX());
    if (xy != null) {
      if (ds.getColorRamp() != null) {
        if (!Double.isNaN(prior.getW())) g.setColor(ds.getColorRamp().getColor(prior.getW()));
      }
      ds.getSymbol().draw(g, xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
    }
    for (int i = 1; i < points.size() - 1; i++) {
      Point4D p = points.get(i);
      xy = proj.sphericalToPixel(p.getY(), p.getX());
      if (xy != null) {
        if (ds.getColorRamp() != null) {
          if (!Double.isNaN(p.getW())) g.setColor(ds.getColorRamp().getColor(p.getW()));
        }
        ds.getSymbol().draw(g, xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
      }
    }

    Point4D next = points.getLast();
    xy = proj.sphericalToPixel(next.getY(), next.getX());
    if (xy != null)
      ds.getSymbol().draw(g, xy.getX() + config.leftMargin(), xy.getY() + config.topMargin());
  }
}
