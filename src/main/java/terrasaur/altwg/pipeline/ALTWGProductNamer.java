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
package terrasaur.altwg.pipeline;

import terrasaur.enums.AltwgDataType;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;
import terrasaur.fits.HeaderTag;
import terrasaur.utils.StringUtil;

public class ALTWGProductNamer implements ProductNamer {

  public ALTWGProductNamer() {
    super();
  }

  /**
   * Parse the productName and return the portion of the name corresponding to a given field. Fields
   * are assumed separated by "_" in the filename.
   *
   * @param productName
   * @param fieldNum
   * @return
   */
  @Override
  public String getNameFrag(String productName, int fieldNum) {

    String[] fields = productName.split("_");
    String returnField = "ERROR";
    if (fieldNum > fields.length) {
      System.out.println(
          "ERROR, field:" + fieldNum + " requested is beyond the number of fields found.");
      System.out.println("returning:" + returnField);
    } else {
      returnField = fields[fieldNum];
    }
    return returnField;
  }

  @Override
  public String productbaseName(
      FitsHdrBuilder hdrBuilder, AltwgDataType altwgProduct, boolean isGlobal) {

    String gsd = "gsd";
    String dataSrc = "dataSrc";
    String productType = altwgProduct.getFileFrag();
    String prodVer = getVersion(hdrBuilder);

    // extract ground sample distance. gsdD is in mm!
    double gsdD = gsdFromHdr(hdrBuilder);

    int gsdI = (int) Math.round(gsdD);
    String fileUnits = "mm";
    gsd = String.format("%05d", gsdI) + fileUnits;

    // System.out.println("gsd:" + gsd);
    // System.out.println("file units:" + fileUnits);

    HeaderTag key = HeaderTag.DATASRC;
    if (hdrBuilder.containsKey(key)) {
      dataSrc = hdrBuilder.getCard(key).getValue().toLowerCase();

      // check whether dataSrc needs to be modified
      dataSrc = HeaderTag.getSDP(dataSrc);
      // data source should only be 3 chars long
      if (dataSrc.length() > 3) {
        dataSrc = dataSrc.substring(0, 3);
      }
    }

    key = HeaderTag.CLON;
    String cLon = null;
    if (hdrBuilder.containsKey(key)) {
      cLon = hdrBuilder.getCard(key).getValue();
    }
    if (cLon == null) {
      if (isGlobal) {
        // set center longitude to 0.0 if value not parsed and this is a global product
        cLon = "0.0";
      } else {
        String errMesg = "ERROR! Could not parse CLON from fits header!";
        throw new RuntimeException(errMesg);
      }
    }
    // System.out.println("clon:" + cLon);
    key = HeaderTag.CLAT;
    String cLat = null;
    if (hdrBuilder.containsKey(key)) {
      cLat = hdrBuilder.getCard(key).getValue();
    }
    if (cLat == null) {
      if (isGlobal) {
        // set center latitude to 0.0 if value not parsed and this is a global product
        cLat = "0.0";
      } else {
        String errMesg = "ERROR! Could not parse CLAT from fits header!";
        throw new RuntimeException(errMesg);
      }
    }
    // System.out.println("clat" + cLat);

    String region = "l";
    if (isGlobal) {
      region = "g";
    }

    String clahLon = ALTWGProductNamer.clahLon(cLat, cLon);

    // pds likes having filenames all in the same case, so chose lowercase
    String outFile =
        ALTWGProductNamer.altwgBaseName(region, gsd, dataSrc, productType, clahLon, prodVer);
    return outFile;
  }

  /**
   * Retrieve the product version string. Returns initial default value if product version keyword
   * not found in builder.
   *
   * @param hdrBuilder
   */
  @Override
  public String getVersion(FitsHdrBuilder hdrBuilder) {
    String prodVer = "prodVer";

    // note: this has been changed to MAP_VER in the SIS
    HeaderTag key = HeaderTag.MAP_VER;
    // key = HeaderTag.PRODVERS;
    if (hdrBuilder.containsKey(key)) {
      prodVer = hdrBuilder.getCard(key).getValue();
      prodVer = prodVer.replaceAll("\\.", "");
    }

    return prodVer;
  }

  // Given the fields return the altwg PDS base name
  public static String altwgBaseName(
      String region, String gsd, String dataSource, String desc, String lahLon, String version) {

    StringBuilder sb = new StringBuilder();
    String delim = "_";
    sb.append(region);
    sb.append(delim);
    sb.append(gsd);
    sb.append(delim);

    // data source should only be 3 characters long
    if (dataSource.length() > 3) {
      System.out.println("WARNING! dataSource:" + dataSource + " longer than 3 chars!");
      dataSource = dataSource.substring(0, 3);
      System.out.println(
          "Will set data source to:"
              + dataSource
              + " but"
              + " this might NOT conform to the ALTWG naming convention!");
    }
    sb.append(dataSource);
    sb.append(delim);
    sb.append(desc);
    sb.append(delim);
    sb.append(lahLon);
    sb.append(delim);
    sb.append("v");

    // remove '.' from version string
    version = version.replaceAll("\\.", "");
    sb.append(version);

    // pds likes having filenames all in the same case, so chose lowercase
    String outFile = sb.toString().toLowerCase();

    return outFile;
  }

  /**
   * Parse center lat, lon strings and return the formatted clahLon portion of the PDS filename.
   *
   * @param clat
   * @param clon
   * @return
   */
  public static String clahLon(String clat, String clon) {

    // String cLon = "";

    // remove all whitespace that may exist in the strings
    clat = clat.replaceAll("\\s+", "");
    clon = clon.replaceAll("\\s+", "");

    double cLonD = StringUtil.parseSafeD(clon);
    double cLatD = StringUtil.parseSafeD(clat);

    String clahLon = clahLon(cLatD, cLonD);
    return clahLon;
  }

  public static String clahLon(double cLatD, double cLonD) {

    String cLon = "";

    if (cLonD == Double.NaN) {

      // unable to parse center longitude using normal method (see V in getProductCards())
      cLon = "xxxxxx";

    } else {

      if (cLonD < 0) {
        // transform to 0-360
        cLonD = cLonD + 360D;
      }
      // format double to 2 significant digits
      cLon = String.format("%06.2f", cLonD);
    }

    // remove decimal point
    cLon = cLon.replace(".", "");

    String cLat = "";

    // System.out.println("cLatD:" + Double.toString(cLatD));
    if (cLatD == Double.NaN) {

      // unable to parse center latitude
      cLat = "xxxxxx";

    } else {

      double tol = 0.0101D;

      // determine whether latitude is within tolerance of its rounded value.
      // if so then use rounded value
      double roundValue = Math.round(cLatD);
      double diffTol = Math.abs(roundValue - cLatD);
      if (diffTol < tol) {
        cLatD = roundValue;
      }
      String hemiSphere = (cLatD >= 0) ? "N" : "S";

      if (cLatD < 0) {
        // remove negative sign if in southern hemisphere
        cLatD = cLatD * -1.0D;
      }
      // format cLat to 2 significant digits
      cLat = String.format("%05.2f", cLatD);
      cLat = cLat.replace(".", "");

      // trim to length 4.
      cLat = cLat.substring(0, Math.min(cLat.length(), 4));
      cLat = cLat + hemiSphere;
    }

    String clahLon = cLat + cLon;
    return clahLon;
  }

  /**
   * return GSD parsed from FitsHdrBuilder. Returns 0 if valid GSD could not be parsed. GSD is in
   * mm.
   *
   * @param hdrBuilder
   * @return
   */
  @Override
  public double gsdFromHdr(FitsHdrBuilder hdrBuilder) {

    String gsd = "gsd";
    double gsdD = Double.NaN;

    // extract ground sample distance using GSD first
    HeaderTag key = HeaderTag.GSD;
    if (hdrBuilder.containsKey(key)) {
      gsd = hdrBuilder.getCard(key).getValue();
      gsdD = StringUtil.parseSafeD(gsd);
      if (gsdD < 0D) {
        // keyword value not initialized
        gsdD = Double.NaN;
        System.out.println("WARNING! keyword GSD not set!");
      }
    } else {
      System.out.println("could not find " + key.toString() + " to parse GSD from.");
    }
    if (Double.isNaN(gsdD)) {
      // could not parse GSD into valid number, try GSDI
      key = HeaderTag.GSDI;
      if (hdrBuilder.containsKey(key)) {
        gsdD = StringUtil.parseSafeD(hdrBuilder.getCard(key).getValue());
        if (gsdD < 0D) {
          // keyword value not initialized
          gsdD = Double.NaN;
          System.out.println("WARNING! keyword GSDI not set!");
        }
      } else {
        System.out.println("could not find " + key.toString() + " to parse GSD from.");
      }
      if (Double.isNaN(gsdD)) {
        // still cannot parse gsd. Set to -999
        System.out.println(
            "WARNING: No valid values of GSD or GSDI could be parsed from fits header!");
        System.out.println("Setting gsd = -999");
        gsdD = -999D;
      }
    }

    if (hdrBuilder.getCard(key).getComment().contains("[cm]")) {

      // mandated to use mm! change the units
      gsdD = gsdD * 10.0D;
    }

    return gsdD;
  }

  @Override
  public NameConvention getNameConvention() {
    return NameConvention.ALTPRODUCT;
  }

  @Override
  /** Parse the filename using the ALTWGProduct naming convention and return the GSD value. */
  public double gsdFromFilename(String filename) {
    String[] splitStr = filename.split("_");
    // GSD is second element
    String gsd = splitStr[1];
    gsd = gsd.replace("mm", "");
    return StringUtil.parseSafeD(gsd);
  }
}
