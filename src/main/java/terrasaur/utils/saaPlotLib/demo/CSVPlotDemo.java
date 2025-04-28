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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;

public class CSVPlotDemo {

  private final Map<Integer, List<Double>> data;

  public CSVPlotDemo(List<String> lines) {
    this.data = new HashMap<>();

    for (String line : lines) {
      if (line.trim().isEmpty() || line.trim().startsWith("#"))
        continue;
      String[] parts = line.split(",");
      for (int i = 0; i < parts.length; i++) {
          List<Double> thisColumn = data.computeIfAbsent(i, k -> new ArrayList<>());
          try {
          thisColumn.add(Double.parseDouble(parts[i].trim()));
        } catch (NumberFormatException e) {
          System.err.println("Parse error " + e.getLocalizedMessage());
        }
      }
    }
  }

  public BufferedImage plot(String xTitle, int xCol, String yTitle, int yCol, String title) {
    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).title(title).build();

    DiscreteDataSet thisData = new DiscreteDataSet(yTitle);
    thisData.add(data.get(xCol), data.get(yCol));

    DiscreteDataPlot plot = new DiscreteDataPlot(config);
    plot.setAxes(thisData.defaultXAxis(xTitle), thisData.defaultYAxis(yTitle));
    plot.drawAxes();
    plot.plot(thisData);

    return plot.getImage();
  }

  public static void test() {

    List<String> lines = new ArrayList<>();
    for (int i = 0; i < 314; i++) {
      double angle = i * 0.02;
      lines.add(
          String.format("%f,%f,%f,%f", angle, Math.sin(angle), Math.cos(angle), Math.tan(angle)));
    }

    CSVPlotDemo csv = new CSVPlotDemo(lines);

    PlotCanvas.showJFrame(csv.plot("Angle", 0, "Sin", 1, "Sin Plot"));
    PlotCanvas.showJFrame(csv.plot("Angle", 0, "Cos", 2, "Cos Plot"));
    PlotCanvas.showJFrame(csv.plot("Angle", 0, "Tan", 3, "Tan Plot"));

  }

  public static void main(String[] args) {
    test();
  }

}
