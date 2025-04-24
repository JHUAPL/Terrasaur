package terrasaur.fits;

import terrasaur.enums.FitsHeaderType;

/**
 * Contains methods for building fits header corresponding to the Generic Ancillary Local fits
 * header as specified in the Map Formats SIS.
 * 
 * See concrete methods and attributes in AnciTableFits unless overridden. Overridden or
 * implementation methods specific to this fits type are here.
 * 
 * @author espirrc1
 *
 */
public class GenericAnciLocal extends AnciTableFits implements AnciFitsHeader {

  public GenericAnciLocal(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.ANCILOCALGENERIC);
  }


}
