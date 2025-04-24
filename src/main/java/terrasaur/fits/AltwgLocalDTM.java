package terrasaur.fits;

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;
import terrasaur.utils.DTMHeader;

/**
 * Contains methods for building fits header corresponding to ALTWG local DTM. Methods that are
 * specific to the ALTWG Local DTM fits header are contained here. Default methods are contained in
 * DTMFits class.
 * 
 * @author espirrc1
 *
 */
public class AltwgLocalDTM extends DTMFits implements DTMHeader {

  public AltwgLocalDTM(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.DTMLOCALALTWG);
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

  @Override
  public List<HeaderCard> getSpecificInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCardD(HeaderTag.SIGMA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIG_DEF));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DQUAL_1));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DQUAL_2));
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
   * Include corner points, center vector and ux,uy,uz describing local plane
   */
  @Override
  public List<HeaderCard> getSpatialInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLON));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.CLAT));

    headers.addAll(getCornerCards());
    headers.addAll(getCenterVec());
    headers.addAll(getUX());
    headers.addAll(getUY());
    headers.addAll(getUZ());

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
