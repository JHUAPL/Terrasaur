/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package terrasaur.utils.saaPlotLib.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Random;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;

public class PlotUtils {

  /**
   * Return a value greater than number with the desired number of significant digits. For example,
   * if the number is 8803.364:
   *
   * <p>
   *
   * <table>
   * <thead>
   * <tr>
   * <td>digits</td>
   * <td>roundUpper</td>
   * </tr>
   * </thead>
   * <tr>
   * <td>0</td>
   * <td>10000</td>
   * </tr>
   * <tr>
   * <td>1</td>
   * <td>9000</td>
   * </tr>
   * <tr>
   * <td>2</td>
   * <td>8900</td>
   * </tr>
   * <tr>
   * <td>3</td>
   * <td>8810</td>
   * </tr>
   * </table>
   *
   * @param number number to round
   * @param digits number of significant digits
   * @return number rounded up
   */
  public static double getRoundCeiling(double number, int digits) {
    int sign = (int) Math.signum(number);
    if (sign == 0) return sign;

    number = Math.abs(number);
    if (sign > 0) {
      int exponent = (int) Math.floor(Math.log10(number) - digits + 1);
      int mantissa = (int) Math.ceil(Math.pow(10, Math.log10(number) - exponent));
      return sign * mantissa * Math.pow(10, exponent);
    } else {
      return sign * getRoundFloor(number, digits);
    }
  }

  /**
   * Return a value less than number with the desired number of significant digits. For example, if
   * the number is 8803.364:
   *
   * <p>
   *
   * <table>
   * <thead>
   * <tr>
   * <td>digits</td>
   * <td>roundUpper</td>
   * </tr>
   * </thead>
   * <tr>
   * <td>0</td>
   * <td>0</td>
   * </tr>
   * <tr>
   * <td>1</td>
   * <td>8000</td>
   * </tr>
   * <tr>
   * <td>2</td>
   * <td>8800</td>
   * </tr>
   * <tr>
   * <td>3</td>
   * <td>8800</td>
   * </tr>
   * </table>
   *
   * @param number number to round
   * @param digits number of significant digits
   * @return number rounded down
   */
  public static double getRoundFloor(double number, int digits) {
    int sign = (int) Math.signum(number);
    if (sign == 0) return sign;

    number = Math.abs(number);
    if (sign > 0) {
      int exponent = (int) Math.floor(Math.log10(number) - digits + 1);
      int mantissa = (int) Math.floor(Math.pow(10, Math.log10(number) - exponent));
      return sign * mantissa * Math.pow(10, exponent);
    } else {
      return sign * getRoundCeiling(number, digits);
    }
  }

  /**
   * Write the current time/date at the lower left. For example, "Created Mon Dec 28 16:07:04 EST
   * 2020"
   *
   * @param image image to annotate
   */
  public static void addCreationDate(BufferedImage image) {
    addCreationDate(image, "Created %s");
  }

  /**
   * Write the current time/date at the lower left. For example, "Created Mon Dec 28 16:07:04 EST
   * 2020"
   *
   * @param image image to annotate
   * @param format %s can be used to include {@link Date#toString()}
   */
  public static void addCreationDate(BufferedImage image, String format) {
    Graphics2D g = image.createGraphics();
    PlotCanvas.configureHintsForSubpixelQuality(g);
    g.setColor(Color.BLACK);

    String date = String.format(format, new Date());
    Point2D.Double coords =
        StringUtils.stringCoordinates(
            g,
            date,
            image.getWidth() - 10,
            image.getHeight() - 10,
            Keyword.ALIGN_BOTTOM,
            Keyword.ALIGN_RIGHT);
    g.drawString(date, (int) (coords.getX() + 0.5), (int) (coords.getY() + 0.5));
  }

  /**
   * Write a string in the lower left.
   *
   * @param image image to annotate
   * @param text to write
   */
  public static void addAnnotation(BufferedImage image, String text) {
    Graphics2D g = image.createGraphics();
    PlotCanvas.configureHintsForSubpixelQuality(g);
    g.setColor(Color.BLACK);

    if (text.trim().isEmpty()) return;

    String[] lines = text.split("\n");

    Keyword vert = Keyword.ALIGN_BOTTOM;
    Keyword horiz = Keyword.ALIGN_RIGHT;

    double x = image.getWidth() - 10;
    double y = image.getHeight() - 10;

    for (int i = lines.length - 1; i >= 0; i--) {
      String line = lines[i];
      Point2D.Double coords = StringUtils.stringCoordinates(g, line, x, y, vert, horiz);
      g.drawString(line, (int) (coords.getX() + 0.5), (int) (coords.getY() + 0.5));
      y -= g.getFontMetrics().getHeight();
    }
  }

  /**
   * @param src source image
   * @param ratio less than 1 to create a smaller image, greater than 1 for a larger image
   * @return scaled image
   */
  public static BufferedImage createThumbnail(BufferedImage src, double ratio) {
    int w = (int) (src.getWidth() * ratio);
    int h = (int) (src.getHeight() * ratio);
    BufferedImage dst = new BufferedImage(w, h, src.getType());
    dst.createGraphics().drawImage(src.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
    return dst;
  }

  public static void main(String[] args) {
    Random r = new Random();
    double base = r.nextDouble();
    int exponent = r.nextInt(6);
    for (int i = 0; i < 10; i++) {
      double number = base * Math.pow(10, exponent);
      System.out.printf(
          "%2d %f %f %f ", i, number, getRoundFloor(number, i), getRoundCeiling(number, i));
      System.out.printf(
          "%f %f %f\n", -number, getRoundFloor(-number, i), getRoundCeiling(-number, i));
    }
  }
}
