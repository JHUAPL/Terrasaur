package terrasaur.utils.saaPlotLib.colorMaps.tables;

import java.awt.Color;
import java.util.List;

public abstract class ColorTable {

  public enum FAMILY {
    CATEGORICAL, CYCLIC, DIVERGENT, LINEAR
  }

  protected final FAMILY family;

  protected ColorTable(FAMILY family) {
    this.family = family;
  }

  public abstract List<Color> getColors();

  public FAMILY getFamily() {
    return family;
  }
}
