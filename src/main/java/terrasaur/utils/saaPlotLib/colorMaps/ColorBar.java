package terrasaur.utils.saaPlotLib.colorMaps;

import java.awt.Rectangle;
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ColorBar {

  public abstract Rectangle rect();

  public abstract ColorRamp ramp();

  public abstract int numTicks();

  public abstract Function<Double, String> tickFunction();

  @Value.Default
  public boolean log() {
    return false;
  }

}
