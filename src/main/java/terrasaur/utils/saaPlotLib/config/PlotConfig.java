package terrasaur.utils.saaPlotLib.config;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import org.immutables.value.Value;

/**
 * Create a default configuration:
 *
 * <pre>
 * PlotConfig config = ImmutablePlotConfig.builder().build();
 * </pre>
 *
 * @author Hari.Nair@jhuapl.edu
 */
@Value.Immutable
public abstract class PlotConfig {

  /** Font used for axes and color bar */
  @Value.Default
  public Font axisFont() {
    return new Font("Times New Roman", Font.BOLD, 16);
  }

  @Value.Default
  public Color backgroundColor() {
    return Color.WHITE;
  }

  @Value.Default
  public int bottomMargin() {
    return 80;
  }

  /**
   * @return y coordinate of the bottom plot edge. (0, 0) is at the upper left. This is {@link
   *     #topMargin()}+{@link #height()}.
   */
  public int getBottomPlotEdge() {
    return topMargin() + height();
  }

  /**
   * @return x coordinate of the left plot edge. (0, 0) is at the upper left. This is the same
   *     as{@link #leftMargin()}.
   */
  public int getLeftPlotEdge() {
    return leftMargin();
  }

  /**
   * @return x coordinate of the right plot edge. (0, 0) is at the upper left. This is the same
   *     as{@link #leftMargin()} + {@link #width()}.
   */
  public int getRightPlotEdge() {
    return leftMargin() + width();
  }

  /**
   * @return y coordinate of the top plot edge. (0, 0) is at the upper left. This is the same as
   *     {@link #topMargin()}.
   */
  public int getTopPlotEdge() {
    return topMargin();
  }

  @Value.Default
  public Color gridColor() {
    return Color.lightGray;
  }

  @Value.Default
  public Font gridFont() {
    return new Font("Helvetica", Font.BOLD, 12);
  }

  /**
   * @return height of the plot area. Does not include margins.
   */
  @Value.Default
  public int height() {
    return 900;
  }

  @Value.Default
  public int leftMargin() {
    return 120;
  }

  /** If true, write each legend entry's name in the color of its dataset */
  @Value.Default
  public boolean legendColor() {
    return false;
  }

  @Value.Default
  public Font legendFont() {
    return new Font("Helvetica", Font.BOLD, 12);
  }

  /** If true, draw a black outline around the characters in the name of each legend entry */
  @Value.Default
  public boolean legendOutline() {
    return false;
  }

  @Value.Default
  public Point2D.Double legendPosition() {
    double legendX = width() - 2 * rightMargin();
    double legendY = 1.5 * topMargin();
    return new Point2D.Double(legendX, legendY);
  }

  @Value.Default
  public int rightMargin() {
    return 80;
  }

  @Value.Default
  public String title() {
    return "";
  }

  @Value.Default
  public Font titleFont() {
    return new Font("Times New Roman", Font.BOLD, 24);
  }

  @Value.Default
  public int topMargin() {
    return 80;
  }

  /**
   * @return width of the plot area. Does not include margins.
   */
  @Value.Default
  public int width() {
    return 1200;
  }

  @Value.Default
  public double xMajorTickLength() {
    int minDimension = Math.min(width(), height());
    return 0.02 * minDimension;
  }

  @Value.Default
  public double xMinorTickLength() {
    return 0.5 * xMajorTickLength();
  }

  @Value.Default
  public double yMajorTickLength() {
    int minDimension = Math.min(width(), height());
    return 0.02 * minDimension;
  }

  @Value.Default
  public double yMinorTickLength() {
    return 0.5 * yMajorTickLength();
  }
}
