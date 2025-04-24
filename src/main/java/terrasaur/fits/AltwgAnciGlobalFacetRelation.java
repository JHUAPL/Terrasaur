package terrasaur.fits;

import java.util.ArrayList;
import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.enums.FitsHeaderType;

public class AltwgAnciGlobalFacetRelation extends AnciTableFits implements AnciFitsHeader {

  public AltwgAnciGlobalFacetRelation(FitsHdr fitsHeader) {

    super(fitsHeader, FitsHeaderType.ANCIG_FACETRELATION_ALTWG);
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
   * Return the HeaderCards associated with a specific product. By default we use the ALTWG specific
   * product keywords
   * 
   * @param comment
   * @return
   * @throws HeaderCardException
   */
  @Override
  public List<HeaderCard> getSpecificInfo(String comment) throws HeaderCardException {

    List<HeaderCard> headers = new ArrayList<HeaderCard>();
    if (comment.length() > 0) {
      headers.add(new HeaderCard(COMMENT, comment, false));
    }

    headers.add(fitsHdr.getHeaderCard(HeaderTag.OBJINDX));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.GSDINDX));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.GSDINDXI));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIGMA));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.SIG_DEF));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_1));
    headers.add(fitsHdr.getHeaderCardD(HeaderTag.DQUAL_2));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DSIG_DEF));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.DENSITY));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.ROT_RATE));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.REF_POT));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_MAJ));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_MIN));
    headers.add(fitsHdr.getHeaderCard(HeaderTag.TILT_PA));

    return headers;
  }
}
