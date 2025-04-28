/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
