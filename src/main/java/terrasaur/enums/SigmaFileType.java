package terrasaur.enums;

import com.google.common.base.Strings;

/**
 * Enum for defining the types of sigma files that can be loaded and utilized by the Pipeline. This
 * allows the pipeline to load and parse different formats of sigma files.
 * 
 * @author espirrc1
 *
 */
public enum SigmaFileType {

  SPCSIGMA {

    @Override
    public String commentSymbol() {
      return "";
    }

    @Override
    public String stringArg() {
      return "spc";
    }

    @Override
    public int sigmaCol() {
      return 3;
    }
  },

  ERRORFROMSQLSIGMA {

    @Override
    public String commentSymbol() {
      return "#";
    }

    @Override
    public String stringArg() {
      return "errorfromsql";
    }

    // should be the Standard Deviation column in ErrorFromSQL file.
    public int sigmaCol() {
      return 8;
    }
  },

  NOMATCH {

    @Override
    public String commentSymbol() {
      return "NAN";
    }

    @Override
    public String stringArg() {
      return "NAN";
    }

    public int sigmaCol() {
      return -1;
    }
  };

  // returns the symbol that is used to denote comment lines
  public abstract String commentSymbol();

  // input argument to match
  public abstract String stringArg();

  // column number where sigma values are stored
  public abstract int sigmaCol();

  public static SigmaFileType getFileType(String sigmaFileType) {

    if (!Strings.isNullOrEmpty(sigmaFileType)) {
      for (SigmaFileType thisType : values()) {

        if (sigmaFileType.toLowerCase().equals(thisType.stringArg())) {

          return thisType;
        }
      }
    }
    return NOMATCH;
  }

  /**
   * Return the SigmaFileType associated with the SrcProductType.
   * 
   * @param srcType
   * @return
   */
  public static SigmaFileType sigmaTypeFromSrcType(SrcProductType srcType) {
    SigmaFileType sigmaType = SigmaFileType.NOMATCH;
    switch (srcType) {

      case SPC:
        sigmaType = SigmaFileType.SPCSIGMA;
        break;

      case OLA:
        sigmaType = SigmaFileType.ERRORFROMSQLSIGMA;
        break;

      default:
        sigmaType = SigmaFileType.NOMATCH;
        break;

    }

    return sigmaType;
  }
}
