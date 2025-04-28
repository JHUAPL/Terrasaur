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
package terrasaur.enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;

/**
 * Enumeration containing the values and comments to use for FITS tags describing data stored in
 * FITS data cubes. The enumeration name references the type of data stored in a given plane. This
 * way the user can choose their own value for the FITS keyword (i.e. "PLANE1" or "PLANE10").
 * 
 * @author espirrc1
 * 
 */
public enum PlaneInfo {

  //@formatter:off
  LAT("Latitude of vertices", "[deg]", "deg"),
  LON("Longitude of vertices", "[deg]", "deg"),
  RAD("Radius of vertices", "[km]", "km"),
  X("X coordinate of vertices", "[km]", "km"),
  Y("Y coordinate of vertices", "[km]", "km"),
  Z("Z coordinate of vertices", "[km]", "km"),
  NORM_VECTOR_X("Normal vector X", null, null),
  NORM_VECTOR_Y("Normal vector Y", null, null),
  NORM_VECTOR_Z("Normal vector Z", null, null),
  GRAV_VECTOR_X("Gravity vector X", "[m/s^2]", "m/s**2"),
  GRAV_VECTOR_Y("Gravity vector Y", "[m/s^2]", "m/s**2"),
  GRAV_VECTOR_Z("Gravity vector Z", "[m/s^2]", "m/s**2"),
  GRAV_MAG("Gravitational magnitude", "[m/s^2]", "m/s**2"),
  GRAV_POT("Gravitational potential", "[J/kg]", "J/kg"),
  ELEV("Elevation", "[m]", "m"),
  AREA("Area", "[km^2]", "km**2"),

  // no longer needed! same as HEIGHT!
  // ELEV_NORM("Elevation relative to normal plane", "[m]", "m"),
  SLOPE("Slope", "[deg]", "deg"),
  SHADE("Shaded relief", null, null),
  TILT("Facet tilt", "[deg]", "deg"),
  TILT_DIRECTION("Facet tilt direction", "[deg]", "deg"),
  TILT_AVERAGE("Mean tilt", "[deg]", "deg"),
  TILT_VARIATION("Tilt variation", "[deg]", "deg"),
  TILT_AVERAGE_DIRECTION("Mean tilt direction", "[deg]", "deg"),
  TILT_DIRECTION_VARIATION("Tilt direction variation", "[deg]", "deg"),
  TILT_RELATIVE("Relative tilt", "[deg]", "deg"),
  TILT_RELATIVE_DIRECTION("Relative tilt direction", "[deg]", "deg"),
  TILT_UNCERTAINTY("Tilt Uncertainty", "[deg]", "deg"),
  FACETRAD("Facet radius", "[m]", "m"),
  HEIGHT("Height relative to normal plane", "[km]", "km"),
  RELATIVE_HEIGHT("Max relative height", "[km]", "km"),
  ALBEDO("Relative albedo", null, null),
  INTENSITY("Return Intensity", null, null),
  SIGMA("Sigma", null, null),
  QUALITY("Quality", null, null),
  SHADED("Shaded relief", null, null),
  NUMPOINTS("Number of OLA points used", null, null),
  HEIGHT_RESIDUAL("Mean of residual between points and fitted height", "[km]", "km"),
  HEIGHT_STDDEV("Std deviation of residual between points and fitted height", "[km]", "km"),
  HAZARD("Hazard", "1 indicates a hazard to the spacecraft", null);
  //@formatter:on

  private String keyValue; // value associated with FITS keyword
  private String comment; // comment associated with FITS keyword
  private String units; // units associated with the plane. Usually in PDS4 nomenclature

  PlaneInfo(String keyVal, String comment, String units) {
    this.keyValue = keyVal;
    this.comment = comment;
    this.units = units;
  }

  public String value() {
    return keyValue;
  }

  public String comment() {
    return comment;
  }

  public String units() {
    return units;
  }

  /**
   * Try to parse the enum from the given Keyval string. Needs to match exactly (but case
   * insensitive)!
   * 
   * @param keyVal
   * @return
   */
  public static PlaneInfo keyVal2Plane(String keyVal) {
    for (PlaneInfo planeName : values()) {
      if ((planeName.value() != null) && (planeName.value().equalsIgnoreCase(keyVal))) {
        return planeName;
      }
    }
    return null;
  }

  public static PlaneInfo planeFromString(String plane) {
    for (PlaneInfo planeName : values()) {
      if (planeName.toString().equals(plane)) {
        return planeName;
      }
    }
    return null;
  }

  /*
   * Create enumeration set for the first 6 planes. These are the initial planes created from the
   * Osiris Rex netCDF file.
   */
  public static final EnumSet<PlaneInfo> first6HTags = EnumSet.range(PlaneInfo.LAT, PlaneInfo.Z);

  public static List<PlaneInfo> coreTiltPlanes() {

    List<PlaneInfo> coreTilts = new ArrayList<PlaneInfo>();
    coreTilts.add(PlaneInfo.TILT_AVERAGE);
    coreTilts.add(PlaneInfo.TILT_VARIATION);
    coreTilts.add(PlaneInfo.TILT_AVERAGE_DIRECTION);
    coreTilts.add(PlaneInfo.TILT_DIRECTION_VARIATION);
    coreTilts.add(PlaneInfo.TILT_RELATIVE);
    coreTilts.add(PlaneInfo.TILT_RELATIVE_DIRECTION);
    coreTilts.add(PlaneInfo.RELATIVE_HEIGHT);

    return coreTilts;

  }

  /**
   * Convert List<PlaneInfo> to List<HeaderCard> where each HeaderCard in List follows the
   * convention: for each "thisPlane" in List<PlaneInfo> HeaderCard = new HeaderCard("PLANE" + cc,
   * thisPlane.value(), thisPlane.comment()) The order in List<HeaderCard> follows the order in
   * List<PlaneInfo>
   * 
   * @param planeList
   * @return
   * @throws HeaderCardException
   */
  public static List<HeaderCard> planesToHeaderCard(List<PlaneInfo> planeList)
      throws HeaderCardException {
    List<HeaderCard> planeHeaders = new ArrayList<HeaderCard>();
    String plane = "PLANE";
    int cc = 1;
    for (PlaneInfo thisPlane : planeList) {

      planeHeaders.add(new HeaderCard(plane + cc, thisPlane.value(), thisPlane.comment()));
      cc++;
    }
    return planeHeaders;

  }

}
