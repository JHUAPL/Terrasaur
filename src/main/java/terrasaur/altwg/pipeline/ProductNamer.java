package terrasaur.altwg.pipeline;

import terrasaur.enums.AltwgDataType;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;

public interface ProductNamer {

  public String getNameFrag(String productName, int fieldNum);

  public String productbaseName(FitsHdrBuilder hdrBuilder, AltwgDataType altwgProduct,
      boolean isGlobal);

  public String getVersion(FitsHdrBuilder hdrBuilder);

  public double gsdFromHdr(FitsHdrBuilder hdrBuilder);

  public NameConvention getNameConvention();

  public double gsdFromFilename(String filename);

}
