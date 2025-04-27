package terrasaur.utils.saaPlotLib.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisRange;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet.PLOTTYPE;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class SandPlotDemo {

  private static DiscreteDataSet createRandomDataSet(AxisRange range, String name) {
    DiscreteDataSet ds = new DiscreteDataSet(name);
    for (int i = 0; i < 100; i++) {
      double x = range.getMin() + range.getLength() * i / 99.;
      double y = new Random().nextDouble();
      ds.add(x, y);
    }
    return ds;
  }

  public static BufferedImage makePlot() {
    PlotConfig config =
        ImmutablePlotConfig.builder().width(800).height(600).title("Sand Plot Example").build();

    config = ImmutablePlotConfig.builder().from(config)
        .legendPosition(new Point2D.Double(config.leftMargin() + 50, config.topMargin() + 50))
        .legendFont(new Font("Helvetica", Font.BOLD, 24)).gridColor(Color.LIGHT_GRAY)
        .legendColor(true).legendOutline(true).build();

    int nCurves = 4;
    AxisX xLowerAxis = new AxisX(0, 10, "X Axis");
    AxisY yLeftAxis = new AxisY(0, nCurves, "Y Axis");

    DiscreteDataPlot canvas = new DiscreteDataPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);

    for (int i = 0; i < 4; i++) {
      DiscreteDataSet thisData =
          createRandomDataSet(xLowerAxis.getRange(), String.format("data set %d", i));
      thisData.setPlotType(PLOTTYPE.SAND);
      float h = ((float) i) / nCurves;
      thisData.setColor(Color.getHSBColor(h, 1.0f, 1.0f));
      canvas.plot(thisData);
      canvas.addToLegend(thisData.getLegendEntry());
    }

    canvas.drawAxes();
    canvas.drawLegend();
    canvas.drawGrid();

    return canvas.getImage();
  }

  public static void main(String[] args) {
    BufferedImage image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
//     PlotCanvas.writeImage("doc/images/sandPlotDemo.png", image);
  }

}
