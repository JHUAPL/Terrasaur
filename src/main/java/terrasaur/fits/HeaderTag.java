package terrasaur.fits;

import java.util.EnumSet;

/**
 * Enumeration contains FITS tags (keyword,value,comment) that only describe metadata contained in
 * the fits header. For example, ULLAT, ULLON are only contained in the FITS header. Other fits
 * tags, such as LAT or LON also describe image planes in the FITS data cube and are in a separate
 * enumeration. A few keywords may have overlap. For example, SIGMA is defined here to represent the
 * global uncertainty of the data. It is also in PlaneInfo to define an entire image plane
 * consisting of sigma values.
 * 
 * @author espirrc1
 * 
 */
public enum HeaderTag {

  // fits data tags. Included here in case we want to have values or comments
  // that are not null. Some of the default values obviously need to be updated when actual fits
  // file
  // is created.
  SIMPLE("T", null), BITPIX(null, null), NAXIS("3", null), NAXIS1(null, null), NAXIS2(null, null),

  NAXIS3(null, null), EXTEND("T", null), HDRVERS("1.0.0", null), NPRDVERS("1.0.0",
      null), MISSION("OSIRIS-REx", "Name of mission"), HOSTNAME("OREX", "PDS ID"),

  TARGET("101955 BENNU", "Target object"), ORIGIN("OREXSPOC", null),

  SPOC_ID("SPOCUPLOAD", null), SDPAREA("SPOCUPLOAD", null), SDPDESC("SPOCUPLOAD", null),

  MPHASE("FillMeIn", "Mission phase."), DATASRC("FillMeIn",
      "Shape model data source, i.e. 'SPC' or 'OLA'"),

  DATASRCF("FillMeIn", "Source shape model data filename"), DATASRCV("FillMeIn",
      "Name and version of shape model"), DATASRCD("FillMeIn",
          "Creation date of shape model in UTC"), DATASRCS("N/A",
              "[m/pix] Shpe model plt scale"), SOFTWARE("FillMeIn",
                  "Software used to create map data"),

  SOFT_VER("FillMeIn", "Version of software used to create map data"),


  DATEPRD("1701-10-09", "Date this product was produced in UTC"), DATENPRD("1701-10-09",
      "Date this NFT product was produced in UTC"), PRODNAME("FillMeIn",
          "Product filename"), PRODVERS("1.0.0", "Product version number"),

  MAP_VER("999", "Product version number."), CREATOR("ALT-pipeline",
      "Name of software that created this product"), AUTHOR("Espiritu",
          "Name of person that compiled this product"), PROJECTN("Equirectangular",
              "Simple cylindrical projection"), CLON("-999",
                  "[deg] longitude at center of image"), CLAT("-999",
                      "[deg] latitude at center of image"), MINLON(null,
                          "[deg] minimum longitude of global DTM"), MAXLON(null,
                              "[deg] maximum longitude of global DTM"), MINLAT(null,
                                  "[deg] minimum latitude of global DTM"), MAXLAT(null,
                                      "[deg] maximum latitude of global DTM"),

  PXPERDEG("-999", "[pixel per degree] grid spacing of global map."), LLCLAT("-999",
      "[deg]"), LLCLNG("-999", "[deg]"), ULCLAT("-999", "[deg]"), ULCLNG("-999", "[deg]"),

  URCLAT("-999", "[deg]"), URCLNG("-999", "[deg]"), LRCLAT("-999", "[deg]"), LRCLNG("-999",
      "[deg]"),

  CNTR_V_X("-999", "[km]"), CNTR_V_Y("-999", "[km]"), CNTR_V_Z("-999", "[km]"), UX_X("-999",
      "[m]"), UX_Y("-999", "[m]"),

  UX_Z("-999", "[m]"), UY_X("-999", "[m]"), UY_Y("-999", "[m]"), UY_Z("-999", "[m]"), UZ_X("-999",
      "[m]"),

  UZ_Y("-999", "/[m]"), UZ_Z("-999", "[m]"), GSD("-999", "[mm] grid spacing in units/pixel"), GSDI(
      "-999",
      "[unk] Ground sample distance integer"), SIGMA("-999", "Global uncertainty of the data [m]"),

  SIG_DEF("Uncertainty", "SIGMA uncertainty metric"), DQUAL_1("-999",
      "Data quality metric; incidence directions"),

  DQUAL_2("-999", "Data quality metric; emission directions"), DSIG_DEF("UNK",
      "Defines uncertainty metric in ancillary file"), END(null, null),

  // additional fits tags describing gravity derived values
  DENSITY("-999", "[kgm^-3] density of body"), ROT_RATE("-999",
      "[rad/sec] rotation rate of body"), REF_POT("-999", "[J/kg] reference potential of body"),

  // additional fits tags describing tilt derived values
  TILT_RAD("-999", "[m]"), TILT_MAJ("-999",
      "[m] semi-major axis of ellipse for tilt calcs"), TILT_MIN("-999",
          "[m] semi-minor axis of ellipse for tilt calcs"), TILT_PA("-999",
              "[deg] position angle of ellipse for tilt calcs"),

  // Additional fits tags specific to ancillary fits files
  MAP_NAME("FillMeIn", "Map data type"), MAP_TYPE("FillMeIn",
      "Defines whether this is a global or local map"), OBJ_FILE("TEMPLATEENTRY", null),

  // keywords specific to facet mapping ancillary file
  OBJINDX("UNK", "OBJ indexed to OBJ_FILE"), GSDINDX("-999",
      "[unk] Ground sample distance of OBJINDX"), GSDINDXI("-999", "[unk] GSDINDX integer"),

  // return this enum when no match is found
  NOMATCH(null, "could not determine");

  // PIXPDEG(null,"pixels per degree","pixel/deg"),
  // PIX_SZ(null, "mean size of pixels at equator (meters)","m");


  private FitsValCom fitsValCom;

  private HeaderTag(String value, String comment) {
    this.fitsValCom = new FitsValCom(value, comment);
  }

  public String value() {
    return this.fitsValCom.getV();
  }

  public String comment() {
    return this.fitsValCom.getC();
  }

  /**
   * Contains all valid Fits keywords for this Enum. 'NOMATCH' is not a valid Fits keyword
   */
  public static final EnumSet<HeaderTag> fitsKeywords =
      EnumSet.range(HeaderTag.SIMPLE, HeaderTag.GSDINDXI);

  public static final EnumSet<HeaderTag> globalDTMFitsData =
      EnumSet.of(HeaderTag.CLAT, HeaderTag.CLON);


  /**
   * Return the HeaderTag associated with a given string. returns NOMATCH enum if no match found.
   * 
   * @param value
   * @return
   */
  public static HeaderTag tagFromString(String value) {
    for (HeaderTag tagName : values()) {
      if (tagName.toString().equals(value)) {
        return tagName;
      }
    }
    return NOMATCH;
  }

  /**
   * Return the "Source Data Product" (SDP) string to be used in a product file naming convention.
   * The SDP string may not always be the same as the data source.
   * 
   * For example, for ALTWG products, the dataSource could be "OLA" but the SDP string in the
   * filename is supposed to be "alt".
   * 
   * @param dataSource
   * @return
   */
  public static String getSDP(String dataSource) {
    String sdp = dataSource;
    if (dataSource.equals("ola")) {
      sdp = "alt";
    }
    return sdp;
  }
}
