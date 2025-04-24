package terrasaur.utils.saaPlotLib.util;

import java.util.function.Function;

public class StringFunctions {

  public static Function<Double, String> fixedFormat(String format) {
    return (t) -> String.format(format, t);
  }

  public static Function<Double, String> scale(String format, double scale) {
    return (t) -> String.format(format, t * scale);
  }

  public static Function<Double, String> toDegrees(String format) {
    return (t) -> String.format(format, Math.toDegrees(t));
  }

  public static Function<Double, String> toDegreesLat(String format) {
    return (t) -> String.format(format + (t > 0 ? "N" : "S"), Math.abs(Math.toDegrees(t)));
  }

  public static Function<Double, String> toDegreesLon(String format) {
    return (t) -> String.format(format + (t > 0 ? "E" : "W"), Math.abs(Math.toDegrees(t)));
  }

  public static Function<Double, String> toDegreesELon(String format) {
    return t -> {
      t = Math.toDegrees(t);
      if (t < 0) t += 360;
      if (t >= 360) t -= 360;
      return String.format(format + "E", t);
    };
  }

  public static Function<Double, String> toDegreesWLon(String format) {
    return t -> {
      t = -Math.toDegrees(t);
      if (t < 0) t += 360;
      if (t > 360) t -= 360;
      return String.format(format + "W", t);
    };
  }

  public static Function<Double, String> toRadians(String format) {
    return (t) -> String.format(format, Math.toRadians(t));
  }
}
