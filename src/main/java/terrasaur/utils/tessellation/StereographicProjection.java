package terrasaur.utils.tessellation;

import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;

import java.awt.geom.Point2D;

/**
 * Implement a stereographic projection. This is package private so that there's no conflict with
 * anything in the cartography package. Based on Snyder (1987).
 * 
 * @author nairah1
 *
 */
class StereographicProjection {

  private final double centerLat, centerLon;
  private final double sinCenterLat, cosCenterLat;
  private final double k0, R;

  /**
   * Create a stereographic projection centered on the desired coordinates. The radius of the
   * projection is 1.
   * 
   * @param center projection center
   */
  StereographicProjection(LatitudinalVector center) {
    this(1.0, center);
  }

  /**
   * Create a stereographic projection centered on the desired coordinates. The radius of the
   * projection may be specified.
   * 
   * @param R scale
   * @param center projection center
   */
  StereographicProjection(double R, LatitudinalVector center) {
    this.R = R;
    this.centerLat = center.getLatitude();
    this.centerLon = center.getLongitude();

    sinCenterLat = Math.sin(centerLat);
    cosCenterLat = Math.cos(centerLat);
    k0 = 1.0;
  }

  /**
   * Return x, y coordinates of the input coordinates.
   * 
   * @param xyz 3D input coordinate
   * @return 2D projected coordinate
   */
  public Point2D forward(UnwritableVectorIJK xyz) {
    LatitudinalVector lv = CoordConverters.convertToLatitudinal(xyz);
    return forward(lv);
  }

  /**
   * Return x, y coordinates of the input coordinates.
   * 
   * @param lv 3D input coordinate (radius is not used - only lat and lon)
   * @return 2D projected coordinate
   */
  public Point2D forward(LatitudinalVector lv) {
    return forward(lv.getLatitude(), lv.getLongitude());
  }

  /**
   * Return x, y coordinates of the input coordinates.
   * 
   * @param lat latitude (radians)
   * @param lon longitude (radians)
   * @return 2D projected coordinate
   */
  public Point2D forward(double lat, double lon) {
    double sinLat = Math.sin(lat);
    double cosLat = Math.cos(lat);
    double sinLon = Math.sin(lon - centerLon);
    double cosLon = Math.cos(lon - centerLon);

    double k = 2 * k0 / (1 + sinCenterLat * sinLat + cosCenterLat * cosLat * cosLon);
    double x = R * k * cosLat * sinLon;
    double y = R * k * (cosCenterLat * sinLat - sinCenterLat * cosLat * cosLon);

    return new Point2D.Double(x, y);
  }

  public LatitudinalVector inverse(double x, double y) {
    final double rho = Math.sqrt(x * x + y * y);
    double c = 2 * Math.atan(rho / (2 * R * k0));
    double sinC = Math.sin(c);
    double cosC = Math.cos(c);

    double lat =
        rho == 0 ? centerLat : Math.asin(cosC * sinCenterLat + y * sinC * cosCenterLat / rho);
    double lon = centerLon;
    if (rho != 0) {
      if (sinCenterLat == 1) {
        lon += Math.atan(-x / y);
      } else if (sinCenterLat == -1) {
        lon += Math.atan(x / y);
      } else {
        lon += Math.atan(x * sinC / (rho * cosCenterLat * cosC - y * sinCenterLat * sinC));
      }
    }

    return new LatitudinalVector(1.0, lat, lon);
  }

}
