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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import picante.math.functions.UnivariateFunction;
import picante.math.vectorspace.MatrixIJK;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

/**
 * Create diverging colormaps from RGB1 to RGB2 using the method of Moreland or a simple
 * CIELAB-interpolation.
 * <p>
 * Based on <a href=
 * "https://www.kennethmoreland.com/color-maps/">https://www.kennethmoreland.com/color-maps/</a>
 *
 * @author nairah1
 */
public class DivergentColorRamp {

    /**
     * Reference white-point D65 from Adobe Cookbook
     */
    private static final UnwritableVectorIJK D65 = new UnwritableVectorIJK(95.047, 100.0, 108.883);

    /**
     * Transfer-matrix for the conversion of RGB to XYZ color space
     */
    private static final MatrixIJK transferM = new MatrixIJK(0.4124564, 0.2126729, 0.0193339, 0.3575761, 0.7151522, 0.1191920, 0.1804375, 0.0721750, 0.9503041);

    private static final MatrixIJK transferI = transferM.createInverse();

    public DivergentColorRamp() {

    }

    public static void test(List<Color> colors) {
        double[] rgb = new double[3];
        rgb[0] = colors.get(0).getRed();
        rgb[1] = colors.get(0).getGreen();
        rgb[2] = colors.get(0).getBlue();

        DivergentColorRamp app = new DivergentColorRamp();

        double[] lin = app.rgb2lin(rgb);
        double[] rgb2 = app.lin2rgb(lin);

        System.out.println("RGB->Linear->RGB");
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, rgb[i], lin[i], rgb2[i]);

        System.out.println("RGB->XYZ->RGB");
        double[] xyz = app.rgb2xyz(rgb);
        rgb2 = app.xyz2rgb(xyz);
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, rgb[i], xyz[i], rgb2[i]);

        System.out.println("RGB->LAB->RGB");
        double[] lab = app.rgb2lab(rgb);
        rgb2 = app.lab2rgb(lab);
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, rgb[i], lab[i], rgb2[i]);

        System.out.println("RGB->MSH->RGB");
        double[] msh = app.rgb2msh(rgb);
        rgb2 = app.msh2rgb(msh);
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, rgb[i], msh[i], rgb2[i]);

        double[] color2 = {colors.get(1).getRed(), colors.get(1).getGreen(), colors.get(1).getBlue()};
        System.out.printf("Color 2: %.0f %.0f %.0f\n", color2[0], color2[1], color2[2]);

        System.out.println("RGB2->LAB2->RGB2");
        double[] lab2 = app.rgb2lab(color2);
        rgb2 = app.lab2rgb(lab2);
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, color2[i], lab2[i], rgb2[i]);

        double[] colorN = {colors.get(colors.size() - 1).getRed(), colors.get(colors.size() - 1).getGreen(), colors.get(colors.size() - 1).getBlue()};
        System.out.printf("Color %d: %.0f %.0f %.0f\n", colors.size(), colorN[0], colorN[1], colorN[2]);

        System.out.println("RGBN->LABN->RGBN");
        double[] labN = app.rgb2lab(colorN);
        rgb2 = app.lab2rgb(labN);
        for (int i = 0; i < 3; i++)
            System.out.printf("%d %f %f %f\n", i, colorN[i], colorN[i], colorN[i]);

        double x = lab[0] - lab2[0];
        double y = lab[1] - lab2[1];
        double z = lab[2] - lab2[2];
        double localDE = Math.sqrt(x * x + y * y + z * z) * (colors.size() - 1);
        double startDE = Math.sqrt(x * x + y * y + z * z) * (colors.size() - 1);
        x = lab[0] - labN[0];
        y = lab[1] - labN[1];
        z = lab[2] - labN[2];
        double endDE = Math.sqrt(x * x + y * y + z * z);

        System.out.printf("LocalDE %f startDE %f endDE %f\n", localDE, startDE, endDE);

    }

    /**
     * @param rgb sRGB
     * @return linear RGB
     */
    private double[] rgb2lin(double[] rgb) {
        double[] lin = new double[3];
        for (int i = 0; i < 3; i++) {
            double value = rgb[i] / 255f;
            if (value > 0.04045) value = Math.pow((value + 0.055) / 1.055, 2.4);
            else value /= 12.92;
            lin[i] = value * 100;
        }
        return lin;
    }

    /**
     * @param RGBLinear linear RGB
     * @return sRGB
     */
    private double[] lin2rgb(double[] RGBLinear) {

        double[] sRGB = new double[3];
        for (int i = 0; i < 3; i++) {
            double value = RGBLinear[i] / 100.;
            if (value > 0.00313080495356037152) value = (1.055 * Math.pow(value, 1. / 2.4)) - 0.055;
            else value *= 12.92;

            sRGB[i] = Math.round(value * 255.);
        }

        return sRGB;
    }

    /**
     * @param rgb RGB
     * @return XYZ
     */
    private double[] rgb2xyz(double[] rgb) {

        VectorIJK lin = new VectorIJK(rgb2lin(rgb));
        VectorIJK v = transferM.mxv(lin);

        return new double[]{v.getI(), v.getJ(), v.getK()};
    }

    /**
     * @param xyz XYZ
     * @return RGB
     */
    private double[] xyz2rgb(double[] xyz) {

        VectorIJK v = new VectorIJK(xyz);
        VectorIJK lin = transferI.mxv(v);

        double[] rgbLinear = {lin.getI(), lin.getJ(), lin.getK()};

        return lin2rgb(rgbLinear);
    }

    /**
     * Conversion of RGB to CIELAB
     */
    private double[] rgb2lab(double[] rgb) {
        UnivariateFunction f = t -> {
            double limit = 0.008856;
            if (t > limit) return Math.pow(t, 1. / 3.);
            else return 7.787 * t + 16. / 116.;
        };

        double[] xyz = rgb2xyz(rgb);
        double[] lab = new double[3];
        lab[0] = 116. * (f.evaluate(xyz[1] / D65.getJ()) - (16. / 116.));
        lab[1] = 500. * (f.evaluate(xyz[0] / D65.getI()) - f.evaluate(xyz[1] / D65.getJ()));
        lab[2] = 200. * (f.evaluate(xyz[1] / D65.getJ()) - f.evaluate(xyz[2] / D65.getK()));

        return lab;
    }

    /**
     * Conversion of CIELAB to RGB
     */
    private double[] lab2rgb(double[] lab) {
        UnivariateFunction f = t -> {
            double xlim = 0.008856;
            double a = 7.787;
            double b = 16. / 116.;
            double ylim = a * xlim + b;
            if (t > ylim) return Math.pow(t, 3);
            else return (t - b) / a;
        };

        double[] xyz = new double[3];
        xyz[0] = D65.getI() * f.evaluate((lab[1] / 500.) + (lab[0] + 16.) / 116.);
        xyz[1] = D65.getJ() * f.evaluate((lab[0] + 16.) / 116.);
        xyz[2] = D65.getK() * f.evaluate((lab[0] + 16.) / 116. - (lab[2] / 200.));

        return xyz2rgb(xyz);

    }

    /**
     * Conversion of CIELAB to Msh
     */
    private double[] lab2msh(double[] lab) {

        double sum = 0;
        for (int i = 0; i < 3; i++)
            sum += lab[i] * lab[i];

        double[] msh = new double[3];
        msh[0] = Math.sqrt(sum);
        msh[1] = Math.acos(lab[0] / msh[0]);
        msh[2] = Math.atan2(lab[2], lab[1]);

        return msh;
    }

    /**
     * Conversion of Msh to CIELAB
     */
    private double[] msh2lab(double[] msh) {

        double[] lab = new double[3];
        lab[0] = msh[0] * Math.cos(msh[1]);
        lab[1] = msh[0] * Math.sin(msh[1]) * Math.cos(msh[2]);
        lab[2] = msh[0] * Math.sin(msh[1]) * Math.sin(msh[2]);

        return lab;
    }

    /**
     * Direct conversion of RGB to Msh
     */
    private double[] rgb2msh(double[] rgb) {
        return lab2msh(rgb2lab(rgb));
    }

    /**
     * Direct conversion of Msh to RGB
     */
    private double[] msh2rgb(double[] msh) {
        return lab2rgb(msh2lab(msh));
    }

    /**
     * Function to provide an adjusted hue when interpolating to an unsaturated color in Msh space.
     */
    private double adjustHue(double[] mshSat, double mUnsat) {
        if (mshSat[0] >= mUnsat) return mshSat[2];

        double hSpin = mshSat[1] * Math.sqrt(mUnsat * mUnsat - mshSat[0] * mshSat[0]) / (mshSat[0] * Math.sin(mshSat[1]));

        if (mshSat[2] > -Math.PI / 3) return mshSat[2] + hSpin;

        return mshSat[2] - hSpin;
    }

    /**
     * Interpolation algorithm to automatically create continuous diverging color maps.
     */
    private double[] interpolateColor(double[] rgb1, double[] rgb2, double interp) {

        double[] msh1 = rgb2msh(rgb1);
        double[] msh2 = rgb2msh(rgb2);

        if (msh1[1] > 0.05 && msh2[1] > 0.05 && Math.abs(msh1[2] - msh2[2]) > Math.PI / 3) {
            double mMid = Math.max(msh1[0], Math.max(msh2[0], 88));
            if (interp < 0.5) {
                msh2[0] = mMid;
                msh2[1] = 0;
                msh2[2] = 0;
                interp *= 2;
            } else {
                msh1[0] = mMid;
                msh1[1] = 0;
                msh1[2] = 0;
                interp = 2 * interp - 1;
            }
        }

        if (msh1[1] < 0.05 && msh2[1] > 0.05) {
            msh1[2] = adjustHue(msh2, msh1[0]);
        } else if (msh2[1] < 0.05 && msh1[1] > 0.05) {
            msh2[2] = adjustHue(msh1, msh2[0]);
        }

        double[] mshMid = {(1 - interp) * msh1[0] + interp * msh2[0], (1 - interp) * msh1[1] + interp * msh2[1], (1 - interp) * msh1[2] + interp * msh2[2]};

        return msh2rgb(mshMid);
    }

    /**
     * @param numColors number of colors to use
     * @return the complete diverging color map using the Moreland-technique from RGB1 to RGB2,
     * placing "white" in the middle.
     */
    public List<Color> generateColorMap(Color minColor, Color maxColor, int numColors) {

        double[] min = {minColor.getRed(), minColor.getGreen(), minColor.getBlue()};
        double[] max = {maxColor.getRed(), maxColor.getGreen(), maxColor.getBlue()};

        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < numColors; i++) {
            double frac = i / (numColors - 1.);
            double[] color = interpolateColor(min, max, frac);
            int r = (int) Math.max(0, Math.min(255, Math.round(color[0])));
            int g = (int) Math.max(0, Math.min(255, Math.round(color[1])));
            int b = (int) Math.max(0, Math.min(255, Math.round(color[2])));
      /*-
      			double[] lin = rgbLinear(color);
      			double[] lab = rgb2lab(color);
      			double[] msh = rgb2msh(color);

      			System.out.printf("%d %f %3d %3d %3d %5.3f %5.3f %5.3f %5.3f %5.3f %5.3f %5.3f %5.3f %5.3f\n", i, frac, r,
      					g, b, lin[0] / 100, lin[1] / 100, lin[2] / 100, lab[0], lab[1], lab[2], msh[0], msh[1], msh[2]);
      */
            colors.add(new Color(r, g, b));
        }
        return colors;
    }

    public BufferedImage deltaEPlot(List<Color> colors) {

        DiscreteDataSet localDE = new DiscreteDataSet("Local ΔE");
        DiscreteDataSet startDE = new DiscreteDataSet("Start ΔE");
        DiscreteDataSet endDE = new DiscreteDataSet("End ΔE");
        double[] rgb0 = {colors.get(0).getRed(), colors.get(0).getGreen(), colors.get(0).getBlue()};
        double[] rgbN = {colors.get(colors.size() - 1).getRed(), colors.get(colors.size() - 1).getGreen(), colors.get(colors.size() - 1).getBlue()};
        double[] lab0 = rgb2lab(rgb0);
        double[] labN = rgb2lab(rgbN);
        double frac0 = 0;
        double fracN = 1;
        for (int i = 0; i < colors.size() - 1; i++) {
            double fraci = i / (colors.size() - 1.);
            double fracip1 = (i + 1) / (colors.size() - 1.);
            Color c0 = colors.get(i);
            Color c1 = colors.get(i + 1);

            double[] rgbi = {c0.getRed(), c0.getGreen(), c0.getBlue()};
            double[] rgbip1 = {c1.getRed(), c1.getGreen(), c1.getBlue()};

            double[] labi = rgb2lab(rgbi);
            double[] labip1 = rgb2lab(rgbip1);
            double x = labi[0] - labip1[0];
            double y = labi[1] - labip1[1];
            double z = labi[2] - labip1[2];
            double local = Math.sqrt(x * x + y * y + z * z) * (colors.size() - 1);
            localDE.add(i, local);
            x = labip1[0] - lab0[0];
            y = labip1[1] - lab0[1];
            z = labip1[2] - lab0[2];
            double start = Math.sqrt(x * x + y * y + z * z) / (fracip1 - frac0);
            startDE.add(i, start);
            x = labi[0] - labN[0];
            y = labi[1] - labN[1];
            z = labi[2] - labN[2];
            double end = Math.sqrt(x * x + y * y + z * z) / (fracN - fraci);
            endDE.add(i, end);
            //
            // System.out.printf("%d %.4f %4d %4d %4d %6.2f %6.2f %6.2f %10.4f %10.4f %10.4f\n", i, fraci,
            // c0.getRed(),
            // c0.getGreen(), c0.getBlue(), labi[0], labi[1], labi[2], local, start, end);
        }

        localDE.setColor(Color.BLUE);
        startDE.setColor(Color.MAGENTA);
        endDE.setColor(Color.ORANGE);

        PlotConfig config = ImmutablePlotConfig.builder().title(String.format("Min (%d,%d,%d) Max (%d,%d,%d)", colors.get(0).getRed(), colors.get(0).getGreen(), colors.get(0).getBlue(), colors.get(colors.size() - 1).getRed(), colors.get(colors.size() - 1).getGreen(), colors.get(colors.size() - 1).getBlue())).build();
        config = ImmutablePlotConfig.builder().from(config).legendPosition(new Point2D.Double(config.getRightPlotEdge() + 10, config.getTopPlotEdge() + 5)).build();

        AxisX xAxis = new AxisX(0, colors.size() - 1, "index", "%.0f");
        AxisY yAxis = new AxisY(0, 255, "value", "%.0f");

        DiscreteDataPlot canvas = new DiscreteDataPlot(config);
        canvas.setAxes(xAxis, yAxis);
        canvas.drawAxes();
        canvas.plot(localDE);
        canvas.plot(startDE);
        canvas.plot(endDE);

        canvas.addToLegend(localDE.getLegendEntry());
        canvas.addToLegend(startDE.getLegendEntry());
        canvas.addToLegend(endDE.getLegendEntry());
        canvas.drawLegend();

        ColorRamp ramp = ImmutableColorRamp.builder().min(0).max(1).colors(colors).build();
        ColorBar cb = ImmutableColorBar.builder().rect(new Rectangle(config.getLeftPlotEdge(), config.getTopPlotEdge() - 40, config.width(), 15)).ramp(ramp).numTicks(5).tickFunction(StringFunctions.fixedFormat("%.2f")).build();
        canvas.drawColorBar(cb);

        return canvas.getImage();
    }

    /**
     * @param startColor starting color
     * @return estimate ending color to feed into
     * {@link #generateColorMap(Color, Color, int)}.
     */
    public Color estimateEndColor(Color startColor) {
        double[] rgb0 = {startColor.getRed(), startColor.getGreen(), startColor.getBlue()};
        double[] lab0 = rgb2lab(rgb0);

        double[] labN = {lab0[0], -lab0[2], lab0[1]};
        double[] rgbN = lab2rgb(labN);

        int r = (int) Math.min(255, Math.max(0, Math.round(rgbN[0])));
        int g = (int) Math.min(255, Math.max(0, Math.round(rgbN[1])));
        int b = (int) Math.min(255, Math.max(0, Math.round(rgbN[2])));

        return new Color(r, g, b);
    }


}
