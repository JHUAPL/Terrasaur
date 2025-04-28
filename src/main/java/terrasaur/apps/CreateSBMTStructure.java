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

import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.RotationUtils;
import vtk.vtkPolyData;

public class CreateSBMTStructure implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  /**
   * This doesn't need to be private, or even declared, but you might want to if you have other
   * constructors.
   */
  private CreateSBMTStructure() {}

  @Override
  public String shortDescription() {
    return "Construct ellipses from user-defined points on an image.";
  }

  @Override
  public String fullDescription(Options options) {
    String header =
        "This tool creates an SBMT ellipse file from a set of point on an image.";
    String footer = "";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  /**
   * Create an Ellipse as an SBMT structure from three points. The first two points define the long
   * axis and the third point defines the short axis.
   *
   * @param p1 First point
   * @param p2 Second point
   * @param p3 Third point
   * @return An SBMT structure describing the ellipse
   */
  private static SBMTEllipseRecord createRecord(
      int id, String name, Vector3D p1, Vector3D p2, Vector3D p3) {
    // Create a local coordinate system where X axis contains long axis and Y axis contains short
    // axis

    Vector3D origin = p1.add(p2).scalarMultiply(0.5);
    Vector3D X = p1.subtract(p2).normalize();
    Vector3D Y = p3.subtract(origin).normalize();

    // Create a rotation matrix to go from body fixed frame to this local coordinate system
    Rotation globalToLocal = RotationUtils.IprimaryJsecondary(X, Y);

    // All of these vectors should have a Z coordinate of zero
    Vector3D p1Local = globalToLocal.applyTo(p1);
    Vector3D p2Local = globalToLocal.applyTo(p2);
    Vector3D p3Local = globalToLocal.applyTo(p3);

    // fit an ellipse to the three points on the plane
    Vector2D a = new Vector2D(p1Local.getX(), p1Local.getY());
    Vector2D b = new Vector2D(p2Local.getX(), p2Local.getY());
    Vector2D c = new Vector2D(p3Local.getX(), p3Local.getY());

    Vector2D center = a.add(b).scalarMultiply(0.5);
    double majorAxis = a.subtract(b).getNorm();
    double minorAxis = 2 * c.subtract(center).getNorm();

    double rotation = Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
    double flattening = (majorAxis - minorAxis) / majorAxis;

    ImmutableSBMTEllipseRecord.Builder record =
        ImmutableSBMTEllipseRecord.builder()
            .id(id)
            .name(name)
            .x(origin.getX())
            .y(origin.getY())
            .z(origin.getZ())
            .lat(origin.getDelta())
            .lon(origin.getAlpha())
            .radius(origin.getNorm())
            .slope(0)
            .elevation(0)
            .acceleration(0)
            .potential(0)
            .diameter(majorAxis)
            .flattening(flattening)
            .angle(rotation)
            .color(Color.BLACK)
            .dummy("")
            .label("");
    return record.build();
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("input")
            .required()
            .hasArg()
            .desc(
                """
Required.  Name or input file.  This is a text file with a pair of pixel coordinates per line.  The pixel
coordinates are offsets from the image center. For example:

# My test file

627.51274 876.11775
630.53612 883.55992
626.3499 881.46681

Empty lines or lines beginning with # are ignored.

Each set of three points are used to create the SBMT structures.  The first two points are the long
axis and the third is a location for the semi-minor axis.""")
            .build());
    options.addOption(
        Option.builder("objFile")
            .required()
            .hasArg()
            .desc("Required.  Name of OBJ shape file.")
            .build());
    options.addOption(
        Option.builder("output")
            .required()
            .hasArg()
            .desc("Required.  Name of output file.")
            .build());
    options.addOption(
        Option.builder("sumFile")
            .required()
            .hasArg()
            .desc("Required.  Name of sum file to read.")
            .build());
    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new CreateSBMTStructure();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadSpiceLibraries();
    NativeLibraryLoader.loadVtkLibraries();

    SumFile sumFile = SumFile.fromFile(new File(cl.getOptionValue("sumFile")));

    try {
      String objFile = cl.getOptionValue("objFile");
      vtkPolyData polyData = PolyDataUtil.loadShapeModel(objFile);
      if (polyData == null) {
        logger.error("Cannot read shape model {}!", objFile);
        System.exit(0);
      }
      RangeFromSumFile rfsf = new RangeFromSumFile(sumFile, polyData);

      List<Vector3D> intercepts = new ArrayList<>();
      List<String> lines =
          FileUtils.readLines(new File(cl.getOptionValue("input")), Charset.defaultCharset());
      for (String line :
          lines.stream().filter(s -> !(s.isBlank() || s.strip().startsWith("#"))).toList()) {
        String[] parts = line.split("\\s+");
        int ix = (int) Math.round(Double.parseDouble(parts[0]));
        int iy = (int) Math.round(Double.parseDouble(parts[1]));

        Map.Entry<Long, Vector3D> entry = rfsf.findIntercept(ix, iy);
        long cellID = entry.getKey();
        if (cellID > -1) intercepts.add(entry.getValue());
      }

      logger.info("Found {} sets of points", intercepts.size() / 3);

      List<SBMTEllipseRecord> records = new ArrayList<>();
      for (int i = 0; i < intercepts.size(); i += 3) {

        // p1 and p2 define the long axis of the ellipse
        Vector3D p1 = intercepts.get(i);
        Vector3D p2 = intercepts.get(i+1);

        // p3 lies on the short axis
        Vector3D p3 = intercepts.get(i+2);

        SBMTEllipseRecord record = createRecord(i/3, String.format("Ellipse %d", i/3), p1, p2, p3);
        records.add(record);
      }

      try (PrintWriter pw = new PrintWriter(cl.getOptionValue("output"))) {
        for (SBMTEllipseRecord record : records) pw.println(record.toString());
      }
      logger.info("Wrote {}", cl.getOptionValue("output"));
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      throw new RuntimeException(e);
    }

    logger.info("Finished");
  }
}
