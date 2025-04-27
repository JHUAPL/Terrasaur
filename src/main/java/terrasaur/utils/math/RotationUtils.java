package terrasaur.utils.math;

import java.util.List;
import java.util.Random;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import com.google.common.collect.ImmutableList;
import terrasaur.utils.VectorUtils;

/**
 * Methods to generate rotation matrices.
 *
 * @author nairah1
 */
public class RotationUtils {

  /** Value used in {@link Rotation#Rotation(double[][], double)}. */
  public static final double THRESHOLD = 1e-6;

  /**
   * Construct a {@link Rotation} matrix representing an orthonormal frame defined by primary and
   * secondary vectors. The primary vector defines an axis of the orthonormal frame. Another axis is
   * the cross product of the primary and secondary vectors (or secondary and primary vectors,
   * following the right hand rule). The second axis is the cross product of the first and third
   * axes, again following the right hand rule.
   *
   * <p>This matrix will transform vectors in the old frame to the new one.
   *
   * @param iRow X axis
   * @param jRow Y axis
   * @param kRow Z axis
   * @return rotation to transform vectors in old frame to new frame
   */
  private static Rotation fromBasisVectors(Vector3D iRow, Vector3D jRow, Vector3D kRow) {
    double[][] m = new double[3][3];
    m[0] = iRow.toArray();
    m[1] = jRow.toArray();
    m[2] = kRow.toArray();
    Rotation r = new Rotation(m, THRESHOLD);
    return r;
  }

  /**
   * Return a {@link Rotation} to a frame where the X axis is aligned along iRow and the Y axis is
   * aligned along the component of jRow that is orthogonal to iRow.
   *
   * <p>iRow and jRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.IprimaryJsecondary(iInOldFrame, jInOldFrame);
   *
   * // this should be Vector3D.PLUS_I
   * Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
   *
   * // this should be iInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
   *
   * </pre>
   *
   * @param iRow X axis
   * @param jRow Y axis
   * @return rotation to new frame
   */
  public static Rotation IprimaryJsecondary(Vector3D iRow, Vector3D jRow) {
    Vector3D kRow = iRow.crossProduct(jRow).normalize();
    jRow = kRow.crossProduct(iRow).normalize();
    iRow = iRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a {@link Rotation} to a frame where the X axis is aligned along iRow and the Z axis is
   * aligned along the component of kRow that is orthogonal to iRow.
   *
   * <p>iRow and kRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.IprimaryKsecondary(iInOldFrame, kInOldFrame);
   *
   * // this should be Vector3D.PLUS_I
   * Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
   *
   * // this should be iInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
   *
   * </pre>
   *
   * @param iRow
   * @param kRow
   * @return
   */
  public static Rotation IprimaryKsecondary(Vector3D iRow, Vector3D kRow) {
    Vector3D jRow = kRow.crossProduct(iRow).normalize();
    kRow = iRow.crossProduct(jRow).normalize();
    iRow = iRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a {@link Rotation} to a frame where the Y axis is aligned along jRow and the X axis is
   * aligned along the component of iRow that is orthogonal to jRow.
   *
   * <p>jRow and iRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.JprimaryIsecondary(jInOldFrame, iInOldFrame);
   *
   * // this should be Vector3D.PLUS_J
   * Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
   *
   * // this should be jInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
   *
   * </pre>
   *
   * @param jRow
   * @param iRow
   * @return
   */
  public static Rotation JprimaryIsecondary(Vector3D jRow, Vector3D iRow) {
    Vector3D kRow = iRow.crossProduct(jRow).normalize();
    iRow = jRow.crossProduct(kRow).normalize();
    jRow = jRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a {@link Rotation} to a frame where the Y axis is aligned along jRow and the Z axis is
   * aligned along the component of kRow that is orthogonal to jRow.
   *
   * <p>jRow and kRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.JprimaryKsecondary(jInOldFrame, kInOldFrame);
   *
   * // this should be Vector3D.PLUS_J
   * Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
   *
   * // this should be jInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
   *
   * </pre>
   *
   * @param jRow
   * @param kRow
   * @return
   */
  public static Rotation JprimaryKsecondary(Vector3D jRow, Vector3D kRow) {
    Vector3D iRow = jRow.crossProduct(kRow).normalize();
    kRow = iRow.crossProduct(jRow).normalize();
    jRow = jRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a {@link Rotation} to a frame where the Z axis is aligned along kRow and the X axis is
   * aligned along the component of iRow that is orthogonal to kRow.
   *
   * <p>kRow and iRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.KprimaryIsecondary(kInOldFrame, iInOldFrame);
   *
   * // this should be Vector3D.PLUS_K
   * Vector3D v = oldFrameToNewFrame.applyTo(kInOldFrame);
   *
   * // this should be kInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
   *
   * </pre>
   *
   * @param kRow
   * @param iRow
   * @return
   */
  public static Rotation KprimaryIsecondary(Vector3D kRow, Vector3D iRow) {
    Vector3D jRow = kRow.crossProduct(iRow).normalize();
    iRow = jRow.crossProduct(kRow).normalize();
    kRow = kRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a {@link Rotation} to a frame where the Z axis is aligned along kRow and the Y axis is
   * aligned along the component of jRow that is orthogonal to kRow.
   *
   * <p>kRow and jRow are not assumed to be normalized.
   *
   * <p>When transforming a {@link Vector3D} from the old frame to the new frame, use {@link
   * Rotation#applyTo(Vector3D)}. For example:
   *
   * <pre>
   * Rotation oldFrameToNewFrame = RotationUtils.KprimaryJsecondary(kInOldFrame, jInOldFrame);
   *
   * // this should be Vector3D.PLUS_K
   * Vector3D v = oldFrameToNewFrame.applyTo(kInOldFrame);
   *
   * // this should be kInOldFrame
   * v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
   *
   * </pre>
   *
   * @param kRow
   * @param jRow
   * @return
   */
  public static Rotation KprimaryJsecondary(Vector3D kRow, Vector3D jRow) {
    Vector3D iRow = jRow.crossProduct(kRow).normalize();
    jRow = kRow.crossProduct(iRow).normalize();
    kRow = kRow.normalize();
    return fromBasisVectors(iRow, jRow, kRow);
  }

  /**
   * Return a random rotation.
   *
   * @return
   */
  public static Rotation randomRotation() {
    Vector3D axis = VectorUtils.randomVector();
    double angle = new Random().nextDouble() * 2 * Math.PI;

    // the RotationConvention doesn't matter since it's a random rotation
    return new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
  }

  /**
   * Write a rotation as an angle in degrees and unit axis. The axis will be written consistent with
   * {@link RotationConvention#FRAME_TRANSFORM}.
   *
   * @param r
   * @return
   */
  public static String rotationToString(Rotation r) {
    RotationConvention rc = RotationConvention.FRAME_TRANSFORM;
    Vector3D axis = r.getAxis(rc);
    double angle = r.getAngle();

    String s =
        String.format(
            "%.16f,%.16f,%.16f,%.16f",
            Math.toDegrees(angle), axis.getX(), axis.getY(), axis.getZ());

    return s;
  }

  /**
   * Returns a rotation matrix. Specify rotation by an angle (degrees) and a 3d rotation axis vector
   * separated by commas (no spaces)
   *
   * <p>The Rotation returned will transform a fixed vector between two frames, consistent with
   * {@link RotationConvention#FRAME_TRANSFORM}.
   *
   * @param args
   * @return
   */
  public static Rotation stringToRotation(String args) {
    String[] rotationParams = args.split(",");
    double angle = Double.parseDouble(rotationParams[0].trim());
    double[] axis = new double[3];
    for (int i = 0; i < 3; i++) axis[i] = Double.parseDouble(rotationParams[i + 1].trim());
    Rotation rotation =
        new Rotation(new Vector3D(axis), Math.toRadians(angle), RotationConvention.FRAME_TRANSFORM);
    return rotation;
  }

  /**
   * Write a translation and rotation to a String containing a 4x4 combined translation/rotation
   * matrix. The top left 3x3 matrix is the rotation matrix. The top three entries in the right hand
   * column are the translation vector. The bottom row is ignored (but is usually 0 0 0 1). *
   *
   * <p>The Rotation returned will transform a fixed vector between two frames, consistent with
   * {@link RotationConvention#FRAME_TRANSFORM}.
   *
   * @param translation
   * @param rotation
   * @return
   */
  public static List<String> transformToString(Vector3D translation, Rotation rotation) {
    double[] transArray = translation.toArray();
    double[][] rotArray = rotation.getMatrix();

    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int i = 0; i < 3; i++)
      builder.add(
          String.format(
              "%.16f %.16f %.16f %.16f",
              rotArray[i][0], rotArray[i][1], rotArray[i][2], transArray[i]));
    builder.add(String.format("%.16f %.16f %.16f %.16f", 0., 0., 0., 1.));

    return builder.build();
  }

  /**
   * Return a translation and rotation from a file containing a 4x4 combined translation/rotation
   * matrix. The top left 3x3 matrix is the rotation matrix. The top three entries in the right hand
   * column are the translation vector. The bottom row is ignored (but is usually 0 0 0 1).
   *
   * <p>Any blank lines or lines starting with # will be ignored.
   *
   * <p>The Rotation returned will transform a fixed vector between two frames, consistent with
   * {@link RotationConvention#FRAME_TRANSFORM}.
   *
   * @param lines
   * @return
   */
  public static Pair<Vector3D, Rotation> stringToTransform(List<String> lines) {
    double[][] rotArray = new double[3][3];
    double[] transArray = new double[3];
    int row = 0;
    for (String line : lines) {
      if (row > 2) break;
      String stripped = line.strip();
      if (stripped.length() == 0 || stripped.startsWith("#")) continue;

      String[] parts = lines.get(row).trim().split("\\s+");

      // Store matrix in row, column order for consistency with
      // RotationConvention#FRAME_TRANSFORM.
      for (int column = 0; column < 3; column++)
        rotArray[row][column] = Double.parseDouble(parts[column].trim());

      transArray[row] = Double.parseDouble(parts[3].trim());
      row++;
    }
    return new Pair<Vector3D, Rotation>(
        new Vector3D(transArray), new Rotation(rotArray, THRESHOLD));
  }
}
