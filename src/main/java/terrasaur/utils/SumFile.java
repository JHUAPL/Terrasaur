package terrasaur.utils;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import net.jafama.FastMath;
import terrasaur.utils.ImmutableSumFile.Builder;
import terrasaur.utils.math.MathConversions;
import spice.basic.Plane;
import spice.basic.Ray;
import spice.basic.RayPlaneIntercept;
import spice.basic.SpiceException;
import spice.basic.Vector3;

/**
 * Class describing Bob Gaskell's sum file object
 * 
 * @author nairah1
 *
 */
@Value.Immutable
public abstract class SumFile {

  private final static Logger logger = LogManager.getLogger(SumFile.class);

  // line 1
  public abstract String picnm();

  // line 2
  public abstract String utcString();

  // line 3
  public abstract int npx();

  public abstract int nln();

  public abstract int t1();

  public abstract int t2();

  // line 4
  public abstract double mmfl();

  public abstract double px0();

  public abstract double ln0();

  // line 5
  public abstract Vector3D scobj();

  // line 6
  public abstract Vector3D cx();

  // line 7
  public abstract Vector3D cy();

  // line 8
  public abstract Vector3D cz();

  // line 9
  public abstract Vector3D sz();

  // line 10
  public abstract Vector3D kmat1();

  public abstract Vector3D kmat2();

  // line 11
  public abstract List<Double> distortion();

  // line 12
  public abstract Vector3D sig_vso();

  // line 13
  public abstract Vector3D sig_ptg();

  @Nullable
  public abstract Vector3D frustum1();

  @Nullable
  public abstract Vector3D frustum2();

  @Nullable
  public abstract Vector3D frustum3();

  @Nullable
  public abstract Vector3D frustum4();

  /**
   * 
   * @param translation
   * @param rotation
   * @return A SumFile with the scobj transformed and the cx, cy, cz, and sz vectors rotated.
   */
  public SumFile transform(Vector3D translation, Rotation rotation) {
    Builder b = ImmutableSumFile.builder().from(this);

    Vector3D scObj = rotation.applyTo(scobj());
    scObj = scObj.add(translation);

    b.scobj(scObj);
    b.cx(rotation.applyTo(cx()));
    b.cy(rotation.applyTo(cy()));
    b.cz(rotation.applyTo(cz()));
    b.sz(rotation.applyTo(sz()));

    return b.build();
  }

  /**
   * Construct a SumFile from the input file
   * 
   * @param file
   * @return
   */
  public static SumFile fromFile(File file) {
    SumFile s = null;
    try {
      s = fromLines(FileUtils.readLines(file, Charset.defaultCharset()));
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
    return s;
  }

  /**
   * Construct a SumFile from the input string list
   * 
   * @param lines
   * @return
   */
  public static SumFile fromLines(List<String> lines) {
    Builder b = ImmutableSumFile.builder();
    b.picnm(lines.get(0).trim());
    b.utcString(lines.get(1).trim());

    String[] parts = lines.get(2).trim().split("\\s+");
    b.npx(Integer.parseInt(parts[0]));
    b.nln(Integer.parseInt(parts[1]));
    b.t1(Integer.parseInt(parts[2]));
    b.t2(Integer.parseInt(parts[3]));

    parts = lines.get(3).trim().split("\\s+");
    b.mmfl(parseFortranDouble(parts[0]));
    b.px0(parseFortranDouble(parts[1]));
    b.ln0(parseFortranDouble(parts[2]));

    parts = lines.get(4).trim().split("\\s+");
    double[] tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.scobj(new Vector3D(tmp));

    parts = lines.get(5).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.cx(new Vector3D(tmp).normalize());

    parts = lines.get(6).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.cy(new Vector3D(tmp).normalize());

    parts = lines.get(7).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.cz(new Vector3D(tmp).normalize());

    parts = lines.get(8).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.sz(new Vector3D(tmp).normalize());

    parts = lines.get(9).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.kmat1(new Vector3D(tmp));
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i + 3]);
    b.kmat2(new Vector3D(tmp));

    parts = lines.get(10).trim().split("\\s+");
    for (int i = 0; i < 4; i++)
      b.addDistortion(parseFortranDouble(parts[i]));

    parts = lines.get(11).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.sig_vso(new Vector3D(tmp));

    parts = lines.get(11).trim().split("\\s+");
    tmp = new double[3];
    for (int i = 0; i < 3; i++)
      tmp[i] = parseFortranDouble(parts[i]);
    b.sig_ptg(new Vector3D(tmp));

    SumFile s = b.build();
    double fov1 = Math.abs(Math.atan(s.npx() / (2.0 * s.mmfl() * s.kmat1().getX())));
    double fov2 = Math.abs(Math.atan(s.nln() / (2.0 * s.mmfl() * s.kmat2().getY())));
    Vector3D cornerVector = new Vector3D(-FastMath.tan(fov1), -FastMath.tan(fov2), 1.0);

    double fx = cornerVector.getX() * s.cx().getX() + cornerVector.getY() * s.cy().getX()
        + cornerVector.getZ() * s.cz().getX();
    double fy = cornerVector.getX() * s.cx().getY() + cornerVector.getY() * s.cy().getY()
        + cornerVector.getZ() * s.cz().getY();
    double fz = cornerVector.getX() * s.cx().getZ() + cornerVector.getY() * s.cy().getZ()
        + cornerVector.getZ() * s.cz().getZ();
    b.frustum3(new Vector3D(fx, fy, fz).normalize());

    fx = -cornerVector.getX() * s.cx().getX() + cornerVector.getY() * s.cy().getX()
        + cornerVector.getZ() * s.cz().getX();
    fy = -cornerVector.getX() * s.cx().getY() + cornerVector.getY() * s.cy().getY()
        + cornerVector.getZ() * s.cz().getY();
    fz = -cornerVector.getX() * s.cx().getZ() + cornerVector.getY() * s.cy().getZ()
        + cornerVector.getZ() * s.cz().getZ();
    b.frustum4(new Vector3D(fx, fy, fz).normalize());

    fx = cornerVector.getX() * s.cx().getX() - cornerVector.getY() * s.cy().getX()
        + cornerVector.getZ() * s.cz().getX();
    fy = cornerVector.getX() * s.cx().getY() - cornerVector.getY() * s.cy().getY()
        + cornerVector.getZ() * s.cz().getY();
    fz = cornerVector.getX() * s.cx().getZ() - cornerVector.getY() * s.cy().getZ()
        + cornerVector.getZ() * s.cz().getZ();
    b.frustum1(new Vector3D(fx, fy, fz).normalize());

    fx = -cornerVector.getX() * s.cx().getX() - cornerVector.getY() * s.cy().getX()
        + cornerVector.getZ() * s.cz().getX();
    fy = -cornerVector.getX() * s.cx().getY() - cornerVector.getY() * s.cy().getY()
        + cornerVector.getZ() * s.cz().getY();
    fz = -cornerVector.getX() * s.cx().getZ() - cornerVector.getY() * s.cy().getZ()
        + cornerVector.getZ() * s.cz().getZ();
    b.frustum2(new Vector3D(fx, fy, fz).normalize());

    return b.build();
  }

  /**
   * Account for numbers of the form .1192696009D+03 rather than .1192696009E+03 (i.e. a D instead
   * of an E). This function replaces D's with E's.
   * 
   * @param s
   * @return
   */
  private static double parseFortranDouble(String s) {
    return Double.parseDouble(s.replace('D', 'E'));
  }

  /**
   * Write the sum file object to a string
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s\n", picnm()));
    sb.append(String.format("%s\n", utcString()));
    sb.append(
        String.format("%6d%6d%6d%6d%56s\n", npx(), nln(), t1(), t2(), "  NPX, NLN, THRSH   "));
    sb.append(
        String.format("%20.10e%20.10e%20.10e%20s\n", mmfl(), px0(), ln0(), "  MMFL, CTR         "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", scobj().getX(), scobj().getY(),
        scobj().getZ(), "  SCOBJ             "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", cx().getX(), cx().getY(), cx().getZ(),
        "  CX                "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", cy().getX(), cy().getY(), cy().getZ(),
        "  CY                "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", cz().getX(), cz().getY(), cz().getZ(),
        "  CZ                "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", sz().getX(), sz().getY(), sz().getZ(),
        "  SZ                "));
    sb.append(String.format("%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%20s\n", kmat1().getX(),
        kmat1().getY(), kmat1().getZ(), kmat2().getX(), kmat2().getY(), kmat2().getZ(),
        "  K-MATRIX          "));
    sb.append(String.format("%15.5f%15.5f%15.5f%15.5f%20s\n", distortion().get(0),
        distortion().get(1), distortion().get(2), distortion().get(3), "  DISTORTION        "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", sig_vso().getX(), sig_vso().getY(),
        sig_vso().getZ(), "  SIGMA_VSO                "));
    sb.append(String.format("%20.10e%20.10e%20.10e%20s\n", sig_ptg().getX(), sig_ptg().getY(),
        sig_ptg().getZ(), "  SIGMA_PTG                "));
    sb.append(String.format("%s\n", "LANDMARKS"));
    sb.append(String.format("%s\n", "LIMB FITS"));
    sb.append(String.format("%s\n", "END FILE"));

    return sb.toString();
  }

  /**
   * Boresight direction. This is the same as {@link #cz()}.
   * 
   * @return
   */
  public Vector3D boresight() {
    return cz();
  }

  /**
   * Sun direction. This is the same as {@link #sz()}.
   * 
   * @return
   */
  public Vector3D sunDirection() {
    return sz();
  }

  /**
   * <pre>
   * return new Vector3D(1. / npx(), frustum3().subtract(frustum4()))
   * </pre>
   * 
   * @return
   */
  public Vector3D xPerPixel() {
    return new Vector3D(1. / npx(), frustum3().subtract(frustum4()));
  }

  /**
   * <pre>
   * return new Vector3D(1. / nln(), frustum3().subtract(frustum1()));
   * </pre>
   * 
   * @return
   */
  public Vector3D yPerPixel() {
    return new Vector3D(1. / nln(), frustum3().subtract(frustum1()));
  }

  /**
   * Angular size per pixel in the X direction.
   * 
   * @return
   */
  public double horizontalResolution() {
    return Vector3D.angle(frustum3(), frustum4()) / npx();
  }

  /**
   * Angular size per pixel in the Y direction.
   * 
   * @return
   */
  public double verticalResolution() {
    return Vector3D.angle(frustum3(), frustum1()) / nln();
  }

  /**
   * Height of the image in pixels.
   * 
   * @return
   */
  public int imageHeight() {
    double kmatrix00 = Math.abs(kmat1().getX());
    double kmatrix11 = Math.abs(kmat2().getY());
    int imageHeight = nln();
    if (kmatrix00 > kmatrix11)
      imageHeight = (int) Math.round(nln() * (kmatrix00 / kmatrix11));
    return imageHeight;
  }

  /**
   * Width of the image in pixels.
   * 
   * @return
   */
  public int imageWidth() {
    double kmatrix00 = Math.abs(kmat1().getX());
    double kmatrix11 = Math.abs(kmat2().getY());
    int imageWidth = npx();
    if (kmatrix11 > kmatrix00)
      imageWidth = (int) Math.round(npx() * (kmatrix11 / kmatrix00));
    return imageWidth;
  }

  /**
   * Get the rotation to convert from body fixed coordinates to camera coordinates. {@link #cx()},
   * {@link #cx()}, {@link #cz()} are the rows of this matrix.
   * 
   */
  public Rotation getBodyFixedToCamera() {
    double[][] m = new double[3][];
    m[0] = cx().toArray();
    m[1] = cy().toArray();
    m[2] = cz().toArray();
    return new Rotation(m, 1e-10);
  }

  /**
   * 
   * @param directions from the spacecraft, in the body fixed frame.
   * @return
   * @throws SpiceException
   */
  public List<Boolean> isInFOV(List<Vector3> directions) throws SpiceException {
    List<Boolean> isInFOV = new ArrayList<>();

    Vector3 boresight = MathConversions.toVector3(boresight());
    Plane fovPlane = new Plane(boresight, boresight);
    Vector3 origin = fovPlane.getPoint();
    Vector3 xAxis = fovPlane.getSpanningVectors()[0];
    Vector3 yAxis = fovPlane.getSpanningVectors()[1];

    List<Vector3> boundaries = new ArrayList<>();
    boundaries.add(MathConversions.toVector3(frustum1()));
    boundaries.add(MathConversions.toVector3(frustum2()));
    boundaries.add(MathConversions.toVector3(frustum4()));
    boundaries.add(MathConversions.toVector3(frustum3()));
    List<Vector3> points = new ArrayList<>();
    for (Vector3 boundary : boundaries) {
      RayPlaneIntercept rpi =
          new RayPlaneIntercept(new Ray(new Vector3(0, 0, 0), boundary), fovPlane);
      points.add(rpi.getIntercept().sub(origin));
    }

    Path2D.Double shape = new Path2D.Double();
    shape.moveTo(xAxis.dot(points.get(0)), yAxis.dot(points.get(0)));
    for (int i = 1; i < points.size(); i++)
      shape.lineTo(xAxis.dot(points.get(i)), yAxis.dot(points.get(i)));
    shape.lineTo(xAxis.dot(points.get(0)), yAxis.dot(points.get(0)));

    for (Vector3 direction : directions) {
      RayPlaneIntercept rpi =
          new RayPlaneIntercept(new Ray(new Vector3(0, 0, 0), direction), fovPlane);
      Vector3 pointOnPlane = rpi.getIntercept().sub(origin);
      isInFOV.add(shape.contains(xAxis.dot(pointOnPlane), yAxis.dot(pointOnPlane)));
    }

    return Collections.unmodifiableList(isInFOV);
  }

}
