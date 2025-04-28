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
package terrasaur.utils.saaPlotLib.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.colorMaps.ColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.colorMaps.tables.ColorTable;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotation;
import terrasaur.utils.saaPlotLib.data.ImmutableAnnotation;
import terrasaur.utils.saaPlotLib.util.Keyword;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class ColorRampDemo {

  public static BufferedImage getRampImage() {
    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(1800)
            .height(1000)
            .topMargin(0)
            .leftMargin(0)
            .rightMargin(0)
            .bottomMargin(0)
            .build();
    DiscreteDataPlot canvas = new DiscreteDataPlot(config);

    int x = 20;
    int y = 20;
    int width = 200;
    int height = 25;

    AxisX xAxis = new AxisX(0, config.width(), "");
    AxisY yAxis = new AxisY(0, config.height(), "");
    canvas.setAxes(xAxis, yAxis);

    for (ColorTable.FAMILY f : ColorTable.FAMILY.values()) {
      Annotation a =
          ImmutableAnnotation.builder()
              .text(f.name())
              .color(Color.BLACK)
              .font(new Font("HELVETICA", Font.BOLD, 24))
              .horizontalAlignment(Keyword.ALIGN_LEFT)
              .verticalAlignment(Keyword.ALIGN_TOP)
              .build();

      canvas.addAnnotation(a, x, config.height() - y);
      x += 50;
      y += 50;

      for (ColorRamp.TYPE type : ColorRamp.TYPE.values()) {
        if (ColorRamp.TYPE.isType(f).test(type)) {
          ColorRamp ramp = ColorRamp.create(type, 0, 1).addTitle(type.name());

          ColorBar cb =
              ImmutableColorBar.builder()
                  .rect(new Rectangle(x, y, width, height))
                  .ramp(ramp)
                  .numTicks(5)
                  .tickFunction(StringFunctions.fixedFormat("%.2f"))
                  .build();
          canvas.drawColorBar(cb);

          x += (int) Math.round(1.5 * width);
          if (x > config.width() - width) {
            x = 70;
            y += 3 * height;
          }
        }
      }
      x = 20;
      y += 3 * height;
    }

    return canvas.getImage();
  }

  public static void main(String[] args) {
    PlotCanvas.showJFrame(getRampImage());
  }
}
