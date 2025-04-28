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

public class AltwgAnciLocal extends AnciTableFits implements AnciFitsHeader {

  public AltwgAnciLocal(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.ANCILOCALALTWG);
  }

  // methods below override the concrete methods in AnciTableFits abstract class or
  // are specific to this class

  /**
   * Create fits header as a list of HeaderCard. List contains the keywords in the order of
   * appearance in the ALTWG fits header. Overrides default implementation in AnciTableFits.
   */
  @Override
  public List<HeaderCard> createFitsHeader() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();

    headers.addAll(getHeaderInfo("header information"));
    headers.addAll(getMissionInfo("mission information"));
    headers.addAll(getIDInfo("identification info"));
    headers.addAll(getMapDataSrc("shape data source"));
    headers.addAll(getProcInfo("processing information"));
    headers.addAll(getMapInfo("map specific information"));
    headers.addAll(getSpatialInfo("summary spatial information"));
    headers.addAll(getSpecificInfo("product specific"));

    return headers;

  }

  /**
   * Contains OREX-SPOC specific keywords.
   */
  @Override
  public List<HeaderCard> getIDInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SPOC_ID));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SDPAREA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SDPDESC));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.MPHASE));

    return headers;

  }


}
