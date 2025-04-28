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
 * Contains methods for building fits header corresponding to the Generic Ancillary GLobal fits
 * header as specified in the Map Formats SIS.
 * 
 * See concrete methods and attributes in AnciTableFits unless overridden. Overridden or
 * implementation methods specific to this fits type are here.
 * 
 * @author espirrc1
 *
 */
public class GenericAnciGlobal extends AnciTableFits implements AnciFitsHeader {

  public GenericAnciGlobal(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.ANCIGLOBALGENERIC);
  }


  /**
   * Build fits header portion that contains the spatial information of the Generic Anci Global
   * product. Overrides the default implementation in AnciTableFits.
   */
  @Override
  public List<HeaderCard> getSpatialInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    // the generic Global Anci product ONLY CONTAINS THE CENTER LAT LON!
    // per map_format_fits_header_normalization_09212917_V02.xlsx
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLON));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLAT));

    return headers;
  }

}
