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
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.LatitudinalVector;
import terrasaur.utils.saaPlotLib.canvas.MapPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisRange;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.projection.Projection;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionOrthographic;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionRectangular;
import terrasaur.utils.saaPlotLib.canvas.symbol.Circle;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotations;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.ImmutableAnnotation;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class MapPlotDemo {

  private static final Logger logger = LogManager.getLogger(MapPlotDemo.class);

  private static List<Point2D> get180Block() {
    List<Point2D> block = new ArrayList<>();
    block.add(new Point2D.Double(Math.toRadians(-175), Math.toRadians(25)));
    block.add(new Point2D.Double(Math.toRadians(-170), Math.toRadians(25)));
    block.add(new Point2D.Double(Math.toRadians(175), Math.toRadians(-25)));
    block.add(new Point2D.Double(Math.toRadians(170), Math.toRadians(-25)));
    return block;
  }

  private static List<Point2D> get170Block() {
    List<Point2D> block = new ArrayList<>();
    block.add(new Point2D.Double(Math.toRadians(169), Math.toRadians(15)));
    block.add(new Point2D.Double(Math.toRadians(171), Math.toRadians(15)));
    block.add(new Point2D.Double(Math.toRadians(171), Math.toRadians(-5)));
    block.add(new Point2D.Double(Math.toRadians(169), Math.toRadians(-5)));
    return block;
  }

  private static List<Point2D> get190Block() {
    List<Point2D> block = new ArrayList<>();
    block.add(new Point2D.Double(Math.toRadians(189), Math.toRadians(15)));
    block.add(new Point2D.Double(Math.toRadians(191), Math.toRadians(15)));
    block.add(new Point2D.Double(Math.toRadians(191), Math.toRadians(-5)));
    block.add(new Point2D.Double(Math.toRadians(189), Math.toRadians(-5)));
    return block;
  }

  private static DiscreteDataSet LAXtoSYD() {
    DiscreteDataSet ds = new DiscreteDataSet("");
    ds.add(Math.toRadians(-118), Math.toRadians(34));
    ds.add(Math.toRadians(151), Math.toRadians(-34));
    ds.setColor(Color.CYAN);
    return ds;
  }

  private static GeneralPath getAustraliaMask() {
    // draw a shape over Australia
    Map<Double, Double> oz = new LinkedHashMap<>();
    oz.put(142.45, -11.1497);
    oz.put(153.31, -27.2056);
    oz.put(146.43, -39.0426);
    oz.put(131.13, -31.853);
    oz.put(115.02, -34.402);
    oz.put(113.83, -22.145);
    oz.put(121.26, -19.298);
    oz.put(132.77, -11.574);
    oz.put(137.00, -12.30);
    oz.put(135.52, -14.90);
    oz.put(140.71, -17.53);

    Map.Entry<Double, Double> first = oz.entrySet().iterator().next();
    GeneralPath gp = new GeneralPath();
    gp.moveTo(first.getKey(), first.getValue());
    for (Double lon : oz.keySet()) gp.lineTo(lon, oz.get(lon));
    gp.closePath();

    return gp;
  }

  public static BufferedImage makeRectangular(File baseMap) {
    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(800)
            .height(600)
            .title("Map Plot Example")
            .leftMargin(120)
            .rightMargin(80)
            .topMargin(100)
            .bottomMargin(80)
            .build();

    AxisX xLowerAxis = new AxisX(-180, 180, "Longitude (degrees)", "%.0fE");
    AxisY yLeftAxis = new AxisY(-90, 90, "Latitude (degrees)", "%.0f");

    GeneralPath gp = getAustraliaMask();
    /*
     * point[0] is longitude in radians, point[1] is latitude in radians.
     */
    MultivariateFunction func =
        point -> {
          // transparent pixel if point is outside the region
          if (!gp.contains(Math.toDegrees(point[0]), Math.toDegrees(point[1]))) return Double.NaN;

          return Math.cos(point[0]) * Math.cos(point[1]);
        };

    // Define the map projection
    LatitudinalVector centerPoint =
        new LatitudinalVector(
            1,
            Math.toRadians(yLeftAxis.getRange().getMiddle()),
            Math.toRadians(xLowerAxis.getRange().getMiddle()));
    Projection p = new ProjectionRectangular(config.width(), config.height(), centerPoint);

    MapPlot canvas = new MapPlot(config, p);
    try {
      BufferedImage map = ImageIO.read(baseMap);
      canvas.setBackgroundMap(map);
    } catch (IOException e) {
      e.printStackTrace();
    }

    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();

    ColorRamp ramp = ColorRamp.createHue(-1, 1);
    canvas.plot(func, ramp, xLowerAxis, yLeftAxis);

    canvas.plot(Color.RED, get170Block());
    canvas.plot(Color.GREEN, get180Block());
    canvas.plot(Color.BLUE, get190Block());

    canvas.plot(LAXtoSYD());

    Annotations a = new Annotations();
    a.addAnnotation(
        ImmutableAnnotation.builder().text("Perth").build(),
        Math.toRadians(115.82),
        Math.toRadians(-31.97));
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("Auckland")
            .color(Color.ORANGE)
            .font(new Font("Times New Roman", Font.BOLD, 24))
            .build(),
        Math.toRadians(174.74),
        Math.toRadians(-36.840556));
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("Singapore")
            .color(Color.WHITE)
            .font(new Font("Times New Roman", Font.BOLD, 24))
            .build(),
        Math.toRadians(103.9),
        Math.toRadians(1.20));
    canvas.drawLatLonGrid(Math.toRadians(30), Math.toRadians(30), true);
    canvas.addAnnotations(a);

    return canvas.getImage();
  }

  public static BufferedImage makeOrthographic(File baseMap) {
    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(800)
            .height(600)
            .title("Map Plot Example")
            .leftMargin(80)
            .rightMargin(80)
            .topMargin(100)
            .bottomMargin(80)
            .build();

    GeneralPath gp = getAustraliaMask();
    /*
     * point[0] is longitude in radians, point[1] is latitude in radians.
     */
    MultivariateFunction func =
        point -> {
          // transparent pixel if point is outside the region
          if (!gp.contains(Math.toDegrees(point[0]), Math.toDegrees(point[1]))) return Double.NaN;

          return Math.cos(point[0]) * Math.cos(point[1]);
        };

    // Define the map projection
    LatitudinalVector centerPoint =
        new LatitudinalVector(1, Math.toRadians(-30), Math.toRadians(140));

    // Note the Y axis won't be right for this projection, but we'll draw it anyway
    ProjectionOrthographic p =
        new ProjectionOrthographic(config.width(), config.height(), centerPoint);
    p.setRadius(1);

    MapPlot canvas = new MapPlot(config, p);
    try {
      BufferedImage map = ImageIO.read(baseMap);
      canvas.setBackgroundMap(map);
    } catch (IOException e) {
      e.printStackTrace();
    }

    canvas.drawTitle();
    canvas.drawAxes();

    ColorRamp ramp = ColorRamp.createHue(-1, 1);
    Rectangle2D bounds = gp.getBounds2D();
    canvas.plot(
        func,
        ramp,
        new AxisRange(Math.toRadians(133), Math.toRadians(bounds.getMaxX())),
        new AxisRange(Math.toRadians(bounds.getMinY()), Math.toRadians(bounds.getMaxY())));

    DiscreteDataSet outline = new DiscreteDataSet("");
    double[] coords = new double[6];
    for (PathIterator pi = gp.getPathIterator(null); !pi.isDone(); pi.next()) {
      pi.currentSegment(coords);
      outline.add(Math.toRadians(coords[0]), Math.toRadians(coords[1]));
    }

    canvas.plot(outline);
    outline.setSymbol(new Circle().setFill(true).setSize(4));
    outline.setColor(Color.GREEN);
    canvas.plot(outline);

    Annotations a = new Annotations();
    a.addAnnotation(
        ImmutableAnnotation.builder().text("Perth").build(),
        Math.toRadians(115.82),
        Math.toRadians(-31.97));
    a.addAnnotation(
        ImmutableAnnotation.builder()
            .text("Singapore")
            .color(Color.WHITE)
            .font(new Font("Times New Roman", Font.BOLD, 24))
            .build(),
        Math.toRadians(103.9),
        Math.toRadians(1.20));

    canvas.drawLatLonGrid(Math.toRadians(10), Math.toRadians(10), true);
    canvas.addAnnotations(a);

    return canvas.getImage();
  }

  public static void main(String[] args) {
    URL input = MapPlotDemo.class.getResource("earth_day_4096.jpg");
    try {
      File f = File.createTempFile("resource-", ".jpg");
      f.deleteOnExit();

      assert input != null;
      FileUtils.copyURLToFile(input, f);

      BufferedImage image;
      image = makeOrthographic(f);
      PlotUtils.addCreationDate(image);
      PlotCanvas.showJFrame(image);
      image = makeRectangular(f);
      PlotUtils.addCreationDate(image);
      PlotCanvas.showJFrame(image);
      //       PlotCanvas.writeImage("doc/images/mapPlotDemo.png", image);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }
}
