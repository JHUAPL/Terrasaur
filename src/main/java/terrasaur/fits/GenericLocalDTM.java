package terrasaur.fits;

import terrasaur.enums.FitsHeaderType;
import terrasaur.utils.DTMHeader;

/**
 * Contains methods for building generic Local DTM Fits Header. DTM header will contain only those
 * keywords that are deemed common to all Local DTM fits files.
 * 
 * @author espirrc1
 *
 */
public class GenericLocalDTM extends DTMFits implements DTMHeader {

  public GenericLocalDTM(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.DTMLOCALGENERIC);
  }

}
