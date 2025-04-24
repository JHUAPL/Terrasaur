package terrasaur.fits;

import terrasaur.enums.FitsHeaderType;
import terrasaur.utils.DTMHeader;

/**
 * Contains methods for building generic Global DTM fits header. Generic Global DTM header will
 * contain only those keywords that are deemed common to all Global DTM fits files.
 * 
 * @author espirrc1
 *
 */
public class GenericGlobalDTM extends DTMFits implements DTMHeader {

  public GenericGlobalDTM(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.DTMGLOBALGENERIC);
  }

}
