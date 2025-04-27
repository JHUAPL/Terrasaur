package terrasaur.utils.saaPlotLib.data;

import java.awt.Color;
import java.awt.Font;
import org.immutables.value.Value;
import terrasaur.utils.saaPlotLib.util.Keyword;

/**
 * Class defining text annotations to display on plot
 * 
 * @author nairah1
 *
 */
@Value.Immutable
public abstract class Annotation {

  public abstract String text();

  @Value.Default
  public Color color() {
    return Color.lightGray;
  }

  @Value.Default
  public Font font() {
    return new Font("Helvetica", Font.PLAIN, 12);
  }

  @Value.Default
  public Keyword verticalAlignment() {
    return Keyword.ALIGN_CENTER;
  }

  @Value.Default
  public Keyword horizontalAlignment() {
    return Keyword.ALIGN_CENTER;
  }

}
