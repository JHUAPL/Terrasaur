package terrasaur.fits;

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;
import terrasaur.utils.DTMHeader;

/**
 * Contains methods for building fits header corresponding to the NFT Maps for Landmark Navigation.
 * Note that it uses private methods for the different fits blocks instead of using the
 * HeaderCardFactory static methods. This is because the NFT MLN header updates are independent of
 * updates to the ALTWG product fits headers or the Map Formats headers. static methods in the
 * HeaderCardFactory
 * 
 * @author espirrc1
 *
 */
public class NFTmln extends DTMFits implements DTMHeader {

  // private FitsData fitsData;
  private boolean dataContained = false;

  public NFTmln(FitsHdr fitsHeader) {

    super(fitsHeader, FitsHeaderType.NFTMLN);
  }

  public List<HeaderCard> createFitsHeader(List<HeaderCard> planeList) throws HeaderCardException {

    List<HeaderCard> headerCards = new ArrayList<HeaderCard>();

    headerCards.addAll(getHeaderInfo("Header Information"));
    headerCards.addAll(getMissionInfo("Mission Information"));
    headerCards.addAll(getIDInfo("Observation Information"));
    // headerCards.addAll(getShapeSrcInfo());
    headerCards.addAll(getMapDataSrc("Data Source"));
    headerCards.addAll(getTimingInfo());
    headerCards.addAll(getProcInfo("Processing Information"));
    headerCards.addAll(getSpecificInfo("Product Specific Information"));
    headerCards.addAll(getPlaneInfo("", planeList));

    // end keyword
    headerCards.add(getEnd());

    return headerCards;
  }

  /**
   * Custom header block.
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
    headers.add(fitsHdr.getHeaderCard(HeaderTag.NPRDVERS));

    return headers;

  }

  /**
   * Fits header block containing information about the source data used for the shape model. This
   * method does not exist in the parent class.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getShapeSrcInfo() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "Shape Data Source", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCF));

    return headers;

  }

  /**
   * Custom source map data block.
   */
  @Override
  public List<HeaderCard> getMapDataSrc(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRC));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCV));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFTWARE));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SOFT_VER));

    return headers;

  }

  /**
   * Custom product specific info data block
   * 
   * @throws HeaderCardException
   */
  public List<HeaderCard> getSpecificInfo(String comment) throws HeaderCardException {

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
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.GSD));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.SIGMA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIG_DEF));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_1));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_2));

    return headers;

  }

  /**
   * Fits header block containing information about when products were created. This method does not
   * exist in the parent class.
   * 
   * @return
   * @throws HeaderCardException
   */
  public List<HeaderCard> getTimingInfo() throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    headers.add(new HeaderCard(COMMENT, "Timing Information", false));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCD));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DATENPRD));

    return headers;

  }

  /**
   * Custom processing info block.
   */
  public List<HeaderCard> getProcInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }
    headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODNAME));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODVERS));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.CREATOR));

    return headers;

  }

}
