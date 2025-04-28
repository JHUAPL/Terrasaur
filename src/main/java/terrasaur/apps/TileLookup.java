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
package terrasaur.apps;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.LatitudinalVector;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.saaPlotLib.canvas.MapPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.canvas.projection.Projection;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionOrthographic;
import terrasaur.utils.saaPlotLib.canvas.projection.ProjectionRectangular;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorRamp;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.ImmutableAnnotation;
import terrasaur.utils.tessellation.FibonacciSphere;

/**
 * Given a number of tiles on a spherical tesselation, look up a tile index for
 *
 * @author nairah1
 */
public class TileLookup implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Locate tiles on the unit sphere.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer = "";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  /**
   * Given the base database name and a tile number, return the path to that database file. For
   * example:
   *
   * <pre>
   * System.out.println(TileLookup.getDBName("/path/to/database/ola.db", 6));
   * System.out.println(TileLookup.getDBName("./ola.db", 6));
   * System.out.println(TileLookup.getDBName("ola.db", 6));
   *
   * /path/to/database/ola.6.db
   * ./ola.6.db
   * ./ola.6.db
   * </pre>
   *
   * @param dbName basename for database (e.g. /path/to/database/ola.db)
   * @param tile tile index (e.g. 6)
   * @return path to database file (e.g. /path/to/database/ola.6.db)
   */
  public static String getDBName(String dbName, int tile) {
    String fullPath = FilenameUtils.getFullPath(dbName);
    if (fullPath.trim().isEmpty()) fullPath = ".";
    if (!fullPath.endsWith(File.separator)) fullPath += File.separator;
    return String.format(
        "%s%s.%d.%s",
        fullPath, FilenameUtils.getBaseName(dbName), tile, FilenameUtils.getExtension(dbName));
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("nTiles")
            .hasArg()
            .required()
            .desc("Number of points covering the sphere.")
            .build());
    options.addOption(
        Option.builder("printCoords")
            .desc(
                "Print a table of points along with their coordinates.  Takes precedence over -printStats, -printDistance, and -png.")
            .build());
    options.addOption(
        Option.builder("printDistance")
            .hasArg()
            .desc(
                "Print a table of points sorted by distance from the input point.  "
                    + "Format of the input point is longitude,latitude in degrees, comma separated without spaces.  Takes precedence over -png.")
            .build());
    options.addOption(
        Option.builder("printStats")
            .desc(
                "Print statistics on the distances (in degrees) between each point and its nearest neighbor.  Takes precedence over -printDistance and -png.")
            .build());
    options.addOption(
        Option.builder("png")
            .hasArg()
            .desc(
                "Plot points and distance to nearest point in degrees.  Argument is the name of the PNG file to write.")
            .build());
    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new TileLookup();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    final int npts = Integer.parseInt(cl.getOptionValue("nTiles"));
    FibonacciSphere fs = new FibonacciSphere(npts);

    if (cl.hasOption("printCoords")) {
      String header = String.format("%7s, %10s, %9s", "# index", "longitude", "latitude");
      System.out.println(header);
      // System.out.printf("%7s, %10s, %9s, %6s\n", "# index", "longitude", "latitude", "mapola");
      for (int i = 0; i < npts; i++) {
        LatitudinalVector lv = fs.getTileCenter(i);
        double lon = Math.toDegrees(lv.getLongitude());
        if (lon < 0) lon += 360;
        double lat = Math.toDegrees(lv.getLatitude());
        System.out.printf("%7d, %10.5f, %9.5f\n", i, lon, lat);
      }
      System.exit(0);
    }

    if (cl.hasOption("printStats")) {
      System.out.println(
          "Statistics on distances between each point and its nearest neighbor (degrees):");
      System.out.println(fs.getDistanceStats());
      System.exit(0);
    }

    if (cl.hasOption("printDistance")) {
      String[] parts = cl.getOptionValue("printDistance").split(",");
      double lon = Math.toRadians(Double.parseDouble(parts[0].trim()));
      double lat = Math.toRadians(Double.parseDouble(parts[1].trim()));
      LatitudinalVector lv = new LatitudinalVector(1, lat, lon);
      NavigableMap<Double, Integer> distanceMap = fs.getDistanceMap(lv);
      System.out.printf("%11s, %5s, %10s, %9s\n", "#  distance", "index", "longitude", "latitude");
      System.out.printf("%11s, %5s, %10s, %9s\n", "# (degrees)", "", "(degrees)", "(degrees)");
      for (Double dist : distanceMap.keySet()) {
        int index = distanceMap.get(dist);
        lv = fs.getTileCenter(index);
        System.out.printf(
            "%11.5f, %5d, %10.5f, %9.5f\n",
            Math.toDegrees(dist),
            index,
            Math.toDegrees(lv.getLongitude()),
            Math.toDegrees(lv.getLatitude()));
      }
      System.exit(0);
    }

    if (cl.hasOption("png")) {
      PlotConfig config = ImmutablePlotConfig.builder().width(1000).height(1000).build();

      String title = String.format("Fibonacci Sphere, n = %d, ", npts);

      Map<String, Projection> projections = new LinkedHashMap<>();
      projections.put(
          title + "Rectangular Projection",
          new ProjectionRectangular(config.width(), config.height() / 2));
      projections.put(
          title + "Orthographic Projection (0, 90)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, Math.PI / 2, 0)));
      projections.put(
          title + "Orthographic Projection (0, 0)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, 0, 0)));
      projections.put(
          title + "Orthographic Projection (90, 0)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, 0, Math.PI / 2)));
      projections.put(
          title + "Orthographic Projection (180, 0)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, 0, Math.PI)));
      projections.put(
          title + "Orthographic Projection (270, 0)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, 0, 3 * Math.PI / 2)));
      projections.put(
          title + "Orthographic Projection (0, -90)",
          new ProjectionOrthographic(
              config.width(), config.height(), new LatitudinalVector(1, -Math.PI / 2, 0)));

      final int nColors = 6;
      ColorRamp ramp = ColorRamp.createLinear(1, nColors - 1);
      List<Color> colors = new ArrayList<>();
      colors.add(Color.BLACK);
      for (int i = 1; i < nColors; i++) colors.add(ramp.getColor(i));
      colors.add(Color.WHITE);
      ramp = ImmutableColorRamp.builder().min(0).max(nColors).colors(colors).build();

      double radius = fs.getDistanceStats().getMax();
      ramp = ColorRamp.createLinear(0, radius).addLimitColors();

      ArrayList<BufferedImage> images = new ArrayList<>();
      for (String t : projections.keySet()) {
        config = ImmutablePlotConfig.builder().from(config).title(t).build();
        Projection p = projections.get(t);

        if (p instanceof ProjectionRectangular)
          config = ImmutablePlotConfig.builder().from(config).height(500).build();
        else config = ImmutablePlotConfig.builder().from(config).height(1000).build();

        MapPlot canvas = new MapPlot(config, p);
        AxisX xLowerAxis = new AxisX(0, 360, "Longitude (degrees)", "%.0fE");
        AxisY yLeftAxis = new AxisY(-90, 90, "Latitude (degrees)", "%.0f");

        canvas.drawTitle();
        canvas.setAxes(xLowerAxis, yLeftAxis);
        // canvas.drawAxes();

        BufferedImage image = canvas.getImage();
        for (int i = 0; i < config.width(); i++) {
          for (int j = 0; j < config.height(); j++) {
            LatitudinalVector lv = p.pixelToSpherical(i, j);
            if (lv == null) continue;
            double closestDistance = Math.toDegrees(fs.getNearest(lv).getKey());
            // int numPoints = fs.getDistanceMap(lv).subMap(0., Math.toRadians(radius)).size();
            image.setRGB(
                config.leftMargin() + i,
                config.topMargin() + j,
                ramp.getColor(closestDistance).getRGB());
          }
        }

        DiscreteDataSet points = new DiscreteDataSet("");
        for (int i = 0; i < fs.getNumTiles(); i++) {
          LatitudinalVector lv = fs.getTileCenter(i);
          points.add(lv.getLongitude(), lv.getLatitude());
        }

        if (p instanceof ProjectionRectangular) {
          canvas.drawColorBar(
              ImmutableColorBar.builder()
                  .rect(
                      new Rectangle(
                          canvas.getPageWidth() - 60, config.topMargin(), 10, config.height()))
                  .ramp(ramp)
                  .numTicks(nColors + 1)
                  .tickFunction(aDouble -> String.format("%.1f", aDouble))
                  .build());

//          for (int i = 0; i < fs.getNumTiles(); i++) {
//            LatitudinalVector lv = fs.getTileCenter(i);
//            canvas.drawCircle(lv, radius, Math.toRadians(1), Color.RED);
//          }
        }

        for (int i = 0; i < fs.getNumTiles(); i++) {
          LatitudinalVector lv = fs.getTileCenter(i);
          canvas.addAnnotation(
                  ImmutableAnnotation.builder().text(String.format("%d", i)).build(),
                  lv.getLongitude(),
                  lv.getLatitude());
        }

        images.add(canvas.getImage());
      }

      int width = 2400;
      int height = 2400;
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

      Graphics2D g = image.createGraphics();
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      int imageWidth = width;
      int imageHeight = height / 3;
      g.drawImage(
          images.getFirst(),
          width / 6,
          0,
          5 * width / 6,
          imageHeight,
          0,
          0,
          images.getFirst().getWidth(),
          images.getFirst().getHeight(),
          null);

      imageWidth = width / 3;
      for (int i = 1; i < 4; i++) {
        g.drawImage(
            images.get(i),
            (i - 1) * imageWidth,
            imageHeight,
            i * imageWidth,
            2 * imageHeight,
            0,
            0,
            images.get(i).getWidth(),
            images.get(i).getHeight(),
            null);
      }
      for (int i = 4; i < 7; i++) {
        g.drawImage(
            images.get(i),
            (i - 4) * imageWidth,
            2 * imageHeight,
            (i - 3) * imageWidth,
            3 * imageHeight,
            0,
            0,
            images.get(i).getWidth(),
            images.get(i).getHeight(),
            null);
      }
      g.dispose();

      PlotCanvas.writeImage(cl.getOptionValue("png"), image);
    }
  }
}
