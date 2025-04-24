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
