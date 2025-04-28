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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.CardanEulerSingularityException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import terrasaur.templates.TerrasaurTool;

public class RotationConversion implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
      return "Convert rotations between different types.";
  }

  @Override
  public String fullDescription(Options options) {

    String header = "";
    String footer =
            """
                    This program converts rotations between angle and axis, 3x3 matrix, quaternions, \
                    and ZXZ rotation Euler angles.  Note that the rotation modifies the frame; \
                    the vector is considered to be fixed.  To find the rotation that modifies the \
                    vector in a fixed frame, take the transpose of this matrix.
                    """;

    return TerrasaurTool.super.fullDescription(options, header, footer);

  }


  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("logFile").hasArg()
            .desc("If present, save screen output to log file.").build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values())
      sb.append(String.format("%s ", l.name()));
    options.addOption(Option.builder("logLevel").hasArg()
            .desc("If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim() + ".  Default is INFO.")
            .build());

    options.addOption(Option.builder("angle").hasArg().desc("Rotation angle, in radians.").build());
    options.addOption(
            Option.builder("axis0").hasArg().desc("First element of rotation axis.").build());
    options.addOption(
            Option.builder("axis1").hasArg().desc("Second element of rotation axis.").build());
    options.addOption(
            Option.builder("axis2").hasArg().desc("Third element of rotation axis.").build());
    options.addOption(Option.builder("cardanXYZ1").hasArg()
            .desc("Cardan angle for the first rotation (about the X axis) in radians.").build());
    options.addOption(Option.builder("cardanXYZ2").hasArg()
            .desc("Cardan angle for the second rotation (about the Y axis) in radians.").build());
    options.addOption(Option.builder("cardanXYZ3").hasArg()
            .desc("Cardan angle for the third rotation (about the Z axis) in radians.").build());
    options.addOption(Option.builder("eulerZXZ1").hasArg()
            .desc("Euler angle for the first rotation (about the Z axis) in radians.").build());
    options.addOption(Option.builder("eulerZXZ2").hasArg()
            .desc("Euler angle for the second rotation (about the rotated X axis) in radians.")
            .build());
    options.addOption(Option.builder("eulerZXZ3").hasArg()
            .desc("Euler angle for the third rotation (about the rotated Z axis) in radians.").build());
    options.addOption(
            Option.builder("q0").hasArg().desc("Scalar term for quaternion: cos(theta/2)").build());
    options.addOption(Option.builder("q1").hasArg()
            .desc("First vector term for quaternion: sin(theta/2) * V[0]").build());
    options.addOption(Option.builder("q2").hasArg()
            .desc("Second vector term for quaternion: sin(theta/2) * V[1]").build());
    options.addOption(Option.builder("q3").hasArg()
            .desc("Third vector term for quaternion: sin(theta/2) * V[2]").build());
    options.addOption(Option.builder("matrix").hasArg()
            .desc("name of file containing rotation matrix to convert to Euler angles.  "
                    + "Format is 3x3 array in plain text separated by white space.")
            .build());
    options.addOption(Option.builder("anglesInDegrees").desc(
                    "If present, input angles in degrees and print output angles in degrees.  Default is false.")
            .build());    return options;
  }

  public static void main(String[] args) throws Exception {
    RotationConversion defaultOBJ = new RotationConversion();
    
    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));
    boolean inDegrees = cl.hasOption("anglesInDegrees");

    boolean axisAndAngle = cl.hasOption("angle") && cl.hasOption("axis0") && cl.hasOption("axis1")
        && cl.hasOption("axis2");
    boolean cardanXYZ =
        cl.hasOption("cardanXYZ1") && cl.hasOption("cardanXYZ2") && cl.hasOption("cardanXYZ3");
    boolean eulerZXZ =
        cl.hasOption("eulerZXZ1") && cl.hasOption("eulerZXZ3") && cl.hasOption("eulerZXZ3");
    boolean quaternion =
        cl.hasOption("q0") && cl.hasOption("q1") && cl.hasOption("q2") && cl.hasOption("q3");
    boolean matrix = cl.hasOption("matrix");

    if (!(axisAndAngle || cardanXYZ || eulerZXZ || quaternion || matrix)) {
      logger.warn(
          "Must specify input rotation as axis and angle, Cardan or Euler angles, matrix, or quaternion.");
      System.exit(0);
    }

    Rotation r = null;
    if (matrix) {
      List<String> lines =
          FileUtils.readLines(new File(cl.getOptionValue("matrix")), Charset.defaultCharset());
      double[][] m = new double[3][3];
      for (int i = 0; i < 3; i++) {
        String[] parts = lines.get(i).trim().split("\\s+");
        for (int j = 0; j < 3; j++)
          m[i][j] = Double.parseDouble(parts[j].trim());
      }
      r = new Rotation(m, 1e-10);
    }

    if (axisAndAngle) {
      double angle = Double.parseDouble(cl.getOptionValue("angle").trim());
      if (inDegrees)
        angle = Math.toRadians(angle);
      r = new Rotation(
          new Vector3D(Double.parseDouble(cl.getOptionValue("axis0").trim()),
              Double.parseDouble(cl.getOptionValue("axis1").trim()),
              Double.parseDouble(cl.getOptionValue("axis2").trim())),
          angle, RotationConvention.FRAME_TRANSFORM);
    }

    if (cardanXYZ) {
      double angle1 = Double.parseDouble(cl.getOptionValue("cardanXYZ1").trim());
      double angle2 = Double.parseDouble(cl.getOptionValue("cardanXYZ2").trim());
      double angle3 = Double.parseDouble(cl.getOptionValue("cardanXYZ3").trim());
      if (inDegrees) {
        angle1 = Math.toRadians(angle1);
        angle2 = Math.toRadians(angle2);
        angle3 = Math.toRadians(angle3);
      }
      r = new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM, angle1, angle2,
          angle3);
    }

    if (eulerZXZ) {
      double angle1 = Double.parseDouble(cl.getOptionValue("eulerZXZ1").trim());
      double angle2 = Double.parseDouble(cl.getOptionValue("eulerZXZ2").trim());
      double angle3 = Double.parseDouble(cl.getOptionValue("eulerZXZ3").trim());
      if (inDegrees) {
        angle1 = Math.toRadians(angle1);
        angle2 = Math.toRadians(angle2);
        angle3 = Math.toRadians(angle3);
      }
      r = new Rotation(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM, angle1, angle2,
          angle3);
    }

    if (quaternion) {
      r = new Rotation(Double.parseDouble(cl.getOptionValue("q0").trim()),
          Double.parseDouble(cl.getOptionValue("q1").trim()),
          Double.parseDouble(cl.getOptionValue("q2").trim()),
          Double.parseDouble(cl.getOptionValue("q3").trim()), true);
    }

    double[][] m = r.getMatrix();
    String matrixString = String.format(
        "rotation matrix:\n%24.16e %24.16e %24.16e\n%24.16e %24.16e %24.16e\n%24.16e %24.16e %24.16e",
        m[0][0], m[0][1], m[0][2], m[1][0], m[1][1], m[1][2], m[2][0], m[2][1], m[2][2]);
    System.out.println(matrixString);

    String axisAndAngleString = inDegrees
        ? String.format("angle (degrees), axis:\n%g, %s", Math.toDegrees(r.getAngle()),
            r.getAxis(RotationConvention.FRAME_TRANSFORM))
        : String.format("angle (radians), axis:\n%g, %s", r.getAngle(),
            r.getAxis(RotationConvention.FRAME_TRANSFORM));
    System.out.println(axisAndAngleString);

    try {
      double[] angles = r.getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
      String cardanString = inDegrees
          ? String.format("Cardan XYZ angles (degrees):\n%g, %g, %g", Math.toDegrees(angles[0]),
              Math.toDegrees(angles[1]), Math.toDegrees(angles[2]))
          : String.format("Cardan XYZ angles (radians):\n%g, %g, %g", angles[0], angles[1],
              angles[2]);
      System.out.println(cardanString);
    } catch (CardanEulerSingularityException e) {
      System.out.println("Cardan angles: encountered singularity, cannot solve");
    }

    try {
      double[] angles = r.getAngles(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM);
      String eulerString = inDegrees
          ? String.format("Euler ZXZ angles (degrees):\n%g, %g, %g", Math.toDegrees(angles[0]),
              Math.toDegrees(angles[1]), Math.toDegrees(angles[2]))
          : String.format("Euler ZXZ angles (radians):\n%g, %g, %g", angles[0], angles[1],
              angles[2]);
      System.out.println(eulerString);
    } catch (CardanEulerSingularityException e) {
      System.out.println("Euler angles: encountered singularity, cannot solve");
    }

    System.out.printf("Quaternion:\n%g, %g, %g, %g\n", r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
  }
}
