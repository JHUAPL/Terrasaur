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
package terrasaur.fits;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.io.FileUtils;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.util.Cursor;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;

/*
 * Container class that holds all possible fits keywords, values and comments for various fits
 * products. Variable names correspond to the fits keywords, variable contents correspond to the
 * keyword value and associated comment(optional). Not all keywords are contained in all the FITS
 * products. It is up to the code generating a specific FITS product to parse the appropriate values
 * from this class to populate that product's fits header.
 */
public class FitsHdr {

  public final Map<String, HeaderCard> fitsKV;

  private FitsHdr(FitsHdrBuilder b) {
    fitsKV = b.getMap();
  }

  /**
   * Return the map as a list.
   * 
   * @return
   */
  public List<HeaderCard> mapToList() {
    List<HeaderCard> headerList = new ArrayList<HeaderCard>();

    for (String thisKey : fitsKV.keySet()) {
      headerList.add(fitsKV.get(thisKey));
    }

    return headerList;
  }

  public static class FitsHdrBuilder {
    private Map<String, HeaderCard> fitsKV = new LinkedHashMap<String, HeaderCard>();

    // change this to allow code to print out warnings
    private boolean printWarnings = false;

    /**
     * basic no-arg constructor. Note that this constructor does NOT populate the fitsKV map! If you
     * want an initial builder with initialized map then call initHdrBuilder().
     */
    public FitsHdrBuilder() {

    }

    /**
     * Return all HeaderCards in their current state
     * 
     * @return
     */
    public List<HeaderCard> getHeaderCards() {
      List<HeaderCard> headers = new ArrayList<HeaderCard>();
      for (String keyword : fitsKV.keySet()) {
        headers.add(fitsKV.get(keyword));
      }
      return headers;
    }

    /**
     * Put HeaderCard into map with keyword String. Updates the Headercard if record already exists
     * in the map. HeaderCard.
     * 
     * @param keyword
     * @param hdrCard
     * @return
     */
    // public FitsHdrBuilder putCard(String keyword, HeaderCard hdrCard) {
    // this.fitsKV.put(keyword, hdrCard);
    // return this;
    // }

    // public FitsHdrBuilder putCard(HeaderTag hdrTag, HeaderCard hdrCard) {
    // putCard(hdrTag.toString(), hdrCard);
    // return this;
    // }

    /**
     * Set the HeaderCards for HeaderTags associated with the corners of the data. Set value = "N/A"
     * if input data is null. Will use the same longitude range for corner points as in the data.
     * I.e. if the data uses -180 to 180 then will use -180 to 180 range for longitude corner
     * points. If data uses 0 - 360 then will use 0-360 for longitude corner points.
     * 
     * @param data
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setCornerCards(double[][][] data) throws HeaderCardException {
      if (data != null) {

        System.out.println("Data not null");
        int height = data[0].length;
        int width = data[0][0].length;

        double llclat = data[0][0][0];
        double llclng = data[1][0][0];
        double urclat = data[0][height - 1][width - 1];
        double urclng = data[1][height - 1][width - 1];
        double lrclat = data[0][0][width - 1];
        double lrclng = data[1][0][width - 1];
        double ulclat = data[0][height - 1][0];
        double ulclng = data[1][height - 1][0];
        setVCbyHeaderTag(HeaderTag.LLCLNG, llclng, HeaderTag.LLCLNG.comment());
        setVCbyHeaderTag(HeaderTag.LLCLAT, llclat, HeaderTag.LLCLAT.comment());
        setVCbyHeaderTag(HeaderTag.URCLNG, urclng, HeaderTag.URCLNG.comment());
        setVCbyHeaderTag(HeaderTag.URCLAT, urclat, HeaderTag.URCLAT.comment());
        setVCbyHeaderTag(HeaderTag.LRCLNG, lrclng, HeaderTag.LRCLNG.comment());
        setVCbyHeaderTag(HeaderTag.LRCLAT, lrclat, HeaderTag.LRCLAT.comment());
        setVCbyHeaderTag(HeaderTag.ULCLNG, ulclng, HeaderTag.ULCLNG.comment());
        setVCbyHeaderTag(HeaderTag.ULCLAT, ulclat, HeaderTag.ULCLAT.comment());

      } else {

        System.out.println("Data is null");

        setVCbyHeaderTag(HeaderTag.LLCLNG, "N/A", HeaderTag.LLCLNG.comment());
        setVCbyHeaderTag(HeaderTag.LLCLAT, "N/A", HeaderTag.LLCLAT.comment());
        setVCbyHeaderTag(HeaderTag.URCLNG, "N/A", HeaderTag.URCLNG.comment());
        setVCbyHeaderTag(HeaderTag.URCLAT, "N/A", HeaderTag.URCLAT.comment());
        setVCbyHeaderTag(HeaderTag.LRCLNG, "N/A", HeaderTag.LRCLNG.comment());
        setVCbyHeaderTag(HeaderTag.LRCLAT, "N/A", HeaderTag.LRCLAT.comment());
        setVCbyHeaderTag(HeaderTag.ULCLNG, "N/A", HeaderTag.ULCLNG.comment());
        setVCbyHeaderTag(HeaderTag.ULCLAT, "N/A", HeaderTag.ULCLAT.comment());

      }

      return this;
    }

    /**
     * Set the HeaderCards for HeaderTags associated with the center latitude, longitude
     * 
     * @param llr
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setCenterLatLon(LatitudinalVector llr) throws HeaderCardException {
      if (llr != null) {
        setVCbyHeaderTag(HeaderTag.CLON, Math.toDegrees(llr.getLongitude()),
            HeaderTag.CLON.comment());
        setVCbyHeaderTag(HeaderTag.CLAT, Math.toDegrees(llr.getLatitude()),
            HeaderTag.CLAT.comment());
      }
      return this;
    }

    /**
     * Set the HeaderCards for HeaderTags associated with center latitude, longitude where center
     * lat,lon is defined by the vector 'V'. If 'V' is null then uses default global lat,lon if
     * 'isGlobal', otherwise returns with lat,lon headercards set to "N/A";
     * 
     * @param V
     * @param isGlobal
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setCenterLatLon(double[] V, boolean isGlobal) throws HeaderCardException {
      if (V != null) {
        LatitudinalVector llr = CoordConverters.convertToLatitudinal(new UnwritableVectorIJK(V));
        setCenterLatLon(llr);

      } else {

        if (isGlobal) {
          LatitudinalVector llr = new LatitudinalVector(1., 0., 0.);
          setCenterLatLon(llr);
        } else {

          // unknown. Don't know why we wouldn't know the center lat, lon
          setVCbyHeaderTag(HeaderTag.CLON, "N/A", HeaderTag.CLON.comment());
          setVCbyHeaderTag(HeaderTag.CLAT, "N/A", HeaderTag.CLAT.comment());
        }
      }
      return this;
    }

    /**
     * Hardcode center lat, lon to be 0, 0. Do this only if you know this is a global fits file.
     * 
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setGlobalCenter() throws HeaderCardException {
      LatitudinalVector llr = new LatitudinalVector(1., 0., 0.);
      setCenterLatLon(llr);
      return this;
    }

    /**
     * Set HeaderCards for HeaderTags associated with the vector to center of image. Throws
     * RuntimeException if V is null.
     * 
     * @param V
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setCenterVec(double[] V) throws HeaderCardException {
      if (V != null) {
        setVCbyHeaderTag(HeaderTag.CNTR_V_X, V[0], HeaderTag.CNTR_V_X.comment());
        setVCbyHeaderTag(HeaderTag.CNTR_V_Y, V[1], HeaderTag.CNTR_V_Y.comment());
        setVCbyHeaderTag(HeaderTag.CNTR_V_Z, V[2], HeaderTag.CNTR_V_Z.comment());
      } else {

        String errMesg = "ERROR! Center Vector is null! It could be that center"
            + " vector information is already contained in FitsHdrBuilder!";
        throw new RuntimeException(errMesg);

      }
      return this;
    }

    /**
     * Use the current time when this method is called to set the date when product was produced
     * 
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setDateprod() throws HeaderCardException {

      // DateFormat dateFormat = new
      // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssssZ");
      // date-time terminates in 'Z'
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      Date date = new Date();
      setVbyHeaderTag(HeaderTag.DATEPRD, dateFormat.format(date));

      return this;
    }

    /**
     * Set HeaderCards associated with unit vector Ux defining the reference frame
     * 
     * @param unitV
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setUx(double[] unitV) throws HeaderCardException {

      if (unitV != null) {
        setVCbyHeaderTag(HeaderTag.UX_X, unitV[0], null);
        setVCbyHeaderTag(HeaderTag.UX_Y, unitV[1], null);
        setVCbyHeaderTag(HeaderTag.UX_Z, unitV[2], null);

      } else {
        String errMesg = "ERROR! UX Vector is null! It could be that UX"
            + " vector information is already contained in FitsHdrBuilder!";
        throw new RuntimeException(errMesg);

      }

      return this;
    }

    /**
     * Set HeaderCards associated with unit vector Uy defining the reference frame
     * 
     * @param unitV
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setUy(double[] unitV) throws HeaderCardException {

      if (unitV != null) {

        setVCbyHeaderTag(HeaderTag.UY_X, unitV[0], null);
        setVCbyHeaderTag(HeaderTag.UY_Y, unitV[1], null);
        setVCbyHeaderTag(HeaderTag.UY_Z, unitV[2], null);
      } else {
        String errMesg = "ERROR! UY Vector is null! It could be that UY"
            + " vector information is already contained in FitsHdrBuilder!";
        throw new RuntimeException(errMesg);

      }

      return this;

    }

    /**
     * Set HeaderCards associated with unit vector Uz defining the reference frame
     * 
     * @param unitV
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setUz(double[] unitV) throws HeaderCardException {

      if (unitV != null) {
        setVCbyHeaderTag(HeaderTag.UZ_X, unitV[0], null);
        setVCbyHeaderTag(HeaderTag.UZ_Y, unitV[1], null);
        setVCbyHeaderTag(HeaderTag.UZ_Z, unitV[2], null);

      } else {
        String errMesg = "ERROR! UZ Vector is null! It could be that UZ"
            + " vector information is already contained in FitsHdrBuilder!";
        throw new RuntimeException(errMesg);

      }

      return this;

    }

    /**
     * Set the GSD and GSDI values based on input ground sample distance.
     * 
     * @param gsd
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setGSD(double gsd) throws HeaderCardException {

      // determine if GSD is close to an integer value
      long gsdRound = Math.round(gsd);
      double gsdRounded = gsdRound;
      System.out.println("gsd calculated to be:" + String.valueOf(gsdRounded));

      if (Math.abs(gsd - gsdRounded) < 1.0e-5) {
        gsd = gsdRounded;
      }

      setVCbyHeaderTag(HeaderTag.GSD, gsd, HeaderTag.GSD.comment());

      // use default units for GSDI. Round to nearest integer
      String gsdiUnits = HeaderTag.GSDI.comment();
      gsdiUnits = gsdiUnits.replace("[unk]", "[mm]");
      setVCbyHeaderTag(HeaderTag.GSDI, gsdRounded, gsdiUnits);

      return this;
    }

    /**
     * Change the string value for a HeaderCard in the map. If it doesn't already exist then create
     * a new HeaderCard.
     * 
     * @param hdrTag
     * @param value
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setVbyHeaderTag(HeaderTag hdrTag, String value)
        throws HeaderCardException {
      if (fitsKV.containsKey(hdrTag.toString())) {

        fitsKV.get(hdrTag.toString()).setValue(value);

      } else {

        if (printWarnings) {
          System.out.println("WARNING! header:" + hdrTag.toString() + " is new. Will add it to "
              + "fits header builder.");
        }

        HeaderCard newCard = new HeaderCard(hdrTag.toString(), value, hdrTag.comment());
        fitsKV.put(hdrTag.toString(), newCard);
      }
      return this;
    }

    /**
     * Change the value for a HeaderCard in the map
     * 
     * @param hdrTag
     * @param value
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setVbyHeaderTag(HeaderTag hdrTag, double value)
        throws HeaderCardException {

      String hdrKey = hdrTag.toString();
      if (fitsKV.containsKey(hdrKey)) {

        fitsKV.get(hdrKey).setValue(value);

      } else {
        if (printWarnings) {
          System.out.println(
              "WARNING! header:" + hdrKey + " is new. Will add it to " + "fits header builder.");
        }
        HeaderCard newCard = new HeaderCard(hdrKey, value, hdrTag.comment());
        fitsKV.put(hdrTag.toString(), newCard);
      }

      return this;
    }

    /**
     * Change the string value and comment for a HeaderCard in the map. HeaderCard stores value as a
     * string and adds quotes around it in the fits header to indicate that it is a 'string' value
     * 
     * @param hdrTag
     * @param value
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setVCbyHeaderTag(HeaderTag hdrTag, String value, String comment)
        throws HeaderCardException {

      String hdrKey = hdrTag.toString();
      if (fitsKV.containsKey(hdrKey)) {
        fitsKV.get(hdrKey).setValue(value);
        fitsKV.get(hdrKey).setComment(comment);

      } else {
        if (printWarnings) {
          System.out.println(
              "WARNING! header:" + hdrTag + " is new. Will add it to " + "fits header builder.");
        }
        HeaderCard newCard = new HeaderCard(hdrKey, value, comment);
        fitsKV.put(hdrKey, newCard);

      }
      return this;

    }

    /**
     * Change the comment for a given HeaderCard in the map. Leaves the value unchanged
     * 
     * @param hdrTag
     * @param comment
     * @return
     */
    public FitsHdrBuilder setCbyHeaderTag(HeaderTag hdrTag, String comment) {
      String hdrkey = hdrTag.toString();
      if (fitsKV.containsKey(hdrkey)) {

        fitsKV.get(hdrkey).setComment(comment);

      } else {
        String errMesg = "ERROR! HeaderTag:" + hdrTag + " does not exist in FitsHdrBuilder!";
        throw new RuntimeException(errMesg);
      }
      return this;

    }

    /**
     * Change the double value and comment for a HeaderCard in the map. HeaderCard stores value as a
     * string but does NOT add quotes around it in the fits header.
     * 
     * @param hdrTag
     * @param value
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setVCbyHeaderTag(HeaderTag hdrTag, double value, String comment)
        throws HeaderCardException {

      String hdrKey = hdrTag.toString();
      if (fitsKV.containsKey(hdrKey)) {

        fitsKV.get(hdrKey).setValue(value);
        fitsKV.get(hdrKey).setComment(comment);

      } else {
        if (printWarnings) {
          System.out.println(
              "WARNING! header:" + hdrKey + " is new. Will add it to " + "fits header builder.");
        }
        HeaderCard newCard = new HeaderCard(hdrKey, value, comment);
        fitsKV.put(hdrKey, newCard);
      }
      return this;

    }

    /**
     * Update existing HeaderCard with input headercard. If it doesn't exist then add the headercard
     * to the map.
     * 
     * @param headerCard
     * @return
     */
    public FitsHdrBuilder setbyHeaderCard(HeaderCard headerCard) {
      String hdrKey = headerCard.getKey();

      if (!fitsKV.containsKey(hdrKey)) {
        if (printWarnings) {
          System.out.println(
              "WARNING! header:" + hdrKey + " is new. Will add it to " + "fits header builder.");
        }

      }

      fitsKV.put(hdrKey, headerCard);
      return this;
    }

    /**
     * Convenience method for updating all possible keywords using information found in FitsData.
     * 
     * @param fitsData
     * @return
     * @throws HeaderCardException
     */
    public FitsHdrBuilder setByFitsData(FitsData fitsData) throws HeaderCardException {

      HeaderCard hdrCard;

      // set map type, global or local
      String hdrKey = HeaderTag.MAP_TYPE.toString();
      String mapType = "global";
      if (!fitsData.isGlobal()) {
        mapType = "local";
      }
      if (fitsKV.containsKey(hdrKey)) {
        fitsKV.get(hdrKey).setValue(mapType);
      } else {

        // fitsKV is no longer initially populated, so just add this.
        hdrCard = new HeaderCard(hdrKey, mapType, HeaderTag.MAP_TYPE.comment());
        fitsKV.put(hdrKey, hdrCard);
      }

      this.setCornerCards(fitsData.getData());

      /*
       * extract map information from fitsData if present: vector to center: V unit vectors
       * describing reference frame: UX, UY, UZ, gsd
       */
      if (fitsData.hasV()) {
        this.setCenterVec(fitsData.getV()).setCenterLatLon(fitsData.getV(), fitsData.isGlobal());
      } else {

        // no center vector in fitsData. Check if this is a global product.
        if (fitsData.isGlobal()) {

          // set center lat, lon to be 0deg, 0deg
          this.setGlobalCenter();
        }

        // for the case where there is no center vector in fitsData
        // and it is NOT a global product, assume the info is already captured
        // in fitsKV.
      }

      if (fitsData.hasUnitv()) {
        // assume all unit vectors are present
        this.setUx(fitsData.getUnit(UnitDir.UX)).setUy(fitsData.getUnit(UnitDir.UY))
            .setUz(fitsData.getUnit(UnitDir.UZ));

      }

      if (fitsData.hasGsd()) {
        this.setGSD(fitsData.getGSD());
      }

      return this;
    }

    private Map<String, HeaderCard> getMap() {
      return fitsKV;
    }

    /**
     * Check whether map contains the String keyword.
     * 
     * @return
     */
    public boolean containsKey(String keyWord) {

      return fitsKV.containsKey(keyWord);
    }

    public boolean containsKey(HeaderTag keyWord) {
      return fitsKV.containsKey(keyWord.toString());
    }

    /**
     * Search map and return the HeaderCard associated with the String keyword. Returns null if key
     * not contained in map.
     * 
     * @param keyWord
     * @return
     */
    public HeaderCard getCard(String keyWord) {

      return fitsKV.get(keyWord);
    }

    public HeaderCard getCard(HeaderTag keyWord) {
      return fitsKV.get(keyWord.toString());
    }

    /**
     * Add default values for the header tag to fitsKV map. The method checks to see if the
     * headertag exists in the map. If it does exist then it will not modify the headercard!
     * 
     * @param headerTag
     * @throws HeaderCardException
     */
    public void addCard(HeaderTag headerTag) throws HeaderCardException {

      if (this.containsKey(headerTag)) {
        System.out.println("WARNING: keyword:" + headerTag.toString()
            + " already exists in map. Will not" + " add a default headercard!");
      } else {

        HeaderCard newCard =
            new HeaderCard(headerTag.toString(), headerTag.value(), headerTag.comment());
        fitsKV.put(headerTag.toString(), newCard);
      }

    }

    public FitsHdr build() {
      return new FitsHdr(this);
    }

    /**
     * Check to make sure global corners and center lat,lon have valid values. If not then reset to
     * defaults
     * 
     * @param thisBuilder
     * @return
     * @throws HeaderCardException
     */
    public static FitsHdrBuilder checkGlobalLatLon(FitsHdrBuilder thisBuilder)
        throws HeaderCardException {

      // check if this keyword exists first. If not, then add all the corner keywords!
      if (!thisBuilder.containsKey(HeaderTag.LLCLNG)) {
        thisBuilder.addCard(HeaderTag.LLCLNG);
        thisBuilder.addCard(HeaderTag.LLCLAT);
        thisBuilder.addCard(HeaderTag.ULCLAT);
        thisBuilder.addCard(HeaderTag.ULCLNG);
        thisBuilder.addCard(HeaderTag.URCLAT);
        thisBuilder.addCard(HeaderTag.URCLNG);
        thisBuilder.addCard(HeaderTag.LRCLAT);
        thisBuilder.addCard(HeaderTag.LRCLNG);
      }

      if (!validLatLon(thisBuilder, HeaderTag.LLCLNG)) {
        System.out.println("WARNING! check of global corners shows they have not been set!");
        System.out.println("Setting to default global values!");
        thisBuilder.setVbyHeaderTag(HeaderTag.LLCLNG, -180D);
        thisBuilder.setVbyHeaderTag(HeaderTag.LLCLAT, -90D);
        thisBuilder.setVbyHeaderTag(HeaderTag.ULCLNG, -180D);
        thisBuilder.setVbyHeaderTag(HeaderTag.ULCLAT, 90D);
        thisBuilder.setVbyHeaderTag(HeaderTag.URCLNG, 180D);
        thisBuilder.setVbyHeaderTag(HeaderTag.URCLAT, 90D);
        thisBuilder.setVbyHeaderTag(HeaderTag.LRCLNG, 180D);
        thisBuilder.setVbyHeaderTag(HeaderTag.LRCLAT, -90D);

      }

      // check if this keyword exists first. If not then add the center lat,lon keywords!
      if (!thisBuilder.containsKey(HeaderTag.CLAT)) {
        thisBuilder.addCard(HeaderTag.CLAT);
        thisBuilder.addCard(HeaderTag.CLON);

      }
      if ((!validLatLon(thisBuilder, HeaderTag.CLAT))
          || (!validLatLon(thisBuilder, HeaderTag.CLON))) {
        System.out.println("WARNING! check of global center keywords shows they"
            + "have not been set! Setting to default global values (0,0)");
        LatitudinalVector llr = new LatitudinalVector(1., 0., 0.);
        thisBuilder.setCenterLatLon(llr);
      }
      return thisBuilder;
    }

    /**
     * Check to make sure local corners and center lat,lon have valid values. If not then throw
     * RuntimeException.
     * 
     * @param thisBuilder
     */
    public static void checkLocalLatLon(FitsHdrBuilder thisBuilder) {

      HeaderTag corner = HeaderTag.LLCLNG;
      throwBadLatLon(thisBuilder, corner);
      corner = HeaderTag.LLCLAT;
      throwBadLatLon(thisBuilder, corner);

      corner = HeaderTag.ULCLNG;
      throwBadLatLon(thisBuilder, corner);
      corner = HeaderTag.ULCLAT;
      throwBadLatLon(thisBuilder, corner);

      corner = HeaderTag.URCLNG;
      throwBadLatLon(thisBuilder, corner);
      corner = HeaderTag.URCLAT;
      throwBadLatLon(thisBuilder, corner);

      corner = HeaderTag.LRCLNG;
      throwBadLatLon(thisBuilder, corner);
      corner = HeaderTag.LRCLAT;
      throwBadLatLon(thisBuilder, corner);

      corner = HeaderTag.CLAT;
      throwBadLatLon(thisBuilder, corner);

      corner = HeaderTag.CLON;
      throwBadLatLon(thisBuilder, corner);

    }

    private static boolean validLatLon(FitsHdrBuilder thisBuilder, HeaderTag cornerTag) {

      String chkVal = thisBuilder.getCard(cornerTag).getValue();
      if ((chkVal.contains("-999")) || (chkVal.contains("NaN")) || (chkVal.length() < 1)) {
        return false;
      }
      return true;
    }

    private static void throwBadLatLon(FitsHdrBuilder thisBuilder, HeaderTag cornerTag) {

      if (!validLatLon(thisBuilder, cornerTag)) {
        String chkVal = thisBuilder.getCard(cornerTag).getValue();
        String errMesg =
            ("ERROR!" + cornerTag.toString() + " in FitsHdrBuilder not valid:" + chkVal);
        throw new RuntimeException(errMesg);
      }

    }

  } // end static class FitsHdrBuilder

  /**
   * Copy the fits header from fits file and use it to populate and return FitsHdrBuilder. There are
   * NO checks to see if the keywords match those in the Toolkit and the hdrBuilder is NOT
   * initialized with enums from HeaderTag!
   * 
   * @param fitsFile
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder copyFitsHeader(File fitsFile) throws HeaderCardException {

    FitsHdrBuilder hdrBuilder = new FitsHdrBuilder();
    try {
      Map<String, HeaderCard> map = FitsUtil.getFitsHeaderAsMap(fitsFile.getAbsolutePath());
      boolean initHdr = false;
      hdrBuilder = copyFitsHeader(map, initHdr);
      return hdrBuilder;
    } catch (FitsException | IOException e) {
      e.printStackTrace();
      String errmesg = "ERROR in FitsHeader.copyFitsHeader()! " + " Unable to parse fits file:"
          + fitsFile.toString() + " for fits header!";
      System.err.println(errmesg);
      System.exit(1);

    }
    return hdrBuilder;
  }

  /**
   * Copy the fits header from fits file and use it to populate and return FitsHdrBuilder. There are
   * NO checks to see if the keywords match those in the Toolkit. The user has the option to
   * initializehdrBuilder with enums from HeaderTag or not.
   * 
   * @param fitsFile
   * @param initHdr - If true the initialize FitsHdrBuilder w/ the enums in HeaderTag. if false,
   *        then FitsHdrBuilder initialized with an empty map.
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder copyFitsHeader(File fitsFile, boolean initHdr)
      throws HeaderCardException {

    FitsHdrBuilder hdrBuilder = new FitsHdrBuilder();
    try {
      Map<String, HeaderCard> map = FitsUtil.getFitsHeaderAsMap(fitsFile.getAbsolutePath());
      hdrBuilder = copyFitsHeader(map, initHdr);
      return hdrBuilder;
    } catch (FitsException | IOException e) {
      e.printStackTrace();
      String errmesg = "ERROR in FitsHeader.copyFitsHeader()! " + " Unable to parse fits file:"
          + fitsFile.toString() + " for fits header!";
      System.err.println(errmesg);
      System.exit(1);

    }
    return hdrBuilder;
  }


  /**
   * Loops through map of fits header cards and use it to populate and return FitsHdrBuilder. There
   * are NO checks to see if the keywords match those in the toolkit!
   * 
   * @param map
   * @param initHdr - If true the initialize FitsHdrBuilder w/ the enums in HeaderTag. if false,
   *        then FitsHdrBuilder initialized with an empty map.
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder copyFitsHeader(Map<String, HeaderCard> map, boolean initHdr)
      throws HeaderCardException {

    // initialize hdrBuilder with the full set of keywords specified in HeaderTag
    FitsHdrBuilder hdrBuilder = new FitsHdrBuilder();
    if (initHdr) {
      hdrBuilder = initHdrBuilder();
    }

    // loop through each of the HeaderCards in the map and see if any will help
    // build the header
    for (Map.Entry<String, HeaderCard> entry : map.entrySet()) {
      HeaderCard thisCard = entry.getValue();
      hdrBuilder.setbyHeaderCard(thisCard);
    }
    return hdrBuilder;
  }

  /**
   * Overloaded method. This is the original implementation. Set the initHdr flag to False.
   * 
   * @param map
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder copyFitsHeader(Map<String, HeaderCard> map)
      throws HeaderCardException {

    // initialize hdrBuilder with the full set of keywords specified in HeaderTag
    boolean initHdr = false;
    FitsHdrBuilder hdrBuilder = copyFitsHeader(map, initHdr);

    return hdrBuilder;
  }

  /**
   * Loop through a map of fits header cards to set headercards on an existing builder. If input
   * headerBuilder is null then will generate a new FitsHdrBuilder, update it, and return it.
   * 
   * @param map
   * @param hdrBuilder
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder addToFitsHeader(Map<String, HeaderCard> map,
      FitsHdrBuilder hdrBuilder) throws HeaderCardException {

    if (hdrBuilder == null) {
      hdrBuilder = new FitsHdrBuilder();
    }

    for (Map.Entry<String, HeaderCard> entry : map.entrySet()) {
      HeaderCard thisCard = entry.getValue();
      hdrBuilder.setbyHeaderCard(thisCard);
    }
    return hdrBuilder;

  }

  /**
   * Copy the fits header from fits file and use it to update an existing builder. If builder is
   * null then will generate a new FitsHdrBuilder, update it, and return it.
   * 
   * @param fitsFile
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder addToFitsHeader(File fitsFile, FitsHdrBuilder hdrBuilder)
      throws HeaderCardException {

    if (hdrBuilder == null) {
      hdrBuilder = new FitsHdrBuilder();
    }

    try {
      Map<String, HeaderCard> map = FitsUtil.getFitsHeaderAsMap(fitsFile.getAbsolutePath());
      hdrBuilder = addToFitsHeader(map, hdrBuilder);
      return hdrBuilder;
    } catch (FitsException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      String errmesg = "ERROR in FitsHeader.copyFitsHeader()! " + " Unable to parse fits file:"
          + fitsFile.toString() + " for fits header!";
      System.err.println(errmesg);
      System.exit(1);

    }
    return hdrBuilder;
  }

  /**
   * Initialize builder with all the keywords specified in HeaderTag enum.
   * 
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder initHdrBuilder() throws HeaderCardException {
    FitsHdrBuilder builder = new FitsHdrBuilder();
    for (HeaderTag tag : HeaderTag.fitsKeywords) {
      builder.fitsKV.put(tag.toString(),
          new HeaderCard(tag.toString(), tag.value(), tag.comment()));
    }
    return builder;
  }

  /**
   * Configures an existing FitsHdrBuilder according to values read from a fits configuration file.
   * Skips lines that start with '//'; assumes these are comments in the fits configuration file.
   * Does NOT check to see if keywords in configFile match keywords in HeaderTag.
   * 
   * @param configFile
   * @param hdrBuilder
   * @return
   * @throws HeaderCardException
   * @throws IOException
   */
  public static FitsHdrBuilder configHdrBuilder(String configFile, FitsHdrBuilder hdrBuilder)
      throws HeaderCardException, IOException {
    File checkF = new File(configFile);
    if (!checkF.exists()) {
      String errMesg = "ERROR:FITS header configuration file:" + configFile + " does not exist!";
      throw new RuntimeException(errMesg);
    }

    if (hdrBuilder == null) {
      System.out.println("builder passed to FitsHeader.configHdrBuilder() is null. Generating"
          + " new FitsHeaderBuilder");
      hdrBuilder = new FitsHdrBuilder();
    }
    List<String> content = FileUtils.readLines(new File(configFile), Charset.defaultCharset());
    boolean separatorFound = false;
    for (String line : content) {

      if (line.startsWith("//")) {
        // treat as a comment line. Skip and go to next line
      } else {

        String[] keyval = line.split("#");
        if (keyval.length > 1) {

          separatorFound = true;
          // check if there is a match w/ HeaderTags. Returns 'NOMATCH' if match not found
          HeaderTag thisTag = HeaderTag.tagFromString(keyval[0]);

          // pass to fits header builder and see if it matches on a fits keyword
          if (keyval.length == 2) {
            // assume user only wants to overwrite the value portion. Leave the comments
            // alone.
            System.out.println("setting " + thisTag.toString() + " to " + keyval[1]);
            hdrBuilder.setVbyHeaderTag(thisTag, keyval[1]);
          } else if (keyval.length == 3) {
            if (keyval[2].contains("null")) {
              // user explicitly wants to override any comment in this header with null
              hdrBuilder.setVCbyHeaderTag(thisTag, keyval[1], null);
            } else {
              System.out.println("setting " + thisTag.toString() + " to " + keyval[1]
                  + ", comment to " + keyval[2]);
              hdrBuilder.setVCbyHeaderTag(thisTag, keyval[1], keyval[2]);
            }
          } else {
            System.out.println(
                "Warning: the following line in the config file line has more than 2 colons:");
            System.out.println(line);
            System.out.println("Cannot parse. skipping this line");
          }

        }

      }
    }
    if (!separatorFound) {
      System.out.println("WARNING! The fits config file:" + configFile
          + " does not appear to be a valid config file! There is no # separator!");
    }
    return hdrBuilder;
  }

  /**
   * Initializes and returns a FitsHdrBuilder based on values read from a fits configuration file.
   * 
   * @param configFile
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder initHdrBuilder(String configFile) throws HeaderCardException {
    FitsHdrBuilder hdrBuilder = new FitsHdrBuilder();
    // try to load config file if it exists and modify fits header builder with it.
    try {
      configHdrBuilder(configFile, hdrBuilder);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      String errMesg = "Error trying to read config file:" + configFile;
      throw new RuntimeException(errMesg);
    }
    return hdrBuilder;
  }

  /**
   * Return a new FitsHdrBuilder with EMPTY map! Call initHdrBuilder() to get a FitsHdrBuilder with
   * an initialized map.
   * 
   * @return
   * @throws HeaderCardException
   */
  public static FitsHdrBuilder getBuilder() throws HeaderCardException {
    return new FitsHdrBuilder();
  }

  /**
   * Return the desired HeaderCard. Return default value specified by HeaderTag if not found in
   * FitsHdr.
   * 
   * @param tag
   * @return
   * @throws HeaderCardException
   */
  public HeaderCard getHeaderCard(HeaderTag tag) throws HeaderCardException {
    String keyword = tag.toString();
    if (fitsKV.containsKey(keyword)) {
      return fitsKV.get(keyword);
    } else {

      System.out.println("WARNING! keyword:" + keyword + " not set in fits header builder."
          + " using default value.");
      return new HeaderCard(tag.toString(), tag.value(), tag.comment());
    }
  }

  /**
   * Returns the desired headercard with the value stored as a double. This guarantees that when the
   * fits header is created, the value for this fits keyword will be displayed without quotes around
   * it. In the FITS format this denotes that the value is a numerical value. Return default value
   * specified by HeaderTag if not found in FitsHdr.
   * 
   * @param tag
   * @return
   * @throws HeaderCardException
   */
  public HeaderCard getHeaderCardD(HeaderTag tag) throws HeaderCardException {

    String keyword = tag.toString();
    if (fitsKV.containsKey(keyword)) {
      HeaderCard thisCard = fitsKV.get(keyword);
      return new HeaderCard(thisCard.getKey(),
          Double.parseDouble(thisCard.getValue().replace('d', 'e').replace('D', 'e')),
          thisCard.getComment());

    } else {

      System.out.println(
          "WARNING! keyword:" + keyword + " not set in FitsHdr," + " using default value.");
      return new HeaderCard(keyword, tag.value(), tag.comment());

    }

  }

  /**
   * Return the desired HeaderCard - the double value is set to the desired string format. Return
   * default value if card is not found.
   * 
   * @param tag
   * @return
   */
  public HeaderCard getHeaderCard(HeaderTag tag, String fmtS) throws HeaderCardException {
    String keyword = tag.toString();
    if (fitsKV.containsKey(keyword)) {
      HeaderCard thisCard = fitsKV.get(keyword);
      double value = Double.parseDouble(thisCard.getValue());
      if (Double.isFinite(value))
        value = Double.parseDouble(String.format(fmtS, value));
      return new HeaderCard(thisCard.getKey(), value, thisCard.getComment());
    } else {
      String mesg =
          "WARNING! keyword:" + tag.toString() + " not set in FitsHdr," + " using default value.";
      System.out.println(mesg);
      return new HeaderCard(keyword, tag.value(), tag.comment());

    }
  }

  /**
   * Generic function that searches a FITS HDU Header and returns HeaderCard matching the keyword
   * Returns null if HeaderCard not found.
   * 
   * @param header
   * @param keyword
   * @return
   */
  public static HeaderCard getHeaderCardorNull(Header header, String keyword) {

    HeaderCard matchCard = null;
    Cursor<String, HeaderCard> iterator = header.iterator();
    boolean opDone = false;
    while ((iterator.hasNext()) && (!opDone)) {

      HeaderCard thisCard = iterator.next();
      if (thisCard.getKey().equals(keyword)) {
        matchCard = thisCard;
        opDone = true;
      }
    }

    return matchCard;

  }

}
