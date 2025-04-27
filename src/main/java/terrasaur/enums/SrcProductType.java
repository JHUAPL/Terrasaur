package terrasaur.enums;

/**
 * Enumeration storing the source product type: the product type of the source data used in creation
 * of an ALTWG product.
 * 
 * @author espirrc1
 *
 */
public enum SrcProductType {

  SFM {

    @Override
    public String getAltwgFrag() {
      return "sfm";
    }

  },

  SPC {
    @Override
    public String getAltwgFrag() {
      return "spc";
    }
  },

  // OLA Altimetry
  OLA {
    @Override
    public String getAltwgFrag() {
      return "alt";
    }
  },

  // SPC-OLA
  SPO {
    @Override
    public String getAltwgFrag() {
      return "spo";
    }
  },

  TRUTH {
    @Override
    public String getAltwgFrag() {
      return "tru";
    }

  },

  UNKNOWN {
    @Override
    public String getAltwgFrag() {
      return "unk";
    }
  };

  public static SrcProductType getType(String value) {
    value = value.toUpperCase();
    for (SrcProductType srcType : values()) {
      if (srcType.toString().equals(value)) {
        return srcType;
      }
    }
    return UNKNOWN;
  }

  /**
   * Returns the string fragment associated with the source product type. Follows the ALTWG naming
   * convention.
   * 
   * @return
   */
  public abstract String getAltwgFrag();

  /**
   * Return the SrcProductType whose getAltwgFrag() string matches the stringFrag. Return UNKNOWN if
   * a match is not found.
   * 
   * @param stringFrag
   * @return
   */
  public static SrcProductType fromAltwgFrag(String stringFrag) {

    for (SrcProductType prodType : SrcProductType.values()) {
      if (prodType.getAltwgFrag().equals(stringFrag))
        return prodType;
    }
    return UNKNOWN;
  }

}
