package terrasaur.fits;

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;
import terrasaur.utils.DTMHeader;

/**
 * Contains methods for building fits header corresponding to ALTWG Global DTM. Methods that are
 * specific to the ALTWG Global DTM fits header are contained here. Default methods contained in
 * DTMFits class.
 * 
 * @author espirrc1
 *
 */
public class AltwgGlobalDTM extends DTMFits implements DTMHeader {

  public AltwgGlobalDTM(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.DTMGLOBALALTWG);
  }

  /**
   * Fits header block containing observation or ID related information. Includes keywords specific
   * to OREX-SPOC
   * 
   * @return
   * @throws HeaderCardException
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

  /**
   * return Fits header block that contains information about the fits header itself. Custom to
   * OREX-SPOC
   * 
   * @return
   * @throws HeaderCardException
   */
  @Override
  public List<HeaderCard> getHeaderInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.HDRVERS));

    return headers;

  }


  /**
   * Added GSDI - specific to OREX-SPOC.
   */
  @Override
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

}
