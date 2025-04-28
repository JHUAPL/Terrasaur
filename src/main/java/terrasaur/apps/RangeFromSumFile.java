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

import java.io.File;
import java.util.AbstractMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.vectorspace.VectorIJK;
import spice.basic.Plane;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkIdList;
import vtk.vtkPolyData;

public class RangeFromSumFile implements TerrasaurTool {
  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Calculate range to surface from a sumfile.";
  }

  @Override
  public String fullDescription(Options options) {

    String header = "";
    String footer =
            """
                    This program reads a sumfile along with a shape model and \
                    calculates the range to the surface.  NOTE: Spacecraft position is \
                    assumed to be in kilometers.  If not, use the -distanceScale option \
                    to convert to km.
                    """;
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private SumFile sumFile;
  private vtkPolyData polyData;
  private SmallBodyModel smallBodyModel;

  private int xOffset;
  private int yOffset;

  private long facet;

  private Vector3D scPos;
  private Vector3D sunXYZ;
  private Vector3D surfaceIntercept;

  private double tiltDeg;
  private double tiltDir;

  private double incidence;
  private double emission;
  private double phase;

  private double scAzimuth;
  private double scElevation;

  private double sunAzimuth;
  private double sunElevation;

  private DescriptiveStatistics stats;
  private double centerX, centerY;

  public DescriptiveStatistics getStats() {
    return stats;
  }

  private RangeFromSumFile() {}

  public RangeFromSumFile(SumFile sumFile, vtkPolyData polyData) {

    this.sumFile = sumFile;

    int nPixelsX = sumFile.imageWidth();
    int nPixelsY = sumFile.imageHeight();
    centerX = 0.5 * (nPixelsX - 1);
    centerY = 0.5 * (nPixelsY - 1);

    this.polyData = polyData;

    smallBodyModel = new SmallBodyModel(polyData);

    scPos = sumFile.scobj().negate();
    sunXYZ = sumFile.sunDirection();

    stats = new DescriptiveStatistics();
  }

  public void setDistanceScale(double distanceScale) {
    this.scPos = sumFile.scobj().scalarMultiply(distanceScale).negate();
  }

  /**
   * @param xOffset x offset in pixels
   * @param yOffset y offset in pixels
   * @return key is cell index, value is surface intercept for the desired pixel offset from the center of the image.
   */
  public Map.Entry<Long, Vector3D> findIntercept(int xOffset, int yOffset) {

    this.xOffset = xOffset;
    this.yOffset = yOffset;

    Vector3D lookDir = new Vector3D(1.0, sumFile.boresight());

    if (xOffset != 0) {
      Vector3D offset = new Vector3D(-xOffset, sumFile.xPerPixel());
      lookDir = lookDir.add(offset);
    }

    if (yOffset != 0) {
      Vector3D offset = new Vector3D(-yOffset, sumFile.yPerPixel());
      lookDir = lookDir.add(offset);
    }

    double[] tmp = new double[3];
    facet = smallBodyModel.computeRayIntersection(scPos.toArray(), lookDir.toArray(), tmp);

    if (facet == -1) {
      surfaceIntercept = null;
    } else {
      surfaceIntercept = new Vector3D(tmp);

      vtkIdList idList = new vtkIdList();
      double[] pt0 = new double[3];
      double[] pt1 = new double[3];
      double[] pt2 = new double[3];

      polyData.GetCellPoints(facet, idList);

      // get the ids for each point
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);

      // get points that comprise the cell
      polyData.GetPoint(id0, pt0);
      polyData.GetPoint(id1, pt1);
      polyData.GetPoint(id2, pt2);

      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));

      Vector3 center3 = MathConversions.toVector3(facet.getCenter());
      Vector3D center3D = MathConversions.toVector3D(facet.getCenter());
      Vector3 normal3 = MathConversions.toVector3(facet.getNormal());
      Vector3D normal3D = MathConversions.toVector3D(facet.getNormal());

      tiltDeg = Math.toDegrees(center3.sep(normal3));
      if (tiltDeg > 90) tiltDeg = 180 - tiltDeg;

      tiltDir = Tilts.basicTiltDirDeg(surfaceIntercept.getAlpha(), normal3D);


      incidence = Vector3D.angle(sunXYZ, normal3D);
      emission = Vector3D.angle(scPos, normal3D);
      phase = Vector3D.angle(sunXYZ, scPos.subtract(center3D));

      try {
        // scPos is in body fixed coordinates
        Plane p = new Plane(normal3, center3);
        Vector3 projectedNorth = p.project(new Vector3(0, 0, 1).add(center3)).sub(center3);
        Vector3 projected = p.project(MathConversions.toVector3(scPos)).sub(center3);

        scAzimuth = projected.sep(projectedNorth);
        if (projected.cross(projectedNorth).dot(center3) < 0) scAzimuth = 2 * Math.PI - scAzimuth;
        scElevation = Math.PI / 2 - emission;

        // sunXYZ is a unit vector pointing to the sun
        projected = p.project(MathConversions.toVector3(sunXYZ).add(center3)).sub(center3);

        sunAzimuth = projected.sep(projectedNorth);
        if (projected.cross(projectedNorth).dot(center3) < 0) sunAzimuth = 2 * Math.PI - sunAzimuth;
        sunElevation = Math.PI / 2 - incidence;
      } catch (SpiceException e) {
        logger.error(e.getLocalizedMessage(), e);
      }

      stats.addValue(scPos.distance(surfaceIntercept));
    }
    return new AbstractMap.SimpleEntry<>(facet, surfaceIntercept);
  }

  public String getHeader(String filename) {
    StringBuffer sb = new StringBuffer();
    sb.append("# x increases to the right and y increases down.  Top left corner is 0, 0.\n");
    sb.append(String.format("# %s\n", filename));
    sb.append(String.format("%7s", "#   x"));
    sb.append(String.format("%7s", "y"));
    sb.append(StringUtils.center("facet", 8));
    sb.append(StringUtils.center("Tilt", 12));
    sb.append(StringUtils.center("Tilt Dir", 12));
    sb.append(StringUtils.center("s/c position XYZ", 36));
    sb.append(StringUtils.center("surface intercept XYZ", 36));
    sb.append(StringUtils.center("lon", 12));
    sb.append(StringUtils.center("lat", 12));
    sb.append(StringUtils.center("rad", 12));
    sb.append(StringUtils.center("range", 12));
    sb.append(StringUtils.center("inc", 12));
    sb.append(StringUtils.center("ems", 12));
    sb.append(StringUtils.center("phase", 12));
    sb.append(StringUtils.center("s/c az", 12));
    sb.append(StringUtils.center("s/c el", 12));
    sb.append(StringUtils.center("sun az", 12));
    sb.append(StringUtils.center("sun el", 12));

    sb.append("\n");
    sb.append(String.format("%7s", "#    "));
    sb.append(String.format("%7s", ""));
    sb.append(String.format("%8s", ""));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(km)", 36));
    sb.append(StringUtils.center("(km)", 36));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(km)", 12));
    sb.append(StringUtils.center("(km)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    sb.append(StringUtils.center("(deg)", 12));
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%7.2f", xOffset + centerX));
    sb.append(String.format("%7.2f", yOffset + centerY));
    sb.append(String.format("%8d", facet));
    sb.append(String.format("%12.6f", tiltDeg));
    sb.append(String.format("%12.6f", tiltDir));
    sb.append(String.format("%12.6f", scPos.getX()));
    sb.append(String.format("%12.6f", scPos.getY()));
    sb.append(String.format("%12.6f", scPos.getZ()));
    sb.append(String.format("%12.6f", surfaceIntercept.getX()));
    sb.append(String.format("%12.6f", surfaceIntercept.getY()));
    sb.append(String.format("%12.6f", surfaceIntercept.getZ()));

    double lon = Math.toDegrees(surfaceIntercept.getAlpha());
    if (lon < 0) lon += 360;
    sb.append(String.format("%12.6f", lon));
    sb.append(String.format("%12.6f", Math.toDegrees(surfaceIntercept.getDelta())));
    sb.append(String.format("%12.6f", surfaceIntercept.getNorm()));

    sb.append(String.format("%12.6f", scPos.distance(surfaceIntercept)));
    sb.append(String.format("%12.6f", Math.toDegrees(incidence)));
    sb.append(String.format("%12.6f", Math.toDegrees(emission)));
    sb.append(String.format("%12.6f", Math.toDegrees(phase)));
    sb.append(String.format("%12.6f", Math.toDegrees(scAzimuth)));
    sb.append(String.format("%12.6f", Math.toDegrees(scElevation)));
    sb.append(String.format("%12.6f", Math.toDegrees(sunAzimuth)));
    sb.append(String.format("%12.6f", Math.toDegrees(sunElevation)));

    return sb.toString();
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("sumFile")
            .required()
            .hasArg()
            .desc("Required.  Name of sum file to read.")
            .build());
    options.addOption(
        Option.builder("objFile")
            .required()
            .hasArg()
            .desc("Required.  Name of OBJ shape file.")
            .build());
    options.addOption(
        Option.builder("pixelOffset")
            .hasArg()
            .desc(
                "Pixel offset from center of image, given as a comma separated pair (no spaces).  Default is 0,0.  "
                    + "x increases to the right and y increases down.")
            .build());
    options.addOption(
        Option.builder("xRange")
            .hasArg()
            .desc(
                "Range of X pixel offsets from center of image, given as a comma separated triplet (xStart, xStop, xSpacing with no spaces).  "
                    + "For example -50,50,5.")
            .build());
    options.addOption(
        Option.builder("yRange")
            .hasArg()
            .desc(
                "Range of Y pixel offsets from center of image, given as a comma separated triplet (yStart, yStop, ySpacing with no spaces).  "
                    + "For example -50,50,5.")
            .build());
    options.addOption(
        Option.builder("radius")
            .hasArg()
            .desc(
                "Evaluate all pixels within specified distance (in pixels) of desired pixel.  This value will be rounded to the nearest integer.")
            .build());
    options.addOption(
        Option.builder("distanceScale")
            .hasArg()
            .desc(
                "Spacecraft position is assumed to be in kilometers.  If not, scale by this value (e.g. Use 0.001 if s/c pos is in meters).")
            .build());
    options.addOption(
        Option.builder("stats")
            .desc("Print out statistics about range to all selected pixels.")
            .build());
    return options;
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new RangeFromSumFile();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));
    NativeLibraryLoader.loadSpiceLibraries();
    NativeLibraryLoader.loadVtkLibraries();

    SumFile sumFile = SumFile.fromFile(new File(cl.getOptionValue("sumFile")));

    int xStart = 0;
    int xStop = 1;
    int xSpacing = 1;
    int yStart = 0;
    int yStop = 1;
    int ySpacing = 1;
    if (cl.hasOption("pixelOffset")) {
      String[] parts = cl.getOptionValue("pixelOffset").split(",");

      int x = Integer.parseInt(parts[0].trim());
      int y = Integer.parseInt(parts[1].trim());

      xStart = x;
      xStop = x + 1;
      yStart = y;
      yStop = y + 1;
    }

    if (cl.hasOption("xRange")) {
      String[] parts = cl.getOptionValue("xRange").split(",");
      xStart = Integer.parseInt(parts[0].trim());
      xStop = Integer.parseInt(parts[1].trim());
      xSpacing = Integer.parseInt(parts[2].trim());
    }

    if (cl.hasOption("yRange")) {
      String[] parts = cl.getOptionValue("yRange").split(",");
      yStart = Integer.parseInt(parts[0].trim());
      yStop = Integer.parseInt(parts[1].trim());
      ySpacing = Integer.parseInt(parts[2].trim());
    }

    int checkRadius = 0;
    if (cl.hasOption("radius")) {
      checkRadius = (int) Math.round(Double.parseDouble(cl.getOptionValue("radius")));
      xStart -= checkRadius;
      xStop += checkRadius;
      yStart -= checkRadius;
      yStop += checkRadius;
    }

    String objFile = cl.getOptionValue("objFile");
    vtkPolyData polyData = PolyDataUtil.loadShapeModel(objFile);
    RangeFromSumFile rfsf = new RangeFromSumFile(sumFile, polyData);

    if (cl.hasOption("distanceScale"))
      rfsf.setDistanceScale(Double.parseDouble(cl.getOptionValue("distanceScale")));

    System.out.println(rfsf.getHeader(cl.getOptionValue("sumFile")));

    for (int ix = xStart; ix < xStop; ix += xSpacing) {
      for (int iy = yStart; iy < yStop; iy += ySpacing) {
        if (checkRadius > 0) {
          double midx = (xStart + xStop) / 2.;
          double midy = (yStart + yStop) / 2.;
          if ((ix - midx) * (ix - midx) + (iy - midy) * (iy - midy) > checkRadius * checkRadius)
            continue;
        }
        long cellID = rfsf.findIntercept(ix, iy).getKey();
        if (cellID > -1) System.out.println(rfsf);
      }
    }
    if (cl.hasOption("stats")) System.out.println("Range " + rfsf.getStats());
  }
}
