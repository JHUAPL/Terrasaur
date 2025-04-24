package terrasaur.utils;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;

public class Tilts {

  /**
   * Basic tilt direction definition: Project plate normal into the plate facet. Determine angle in
   * CW of projected facet assuming up or N is 0deg. Take longitude and generate a quaternion
   * representing rotation about the Z-axis. Rotate normal vector by quaternion such that new
   * coordinate system x' is now along R. Call this n'.
   * 
   * <pre>
   * Tilt direction B = 90 - atan2(n'[3],n'[2])
   * 
   * if B &lt; 0 then B = 360 + B
   * 
   * <pre>
   * 
   * @param lon
   * @param normal
   * @return tilt direction in degrees
   */
  public static double basicTiltDirDeg(double lon, Vector3D normal) {

    Rotation r = new Rotation(Vector3D.PLUS_K, lon, RotationConvention.FRAME_TRANSFORM);
    Vector3D rotated = r.applyTo(normal.normalize());
    double atan2D = FastMath.atan2(rotated.getZ(), rotated.getY());
    double tiltDir = 90 - Math.toDegrees(atan2D);
    if (tiltDir < 0) {
      tiltDir = 360D + tiltDir;
    }
    return tiltDir;

  }


}
