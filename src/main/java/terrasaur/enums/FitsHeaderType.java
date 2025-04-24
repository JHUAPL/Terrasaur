package terrasaur.enums;

/**
 * Enums for a given fits header format. This is used to keep fits headers for the different types
 * separately configurable.
 * 
 * @author espirrc1
 *
 */
public enum FitsHeaderType {

  ANCIGLOBALGENERIC, ANCILOCALGENERIC, ANCIGLOBALALTWG, ANCIG_FACETRELATION_ALTWG, ANCILOCALALTWG, DTMGLOBALALTWG, DTMLOCALALTWG, DTMGLOBALGENERIC, DTMLOCALGENERIC, NFTMLN;

  public static boolean isGLobal(FitsHeaderType hdrType) {

    boolean globalProduct;

    switch (hdrType) {

      case ANCIGLOBALALTWG:
      case ANCIGLOBALGENERIC:
      case DTMGLOBALALTWG:
      case DTMGLOBALGENERIC:
        globalProduct = true;
        break;

      default:
        globalProduct = false;
    }

    return globalProduct;
  }
}
