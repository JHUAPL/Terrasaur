package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.function.Function;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class AxisX extends Axis {

  public AxisX(double min, double max, String title, Function<Double, String> func) {
    super(min, max, title, func);
  }

  public AxisX(double min, double max, String title, String tickFormat) {
    super(min, max, title, StringFunctions.fixedFormat(tickFormat));
  }

  public AxisX(double min, double max, String title) {
    super(min, max, title);
  }

  public AxisX(AxisX other) {
    super(other);
  }
}
