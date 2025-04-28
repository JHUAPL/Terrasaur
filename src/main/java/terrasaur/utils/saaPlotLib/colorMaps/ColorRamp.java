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
package terrasaur.utils.saaPlotLib.colorMaps;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.immutables.value.Value;
import terrasaur.utils.saaPlotLib.colorMaps.tables.ColorTable;
import terrasaur.utils.saaPlotLib.colorMaps.tables.ColorTable.FAMILY;
import terrasaur.utils.saaPlotLib.colorMaps.tables.Colorcet;
import terrasaur.utils.saaPlotLib.colorMaps.tables.IDL;
import terrasaur.utils.saaPlotLib.colorMaps.tables.MATLAB;
import terrasaur.utils.saaPlotLib.colorMaps.tables.Matplotlib;
import terrasaur.utils.saaPlotLib.colorMaps.tables.ScientificColourMaps6;

/**
 * COLORCET color maps are from <a href="https://colorcet.holoviz.org/">https://colorcet.holoviz.org</a><br>
 * SCM6 color maps are from <a href="http://www.fabiocrameri.ch/colourmaps.php">http://www.fabiocrameri.ch/colourmaps.php</a>
 *
 * @author nairah1
 */
@Value.Immutable
public abstract class ColorRamp {

  private static final int NUM_COLORS = 256;

  /**
   * @param type ColorRamp type
   * @param min minimum value
   * @param max maximum value
   * @return ColorRamp
   */
  public static ColorRamp create(TYPE type, double min, double max) {
    return ImmutableColorRamp.builder().min(min).max(max).colors(type.getColors()).build();
  }

  /**
   *
   * @param min minimum value
   * @param minColor color mapped to minimum
   * @param midColor color mapped to middle
   * @param max maximum value
   * @param maxColor color mapped to maximum
   * @return Color ramp linearly interpolated between minColor and midColor, and midColor and
   *    * maxColor.
   */
  public static ColorRamp createBilinear(
      double min, Color minColor, Color midColor, double max, Color maxColor) {

    int minRed = minColor.getRed();
    int redRange = midColor.getRed() - minColor.getRed();
    int minGreen = minColor.getGreen();
    int greenRange = midColor.getGreen() - minColor.getGreen();
    int minBlue = minColor.getBlue();
    int blueRange = midColor.getBlue() - minColor.getBlue();

    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS / 2; i++) {

      double frac = ((double) i) / (NUM_COLORS / 2. - 1);

      int red = (int) (minRed + frac * redRange);
      int green = (int) (minGreen + frac * greenRange);
      int blue = (int) (minBlue + frac * blueRange);

      colors.add(new Color(red, green, blue));
    }

    minRed = midColor.getRed();
    redRange = maxColor.getRed() - midColor.getRed();
    minGreen = midColor.getGreen();
    greenRange = maxColor.getGreen() - midColor.getGreen();
    minBlue = midColor.getBlue();
    blueRange = maxColor.getBlue() - midColor.getBlue();

    for (int i = 0; i < NUM_COLORS / 2; i++) {

      double frac = ((double) i) / (NUM_COLORS / 2. - 1);

      int red = (int) (minRed + frac * redRange);
      int green = (int) (minGreen + frac * greenRange);
      int blue = (int) (minBlue + frac * blueRange);

      colors.add(new Color(red, green, blue));
    }
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   *
   * @param min minimum
   * @param minColor color mapped to minimum
   * @param max maximum
   * @param maxColor color mapped to maximum
   * @return divergent color map, based on <a href=
   *     "https://www.kennethmoreland.com/color-maps/">https://www.kennethmoreland.com/color-maps</a>
   */
  public static ColorRamp createDivergent(double min, Color minColor, double max, Color maxColor) {
    DivergentColorRamp dcr = new DivergentColorRamp();
    return ImmutableColorRamp.builder()
        .min(min)
        .max(max)
        .colors(dcr.generateColorMap(minColor, maxColor, NUM_COLORS))
        .build();
  }

  /**
   * Assign ColorRamp from an array of ints where red is in bits 16-23, green is in bits 8-15, and
   * blue is in bits 0-7.
   *
   * @param min minimum
   * @param max maximum
   * @param values integer values
   * @return color ramp
   */
  public static ColorRamp createFromInt(double min, double max, int[] values) {
    List<Color> colors = new ArrayList<>();
      for (int value : values) colors.add(new Color(value));
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   *
   * @param min minimum
   * @param max maximum
   * @param r red
   * @param g green
   * @param b blue
   * @return colormap from arrays of red, green, and blue values.  Each color array must have 256 values.
   */
  public static ColorRamp createFromRGB(double min, double max, int[] r, int[] g, int[] b) {
    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS; i++) colors.add(new Color(r[i], g[i], b[i]));
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   * @param min minimum
   * @param max maximum
   * @return a greyscale ramp with the min value black and the max value white.
   */
  public static ColorRamp createGreyscale(double min, double max) {
    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS; i++) {
      colors.add(new Color(i, i, i));
    }
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   * @param min minimum
   * @param max maximum
   * @return a circular color ramp running from hue 0 to 360 with saturation and brightness set to
   *     1.
   */
  public static ColorRamp createHue(double min, double max) {
    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS; i++) {
      float h = ((float) i) / (NUM_COLORS - 1);

      colors.add(Color.getHSBColor(h, 1.0f, 1.0f));
    }
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   *
   * @param min minimum
   * @param minColor color mapped to minimum
   * @param max maximum
   * @param maxColor color mapped to maximum
   * @return color ramp linearly interpolated between minColor and maxColor.
   */
  public static ColorRamp createLinear(double min, Color minColor, double max, Color maxColor) {

    final int minRed = minColor.getRed();
    final int redRange = maxColor.getRed() - minColor.getRed();
    final int minGreen = minColor.getGreen();
    final int greenRange = maxColor.getGreen() - minColor.getGreen();
    final int minBlue = minColor.getBlue();
    final int blueRange = maxColor.getBlue() - minColor.getBlue();

    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS; i++) {

      double frac = ((double) i) / (NUM_COLORS - 1);

      int red = (int) (minRed + frac * redRange);
      int green = (int) (minGreen + frac * greenRange);
      int blue = (int) (minBlue + frac * blueRange);

      colors.add(new Color(red, green, blue));
    }

    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  /**
   * @param min minimum
   * @param max maximum
   * @return a linear color ramp running from hue 240 (blue) to hue 0 degrees (red) with saturation
   *     and brightness set to 1.
   */
  public static ColorRamp createLinear(double min, double max) {
    List<Color> colors = new ArrayList<>();
    for (int i = 0; i < NUM_COLORS; i++) {
      float h = (float) (2. / 3. * (1. - i / (NUM_COLORS - 1.)));
      float s = 1.0f;
      float b = 1.0f;
      colors.add(Color.getHSBColor(h, s, b));
    }
    return ImmutableColorRamp.builder().min(min).max(max).colors(colors).build();
  }

  public abstract List<Color> colors();

  /** set the lower limit color to black and the upper limit color to white. */
  @Value.Default
  public boolean limitColors() {
    return false;
  }

  public abstract double max();

  public abstract double min();

  @Value.Default
  public String title() {
    return "";
  }

  /**
   * @return a ramp with the lower limit color set to black and the upper limit color to white.
   */
  public ColorRamp addLimitColors() {
    return ImmutableColorRamp.builder().from(this).limitColors(true).build();
  }

  public ColorRamp addTitle(String title) {
    return ImmutableColorRamp.builder().from(this).title(title).build();
  }

  /**
   * @return a color ramp that is the reverse of this one
   */
  public ColorRamp createReverse() {
      List<Color> newColors = new ArrayList<>(colors());
    Collections.reverse(newColors);
    return ImmutableColorRamp.builder().from(this).colors(newColors).build();
  }

  /**
   *
   * @param value input value
   * @return input value mapped to a color. If Double.isFinite(value) is false a transparent Color is
   *    returned.
   */
  public Color getColor(double value) {

    if (Double.isFinite(value)) {
      double frac = (value - min()) / (max() - min());
      if (frac < 0) {
        if (limitColors()) return Color.BLACK;
        frac = 0;
      }
      if (frac > 1) {
        if (limitColors()) return Color.WHITE;
        frac = 1;
      }
      frac *= colors().size();
      return colors().get(Math.min((int) frac, colors().size() - 1));
    } else {
      // transparent
      return new Color(0, 0, 0, 0);
    }
  }

  public enum TYPE {
    BATLOW(ScientificColourMaps6.BATLOW),
    BERLIN(ScientificColourMaps6.BERLIN),
    BGY(Colorcet.BGY),
    BGYW(Colorcet.BGYW),
    BJY(Colorcet.BJY),
    BKR(Colorcet.BKR),
    BKY(Colorcet.BKY),
    BLUES(Colorcet.BLUES),
    BMW(Colorcet.BMW),
    BMY(Colorcet.BMY),
    BROCO(ScientificColourMaps6.BROCO),
    BWY(Colorcet.BWY),

    CBSPECTRAL(IDL.CBSPECTRAL),
    COLORWHEEL(Colorcet.COLORWHEEL),
    COOLWARM(Colorcet.COOLWARM),
    CORKO(ScientificColourMaps6.CORKO),
    CWR(Colorcet.CWR),

    DIMGRAY(Colorcet.DIMGRAY),

    FIRE(Colorcet.FIRE),

    GLASBEY(Colorcet.GLASBEY),
    GLASBEY_DARK(Colorcet.GLASBEY_DARK),
    GLASBEY_LIGHT(Colorcet.GLASBEY_LIGHT),
    GRAY(Colorcet.GRAY),
    GWV(Colorcet.GWV),

    ISOLUM(Colorcet.ISOLUM),

    KB(Colorcet.KB),
    KBC(Colorcet.KBC),
    KG(Colorcet.KG),
    KGY(Colorcet.KGY),
    KR(Colorcet.KR),

    OLERON(ScientificColourMaps6.OLERON),

    PARULA(MATLAB.PARULA),

    RAINBOW(Colorcet.RAINBOW),
    ROMA(ScientificColourMaps6.ROMA),
    ROMAO(ScientificColourMaps6.ROMAO),

    VIKO(ScientificColourMaps6.VIKO),
    VIRIDIS(Matplotlib.VIRIDIS);

    private final ColorTable table;

    TYPE(ColorTable table) {
      this.table = table;
    }

    public static Predicate<TYPE> isType(FAMILY type) {
      return t -> t.table.getFamily() == type;
    }

    public List<Color> getColors() {
      return table.getColors();
    }
  }
}
