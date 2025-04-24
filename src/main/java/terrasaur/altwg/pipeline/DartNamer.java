package terrasaur.altwg.pipeline;

import java.util.HashMap;
import java.util.Map;
import terrasaur.enums.AltwgDataType;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;
import terrasaur.fits.HeaderTag;
import terrasaur.utils.StringUtil;

/**
 * Determines product names for DART shape models and ancillary products.
 *
 * @author espirrc1
 */
public class DartNamer implements ProductNamer {

  public static String getBaseName(Map<NameFields, String> nameFragments) {

    // check to see if the map contains all the fragments needed. Throw runtimeexception if it
    // doesn't
    validateMap(nameFragments);

    StringBuilder sb = new StringBuilder();
    String delim = "_";
    sb.append(nameFragments.get(NameFields.REGION));
    sb.append(delim);
    sb.append(nameFragments.get(NameFields.GSD));
    sb.append(delim);

    // data source should only be 3 characters long
    String dataSource = nameFragments.get(NameFields.DATASRC);
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

    sb.append(nameFragments.get(NameFields.DATATYPE));
    sb.append(delim);
    sb.append(nameFragments.get(NameFields.TBODY));
    sb.append(delim);
    sb.append(nameFragments.get(NameFields.CLATLON));
    sb.append(delim);
    sb.append("v");

    // remove '.' from version string
    String version = nameFragments.get(NameFields.VERSION);
    version = version.replaceAll("\\.", "");
    sb.append(version);

    // pds likes having filenames all in the same case, so chose lowercase
    String outFile = sb.toString().toLowerCase();

    return outFile;
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

    Map<NameFields, String> nameFragments = new HashMap<NameFields, String>();

    // data type
    String productType = altwgProduct.getFileFrag();
    nameFragments.put(NameFields.DATATYPE, productType);

    // product version
    String prodVer = getVersion(hdrBuilder);
    nameFragments.put(NameFields.VERSION, prodVer);

    // extract ground sample distance. gsdD is in mm!
    double gsdD = gsdFromHdr(hdrBuilder);

    int gsdI = (int) Math.round(gsdD);
    String fileUnits = "mm";
    gsd = String.format("%05d", gsdI) + fileUnits;
    nameFragments.put(NameFields.GSD, gsd);

    // data source
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
    nameFragments.put(NameFields.DATASRC, dataSrc);

    // center lon
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

    // center lat
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

    String clahLon = ALTWGProductNamer.clahLon(cLat, cLon);
    nameFragments.put(NameFields.CLATLON, clahLon);

    // region
    String region = "l";
    if (isGlobal) {
      region = "g";
    }
    nameFragments.put(NameFields.REGION, region);

    // target body
    key = HeaderTag.TARGET;
    String tBody = "unkn";
    if (hdrBuilder.containsKey(key)) {
      tBody = hdrBuilder.getCard(key).getValue();
      tBody = getBodyStFrag(tBody);
    }
    nameFragments.put(NameFields.TBODY, tBody);

    // pds likes having filenames all in the same case, so chose lowercase
    String outFile = DartNamer.getBaseName(nameFragments);
    return outFile;
  }

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
    return NameConvention.DARTPRODUCT;
  }

  /**
   * Parse target body string to get the proper string fragment for the target body name.
   *
   * @param tBody
   * @return
   */
  private String getBodyStFrag(String tBody) {

    String returnFrag = tBody;

    if (tBody.toLowerCase().contains("didy")) {
      returnFrag = "didy";
    } else {
      if (tBody.toLowerCase().contains("dimo")) {
        returnFrag = "dimo";
      } else {
        System.out.println("Could not parse target string fragment from:" + tBody);
      }
    }

    return returnFrag;
  }

  private static void validateMap(Map<NameFields, String> nameFragments) {

    NameFields[] reqFields = new NameFields[7];
    reqFields[0] = NameFields.REGION;
    reqFields[1] = NameFields.GSD;
    reqFields[2] = NameFields.DATASRC;
    reqFields[3] = NameFields.DATATYPE;
    reqFields[4] = NameFields.TBODY;
    reqFields[5] = NameFields.CLATLON;
    reqFields[6] = NameFields.VERSION;

    for (NameFields requiredField : reqFields) {

      if (!nameFragments.containsKey(requiredField)) {
        String errMesg = "ERROR! Missing required field:" + requiredField.toString();
        throw new RuntimeException(errMesg);
      }
    }
  }

  @Override
  /** Parse the filename using the DART naming convention and return the GSD value. */
  public double gsdFromFilename(String filename) {

    String[] splitStr = filename.split("_");
    // GSD is second element
    String gsd = splitStr[1];
    gsd = gsd.replace("mm", "");
    return StringUtil.parseSafeD(gsd);
  }
}
