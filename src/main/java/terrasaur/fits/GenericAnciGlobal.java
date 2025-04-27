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
