package terrasaur.enums;

import java.util.EnumSet;

/**
 * Contains the enumerations for the different altwg output product types. Used in ALTWG naming
 * conventions and to determine process flow.
 * 
 * @author espirrc1
 *
 */
public enum AltwgDataType {

  /*
   * Each enumeration has the following properties in the order shown: description - general
   * description of the product type fileFrag - string fragment to use in ALTWG file naming
   * convention units - units of the data values headerValue - string identifying the data value
   * comment - comment string (optional) used to fill comment section of fits header anciColName -
   * string to use when defining the column name in the ancillary fits file. If null then
   * getAnciColName() returns the 'description' string
   */

  DSK("SPICE DSK", "dsk", null, "DSK", "SPICE Shape model format", "DSK") {
    @Override
    // no distinction between global and local in ICD.
    public int getGlobalSpocID() {
      return 33;
    }

    @Override
    // no distinction between global and local in ICD.
    public int getLocalSpocID() {
      return 34;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }
  },

  // NFT plane relative DTM with only subset of planes (excludes position data)
  NFTDTM("NFT Plane-relative DTM", "nftdtm", null, "NFTDTM", null, null) {

    @Override
    // no distinction between global and local in ICD.
    public int getGlobalSpocID() {
      return 18;
    }

    @Override
    // no distinction between global and local in ICD.
    public int getLocalSpocID() {
      return 18;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  /*
   * DTM fits. Includes fits files at various intermediate stages such as the initial fits file from
   * Maplet2Fits or CreateMapola, the fits files that contain gravity planes output from
   * DistributedGravity, and the fits files with all planes including gravity and tilts (such as
   * output from Shape2Tilt in ALTWG pipeline).
   */
  DTM("Non-NFT DTM", "dtm", null, "DTM", null, null) {

    @Override
    // value is for SPC data source. For OLA data source add 1.
    public int getGlobalSpocID() {
      return 1;
    }

    @Override
    // value is for SPC data source. For OLA data source add 1.
    public int getLocalSpocID() {
      return 3;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // OBJ shape model as FITS file
  OBJ("OBJ shape model as FITS", "obj", null, "OBJ", null, null) {
    @Override
    // no distinction between OLA or SPC data source.
    public int getGlobalSpocID() {
      return 5;
    }

    @Override
    // no distinction between OLA or SPC data source.
    public int getLocalSpocID() {
      return 23;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // SIGMA. Used in calculation of some of the ALTWG products.
  SIGMA("Sigma", "unk", "km", "Sigma", null, "Sigma") {
    @Override
    public int getGlobalSpocID() {
      return 47;
    }

    @Override
    public int getLocalSpocID() {
      return 48;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  VERTEX_ERROR("Sigma of vertex vector", "vrt", "km", "Sigma", null, "Vertex Radius") {

    @Override
    public int getGlobalSpocID() {
      return 47;
    }

    @Override
    public int getLocalSpocID() {
      return 48;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }


  },

  /*
   * Note: the header values and and comments may already be duplicated in the PlaneInfo
   * enumeration. Changes to the values and comments therefore will have to be done here and in
   * PlaneInfo.
   * ********************************************************************************************
   * CRITICAL: For Ancillary fits files the headerValue MUST match the keyValue in PlaneInfo. This
   * is because the Ancillary Fits code is parsing on the PlaneInfo keyvalue in the Fits header to
   * determine what AltwgProductType the plane corresponds to. Will fix this later to be less sloppy
   * and less prone to user error.
   * ********************************************************************************************
   */
  NORMAL_VECTOR_X("normal vector", "nvf", null, "Normal vector X", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 8;
    }

    @Override
    public int getLocalSpocID() {
      return 24;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.NORM_VECTOR_X;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return AltwgDataType.NORMALX_UNCERTAINTY;
    }

  },

  /*
   * make the fileFrag non-compliant with the ALTWG naming convention and different from the
   * x-component fileFrag. This way we can tell if the code is trying to write this component to a
   * separate file -> IT NEVER SHOULD!
   */
  NORMAL_VECTOR_Y("normal vector", "nv2", null, "Normal vector Y", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 8;
    }

    @Override
    public int getLocalSpocID() {
      return 24;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.NORM_VECTOR_Y;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NORMALY_UNCERTAINTY;
    }

  },

  /*
   * make the fileFrag non-compliant with the ALTWG naming convention and different from the
   * x-component fileFrag. This way we can tell if the code is trying to write this component to a
   * separate file -> IT NEVER SHOULD!
   */
  NORMAL_VECTOR_Z("normal vector", "nv3", null, "Normal vector Z", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 8;
    }

    @Override
    public int getLocalSpocID() {
      return 24;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.NORM_VECTOR_Z;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NORMALZ_UNCERTAINTY;
    }

  },

  GRAVITY_VECTOR_X("gravity vector", "grv", "m/s^2", "Gravity vector X", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 9;
    }

    @Override
    public int getLocalSpocID() {
      return 25;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.GRAV_VECTOR_X;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return GRAVITY_VECTOR_X_UNCERTAINTY;
    }

  },

  /*
   * make the fileFrag non-compliant with the ALTWG naming convention and different from the
   * x-component fileFrag. This way we can tell if the code is trying to write this component to a
   * separate file -> IT NEVER SHOULD!
   */
  GRAVITY_VECTOR_Y("gravity vector", "gr2", "m/s^2", "Gravity vector Y", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 9;
    }

    @Override
    public int getLocalSpocID() {
      return 25;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.GRAV_VECTOR_Y;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return AltwgDataType.GRAVITY_VECTOR_Y_UNCERTAINTY;
    }

  },

  /*
   * make the fileFrag non-compliant with the ALTWG naming convention and different from the
   * x-component fileFrag. This way we can tell if the code is trying to write this component to a
   * separate file -> IT NEVER SHOULD!
   */
  GRAVITY_VECTOR_Z("gravity vector", "gr3", "m/s^2", "Gravity vector Z", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 9;
    }

    @Override
    public int getLocalSpocID() {
      return 25;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.GRAV_VECTOR_Z;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return GRAVITY_VECTOR_Z_UNCERTAINTY;
    }

  },

  GRAVITATIONAL_MAGNITUDE("gravitational magnitude", "grm", "m/s^2", "Gravitational magnitude",
      null, null) {
    @Override
    public int getGlobalSpocID() {
      return 10;
    }

    @Override
    public int getLocalSpocID() {
      return 26;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.GRAV_MAG;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return AltwgDataType.GRAVITATIONAL_MAGNITUDE_UNCERTAINTY;
    }

  },

  GRAVITATIONAL_POTENTIAL("gravitational potential", "pot", "J/kg", "Gravitational potential", null,
      null) {
    @Override
    public int getGlobalSpocID() {
      return 11;
    }

    @Override
    public int getLocalSpocID() {
      return 27;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.GRAV_POT;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return AltwgDataType.GRAVITATIONAL_POTENTIAL_UNCERTAINTY;
    }

  },

  ELEVATION("elevation", "elv", "meters", "Elevation", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 12;
    }

    @Override
    public int getLocalSpocID() {
      return 28;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.ELEV;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return EL_UNCERTAINTY;
    }

  },

  // No longer needed! This is same as HEIGHT plane!
  // ELEV_NORM("elevation relative to normal plane", "elv", "meters", "Elevation relative to normal
  // plane", null, null) {
  // @Override
  // public int getGlobalSpocID() {
  // return -1;
  // }
  //
  // @Override
  // public int getLocalSpocID() {
  // return -1;
  // }
  //
  // public PlaneInfo getPlaneInfo() {
  // return PlaneInfo.ELEV_NORM;
  // }
  //
  // },

  SLOPE("slope", "slp", "degrees", "Slope", null, null) {
    @Override
    public int getGlobalSpocID() {
      return 13;
    }

    @Override
    public int getLocalSpocID() {
      return 29;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.SLOPE;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_UNCERTAINTY;
    }

  },

  FACET_MAP("Facet Map", "ffi", null, "Facet Coarse Index", null, "Facet Coarse Index") {
    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  FACET_TILT("Facet Tilt", "fti", "degrees", "Facet tilt", null, "Facet Tilt") {
    @Override
    public int getGlobalSpocID() {
      return 14;
    }

    @Override
    public int getLocalSpocID() {
      return 30;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_UNCERTAINTY;
    }

  },

  FACET_TILT_DIRECTION("Facet Tilt Direction", "fdi", "degrees", "Facet tilt direction", null,
      "Facet Tilt Direction") {
    @Override
    public int getGlobalSpocID() {
      return 35;
    }

    @Override
    public int getLocalSpocID() {
      return 36;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_DIRECTION;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_UNCERTAINTY;
    }

  },

  TILT_AVG("Mean Tilt", "mti", "degrees", "Mean tilt", null, "Mean Tilt") {
    @Override
    public int getGlobalSpocID() {
      return 15;
    }

    @Override
    public int getLocalSpocID() {
      return 31;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_AVERAGE;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_AVG_UNCERTAINTY;
    }


  },

  TILT_VARIATION("Tilt Variation", "tiv", "degrees", "Tilt variation", null, "Tilt Variation") {
    @Override
    public int getGlobalSpocID() {
      return 16;
    }

    @Override
    public int getLocalSpocID() {
      return 32;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_VARIATION;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_RELATIVE_UNCERTAINTY;
    }

  },

  TILT_AVG_DIRECTION("Mean Tilt Direction", "mdi", "degrees", "Mean tilt direction", null,
      "Mean Tilt Direction") {
    @Override
    public int getGlobalSpocID() {
      return 37;
    }

    @Override
    public int getLocalSpocID() {
      return 38;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_AVERAGE_DIRECTION;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_AVG_DIRECTION_UNCERTAINTY;
    }

  },

  TILT_DIRECTION_VARIATION("Tilt Direction Variation", "div", "degrees", "Tilt direction variation",
      null, "Tilt Direction Variation") {
    @Override
    public int getGlobalSpocID() {
      return 39;
    }

    @Override
    public int getLocalSpocID() {
      return 40;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_DIRECTION_VARIATION;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_RELATIVE_DIRECTION_UNCERTAINTY;
    }

  },

  TILT_RELATIVE("Relative Tilt", "rti", "degrees", "Relative tilt", null, "Relative Tilt") {
    @Override
    public int getGlobalSpocID() {
      return 41;
    }

    @Override
    public int getLocalSpocID() {
      return 42;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_RELATIVE;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_RELATIVE_UNCERTAINTY;
    }

  },

  TILT_RELATIVE_DIRECTION("Relative Tilt Direction", "rdi", "degrees", "Relative tilt direction",
      null, "Relative Tilt Direction") {
    @Override
    public int getGlobalSpocID() {
      return 43;
    }

    @Override
    public int getLocalSpocID() {
      return 44;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.TILT_RELATIVE_DIRECTION;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return TILT_RELATIVE_DIRECTION_UNCERTAINTY;
    }

  },

  /*
   * Each enumeration has the following properties in the order shown: description - general
   * description of the product type fileFrag - string fragment to use in ALTWG file naming
   * convention units - units of the data values headerValue - string identifying the data value
   * comment - comment string (optional) used to fill comment section of fits header anciColName -
   * string to use when defining the column name in the ancillary fits file. If null then
   * getAnciColName() returns the 'description' string
   */

  RELATIVE_HEIGHT("Max height/depth within an ellipse", "mht", "km", "Max relative height", null,
      "Max Relative Height") {
    @Override
    public int getGlobalSpocID() {
      return 49;
    }

    @Override
    public int getLocalSpocID() {
      return 50;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.RELATIVE_HEIGHT;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return RELATIVE_HEIGHT_UNCERTAINTY;
    }

  },

  FACETRAD("Facet Radius", "rad", "km", "Facet radius", null, "Facet_Center_Radius") {
    @Override
    public int getGlobalSpocID() {
      return 45;
    }

    @Override
    public int getLocalSpocID() {
      return 46;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.FACETRAD;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return FACETRAD_UNCERTAINTY;
    }

  },

  /*
   * ancillary template, scalar order of attributes is: AltwgDataType(String description, String
   * fileFrag, String units, String headerValue, String comment, String anciColName)
   */

  TPLATEANCI("Ancillary Template Scalar", "tes", "TBDCOLUNITS", "AncillaryTemplate", null,
      "TBDCOLNAME") {
    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // ancillary template, scalar
  TPLATEANCIVEC("Ancillary Template Vector", "tev", "TBD", "AncillaryTemplate", null,
      "TBDCOLNAME") {
    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  AREA("Facet Area", "are", "km^2", "Facet Area", null, "Facet Area") {

    @Override
    public int getGlobalSpocID() {
      return 52;
    }

    @Override
    public int getLocalSpocID() {
      return 53;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.AREA;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return AREA_UNCERTAINTY;
    }

  },

  /*
   * Each enumeration has the following properties in the order shown: description - general
   * description of the product type fileFrag - string fragment to use in ALTWG file naming
   * convention units - units of the data values headerValue - string identifying the data value -
   * should always be filled in! comment - comment string (optional) used to fill comment section of
   * fits header anciColName - string to use when defining the column name in the ancillary fits
   * file. If null then getAnciColName() returns the 'description' string
   */
  AREA_UNCERTAINTY("FacetArea Uncertainty", "notapplicable", "km^2", "facet area uncertainty", null,
      "facet area uncertainty") {

    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  EL_UNCERTAINTY("Elevation Uncertainty", "notapplicable", "m", "El uncertainty", null,
      "elevation uncertainty") {

    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  /*
   * Each enumeration has the following properties in the order shown: description - general
   * description of the product type fileFrag - string fragment to use in ALTWG file naming
   * convention units - units of the data values headerValue - string identifying the data value -
   * should always be filled in! comment - comment string (optional) used to fill comment section of
   * fits header anciColName - string to use when defining the column name in the ancillary fits
   * file. If null then getAnciColName() returns the 'description' string
   */

  // Uncertainty in gravity vector, x component
  GRAVITY_VECTOR_X_UNCERTAINTY("Grav_X Uncertainty", "notapplicable", "m/s^2", "GravX uncertainty",
      null, "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in gravity vector, y component
  GRAVITY_VECTOR_Y_UNCERTAINTY("Grav_Y Uncertainty", "notapplicable", "m/s^2", "GravY uncertainty",
      null, "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in gravity vector, z component
  GRAVITY_VECTOR_Z_UNCERTAINTY("Grav_Z Uncertainty", "notapplicable", "m/s^2", "GravZ uncertainty",
      null, "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in gravitational magnitude
  GRAVITATIONAL_MAGNITUDE_UNCERTAINTY("Grav Mag Uncertainty", "notapplicable", "m/s^2",
      "GravMag uncertainty", null, "grav magnitude uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in gravitational potential
  GRAVITATIONAL_POTENTIAL_UNCERTAINTY("Grav Pot Uncertainty", "notapplicable", "m/s^2",
      "GravPot Uncertainty", null, "grav potential uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  // Uncertainty in Normal Vector, X component
  NORMALX_UNCERTAINTY("Normal X Uncertainty", "notapplicable", null, "Normal X Uncertainty", null,
      "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in Normal Vector, Y component
  NORMALY_UNCERTAINTY("Normal Y Uncertainty", "notapplicable", null, "Normal Y Uncertainty", null,
      "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // Uncertainty in Normal Vector, Z component
  NORMALZ_UNCERTAINTY("Normal Z Uncertainty", "notapplicable", null, "Normal Z Uncertainty", null,
      "vector component uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // tilt uncertainty
  /*
   * description - general description of the product type fileFrag - string fragment to use in
   * ALTWG file naming convention units - units of the data values headerValue - string identifying
   * the data value comment - comment string (optional) used to fill comment section of fits header
   * anciColName - string to use when defining the column name in the ancillary fits file. If null
   * then getAnciColName() returns the 'description' string
   */
  TILT_UNCERTAINTY("Tilt Uncertainty", "notapplicable", "degrees", "Tilt Uncertainty", null,
      "tilt uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  /*
   * tilt variation standard error. Used to populate the SIGMA column for the tilt variation
   * ancillary product.
   */
  TILTVAR_UNCERTAINTY("Tilt Variation Uncertainty", "notapplicable", "degrees",
      "Tilt Variation Uncertainty", null, "variation uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  /*
   * tilt variation standard error. Used to populate the SIGMA column for the tilt variation
   * ancillary product.
   */
  TILTDIRVAR_UNCERTAINTY("Tilt Direction Variation Uncertainty", "notapplicable", "degrees",
      "Tilt Direction Variation Uncertainty", null, "direction variation uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  /*
   * albedo from SPC description - general description of the product type fileFrag - string
   * fragment to use in ALTWG file naming convention units - units of the data values headerValue -
   * string identifying the data value comment - comment string (optional) used to fill comment
   * section of fits header anciColName - string to use when defining the column name in the
   * ancillary fits file. If null then getAnciColName() returns the 'description' string
   */
  ALBEDO("Relative albedo", "alb", "unitless", "Relative albedo", null, "relative albedo") {

    @Override
    public int getGlobalSpocID() {
      return 55;
    }

    @Override
    public int getLocalSpocID() {
      return 54;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.ALBEDO;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return ALBEDO_UNCERTAINTY;
    }

  },

  ALBEDO_UNCERTAINTY("Albedo Uncertainty", "notapplicable", "unitless", "Albedo uncertainty", null,
      "albedo uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // OLA intensity. Used in place of albedo
  INTENSITY("Intensity", "alb", "unitless", "Intensity", null, "intensity") {

    @Override
    public int getGlobalSpocID() {
      return 55;
    }

    @Override
    public int getLocalSpocID() {
      return 54;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return PlaneInfo.INTENSITY;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return INTENSITY_UNCERTAINTY;
    }

  },


  // OLA intensity. Used in place of albedo
  INTENSITY_UNCERTAINTY("Intensity", "alb", "unitless", "Intensity", null, "intensity") {

    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  /*
   * Each enumeration has the following properties in the order shown: description - general
   * description of the product type fileFrag - string fragment to use in ALTWG file naming
   * convention units - units of the data values headerValue - string identifying the data value
   * comment - comment string (optional) used to fill comment section of fits header anciColName -
   * string to use when defining the column name in the ancillary fits file. If null then
   * getAnciColName() returns the 'description' string
   */

  RELATIVE_HEIGHT_UNCERTAINTY("Max relative height uncertainty", "notapplicable", "km",
      "Max relative height uncertainty", null, "max relative height uncertainty") {
    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // facet radius uncertainty
  FACETRAD_UNCERTAINTY("Facet Radius Uncertainty", "notapplicable", null,
      "Facet Radius Uncertainty", null, "facet radius uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  // uncertainty in mean tilt (TILT_AVG)
  TILT_AVG_UNCERTAINTY("Mean Tilt Uncertainty", "notapplicable", null, "Mean Tilt Uncertainty",
      null, "mean tilt uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  // uncertainty in mean tilt direction (TILT_AVG_DIRECTION)
  TILT_AVG_DIRECTION_UNCERTAINTY("Mean Tilt Direction Uncertainty", "notapplicable", null,
      "Mean Tilt Direction Uncertainty", null, "mean tilt direction uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  // uncertainty in relative tilt (TILT_RELATIVE)
  TILT_RELATIVE_UNCERTAINTY("Relative Tilt Uncertainty", "notapplicable", null,
      "Relative Tilt Uncertainty", null, "relative tilt uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },


  // uncertainty in relative tilt direction (TILT_RELATIVE_DIRECTION)
  TILT_RELATIVE_DIRECTION_UNCERTAINTY("Relative Tilt Direction Uncertainty", "notapplicable", null,
      "Relative Tilt Direction Uncertainty", null, "relative tilt direction uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  // uncertainty in relative tilt direction (TILT_RELATIVE_DIRECTION)
  TILT_DIRECTION_UNCERTAINTY("Tilt Direction Uncertainty", "notapplicable", null,
      "Tilt Direction Uncertainty", null, "tilt direction uncertainty") {

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    // not a SPOC product, just the SIGMA column in the ancillary fits file
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  },

  /*
   * not applicable. Usually indicates that one does not need to follow ALTWG naming convention or
   * that a valid AltwgDataType could not be returned. DO NOT USE AS A SUBSTITUTE FOR TBD!
   */
  NA("not applicable", "unkproducttype", null, "Not applicable", null, null) {
    @Override
    public int getGlobalSpocID() {
      return -1;
    }

    @Override
    public int getLocalSpocID() {
      return -1;
    }

    // no such plane in DTM
    @Override
    public PlaneInfo getPlaneInfo() {
      return null;
    }

    @Override
    public AltwgDataType getSigmaType() {
      return NA;
    }

  };


  private String description; // description of enumeration
  private String fileFrag;
  private String units;
  private String headerValue; // name to use as value in Fits Header
  private String comment; // comment to add to fits header (optional)
  private String anciColName;

  AltwgDataType(String description, String fileFrag, String units, String headerValue,
      String comment, String anciColName) {
    this.description = description;
    this.fileFrag = fileFrag;
    this.units = units;
    this.headerValue = headerValue;
    this.comment = comment;
    this.anciColName = anciColName;
  }

  public String getAnciColName() {
    if (this.anciColName == null) {
      return getDesc();
    } else {
      return this.anciColName;
    }
  }

  public String getDesc() {
    return description;
  }

  public String getFileFrag() {
    return fileFrag;
  }

  public String getUnits() {
    return units;
  }

  public String getHeaderValue() {
    return headerValue;
  }

  public String getHeaderValueWithUnits() {
    if (units == null)
      return headerValue;
    else
      return headerValue + " (" + units + ")";
  }

  public String desc() {
    return description;
  }

  /**
   * Return file fragment w/ "_" delimiters, the way they would look in the altwg filename
   * 
   * @return
   */
  public static String getFragDelim(AltwgDataType prodType) {
    String delim = "_";
    String frag = delim + prodType.getFileFrag() + delim;
    return frag;
  }

  /*
   * Enumeration set that contains all the gravity ancillary fits product types.
   * 
   * For vectors we only store the enum for the X component in this set since all components (x,y,z)
   * will be written together into one data file, so we don't need to do it again for the Y or Z
   * AltwgProductTypes.
   * 
   */
  public static final EnumSet<AltwgDataType> anciGravitySet =
      EnumSet.of(AltwgDataType.GRAVITY_VECTOR_X, AltwgDataType.GRAVITATIONAL_MAGNITUDE,
          AltwgDataType.GRAVITATIONAL_POTENTIAL, AltwgDataType.ELEVATION);

  // slope is no longer in anciGravitySet, because it depends on errors that are determined
  // in Shape2Tilt. See MultiTableToAncillaryFits.java
  // AltwgDataType.SLOPE);

  /*
   * Enumeration set that contains all the ancillary fits types. These are product types only
   * associated w/ ancillary fits files.
   * 
   */
  public static final EnumSet<AltwgDataType> ancillaryTypes =
      EnumSet.range(AltwgDataType.VERTEX_ERROR, AltwgDataType.FACETRAD);

  public static final EnumSet<AltwgDataType> simpleAnciProducts =
      EnumSet.range(AltwgDataType.NORMAL_VECTOR_X, AltwgDataType.FACETRAD);

  /*
   * Enumeration set that contains only the planes which are written out by the
   * DistributedGravity.saveResultsAtCenters() option. This used to include tilt values, but we are
   * migrating to having DistributedGravity ONLY print out gravity derived planes. Another class
   * will take care of adding tilt planes afterwards. This enumset contains all components of the
   * gravity vectors because each component is written out to a separate plane.
   */
  // public static final EnumSet<AltwgDataType> gravityPlanes =
  // EnumSet.range(AltwgDataType.NORMAL_VECTOR_X,
  // AltwgDataType.SLOPE);

  public static final EnumSet<AltwgDataType> gravityPlanes =
      EnumSet.of(AltwgDataType.NORMAL_VECTOR_X, AltwgDataType.NORMAL_VECTOR_Y,
          AltwgDataType.NORMAL_VECTOR_Z, AltwgDataType.GRAVITY_VECTOR_X,
          AltwgDataType.GRAVITY_VECTOR_Y, AltwgDataType.GRAVITY_VECTOR_Z,
          AltwgDataType.GRAVITATIONAL_MAGNITUDE, AltwgDataType.GRAVITATIONAL_POTENTIAL,
          AltwgDataType.ELEVATION, AltwgDataType.SLOPE, AltwgDataType.AREA);

  public static final EnumSet<AltwgDataType> tiltPlanes =
      EnumSet.range(AltwgDataType.FACET_TILT, AltwgDataType.RELATIVE_HEIGHT);


  /**
   * Unique identifier for ALTWG global product in OSIRIS-REx SPOC system.
   * 
   * @return
   */
  public abstract int getGlobalSpocID();

  /**
   * Unique identifier for ALTWG local product in OSIRIS-REx SPOC system.
   * 
   * @return
   */
  public abstract int getLocalSpocID();

  /**
   * Retrieve PlaneInfo associated with this enum. PlaneInfo describes the plane in the DTM that
   * contains data pertaining to this enum. If null then there is no associated plane in the DTM.
   * 
   * @return
   */
  public abstract PlaneInfo getPlaneInfo();

  /**
   * Get the AltwgDataType that is associated with the sigma values for this type. Only applicable
   * if AltwgDataType refers to a set of data values in an ancillary fits file. For example, there
   * is no associated sigma for a DTM fits file or an OBJ shape model in this context.
   * 
   * @return
   */
  public abstract AltwgDataType getSigmaType();

  /**
   * Return a string describing the sigma associated with the data column in an ancillary fits
   * table. Return "NA" if not applicable.
   * 
   * @return
   */
  // public abstract String getDataSigmaDef();
}
