package terrasaur.altwg.pipeline;

import terrasaur.enums.AltwgDataType;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;
import terrasaur.fits.HeaderTag;
import terrasaur.utils.StringUtil;

public class AltwgMLNNamer implements ProductNamer {

  public AltwgMLNNamer() {
    super();
  }

  @Override
  public String getNameFrag(String productName, int fieldNum) {

    String nameFrag = "";

    return nameFrag;
  }

  /**
   * ALTWG MLN naming convention applies only to one productType - the ALTWG NFT-MLN.
   *
   * @param hdrBuilder - contains values that are used to create the MLN according to naming
   *     convention.
   * @param altwgProduct - N/A. Included here as part of the interface structure.
   * @param isGlobal - N/A. Included here as part of the interface structure.
   */
  @Override
  public String productbaseName(
      FitsHdrBuilder hdrBuilder, AltwgDataType altwgProduct, boolean isGlobal) {

    // initialize string fragments for NFT name. This will help identify
    // which string fragments have not been updated by the method.
    String gsd = "gsd";
    String dataSrc = "dataSrc";
    String dataSrcfile = "dataSrcFile";
    String productType = "nftdtm";
    String cLon = "cLon";
    String cLat = "cLat";
    String prodVer = "prodVer";
    String productID = "prodID";

    // find relevant information in the hdrBuilder map.
    double gsdD = gsdFromHdr(hdrBuilder);
    int gsdI = (int) Math.round(gsdD);
    String fileUnits = "mm";
    gsd = String.format("%05d", gsdI) + fileUnits;
    // HeaderTag key = HeaderTag.GSD;
    // if (hdrBuilder.containsKey(key)) {
    // gsd = hdrBuilder.getCard(key).getValue();
    //
    // double gsdD = Double.parseDouble(gsd);
    // String fileUnits = "";
    // if (hdrBuilder.getCard(key).getComment().contains("[mm]")) {
    // fileUnits = "mm";
    // } else if (hdrBuilder.getCard(key).getComment().contains("[cm]")) {
    //
    // // mandated to use mm! change the units
    // gsdD = gsdD * 10.0D;
    // gsdI = (int) Math.round(gsdD);
    // }
    //
    // System.out.println("gsd:" + gsd);
    // System.out.println("file units:" + fileUnits);
    //
    // }

    HeaderTag key = HeaderTag.DATASRC;
    if (hdrBuilder.containsKey(key)) {
      dataSrc = hdrBuilder.getCard(key).getValue().toLowerCase();
      // data source should only be 3 chars long
      if (dataSrc.length() > 3) {
        dataSrc = dataSrc.substring(0, 3);
      }
    }

    key = HeaderTag.CLON;
    if (hdrBuilder.containsKey(key)) {
      cLon = hdrBuilder.getCard(key).getValue();
    }

    key = HeaderTag.CLAT;
    if (hdrBuilder.containsKey(key)) {
      cLat = hdrBuilder.getCard(key).getValue();
    }

    key = HeaderTag.DATASRCF;
    if (hdrBuilder.containsKey(key)) {
      dataSrcfile = hdrBuilder.getCard(key).getValue();
    }

    key = HeaderTag.PRODVERS;
    if (hdrBuilder.containsKey(key)) {
      prodVer = hdrBuilder.getCard(key).getValue();
      prodVer = prodVer.replaceAll("\\.", "");
    }

    // hardcode region to local
    String region = "l";

    StringBuilder sb = new StringBuilder();
    String delim = "_";
    sb.append(region);
    sb.append(delim);
    sb.append(gsd);
    sb.append(delim);
    sb.append(dataSrc);
    sb.append(delim);
    sb.append(productType);
    sb.append(delim);

    /*
     * determine product ID. For OLA it is the center lat,lon For SPC it is the NFT feature id,
     * which is assumed to be the first 5 chars in DATASRC
     */
    if (dataSrc.contains("ola")) {

      // follow ALTWG product naming convention for center lat, lon
      productID = ALTWGProductNamer.clahLon(cLat, cLon);
    } else {
      productID = dataSrcfile.substring(0, 5);
    }
    sb.append(productID);
    sb.append(delim);
    sb.append("v");
    sb.append(prodVer);

    return sb.toString().toLowerCase();
  }

  @Override
  public String getVersion(FitsHdrBuilder hdrBuilder) {

    String version = "";

    return version;
  }

  /**
   * Extract ground sample distance from FitsHdrBuilder. GSD is needed as part of naming convention.
   * GSD in units of mm.
   */
  @Override
  public double gsdFromHdr(FitsHdrBuilder hdrBuilder) {

    // find relevant information in the hdrBuilder map.
    double gsdD = Double.NaN;
    HeaderTag key = HeaderTag.GSD;
    if (hdrBuilder.containsKey(key)) {
      String gsd = hdrBuilder.getCard(key).getValue();

      gsdD = Double.parseDouble(gsd);
      String fileUnits = "";
      if (hdrBuilder.getCard(key).getComment().contains("[mm]")) {
        fileUnits = "mm";
      } else if (hdrBuilder.getCard(key).getComment().contains("[cm]")) {

        // mandated to use mm! change the units
        gsdD = gsdD * 10.0D;
        fileUnits = "mm";
      }
      System.out.println("gsd:" + gsd);
      System.out.println("file units:" + fileUnits);

    } else {
      String errMesg =
          "ERROR! Could not find keyword:" + HeaderTag.GSD.toString() + " in hdrBuilder";
      throw new RuntimeException(errMesg);
    }

    return gsdD;
  }

  @Override
  public NameConvention getNameConvention() {
    return NameConvention.ALTNFTMLN;
  }

  @Override
  /** Parse the filename using the ALTWG MLN naming convention and return the GSD value. */
  public double gsdFromFilename(String filename) {
    String[] splitStr = filename.split("_");
    // GSD is second element
    String gsd = splitStr[1];
    gsd = gsd.replace("mm", "");
    return StringUtil.parseSafeD(gsd);
  }
}
