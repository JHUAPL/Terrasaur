package terrasaur.fits;

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;

public class AltwgAnciGlobal extends AnciTableFits implements AnciFitsHeader {

  public AltwgAnciGlobal(FitsHdr fitsHeader) {
    super(fitsHeader, FitsHeaderType.ANCIGLOBALALTWG);
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
