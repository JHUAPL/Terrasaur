package terrasaur.altwg.pipeline;

import java.io.File;
import java.util.Map;
import terrasaur.enums.AltwgDataType;
import terrasaur.fits.AltPipelnEnum;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;

/**
 * Factory class for returning concrete classes that implement the ProductNamer interface.
 *
 * @author espirrc1
 */
public class NamingFactory {

  public static ProductNamer getNamingConvention(NameConvention namingConvention) {

    switch (namingConvention) {
      case ALTPRODUCT:
        return new ALTWGProductNamer();

      case ALTNFTMLN:
        return new AltwgMLNNamer();

      case DARTPRODUCT:
        return new DartNamer();

      default:
        System.err.println(
            "ERROR! Naming convention:" + namingConvention.toString() + " not supported!");
        throw new RuntimeException();
    }
  }

  /**
   * Parse for keyword in pipeline config file that specifies what naming convention to use.
   *
   * @param pipeConfig
   * @return
   */
  public static ProductNamer parseNamingConvention(
      Map<AltPipelnEnum, String> pipeConfig, boolean verbose) {

    ProductNamer productNamer = null;

    if ((pipeConfig.containsKey(AltPipelnEnum.NAMINGCONVENTION))) {
      String value = pipeConfig.get(AltPipelnEnum.NAMINGCONVENTION);
      productNamer = parseNamingConvention(value);
    } else {
      String errMesg = "ERROR! Naming convention should have been defined in pipeConfig!";
      throw new RuntimeException(errMesg);
    }

    return productNamer;
  }

  /**
   * Parse string to determine the naming convention to use. Naming convention supplied by
   * ProductNamer interface.
   *
   * @param value
   * @return
   */
  public static ProductNamer parseNamingConvention(String value) {

    if (value.length() < 1) {
      String errMesg = "ERROR! Cannot pass empty string to NamingFactory.parseNamingConvention!";
      throw new RuntimeException(errMesg);
    }
    NameConvention nameConvention = NameConvention.parseNameConvention(value);
    return NamingFactory.getNamingConvention(nameConvention);
  }

  /**
   * Given the naming convention, hdrBuilder, productType, and original output file, return the
   * renamed output file and cross reference file. Output is returned as a File[] array where
   * array[0] is the output basename, array[1] is the cross-reference file. If no naming convention
   * is specified (NONEUSED) then array[0] is the same as outfile, array[1] is null.
   *
   * @param namingConvention
   * @param hdrBuilder
   * @param productType
   * @param isGlobal
   * @param outfile - proposed output filename. If Naming convention results in a renamed OBJ then
   *     this is not used. If no naming convention specified then outputFiles[0] = outfile.
   * @return
   */
  public static File[] getBaseNameAndCrossRef(
      NameConvention namingConvention,
      FitsHdrBuilder hdrBuilder,
      AltwgDataType productType,
      boolean isGlobal,
      String outfile) {

    File[] outputFiles = new File[2];

    // default to no renaming.
    File crossrefFile = null;
    String basename = outfile;

    if (namingConvention != NameConvention.NONEUSED) {
      ProductNamer productNamer = NamingFactory.getNamingConvention(namingConvention);
      basename = productNamer.productbaseName(hdrBuilder, productType, isGlobal);
      crossrefFile = new File(outfile + ".crf");
    }

    outputFiles[0] = new File(basename);
    outputFiles[1] = crossrefFile;
    return outputFiles;
  }
}
