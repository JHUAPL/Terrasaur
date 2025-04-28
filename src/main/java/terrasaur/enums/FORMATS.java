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

import org.apache.commons.io.FilenameUtils;

public enum FORMATS {

  ASCII(true), BIN3(true), BIN4(true), BIN7(true), FITS(false), ICQ(false), OBJ(false), PLT(
      false), PLY(false), VTK(false);

  /** True if this format contains no facet information */
  public boolean pointsOnly;

  private FORMATS(boolean pointsOnly) {
    this.pointsOnly = pointsOnly;
  }

  /**
   * Guess the format from the (case-insensitive) filename extension.
   * <p>
   * ASCII: ascii, txt, xyz
   * <p>
   * BINARY: binary, bin
   * <p>
   * FITS: fits, fit
   * <p>
   * L2: l2, dat
   * <p>
   * OBJ: obj
   * <p>
   * PLT: plt
   * <p>
   * PLY: ply
   * <p>
   * VTK: vtk
   * 
   * @param filename
   * @return matched format type, or null if a match is not found
   */
  public static FORMATS formatFromExtension(String filename) {
    String extension = FilenameUtils.getExtension(filename);
    for (FORMATS f : FORMATS.values()) {
      if (f.name().equalsIgnoreCase(extension)) {
        return f;
      }
    }

    switch (extension.toUpperCase()) {
      case "TXT":
      case "XYZ":
        return FORMATS.ASCII;
      case "BIN":
        return FORMATS.BIN3;
      case "FIT":
        return FORMATS.FITS;
    }

    return null;
  }

}
