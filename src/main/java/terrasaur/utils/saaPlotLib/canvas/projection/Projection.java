package terrasaur.utils.saaPlotLib.canvas.projection;

import java.awt.geom.Point2D;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.MatrixIJK;
import picante.math.vectorspace.UnwritableMatrixIJK;
import picante.math.vectorspace.UnwritableVectorIJK;

public abstract class Projection {

  protected final int w, h;
  protected boolean isWrapAround;

  protected boolean rotate;
  private UnwritableMatrixIJK rotateXYZ;
  private UnwritableMatrixIJK rotateZYX;

  public Projection(int w, int h) {
    this(w, h, new LatitudinalVector(1, 0, 0), 0);
  }

  public Projection(int w, int h, LatitudinalVector centerPoint) {
    this(w, h, centerPoint, 0);
  }

  public Projection(int w, int h, LatitudinalVector centerPoint, double rotation) {
    this.w = w;
    this.h = h;
    this.rotate =
        (centerPoint.getLatitude() != 0 || centerPoint.getLongitude() != 0 || rotation != 0);
    if (rotate) {
      setXYZRotationMatrix(rotation, centerPoint.getLatitude(), -centerPoint.getLongitude());
      setZYXRotationMatrix(-rotation, -centerPoint.getLatitude(), centerPoint.getLongitude());
    }
    isWrapAround = false;
  }

  public int getWidth() {
    return w;
  }

  public int getHeight() {
    return h;
  }

  public boolean isWrapAround() {
    return isWrapAround;
  }

  protected void setXYZRotationMatrix(double rotX, double rotY, double rotZ) {
    rotateXYZ = MatrixIJK.IDENTITY;
    if (rotX == 0 && rotY == 0 && rotZ == 0) {
      return;
    }

    double cosx = Math.cos(rotX);
    double cosy = Math.cos(rotY);
    double cosz = Math.cos(rotZ);
    double sinx = Math.sin(rotX);
    double siny = Math.sin(rotY);
    double sinz = Math.sin(rotZ);

    double ii = cosy * cosz;
    double ij = sinx * siny * cosz + cosx * sinz;
    double ik = -cosx * siny * cosz + sinx * sinz;
    double ji = -cosy * sinz;
    double jj = -sinx * siny * sinz + cosx * cosz;
    double jk = cosx * siny * sinz + sinx * cosz;
    double ki = siny;
    double kj = -sinx * cosy;
    double kk = cosx * cosy;

    rotateXYZ = new UnwritableMatrixIJK(ii, ji, ki, ij, jj, kj, ik, jk, kk);
  }

  protected void setZYXRotationMatrix(double rotX, double rotY, double rotZ) {
    rotateZYX = MatrixIJK.IDENTITY;
    if (rotX == 0 && rotY == 0 && rotZ == 0) {
      return;
    }

    double cosx = Math.cos(rotX);
    double cosy = Math.cos(rotY);
    double cosz = Math.cos(rotZ);
    double sinx = Math.sin(rotX);
    double siny = Math.sin(rotY);
    double sinz = Math.sin(rotZ);

    double ii = cosy * cosz;
    double ij = cosy * sinz;
    double ik = -siny;
    double ji = -cosx * sinz + sinx * siny * cosz;
    double jj = sinx * siny * sinz + cosx * cosz;
    double jk = sinx * cosy;
    double ki = cosx * siny * cosz + sinx * sinz;
    double kj = -sinx * cosz + cosx * siny * sinz;
    double kk = cosx * cosy;

    rotateZYX = new UnwritableMatrixIJK(ii, ji, ki, ij, jj, kj, ik, jk, kk);
  }

  public LatitudinalVector rotateXYZ(double lat, double lon) {
    return rotateXYZ(new LatitudinalVector(1, lat, lon));
  }

  public LatitudinalVector rotateXYZ(LatitudinalVector point) {
    UnwritableVectorIJK xyz = CoordConverters.convert(point);
    UnwritableVectorIJK rotated = rotateXYZ.mxv(xyz);

    return CoordConverters.convertToLatitudinal(rotated);
  }

  public LatitudinalVector rotateZYX(double lat, double lon) {
    return rotateZYX(new LatitudinalVector(1, lat, lon));
  }

  public LatitudinalVector rotateZYX(LatitudinalVector point) {
    UnwritableVectorIJK xyz = CoordConverters.convert(point);
    UnwritableVectorIJK rotated = rotateZYX.mxv(xyz);

    return CoordConverters.convertToLatitudinal(rotated);
  }

  public LatitudinalVector pixelToSpherical(double x, double y) {
    return pixelToSpherical(new Point2D.Double(x, y));
  }

  public Point2D sphericalToPixel(double lat, double lon) {
    return sphericalToPixel(new LatitudinalVector(1, lat, lon));
  }

  public abstract LatitudinalVector pixelToSpherical(Point2D xy);

  public abstract Point2D sphericalToPixel(LatitudinalVector latLon);

  /**
   * 
   * @return angle between center pixel and one pixel to the right
   */
  public double radiansPerPixel() {
    UnwritableVectorIJK center =
        CoordConverters.convert(pixelToSpherical(new Point2D.Double(w / 2., h / 2.)));
    UnwritableVectorIJK neighbor =
        CoordConverters.convert(pixelToSpherical(new Point2D.Double(w / 2. + 1, h / 2.)));
    return center.getSeparation(neighbor);
  }

}
