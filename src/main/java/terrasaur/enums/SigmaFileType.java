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
