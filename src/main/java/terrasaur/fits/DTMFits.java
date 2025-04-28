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

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;

/**
 * Abstract generic class with concrete methods and attributes for creating a FITS DTM cube with a
 * generalized fits header. Specific implementations can be written to create custom fits headers as
 * needed.
 * 
 * @author espirrc1
 *
 */
public abstract class DTMFits {

  public final String COMMENT = "COMMENT";
  final FitsHdr fitsHdr;
  private FitsData fitsData;
  private boolean dataContained = false;
  public final FitsHeaderType fitsHeaderType;

  public DTMFits(FitsHdr fitsHdr, FitsHeaderType fitsHeaderType) {
    this.fitsHdr = fitsHdr;
    this.fitsHeaderType = fitsHeaderType;
  }

  public void setData(FitsData fitsData) {
    this.fitsData = fitsData;
    dataContained = true;
  }

  public List<HeaderCard> createFitsHeader(List<HeaderCard> planeList) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();

    headers.addAll(getHeaderInfo("header information"));
    headers.addAll(getMissionInfo("mission information"));
    headers.addAll(getIDInfo("identification info"));
    headers.addAll(getMapDataSrc("data source"));
    headers.addAll(getProcInfo("processing information"));
    headers.addAll(getMapInfo("map specific information"));
    headers.addAll(getSpatialInfo("summary spatial information"));
    headers.addAll(getPlaneInfo("plane information", planeList));
    headers.addAll(getSpecificInfo("product specific"));

    // end keyword
    headers.add(getEnd());

    return headers;
  }

  /**
   * return Fits header block that contains information about the fits header itself. No string
   * passed, so no comment in header.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getHeaderInfo() throws HeaderCardException {
    return getHeaderInfo("");
  }

  /**
   * return Fits header block that contains information about the fits header itself. This is a
   * custom section and so is left empty here. It can be defined in the concrete classes that extend
   * this class.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getHeaderInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    return headers;

  }

  /**
   * Fits header block containing information about the mission.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getMissionInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MISSION));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.HOSTNAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TARGET));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ORIGIN));

    return headers;

  }

  /**
   * Fits header block containing observation or ID related information.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getIDInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MPHASE));

    return headers;

  }

  /**
   * Fits header block containing information about the source data used to create the map.
   * 
   * @param comment
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getMapDataSrc(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRC));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCF));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCV));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCD));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.OBJ_FILE));

    return headers;

  }

  public List<HeaderCard> getMapInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_NAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_VER));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_TYPE));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.GSD));

    return headers;
  }

  /**
   * Fits header block containing information about the software processing done to generate the
   * product.
   * 
   * @param comment
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getProcInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODNAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATEPRD));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFTWARE));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFT_VER));

    return headers;

  }

  /**
   * Creates header block containing spatial information for the DTM, e.g. corner locations, vector
   * to center, Ux, Uy, Uz.
   * 
   * @param comment
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getSpatialInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLON));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLAT));
    headers.addAll(getCornerCards());

    // remove these keywords. They are specific to local and MLNs
    // headers.addAll(getCenterVec());
    // headers.addAll(getUX());
    // headers.addAll(getUY());
    // headers.addAll(getUZ());

    return headers;
  }

  /**
   * Return the HeaderCards describing each DTM plane. Used to build the portion of the fits header
   * that contains information about the planes in the DTM cube. Checks to see that all data planes
   * are described by comparing size of planeList against length of fits data.
   * 
   * @param comment
   * @param planeList
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getPlaneInfo(String comment, List<HeaderCard> planeList)
      throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.addAll(planeList);

    if (!dataContained) {
      String errMesg = "ERROR! Cannot return keywords describing the DTM cube without "
          + "having the actual data!";
      throw new RuntimeException(errMesg);
    }

    // check if planeList describes all the planes in data, throw runtime exception if not.
    if (planeList.size() != fitsData.getData().length) {
      System.out.println("Error: plane List has " + planeList.size() + " planes but datacube has "
          + fitsData.getData().length + " planes");
      for (HeaderCard thisPlane : planeList) {
        System.out.println(thisPlane.getKey() + ":" + thisPlane.getValue());
      }
      String errMesg = "Error: plane List has " + planeList.size() + " planes but datacube "
          + " has " + fitsData.getData().length + " planes";
      throw new RuntimeException(errMesg);
    }

    return headers;
  }

  /**
   * Return the HeaderCards associated with a specific product.
   * 
   * @param comment
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getSpecificInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCardD(HeaderTag.SIGMA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIG_DEF));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.PXPERDEG));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DENSITY));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.ROT_RATE));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.REF_POT));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.TILT_MAJ));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.TILT_MIN));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.TILT_PA));

    return headers;
  }

  /**
   * Return headercards associated with the upper/lower left/right corners of the image.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getCornerCards() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    String fmtS = "%18.13f";
    headers.add(fitsHdr.getHeaderCard(HeaderTag.LLCLNG, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.LLCLAT, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.URCLNG, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.URCLAT, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.LRCLNG, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.LRCLAT, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ULCLNG, fmtS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ULCLAT, fmtS));

    return headers;
  }

  /**
   * Return headercards for vector to center of image.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getCenterVec() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CNTR_V_X));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CNTR_V_Y));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CNTR_V_Z));

    return headers;
  }

  public List<HeaderCard> getUX() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UX_X));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UX_Y));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UX_Z));

    return headers;

  }

  public List<HeaderCard> getUY() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UY_X));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UY_Y));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UY_Z));

    return headers;

  }

  public List<HeaderCard> getUZ() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UZ_X));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UZ_Y));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.UZ_Z));

    return headers;

  }

  public HeaderCard getEnd() throws HeaderCardException {
    return new HeaderCard(HeaderTag.END.toString(), HeaderTag.END.value(), HeaderTag.END.comment());
  }

}
