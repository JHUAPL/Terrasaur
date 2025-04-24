package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.function.Function;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class AxisR extends Axis {

  public AxisR(double min, double max, String title, Function<Double, String> func) {
    super(min, max, title, func);
  }

  public AxisR(double min, double max, String title, String tickFormat) {
    super(min, max, title, StringFunctions.fixedFormat(tickFormat));
  }

  public AxisR(double min, double max, String title) {
    super(min, max, title);
  }

}
