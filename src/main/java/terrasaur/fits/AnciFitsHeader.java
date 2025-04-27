package terrasaur.fits;

import java.util.List;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;

public interface AnciFitsHeader {

  public List<HeaderCard> createFitsHeader() throws HeaderCardException;

}
