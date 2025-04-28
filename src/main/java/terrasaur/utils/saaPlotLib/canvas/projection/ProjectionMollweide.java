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

public class ProjectionMollweide extends Projection {

  private final double radius;

  public ProjectionMollweide(int w, int h, LatitudinalVector centerPoint, double rotation) {
    super(w, h, centerPoint, rotation);
    radius = 0.5;
  }

  public ProjectionMollweide(int w, int h, LatitudinalVector centerPoint) {
    this(w, h, centerPoint, 0);
  }

  public ProjectionMollweide(int w, int h) {
    this(w, h, new LatitudinalVector(1, 0, 0), 0);
  }

  @Override
  public LatitudinalVector pixelToSpherical(Point2D xy) {
    double X = 2.0 * xy.getX() / w - 1;
    double Y = 1 - 2.0 * xy.getY() / h;

    double arg = Y / radius;
    if (Math.abs(arg) > 1)
      return null;
    double theta = Math.asin(arg);

    arg = (2 * theta + Math.sin(2 * theta)) / Math.PI;
    if (Math.abs(arg) > 1)
      return null;
    double lat = Math.asin(arg);

    double lon;
    if (Math.abs(theta) == Math.PI)
      lon = 0;
    else {
      lon = Math.PI * X / (2 * radius * Math.cos(theta));
      if (Math.abs(lon) > Math.PI)
        return null;
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

    double theta = ll.getLatitude();
    double del_theta = 1;
    while (Math.abs(del_theta) > 1e-5) {
      del_theta = -((theta + Math.sin(theta) - Math.PI * Math.sin(ll.getLatitude()))
          / (1 + Math.cos(theta)));
      theta += del_theta;
    }
    theta /= 2;

    double X = (2 * radius / Math.PI) * ll.getLongitude() * Math.cos(theta);
    double Y = radius * Math.sin(theta);

    double x = (X + 1) * w / 2;
    if (x < 0 || x >= w)
      return null;

    double y = h / 2. * (1 - Y);
    if (y < 0 || y >= h)
      return null;

    return new Point2D.Double(x, y);
  }

}
