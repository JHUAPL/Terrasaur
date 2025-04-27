package terrasaur.utils.math;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.vectorspace.RotationMatrixIJK;
import picante.math.vectorspace.UnwritableRotationMatrixIJK;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import picante.mechanics.rotations.Quaternion;
import spice.basic.Matrix33;
import spice.basic.SpiceException;
import spice.basic.SpiceQuaternion;
import spice.basic.Vector3;

/**
 * Utilities to convert between vectors/matrices in Apache Commons Math, Picante, and SPICE.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class MathConversions {

  private final static Logger logger = LogManager.getLogger(MathConversions.class);

  /**
   * Convert an Apache Commons {@link Rotation} to a SPICE {@link Matrix33}.
   * 
   * @param r
   * @return
   */
  public static Matrix33 toMatrix33(Rotation r) {
    try {
      SpiceQuaternion q = new SpiceQuaternion(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
      return q.toMatrix().xpose();
    } catch (SpiceException e) {
      logger.warn("Cannot convert Rotation to Matrix33.");
    }
    return null;
  }

  /**
   * Convert a Picante {@link RotationMatrixIJK} to a SPICE {@link Matrix33}.
   * 
   * @param r
   * @return
   */
  public static Matrix33 toMatrix33(UnwritableRotationMatrixIJK r) {
    try {
      Quaternion q = new Quaternion(r);
      VectorIJK v = q.getVector(new VectorIJK());
      return new SpiceQuaternion(q.getScalar(), v.getI(), v.getJ(), v.getK()).toMatrix();
    } catch (SpiceException e) {
      logger.warn("Cannot convert RotationMatrixIJK to Matrix33.");
    }
    return null;
  }

  /**
   * Convert a SPICE {@link Matrix33} to a Picante {@link RotationMatrixIJK}.
   * 
   * @param m
   * @return
   */
  public static RotationMatrixIJK toRotationMatrixIJK(Matrix33 m) {
    try {
      SpiceQuaternion q = new SpiceQuaternion(m);
      return new Quaternion(q.getElt(0), q.getElt(1), q.getElt(2), q.getElt(3))
          .getRotation(new RotationMatrixIJK());
    } catch (SpiceException e) {
      logger.warn("Cannot convert Matrix33 to RotationMatrixIJK.");
    }
    return null;
  }

  /**
   * Convert an Apache Commons {@link Rotation} to a Picante {@link RotationMatrixIJK}.
   * 
   * @param r
   * @return
   */
  public static RotationMatrixIJK toRotationMatrixIJK(Rotation r) {
    Quaternion q = new Quaternion(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
    return q.getRotation(new RotationMatrixIJK()).transpose();
  }

  /**
   * Convert a SPICE {@link Matrix33} to an Apache Commons {@link Rotation}.
   * 
   * @param m
   * @return
   */
  public static Rotation toRotation(Matrix33 m) {
    try {
      SpiceQuaternion q = new SpiceQuaternion(m.xpose());
      return new Rotation(q.getElt(0), q.getElt(1), q.getElt(2), q.getElt(3), false);
    } catch (SpiceException e) {
      logger.warn("Cannot convert Matrix33 to Rotation.");
    }
    return null;
  }

  /**
   * Convert a Picante {@link RotationMatrixIJK} to an Apache Commons {@link Rotation}.
   * 
   * @param m
   * @return
   */
  public static Rotation toRotation(UnwritableRotationMatrixIJK m) {
    Quaternion q = new Quaternion(m.createTranspose());
    VectorIJK v = q.getVector(new VectorIJK());
    return new Rotation(q.getScalar(), v.getI(), v.getJ(), v.getK(), false);
  }

  /**
   * Convert an Apache Commons {@link Vector3D} to a SPICE {@link Vector3}.
   * 
   * @param v
   * @return
   */
  public static Vector3 toVector3(Vector3D v) {
    return new Vector3(v.toArray());
  }

  /**
   * Convert a Picante {@link VectorIJK} to a SPICE {@link Vector3}.
   * 
   * @param v
   * @return
   */
  public static Vector3 toVector3(UnwritableVectorIJK v) {
    return new Vector3(v.getI(), v.getJ(), v.getK());
  }

  /**
   * Convert a SPICE {@link Vector3} to an Apache Commons {@link Vector3D}.
   * 
   * @param v
   * @return
   */
  public static Vector3D toVector3D(Vector3 v) {
    return new Vector3D(v.toArray());
  }

  /**
   * Convert a Picante {@link VectorIJK} to an Apache Commons {@link Vector3D}.
   * 
   * @param v
   * @return
   */
  public static Vector3D toVector3D(UnwritableVectorIJK v) {
    return new Vector3D(v.getI(), v.getJ(), v.getK());
  }

  /**
   * Convert a SPICE {@link Vector3} to a Picante {@link VectorIJK}.
   * 
   * @param v
   * @return
   */
  public static VectorIJK toVectorIJK(Vector3 v) {
    return new VectorIJK(v.toArray());
  }

  /**
   * Convert an Apache Commons {@link Vector3D} to a Picante {@link VectorIJK}.
   * 
   * @param v
   * @return
   */
  public static VectorIJK toVectorIJK(Vector3D v) {
    return new VectorIJK(v.toArray());
  }


}
