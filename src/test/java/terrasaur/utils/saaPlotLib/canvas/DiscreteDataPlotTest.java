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

import java.awt.image.BufferedImage;
import org.junit.Test;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;

public class DiscreteDataPlotTest {

  @Test
  public void testBlank() {
    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).build();
    DiscreteDataPlot canvas = new DiscreteDataPlot(config);
    BufferedImage image = canvas.getImage();
    // PlotCanvas.writeImage("blank.png", image);
  }

  @Test
  public void testAnnotation() {
    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).leftMargin(160)
        .bottomMargin(160).build();

    DiscreteDataPlot canvas = new DiscreteDataPlot(config);

    AxisX xAxis = new AxisX(0, 1, "Line 1\nLine2\nLine3");
    AxisY yAxis = new AxisY(0, 1, "Line 1\nLine2\nLine3");
    canvas.setAxes(xAxis, yAxis);
    canvas.drawAxes();
    // PlotCanvas.writeImage("annotation.png", canvas.getImage());
  }

}
