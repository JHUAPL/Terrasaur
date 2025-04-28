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
import java.util.List;
import org.junit.Test;

public class DivergentColorRampTest {

  @Test
  public void test01() {

    // like matplotlib's "coolwarm"
    Color minColor = new Color(59, 76, 192);
    Color maxColor = new Color(180, 4, 38);

    // like matplotlib's "RdBu"
    // minColor = new Color(100,0,31);
    // maxColor = new Color(5,48,97);

    // like matplotlib's "PuOr"
    minColor = new Color(124, 58, 20);
    maxColor = new Color(44, 9, 74);

    DivergentColorRamp dcr = new DivergentColorRamp();
    // maxColor = dcr.estimateEndColor(minColor);

    List<Color> colors = dcr.generateColorMap(minColor, maxColor, 33);

    DivergentColorRamp.test(colors);
    //    PlotCanvas.showJFrame(dcr.deltaEPlot(colors));

  }
}
