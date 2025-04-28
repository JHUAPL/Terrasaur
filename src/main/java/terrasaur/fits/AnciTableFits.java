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
 * Abstract generic class with concrete methods and attributes for creating a FITS table with
 * generalized fits header. Specific implementations can be written to create custom fits headers as
 * needed.
 * 
 * @author espirrc1
 *
 */
public abstract class AnciTableFits {

  public final String COMMENT = "COMMENT";
  FitsHdr fitsHdr;
  public final FitsHeaderType fitsHeaderType;

  public AnciTableFits(FitsHdr fitsHdr, FitsHeaderType fitsHeaderType) {
    this.fitsHdr = fitsHdr;
    this.fitsHeaderType = fitsHeaderType;
  }

  public List<HeaderCard> createFitsHeader() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();

    headers.addAll(getHeaderInfo("header information"));
    headers.addAll(getMissionInfo("mission information"));
    headers.addAll(getIDInfo("identification info"));
    headers.addAll(getMapDataSrc("shape data source"));
    headers.addAll(getProcInfo("processing information"));
    headers.addAll(getMapInfo("map specific information"));
    headers.addAll(getSpatialInfo("summary spatial information"));

    return headers;

  }

  /**
   * return Fits header block that contains information about the fits header itself.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getHeaderInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.HDRVERS));

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
    headers.add(fitsHdr.getHeaderCard(HeaderTag.CREATOR));
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
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.GSDI));

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

  public List<HeaderCard> getSpatialInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLON));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLAT));

    headers.addAll(getCornerCards());

    // add CNTR_V_X,Y,Z
    headers.addAll(getCenterVec());

    // add UX_X,Y,Z
    headers.addAll(getUX());

    // add UY_X,Y,Z
    headers.addAll(getUY());

    // add UZ_X,Y,Z
    headers.addAll(getUZ());

    return headers;
  }

  /**
   * Return the HeaderCards associated with a specific product. By default we use the ALTWG specific
   * product keywords
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

    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIGMA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIG_DEF));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_1));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_2));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DSIG_DEF));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DENSITY));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ROT_RATE));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.REF_POT));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_MAJ));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_MIN));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_PA));

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

}
