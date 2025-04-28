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
import terrasaur.utils.DTMHeader;

/**
 * Factory class that returns List<HeaderCard> where the HeaderCard objects are in the correct order
 * for writing to a fits header. Also contains methods for creating List<HeaderCard> for different
 * sections of a fits header
 * 
 * @author espirrc1
 *
 */
public class FitsHeaderFactory {

  private static final String PLANE = "PLANE";
  private static final String COMMENT = "COMMENT";


  public static DTMHeader getDTMHeader(FitsHdr fitsHdr, FitsHeaderType headerType) {

    switch (headerType) {

      case NFTMLN:
        return new NFTmln(fitsHdr);

      case DTMLOCALALTWG:
        return new AltwgLocalDTM(fitsHdr);

      case DTMGLOBALALTWG:
        return new AltwgGlobalDTM(fitsHdr);

      case DTMGLOBALGENERIC:
        return new GenericGlobalDTM(fitsHdr);

      case DTMLOCALGENERIC:
        return new GenericLocalDTM(fitsHdr);

      default:
        return null;
    }

  }

  public static AnciFitsHeader getAnciHeader(FitsHdr fitsHdr, FitsHeaderType headerType) {

    switch (headerType) {
      case ANCIGLOBALGENERIC:
        return new GenericAnciGlobal(fitsHdr);

      case ANCILOCALGENERIC:
        return new GenericAnciLocal(fitsHdr);

      case ANCIGLOBALALTWG:
        return new AltwgAnciGlobal(fitsHdr);

      case ANCIG_FACETRELATION_ALTWG:
        return new AltwgAnciGlobalFacetRelation(fitsHdr);

      case ANCILOCALALTWG:
        return new AltwgAnciLocal(fitsHdr);

      default:
        return null;
    }

  }


  /**
   * Fits Header block that contains information about the fits header itself. Ex. Header version
   * number.
   * 
   * @param fitsHdr
   * @return
   * @throws HeaderCardException
   */
  public static List<HeaderCard> getHeaderInfo(FitsHeader fitsHdr) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "header information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.HDRVERS));

    return headers;

  }

  /**
   * Fits header block that contains information about the mission Ex. MISSION name, HOST name,
   * Target name.
   * 
   * @param fitsHdr
   * @return
   * @throws HeaderCardException
   */
  public static List<HeaderCard> getMissionInfo(FitsHeader fitsHdr) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "mission information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MISSION));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.HOSTNAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TARGET));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ORIGIN));

    return headers;

  }

  /**
   * Fits header block that contains ID information, i.e. information that would uniquely identify
   * the data product.
   * 
   * @param fitsHdr
   * @return
   * @throws HeaderCardException
   */
  public static List<HeaderCard> getIdInfo(FitsHeader fitsHdr) throws HeaderCardException {
    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "identification info", false));

    // check latest Map Format SIS revision to see if SPOC handles these keywords
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SPOC_ID));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SDPAREA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SDPDESC));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MPHASE));

    return headers;

  }

  /**
   * Fits header block that contains information about the source shape model used to create the
   * fits file.
   * 
   * @param fitsHdr
   * @return
   * @throws HeaderCardException
   */
  public static List<HeaderCard> getShapeSourceInfo(FitsHeader fitsHdr) throws HeaderCardException {
    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "shape data source", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRC));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCF));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCV));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCD));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.OBJ_FILE));

    return headers;
  }

  public static List<HeaderCard> getProcInfo(FitsHeader fitsHdr) throws HeaderCardException {
    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "processing information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODNAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATEPRD));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFTWARE));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFT_VER));

    return headers;

  }

  public static List<HeaderCard> getMapSpecificInfo(FitsHeader fitsHdr) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "map specific information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_NAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_VER));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MAP_TYPE));

    return headers;

    // check latest Map Format SIS revision to see if SPOC handles these keywords
    // MAP_PROJ*
    // GSD*
    // GSDI*
  }

  public static List<HeaderCard> getSummarySpatialInfo(FitsHeader fitsHdr,
      FitsHeaderType fitsHeaderType) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();

    // this section common to all fitsHeaderTypes
    headers.add(new HeaderCard(COMMENT, "summary spatial information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.CLON));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.CLAT));

    switch (fitsHeaderType) {
      case ANCILOCALGENERIC:
        headers.add(fitsHdr.getHeaderCard(HeaderTag.LLCLNG));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.LLCLAT));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.URCLNG));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.URCLAT));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.LRCLNG));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.LRCLAT));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.ULCLNG));
        headers.add(fitsHdr.getHeaderCard(HeaderTag.ULCLAT));
        break;

      default:
        // default does nothing because switch only handles specific cases.
        break;
    }
    return headers;

  }
}
