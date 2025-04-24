package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.function.Function;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class AxisY extends Axis {

  public AxisY(double min, double max, String title, Function<Double, String> func) {
    super(min, max, title, func);
    setRotateTitle(-Math.PI / 2);
  }

  public AxisY(double min, double max, String title) {
    super(min, max, title);
    setRotateTitle(-Math.PI / 2);
  }

  public AxisY(double min, double max, String title, String tickFormat) {
    this(min, max, title, StringFunctions.fixedFormat(tickFormat));
  }

  public AxisY(AxisY other) {
    super(other);
  }

}
