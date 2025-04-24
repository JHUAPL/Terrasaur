package terrasaur.utils.saaPlotLib.util;

import java.awt.Color;
import java.awt.Stroke;
import java.util.Optional;
import org.immutables.value.Value;
import terrasaur.utils.saaPlotLib.canvas.symbol.Symbol;

@Value.Immutable
public abstract class LegendEntry implements Comparable<LegendEntry> {

  @Value.Default
  public Color color() {
    return Color.BLACK;
  }

  @Value.Default
  public String name() {
    return "";
  }

  public abstract Optional<Stroke> stroke();

  public abstract Optional<Symbol> symbol();

  @Override
  public int compareTo(LegendEntry o) {
    return name().compareTo(o.name());
  }
}
