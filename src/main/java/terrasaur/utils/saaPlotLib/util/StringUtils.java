package terrasaur.utils.saaPlotLib.util;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class StringUtils {

  /**
   * @param g Graphics2D
   * @param text text to draw
   * @param x x pixel coordinate
   * @param y y pixel coordinate
   * @return the X, Y coordinates to draw the string with its desired alignment at (x, y)
   */
  public static Point2D.Double stringCoordinates(
      Graphics2D g, String text, double x, double y, Keyword vert, Keyword horiz) {

    Rectangle2D r = boundingBox(g, text);
    double width = r.getWidth();
    double height = r.getHeight();

    double widthOffset = 0;
    double heightOffset = 0;

    switch (vert) {
      case ALIGN_CENTER:
        heightOffset = height / 2;
        break;
      case ALIGN_TOP:
        heightOffset = height;
        break;
      case ALIGN_BOTTOM:
      default:
    }

    switch (horiz) {
      case ALIGN_CENTER:
        widthOffset = -width / 2;
        break;
      case ALIGN_RIGHT:
        widthOffset = -width;
        break;
      case ALIGN_LEFT:
      default:
    }

    return new Point2D.Double(x + widthOffset, y + heightOffset);
  }

  public static double stringWidth(Graphics2D g, String text) {
    return boundingBox(g, text).getWidth();
  }

  public static double stringHeight(Graphics2D g, String text) {
    return boundingBox(g, text).getHeight();
  }

  public static Rectangle2D boundingBox(Graphics2D g, String text) {
    FontRenderContext frc = g.getFontRenderContext();
    GlyphVector v = g.getFont().createGlyphVector(frc, text);
    return v.getVisualBounds();
  }

  /**
   * @param g Graphics2D
   * @param text text to draw
   * @param x x pixel value at lower left
   * @param y y pixel value at lower left
   */
  public static void drawOutlinedString(Graphics2D g, String text, double x, double y) {
    g.drawString(text, (int) (x + 0.5), (int) (y + 0.5));
    Rectangle2D r = boundingBox(g, text);

    //    logger.debug("x, y, box w, h: {} {} {} {}", x, y, r.getWidth(), r.getHeight());

    Path2D.Double path = new Path2D.Double();
    path.moveTo(x, y);
    path.lineTo(x + r.getWidth(), y);
    path.lineTo(x + r.getWidth(), y - r.getHeight());
    path.lineTo(x, y - r.getHeight());
    path.closePath();
    g.draw(path);
  }
}
