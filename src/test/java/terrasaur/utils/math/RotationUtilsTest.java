package terrasaur.utils.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import terrasaur.utils.Log4j2Configurator;
import terrasaur.utils.VectorUtils;
import spice.basic.SpiceErrorException;

public class RotationUtilsTest {

  @BeforeClass
  public static void setup() throws SpiceErrorException {
    Log4j2Configurator.getInstance();
  }

  @Test
  public void testIPrimaryJSecondary() {
    Vector3D iInOldFrame = VectorUtils.randomVector();
    Vector3D jInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.IprimaryJsecondary(iInOldFrame, jInOldFrame);

    // this should be Vector3D.PLUS_I
    Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_I)) < RotationUtils.THRESHOLD);

    // this should be iInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
    assertTrue(Math.abs(Vector3D.angle(v, iInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the zAxis is orthogonal to iInOldFrame and jInOldFrame
    Vector3D zAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
    assertTrue(Math.abs(iInOldFrame.dotProduct(zAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(jInOldFrame.dotProduct(zAxis)) < RotationUtils.THRESHOLD);
  }

  @Test
  public void testIPrimaryKSecondary() {
    Vector3D iInOldFrame = VectorUtils.randomVector();
    Vector3D kInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.IprimaryKsecondary(iInOldFrame, kInOldFrame);

    // this should be Vector3D.PLUS_I
    Vector3D v = oldFrameToNewFrame.applyTo(iInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_I)) < RotationUtils.THRESHOLD);

    // this should be iInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
    assertTrue(Math.abs(Vector3D.angle(v, iInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the yAxis is orthogonal to iInOldFrame and kInOldFrame
    Vector3D yAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
    assertTrue(Math.abs(iInOldFrame.dotProduct(yAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(kInOldFrame.dotProduct(yAxis)) < RotationUtils.THRESHOLD);
  }

  @Test
  public void testJPrimaryISecondary() {
    Vector3D jInOldFrame = VectorUtils.randomVector();
    Vector3D iInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.JprimaryIsecondary(jInOldFrame, iInOldFrame);

    // this should be Vector3D.PLUS_J
    Vector3D v = oldFrameToNewFrame.applyTo(jInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_J)) < RotationUtils.THRESHOLD);

    // this should be jInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
    assertTrue(Math.abs(Vector3D.angle(v, jInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the zAxis is orthogonal to kInOldFrame and iInOldFrame
    Vector3D zAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
    assertTrue(Math.abs(jInOldFrame.dotProduct(zAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(iInOldFrame.dotProduct(zAxis)) < RotationUtils.THRESHOLD);
  }

  @Test
  public void testJPrimaryKSecondary() {
    Vector3D jInOldFrame = VectorUtils.randomVector();
    Vector3D kInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.JprimaryKsecondary(jInOldFrame, kInOldFrame);

    // this should be Vector3D.PLUS_J
    Vector3D v = oldFrameToNewFrame.applyTo(jInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_J)) < RotationUtils.THRESHOLD);

    // this should be jInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
    assertTrue(Math.abs(Vector3D.angle(v, jInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the xAxis is orthogonal to iInOldFrame and kInOldFrame
    Vector3D xAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
    assertTrue(Math.abs(jInOldFrame.dotProduct(xAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(kInOldFrame.dotProduct(xAxis)) < RotationUtils.THRESHOLD);
  }


  @Test
  public void testKPrimaryISecondary() {
    Vector3D kInOldFrame = VectorUtils.randomVector();
    Vector3D iInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.KprimaryIsecondary(kInOldFrame, iInOldFrame);

    // this should be Vector3D.PLUS_K
    Vector3D v = oldFrameToNewFrame.applyTo(kInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_K)) < RotationUtils.THRESHOLD);

    // this should be kInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
    assertTrue(Math.abs(Vector3D.angle(v, kInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the yAxis is orthogonal to kInOldFrame and iInOldFrame
    Vector3D yAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_J);
    assertTrue(Math.abs(kInOldFrame.dotProduct(yAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(iInOldFrame.dotProduct(yAxis)) < RotationUtils.THRESHOLD);
  }


  @Test
  public void testKPrimaryJSecondary() {
    Vector3D kInOldFrame = VectorUtils.randomVector();
    Vector3D jInOldFrame = VectorUtils.randomVector();

    Rotation oldFrameToNewFrame = RotationUtils.KprimaryJsecondary(kInOldFrame, jInOldFrame);

    // this should be Vector3D.PLUS_K
    Vector3D v = oldFrameToNewFrame.applyTo(kInOldFrame);
    assertTrue(Math.abs(Vector3D.angle(v, Vector3D.PLUS_K)) < RotationUtils.THRESHOLD);

    // this should be kInOldFrame
    v = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_K);
    assertTrue(Math.abs(Vector3D.angle(v, kInOldFrame)) < RotationUtils.THRESHOLD);

    // Check that the xAxis is orthogonal to kInOldFrame and iInOldFrame
    Vector3D xAxis = oldFrameToNewFrame.applyInverseTo(Vector3D.PLUS_I);
    assertTrue(Math.abs(kInOldFrame.dotProduct(xAxis)) < RotationUtils.THRESHOLD);
    assertTrue(Math.abs(jInOldFrame.dotProduct(xAxis)) < RotationUtils.THRESHOLD);
  }

  @Test
  public void testStringToRotation() {

    RotationConvention rc = RotationConvention.FRAME_TRANSFORM;

    Rotation r = RotationUtils.randomRotation();
    Vector3D axis = r.getAxis(rc);
    double angle = r.getAngle();

    String s = RotationUtils.rotationToString(r);

    r = RotationUtils.stringToRotation(s);

    assertTrue(axis.dotProduct(r.getAxis(rc)) > 1 - RotationUtils.THRESHOLD);
    assertEquals(angle, r.getAngle(), RotationUtils.THRESHOLD);

  }

  @Test
  public void testStringToTransform() {
    RotationConvention rc = RotationConvention.FRAME_TRANSFORM;

    Vector3D v = VectorUtils.randomVector();
    Rotation r = RotationUtils.randomRotation();
    Vector3D axis = r.getAxis(rc);
    double angle = r.getAngle();

    List<String> lines = RotationUtils.transformToString(v, r);

    Pair<Vector3D, Rotation> p = RotationUtils.stringToTransform(lines);

    assertTrue(v.dotProduct(p.getFirst()) > 1 - RotationUtils.THRESHOLD);
    assertTrue(axis.dotProduct(p.getSecond().getAxis(rc)) > 1 - RotationUtils.THRESHOLD);
    assertEquals(angle, p.getSecond().getAngle(), RotationUtils.THRESHOLD);
  }

}
