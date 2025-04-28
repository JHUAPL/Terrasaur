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
package terrasaur.utils.saaPlotLib.canvas.projection;

import java.awt.geom.Point2D;
import picante.math.coords.LatitudinalVector;

public class ProjectionOrthographic extends Projection {

  private double P, Pm1, Pp1, PPm1, Pm1sq;
  private double dispScale;
  private double radius;
  private final double range;

  public ProjectionOrthographic(int w, int h) {
    this(w, h, new LatitudinalVector(1, 0, 0));
  }

  public ProjectionOrthographic(int w, int h, LatitudinalVector centerPoint) {
    this(w, h, centerPoint, 0);

  }

  public ProjectionOrthographic(int w, int h, LatitudinalVector centerPoint, double rotation) {
    super(w, h, centerPoint, rotation);
    isWrapAround = false;

    radius = 0.5;
    range = 1000;

    dispScale = radius * h;

    setRange(range);
  }

  /**
   * 
   * @param radius radius of the rendered disk as a fraction of the image height. Default is 0.5.
   */
  public void setRadius(double radius) {
    this.radius = radius;
    dispScale = radius * h;
    setRange(range);
  }

  public void setRange(double range) {
    P = range;
    Pp1 = (P + 1);
    Pm1 = (P - 1);
    PPm1 = P * Pm1;
    Pm1sq = Pm1 * Pm1;

    dispScale *= Math.sqrt(Pp1 / Pm1);
  }

  @Override
  public LatitudinalVector pixelToSpherical(Point2D xy) {
    final double X = (xy.getX() - w / 2.) / dispScale;
    final double Y = (h / 2. - xy.getY()) / dispScale;

    final double rho2 = X * X + Y * Y;
    if (rho2 > 1)
      return null;

    final double rho = Math.sqrt(rho2);

    double lat;
    double lon;
    if (rho == 0) {
      lat = 0;
      lon = 0;
    } else {
      double arg = Pm1 * (Pm1 - rho2 * Pp1);
      if (arg < 0)
        return null;

      final double N = rho * (PPm1 - Math.sqrt(arg));
      final double D = (Pm1sq + rho2);

      final double sinc = N / D;
      final double cosc = Math.sqrt(1 - sinc * sinc);

      arg = Y * sinc / rho;
      if (Math.abs(arg) > 1)
        return null;

      lat = Math.asin(arg);
      lon = Math.atan2(X * sinc, rho * cosc);
    }

    LatitudinalVector ll = new LatitudinalVector(1, lat, lon);
    if (rotate)
      ll = rotateXYZ(ll);

    return ll;
  }

  @Override
  public Point2D sphericalToPixel(LatitudinalVector latLon) {
    LatitudinalVector ll = new LatitudinalVector(1, latLon.getLatitude(), latLon.getLongitude());
    if (rotate)
      ll = rotateZYX(ll);

    double lat = ll.getLatitude();
    double lon = ll.getLongitude();

    final double cosc = Math.cos(lat) * Math.cos(lon);
    if (cosc < 0)
      return null;

    final double k = (P - 1) / (P - cosc);

    final double X = k * Math.cos(lat) * Math.sin(lon);
    final double Y = k * Math.sin(lat);

    double x = X * dispScale + w / 2.;
    if (x < 0 || x >= w)
      return null;

    double y = h / 2. - Y * dispScale;
    if (y < 0 || y >= h)
      return null;

    if (P * cosc < 1) {
      double dist = Math.sqrt(x * x + y * y);
      if (dist < dispScale)
        return null;
    }

    return new Point2D.Double(x, y);
  }

}
