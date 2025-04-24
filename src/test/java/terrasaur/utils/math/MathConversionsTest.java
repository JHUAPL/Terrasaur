package terrasaur.utils.math;

import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;
import picante.math.vectorspace.RotationMatrixIJK;
import picante.math.vectorspace.UnwritableRotationMatrixIJK;
import picante.math.vectorspace.VectorIJK;
import picante.mechanics.rotations.AxisAndAngle;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.VectorUtils;
import spice.basic.Matrix33;
import spice.basic.SpiceException;
import spice.basic.Vector3;

public class MathConversionsTest {

  private boolean compareRotations(Rotation a, Rotation b) {
    Quaternion qa =
        new Quaternion(a.getQ0(), a.getQ1(), a.getQ2(), a.getQ3()).getPositivePolarForm();
    Quaternion qb =
        new Quaternion(b.getQ0(), b.getQ1(), b.getQ2(), b.getQ3()).getPositivePolarForm();

    return qa.equals(qb, 1e-6);
  }

  private boolean compareRotations(UnwritableRotationMatrixIJK a, UnwritableRotationMatrixIJK b) {

    RotationMatrixIJK identity = RotationMatrixIJK.mtxm(a, b);

    AxisAndAngle aaa = new AxisAndAngle(identity);

    return aaa.getAngle() < 1e-6;
  }

  private boolean compareRotations(Matrix33 a, Matrix33 b) throws SpiceException {

    Matrix33 identity = a.mtxm(b);

    spice.basic.AxisAndAngle aaa = new spice.basic.AxisAndAngle(identity);

    return aaa.getAngle() < 1e-6;
  }

  /**
   * Test Apache -&gt; Picante -&gt; Apache results in identical rotations.
   */
  @Test
  public void testACA() {

    Rotation rInitial = RotationUtils.randomRotation();
    RotationMatrixIJK rPicante = MathConversions.toRotationMatrixIJK(rInitial);
    Rotation rFinal = MathConversions.toRotation(rPicante);

    /*-
    System.out.printf("%f %f %f %f\n", rInitial.getQ0(), rInitial.getQ1(), rInitial.getQ2(),
        rInitial.getQ3());
    System.out.printf("%f %f %f %f\n", rFinal.getQ0(), rFinal.getQ1(), rFinal.getQ2(),
        rFinal.getQ3());
        */

    assertTrue(compareRotations(rInitial, rFinal));
  }

  /**
   * Test Apache -&gt; SPICE -&gt; Apache results in identical rotations.
   */
  @Test
  public void testASA() {

    NativeLibraryLoader.loadSpiceLibraries();

    Rotation rInitial = RotationUtils.randomRotation();
    Matrix33 rSpice = MathConversions.toMatrix33(rInitial);
    Rotation rFinal = MathConversions.toRotation(rSpice);

    /*-
    System.out.printf("%f %f %f %f\n", rInitial.getQ0(), rInitial.getQ1(), rInitial.getQ2(),
        rInitial.getQ3());
    System.out.printf("%f %f %f %f\n", rFinal.getQ0(), rFinal.getQ1(), rFinal.getQ2(),
        rFinal.getQ3());
    */

    assertTrue(compareRotations(rInitial, rFinal));
  }


  /**
   * Test Apache -&gt; Picante translation results in identical frame transformations
   */
  @Test
  public void testAC() {
    Vector3D iRow = VectorUtils.randomVector();
    Vector3D jRow = VectorUtils.randomVector();

    Rotation mApache = RotationUtils.IprimaryJsecondary(iRow, jRow);
    RotationMatrixIJK mPicante = MathConversions.toRotationMatrixIJK(mApache);

    Vector3D vApache = VectorUtils.randomVector();
    VectorIJK vPicante = MathConversions.toVectorIJK(vApache);

    Vector3D vRotated = mApache.applyTo(vApache);
    vPicante = mPicante.mxv(vPicante);

    /*-
    System.out.println(vRotated);
    System.out.println(vPicante);
    */

    assertTrue(Vector3D.angle(vRotated, MathConversions.toVector3D(vPicante)) < 1e-6);
  }

  /**
   * Test Apache -&gt; SPICE translation results in identical frame transformations
   */
  @Test
  public void testAS() {
    NativeLibraryLoader.loadSpiceLibraries();

    Vector3D iRow = VectorUtils.randomVector();
    Vector3D jRow = VectorUtils.randomVector();

    Rotation mApache = RotationUtils.IprimaryJsecondary(iRow, jRow);
    Matrix33 mSpice = MathConversions.toMatrix33(mApache);

    Vector3D vApache = VectorUtils.randomVector();
    Vector3 vSpice = MathConversions.toVector3(vApache);

    Vector3D vRotated = mApache.applyTo(vApache);
    vSpice = mSpice.mxv(vSpice);

    /*-
    System.out.println(vRotated);
    System.out.println(vSpice);
    */

    assertTrue(Vector3D.angle(vRotated, MathConversions.toVector3D(vSpice)) < 1e-6);
  }

  /**
   * Test Picante -&gt; Apache translation results in identical frame transformations
   */
  @Test
  public void testCA() {
    VectorIJK axis = MathConversions.toVectorIJK(VectorUtils.randomVector());
    double angle = new Random().nextDouble() * 2 * Math.PI;
    AxisAndAngle aaa = new AxisAndAngle(axis, angle);

    RotationMatrixIJK mPicante = aaa.getRotation(new RotationMatrixIJK());
    Rotation mApache = MathConversions.toRotation(mPicante);

    Vector3D vApache = VectorUtils.randomVector();
    VectorIJK vPicante = MathConversions.toVectorIJK(vApache);

    vApache = mApache.applyTo(vApache);
    vPicante = mPicante.mxv(vPicante);
    /*-
    System.out.println(vApache);
    System.out.println(vPicante);
    */
    assertTrue(Vector3D.angle(vApache, MathConversions.toVector3D(vPicante)) < 1e-6);
  }

  /**
   * Test Picante -&gt; Apache -&gt; Picante results in identical rotations.
   */
  @Test
  public void testCAC() {

    VectorIJK axis = MathConversions.toVectorIJK(VectorUtils.randomVector());
    double angle = new Random().nextDouble() * 2 * Math.PI;
    AxisAndAngle aaa = new AxisAndAngle(axis, angle);

    UnwritableRotationMatrixIJK rInitial = aaa.getRotation(new RotationMatrixIJK());
    Rotation rApache = MathConversions.toRotation(rInitial);
    UnwritableRotationMatrixIJK rFinal = MathConversions.toRotationMatrixIJK(rApache);

    /*-
    System.out.printf("%f %f %f %f\n", rInitial.getQ0(), rInitial.getQ1(), rInitial.getQ2(),
        rInitial.getQ3());
    System.out.printf("%f %f %f %f\n", rFinal.getQ0(), rFinal.getQ1(), rFinal.getQ2(),
        rFinal.getQ3());
    */

    assertTrue(compareRotations(rInitial, rFinal));
  }


  /**
   * Test Picante -&gt; SPICE translation results in identical frame transformations
   */
  @Test
  public void testCS() {
    NativeLibraryLoader.loadSpiceLibraries();

    VectorIJK axis = MathConversions.toVectorIJK(VectorUtils.randomVector());
    double angle = new Random().nextDouble() * 2 * Math.PI;
    AxisAndAngle aaa = new AxisAndAngle(axis, angle);

    RotationMatrixIJK mPicante = aaa.getRotation(new RotationMatrixIJK());
    Matrix33 mSpice = MathConversions.toMatrix33(mPicante);

    VectorIJK vPicante = MathConversions.toVectorIJK(VectorUtils.randomVector());
    Vector3 vSpice = MathConversions.toVector3(vPicante);

    vSpice = mSpice.mxv(vSpice);
    vPicante = mPicante.mxv(vPicante);
    /*-
    System.out.println(vSpice);
    System.out.println(vPicante);
    */
    assertTrue(vSpice.sep(MathConversions.toVector3(vPicante)) < 1e-6);
  }

  /**
   * Test SPICE -&gt; Apache translation results in identical frame transformations
   */
  @Test
  public void testSA() throws SpiceException {

    NativeLibraryLoader.loadSpiceLibraries();

    Vector3 axis = MathConversions.toVector3(VectorUtils.randomVector());
    double angle = new Random().nextDouble() * 2 * Math.PI;
    spice.basic.AxisAndAngle aaa = new spice.basic.AxisAndAngle(axis, angle);

    Matrix33 mSpice = aaa.toMatrix();
    Rotation mApache = MathConversions.toRotation(mSpice);

    Vector3D vApache = VectorUtils.randomVector();
    Vector3 vSpice = MathConversions.toVector3(vApache);

    vApache = mApache.applyTo(vApache);
    vSpice = mSpice.mxv(vSpice);
    /*-
    System.out.println(vApache);
    System.out.println(vSpice);
    */
    assertTrue(Vector3D.angle(vApache, MathConversions.toVector3D(vSpice)) < 1e-6);

  }

  /**
   * Test SPICE -&gt; Apache -&gt; SPICE results in identical rotations.
   * 
   * @throws SpiceException
   */
  @Test
  public void testSAS() throws SpiceException {

    NativeLibraryLoader.loadSpiceLibraries();

    Vector3 axis = MathConversions.toVector3(VectorUtils.randomVector());
    double angle = new Random().nextDouble() * 2 * Math.PI;
    spice.basic.AxisAndAngle aaa = new spice.basic.AxisAndAngle(axis, angle);

    Matrix33 rInitial = aaa.toMatrix();
    Rotation rApache = MathConversions.toRotation(rInitial);
    Matrix33 rFinal = MathConversions.toMatrix33(rApache);

    /*-
    System.out.printf("%f %f %f %f\n", rInitial.getQ0(), rInitial.getQ1(), rInitial.getQ2(),
        rInitial.getQ3());
    System.out.printf("%f %f %f %f\n", rFinal.getQ0(), rFinal.getQ1(), rFinal.getQ2(),
        rFinal.getQ3());
    */

    assertTrue(compareRotations(rInitial, rFinal));
  }



}
