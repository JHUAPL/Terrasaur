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
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;

public class ProjectionRectangular extends Projection {

  /**
   * coordinates of upper left corner
   */
  private final LatitudinalVector startPos;
  private double delLat;
  private double delLon;

  public ProjectionRectangular(int w, int h) {
    this(w, h, new LatitudinalVector(1, 0, 0));
  }

  public ProjectionRectangular(int w, int h, LatitudinalVector centerPoint) {
    super(w, h, centerPoint);
    isWrapAround = true;

    startPos = new LatitudinalVector(1.0, Math.PI / 2, -Math.PI + centerPoint.getLongitude());
    delLat = Math.PI / h;
    delLon = 2 * Math.PI / w;
  }

  /**
   * For a subset of the entire map.
   * 
   * @param w width
   * @param h height
   * @param xAxis in radians
   * @param yAxis in radians
   * @param rotation rotation angle
   */
  public ProjectionRectangular(int w, int h, AxisX xAxis, AxisY yAxis, double rotation) {
    super(w, h);

    LatitudinalVector centerPoint =
        new LatitudinalVector(1, yAxis.getRange().getMiddle(), xAxis.getRange().getMiddle());

    this.rotate =
        (centerPoint.getLatitude() != 0 || centerPoint.getLongitude() != 0 || rotation != 0);
    if (rotate) {
      setXYZRotationMatrix(rotation, centerPoint.getLatitude(), -centerPoint.getLongitude());
      setZYXRotationMatrix(-rotation, -centerPoint.getLatitude(), centerPoint.getLongitude());
    }

    isWrapAround = true;

    startPos = new LatitudinalVector(1.0, yAxis.getRange().getMax(), xAxis.getRange().getMin());
    delLat = yAxis.getRange().getLength() / h;
    if (yAxis.getRange().isDecreasing())
      delLat *= -1;
    delLon = xAxis.getRange().getLength() / w;
    if (xAxis.getRange().isDecreasing())
      delLon *= -1;
  }

  @Override
  public LatitudinalVector pixelToSpherical(Point2D xy) {
    double lat = startPos.getLatitude() - xy.getY() * delLat;
    double lon = xy.getX() * delLon + startPos.getLongitude();
    if (Math.abs(lat) > Math.PI / 2)
      return null;
    return new LatitudinalVector(1, lat, lon);
  }

  @Override
  public Point2D.Double sphericalToPixel(LatitudinalVector latLon) {
    double x = (latLon.getLongitude() - startPos.getLongitude()) / delLon;
    double y = (startPos.getLatitude() - latLon.getLatitude()) / delLat;
    if (y < 0)
      y = 0;
    if (y >= h)
      y = h - 1;
    while (x < 0)
      x += w;
    while (x >= w)
      x -= w;
    return new Point2D.Double(x, y);
  }

}
