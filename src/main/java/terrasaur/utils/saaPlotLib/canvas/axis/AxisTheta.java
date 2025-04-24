package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.function.Function;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class AxisTheta extends Axis {

  public AxisTheta(double min, double max, String title, Function<Double, String> tickFunc) {
    super(min, max, title, tickFunc);
  }

  public AxisTheta(double min, double max, String title, String tickFormat) {
    super(min, max, title, StringFunctions.fixedFormat(tickFormat));
  }

  public AxisTheta(double min, double max, String title) {
    super(min, max, title);
  }

}
