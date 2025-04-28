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
package terrasaur.utils.saaPlotLib.canvas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import terrasaur.utils.saaPlotLib.canvas.symbol.Symbol;
import terrasaur.utils.saaPlotLib.colorMaps.ColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotation;
import terrasaur.utils.saaPlotLib.util.Keyword;
import terrasaur.utils.saaPlotLib.util.LegendEntry;
import terrasaur.utils.saaPlotLib.util.StringUtils;

/**
 * 0,0 is at top left
 *
 * @author nairah1
 */
public abstract class PlotCanvas {
  protected final BufferedImage image;
  protected final int width, height;
  protected final int pageWidth, pageHeight;
  protected final PlotConfig config;
  protected List<LegendEntry> legend;

  /**
   * @return width of the plot area. Does not include margins.
   */
  public int getWidth() {
    return width;
  }

  /**
   * @return height of the plot area. Does not include margins.
   */
  public int getHeight() {
    return height;
  }

  /**
   * @return width of the entire page, including left and right margins.
   */
  public int getPageWidth() {
    return pageWidth;
  }

  /**
   * @return height of the entire page, including top and bottom margins.
   */
  public int getPageHeight() {
    return pageHeight;
  }

  public PlotCanvas(PlotConfig config) {
    super();
    this.config = config;
    this.legend = new ArrayList<>();

    this.width = config.width();
    this.height = config.height();
    this.pageWidth = config.width() + config.leftMargin() + config.rightMargin();
    this.pageHeight = config.height() + config.topMargin() + config.bottomMargin();
    this.image = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g = image.createGraphics();
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
    g.setComposite(AlphaComposite.Src);
    g.setColor(config.backgroundColor());
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
  }

  /**
   * @return the current plot as a {@link BufferedImage}
   */
  public BufferedImage getImage() {
    return image;
  }

  /**
   * open a JFrame containing the supplied {@link BufferedImage}
   *
   * @param image image to display
   */
  public static void showJFrame(BufferedImage image) {
    JFrame frame = new JFrame();
    JPanel panel = new JPanel();
    panel.add(new JLabel(new ImageIcon(image)));

    JScrollPane pane = new JScrollPane(panel);
    frame.setContentPane(pane);
    frame.pack();
    frame.setVisible(true);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  /**
   * open a JFrame with the supplied dimensions containing the supplied {@link BufferedImage}
   *
   * @param image image to display
   * @param width window width
   * @param height window height
   */
  public static void showJFrame(BufferedImage image, int width, int height) {
    BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = resizedImg.createGraphics();

    g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(image, 0, 0, width, height, null);
    g2.dispose();

    showJFrame(resizedImg);
  }

  /**
   * Write the image to file (format determined by extension)
   *
   * @param filename filename to write
   * @param image image to write
   */
  public static void writeImage(String filename, BufferedImage image) {
    File imageFile = new File(filename);
    try {
      ImageIO.write(image, filename.substring(filename.lastIndexOf(".") + 1), imageFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Write the image to file (format determined by extension)
   *
   * @param filename filename to write
   */
  public void writeImage(String filename) {
    writeImage(filename, image);
  }

  /**
   * Write aligned text onto a graphics2D
   *
   * @param g Graphics2D object
   * @param text text to write
   * @param x X coordinate of the aligned point
   * @param y Y coordinate of the aligned point
   * @param vert {@link Keyword#ALIGN_TOP}, {@link Keyword#ALIGN_CENTER}, or {@link
   *     Keyword#ALIGN_BOTTOM}
   * @param horiz {@link Keyword#ALIGN_LEFT}, {@link Keyword#ALIGN_CENTER}, or {@link
   *     Keyword#ALIGN_RIGHT}
   */
  public void addAnnotation(
      Graphics2D g, String text, double x, double y, Keyword vert, Keyword horiz) {
    for (String line : text.split("\n")) {
      Point2D.Double coords = StringUtils.stringCoordinates(g, line, x, y, vert, horiz);
      // StringUtils.drawOutlinedString(g, line, (int) (coords.getX() + 0.5), (int) (coords.getY() +
      // 0.5));
      g.drawString(line, (int) (coords.getX() + 0.5), (int) (coords.getY() + 0.5));
      y += g.getFontMetrics().getHeight();
    }
  }

  /**
   * @param g Graphics2D object
   * @param text text to write
   * @param x X coordinate of the aligned point
   * @param y Y coordinate of the aligned point
   * @param rotate angle to rotate text
   */
  public void addAnnotation(Graphics2D g, String text, double x, double y, double rotate) {
    g.translate(x, y);
    g.rotate(rotate);
    addAnnotation(g, text, 0, 0, Keyword.ALIGN_CENTER, Keyword.ALIGN_CENTER);
    g.rotate(-rotate);
    g.translate(-x, -y);
  }

  /**
   * @param a Annotation
   * @param x x coordinate. See {@link Annotation#horizontalAlignment()}
   * @param y y coordinate.See {@link Annotation#verticalAlignment()}
   * @param rotate rotation in radians
   */
  public void addAnnotation(Annotation a, double x, double y, double rotate) {
    Graphics2D g = image.createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setColor(a.color());
    g.setFont(a.font());
    g.translate(x, y);
    g.rotate(rotate);
    addAnnotation(g, a.text(), 0, 0, a.verticalAlignment(), a.horizontalAlignment());
  }

  /**
   * Set pixel at (x,y) to color
   *
   * @param x pixel x value
   * @param y pixel y value
   * @param color color
   */
  public void setPixel(double x, double y, Color color) {
    int pixelX = (int) (x + 0.5);
    int pixelY = (int) (y + 0.5);
    if (pixelX >= 0 && pixelX < image.getWidth() && pixelY >= 0 && pixelY < image.getHeight())
      image.setRGB(pixelX, pixelY, color.getRGB());
  }

  /**
   * Set pixel at (x,y) to a shade of gray. Bilinear interpolation is used to spread the pixel over
   * a 2x2 square.
   *
   * <pre>
   * x axis passes through 0 and 2
   * y axis passes through 0 and 1
   *
   * Given a point (x, y), compute the area weighting of each pixel.
   *
   * --- ---
   * | 0 | 1 |
   * --- ---
   * | 2 | 3 |
   * --- ---
   *
   * </pre>
   *
   * Weights are from Numerical Recipes, 2nd Edition<br>
   * weight[0] = (1 - t) * u;<br>
   * weight[1] = t * u;<br>
   * weight[2] = (1-t) * (1-u);<br>
   * weight[3] = t * (1-u);<br>
   *
   * <p>
   *
   * @param x pixel x value
   * @param y pixel y value
   * @param brightness pixel value, must be >0, but can be more than 255
   * @param whiteBackground true if plotting against a white background (brightness of 255 maps to
   *     zero). If false, higher weighted square will be lighter (brightness of 255 stays 255).
   */
  public void setPixel(double x, double y, int brightness, boolean whiteBackground) {

    double t = x - Math.floor(x);
    double u = 1 - (y - Math.floor(y));
    double[] weights = new double[4];
    weights[1] = t * u;
    weights[0] = u - weights[1];
    weights[2] = 1 - t - u + weights[1];
    weights[3] = t - weights[1];

    Map<Point2D, Double> weightMap = new HashMap<>();

    int x0 = (int) x;
    int x1 = x0 + 1;
    int y0 = (int) y;
    int y1 = y0 + 1;

    boolean x0Good = x0 >= 0 && x0 < image.getWidth();
    boolean x1Good = x1 >= 0 && x1 < image.getWidth();

    boolean y0Good = y0 >= 0 && y0 < image.getHeight();
    boolean y1Good = y1 >= 0 && y1 < image.getHeight();

    if (x0Good && y0Good) weightMap.put(new Point2D.Float(x0, y0), weights[0]);
    if (x1Good && y0Good) weightMap.put(new Point2D.Float(x1, y0), weights[1]);
    if (x0Good && y1Good) weightMap.put(new Point2D.Float(x0, y1), weights[2]);
    if (x1Good && y1Good) weightMap.put(new Point2D.Float(x1, y1), weights[3]);

    for (Point2D pair : weightMap.keySet()) {
      float b = Math.min(255, weightMap.get(pair).floatValue() * brightness);
      if (whiteBackground) b = 255 - b;
      Color c = Color.getHSBColor(0, 0, b / 255);
      image.setRGB((int) pair.getX(), (int) pair.getY(), c.getRGB());
    }
  }

  /**
   * Add an entry to the legend.
   *
   * @param entry Legend entry
   */
  public void addToLegend(LegendEntry entry) {
    legend.add(entry);
  }

  public void clearLegend() {
    legend = new ArrayList<>();
  }

  /** Draw the legend. */
  public void drawLegend() {

    if (!legend.isEmpty()) {
      double ulx = config.legendPosition().getX();
      double boxWidth = 0;
      double lry = config.legendPosition().getY();
      Graphics2D g = getImage().createGraphics();
      configureHintsForSubpixelQuality(g);
      g.setFont(config.legendFont());

      double maxTextWidth = -Double.MAX_VALUE;
      double maxSymbolWidth = -Double.MAX_VALUE;
      for (LegendEntry entry : legend) {
        String text = entry.name();
        lry += 1.5 * StringUtils.stringHeight(g, text);
        double textWidth = StringUtils.stringWidth(g, String.format(" %s ", text));
        double symbolWidth = entry.symbol().isEmpty() ? 0 : entry.symbol().get().getSize();

        if (entry.stroke().isPresent()) {
          symbolWidth = Math.max(5, symbolWidth);
          textWidth += 5;
        }

        maxTextWidth = Math.max(textWidth, maxTextWidth);
        maxSymbolWidth = Math.max(symbolWidth, maxSymbolWidth);

        boxWidth = Math.max(maxTextWidth + 2 * maxSymbolWidth, boxWidth);
      }

      double uly = config.legendPosition().getY();
      double boxHeight = lry - uly;
      double charWidth = StringUtils.stringWidth(g, "a");
      double charHeight = StringUtils.stringHeight(g, "a");

      g.setColor(config.backgroundColor());
      g.fill(
          new Rectangle2D.Double(
              ulx - charWidth,
              uly - charHeight,
              boxWidth + 2 * charWidth,
              boxHeight + 0.5 * charHeight));
      g.setColor(Color.BLACK);
      g.draw(
          new Rectangle2D.Double(
              ulx - charWidth,
              uly - charHeight,
              boxWidth + 2 * charWidth,
              boxHeight + 0.5 * charHeight));

      lry = config.legendPosition().getY();
      for (LegendEntry entry : legend) {
        String text = entry.name();
        g.setColor(entry.color());
        double legendX = config.legendPosition().getX();
        if (entry.symbol().isPresent()) {
          Symbol symbol = entry.symbol().get();
          legendX += 0.5 * maxSymbolWidth;
          symbol.draw(g, legendX, lry);
          legendX += 1.5 * maxSymbolWidth;
        }
        if (entry.stroke().isPresent()) {
          g.setStroke(entry.stroke().get());
          GeneralPath gp = new GeneralPath();
          gp.moveTo(legendX, lry);
          legendX += 5;
          gp.lineTo(legendX, lry);
          legendX += 5;
          g.setColor(entry.color());
          g.draw(gp);
        }

        g.setColor(Color.BLACK);
        if (config.legendOutline()) {
          addAnnotation(g, text, legendX + 1, lry, Keyword.ALIGN_CENTER, Keyword.ALIGN_LEFT);
          addAnnotation(g, text, legendX - 1, lry, Keyword.ALIGN_CENTER, Keyword.ALIGN_LEFT);
          addAnnotation(g, text, legendX, lry + 1, Keyword.ALIGN_CENTER, Keyword.ALIGN_LEFT);
          addAnnotation(g, text, legendX, lry - 1, Keyword.ALIGN_CENTER, Keyword.ALIGN_LEFT);
        }

        if (config.legendColor()) g.setColor(entry.color());

        addAnnotation(g, text, legendX, lry, Keyword.ALIGN_CENTER, Keyword.ALIGN_LEFT);

        lry += 1.5 * StringUtils.stringHeight(g, text);
        g.setColor(Color.BLACK);
      }
    }
  }

  /**
   * Taken from crucible.mantle.contacts.GraphicsUtils
   *
   * <p>Configure {@link RenderingHints} on a {@link Graphics2D} object to render with sub-pixel
   * accuracy enabled.
   *
   * <p>This method encapsulates the set of rendering hints we have found largely through trial and
   * error that yield proper results across multiple platforms. Platform to platform so of the hints
   * configured here may be unnecessary, but given the platform and API dependent nature of these
   * values it seems an appropriate route to take.
   *
   * @param graphics the graphics context to adjust the rendering hints
   * @return the original hints, which can be restored with {@link
   *     Graphics2D#setRenderingHints(Map)}
   */
  public static RenderingHints configureHintsForSubpixelQuality(Graphics2D graphics) {

    RenderingHints original = graphics.getRenderingHints();

    /*
     * Enable quality rendering hint, as that is what we are chasing.
     */
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    /*
     * Turn on Anti-Aliasing, as this is necessary for rendering subpixel stuff accurately and
     * smoothly.
     */
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    /*
     * Grant found that enabling pure stroke mode gives proper subpixel stroking, necessary for
     * rendering shapes and other 2D API objects accurately.
     */
    graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    graphics.setRenderingHint(
        RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

    /*
     * Turn on the alpha interpolation to quality, as we are rendering transparent images.
     */
    graphics.setRenderingHint(
        RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

    /*
     * Improve the quality of output fonts.
     */
    graphics.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

    return original;
  }

  /**
   * Draw a color bar.
   *
   * @param colorBar color bar
   */
  public void drawColorBar(ColorBar colorBar) {

    Rectangle rect = colorBar.rect();
    ColorRamp ramp = colorBar.ramp();
    Function<Double, String> tickFunction = colorBar.tickFunction();
    int nTicks = colorBar.numTicks();

    final boolean horizontal = rect.getWidth() > rect.getHeight();

    Graphics2D g = getImage().createGraphics();
    configureHintsForSubpixelQuality(g);
    g.setFont(config.axisFont());
    g.setColor(Color.BLACK);
    g.draw(rect);

    if (horizontal) {
      if (!ramp.title().isEmpty()) {
        addAnnotation(
            g,
            ramp.title(),
            rect.getWidth() / 2 + rect.getX(),
            rect.y - 10,
            Keyword.ALIGN_BOTTOM,
            Keyword.ALIGN_CENTER);
      }

      double tickSpacing = (rect.getWidth() / (nTicks - 1.));
      int pixelX;
      int pixelY = rect.y + rect.height;

      for (double i = 0; i < rect.getWidth(); i += tickSpacing) {
        double frac = i / rect.getWidth();
        double value = frac * (ramp.max() - ramp.min()) + ramp.min();
        if (colorBar.log()) value = Math.pow(10, value);

        pixelX = (int) (rect.x + i + 0.5);
        g.drawLine(pixelX, pixelY, pixelX, pixelY + 5);
        addAnnotation(
            g,
            tickFunction.apply(value),
            pixelX,
            pixelY + 10,
            Keyword.ALIGN_TOP,
            Keyword.ALIGN_CENTER);
      }
      pixelX = rect.x + rect.width;
      g.drawLine(pixelX, pixelY, pixelX, pixelY + 5);

      addAnnotation(
          g,
          tickFunction.apply(colorBar.log() ? Math.pow(10, ramp.max()) : ramp.max()),
          pixelX,
          pixelY + 10,
          Keyword.ALIGN_TOP,
          Keyword.ALIGN_CENTER);

      for (double i = 0; i < rect.getWidth(); i++) {
        double frac = i / (rect.getWidth() - 1);
        double value = frac * (ramp.max() - ramp.min()) + ramp.min();
        pixelX = (int) (rect.x + i + 0.5);
        Color color = ramp.getColor(value);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        g.drawLine(pixelX, rect.y, pixelX, rect.y + rect.height);
      }
    } else {

      if (!ramp.title().isEmpty()) {
        g.setColor(Color.BLACK);
        // rotate axis label
        double x = rect.x - 10;
        double y = rect.getHeight() / 2 + rect.getY();
        g.translate(x, y);
        g.rotate(-Math.PI / 2);
        addAnnotation(g, ramp.title(), 0, 0, Keyword.ALIGN_BOTTOM, Keyword.ALIGN_CENTER);
        g.rotate(Math.PI / 2);
        g.translate(-x, -y);
      }

      double tickSpacing = (rect.getHeight() / (nTicks - 1.));
      int pixelY;
      int pixelX = rect.x + rect.width;
      for (double i = 0; i < rect.getHeight(); i += tickSpacing) {
        double frac = 1 - i / rect.getHeight();
        double value = frac * (ramp.max() - ramp.min()) + ramp.min();
        if (colorBar.log()) value = Math.pow(10, value);

        pixelY = (int) (rect.y + i + 0.5);
        g.drawLine(pixelX, pixelY, pixelX + 5, pixelY);
        addAnnotation(
            g,
            tickFunction.apply(value),
            pixelX + 10,
            pixelY,
            Keyword.ALIGN_CENTER,
            Keyword.ALIGN_LEFT);
      }
      pixelY = rect.y + rect.height;
      g.drawLine(pixelX, pixelY, pixelX + 5, pixelY);
      addAnnotation(
          g,
          tickFunction.apply(colorBar.log() ? Math.pow(10, ramp.min()) : ramp.min()),
          pixelX + 10,
          pixelY,
          Keyword.ALIGN_CENTER,
          Keyword.ALIGN_LEFT);

      for (double i = 0; i < rect.getHeight(); i++) {
        double frac = 1 - i / (rect.getHeight() - 1);
        double value = frac * (ramp.max() - ramp.min()) + ramp.min();
        pixelY = (int) (rect.y + i + 0.5);
        Color color = ramp.getColor(value);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        g.drawLine(rect.x, pixelY, rect.x + rect.width, pixelY);
      }
    }
  }
}
