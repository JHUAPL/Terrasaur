package terrasaur.utils;

import java.util.List;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.fits.FitsData;

public interface DTMHeader {

  public List<HeaderCard> createFitsHeader(List<HeaderCard> planeList) throws HeaderCardException;

  public void setData(FitsData fitsData);

}
