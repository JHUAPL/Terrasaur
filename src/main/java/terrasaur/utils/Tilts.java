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
