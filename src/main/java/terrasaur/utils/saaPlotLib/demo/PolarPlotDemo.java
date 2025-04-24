package terrasaur.utils.saaPlotLib.demo;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.PolarPlot;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisR;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisTheta;
import terrasaur.utils.saaPlotLib.canvas.symbol.Square;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.ImmutableAnnotation;
import terrasaur.utils.saaPlotLib.util.PlotUtils;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class PolarPlotDemo {

  private static DiscreteDataSet createCosX(Interval range) {
    DiscreteDataSet ds = new DiscreteDataSet("cos");
    for (int i = 0; i < 100; i++) {
      double theta = range.getInf() + range.getSize() * i / 99.;
      ds.add(1 + Math.cos(theta), theta);
    }
    return ds;
  }

  public static BufferedImage makePlot() {
    AxisR axisR = new AxisR(0, 1.5, "R");
    NavigableMap<Double, String> tickLabels = new TreeMap<>();
    for (double r = axisR.getRange().getMin(); r < axisR.getRange().getMax(); r += .5)
      tickLabels.put(r, String.format("%.1f", r));
    axisR.setTickLabels(tickLabels, 2);

    AxisTheta axisTheta = new AxisTheta(0, 2 * Math.PI, "θ", StringFunctions.toDegrees("%.0f"));
    tickLabels = new TreeMap<>();
    for (double deg = 0; deg <= 360; deg += 60)
      tickLabels.put(Math.toRadians(deg), String.format("%.0f", deg));
    axisTheta.setTickLabels(tickLabels, 6);

    DiscreteDataSet dds = createCosX(new Interval(-Math.PI, Math.PI));
    dds.setSymbol(new Square().setFill(true).setSize(4).setRotate(45));

    double rotate = -Math.PI / 2;

    PlotConfig config =
        ImmutablePlotConfig.builder()
            .width(800)
            .height(600)
            .title("Polar Plot Demo, rotate " + Math.toDegrees(rotate))
            .build();
    PolarPlot canvas = new PolarPlot(config, axisR, axisTheta);
    canvas.setRotate(rotate);
    canvas.drawTitle();
    canvas.drawGrid();
    canvas.drawAxes();
    canvas.plot(dds);

    Point2D point = canvas.dataToPixel(axisR, 0.75, Math.toRadians(300));
    canvas.addAnnotation(
        ImmutableAnnotation.builder().text("0.75, 300").color(Color.RED).build(),
        point.getX(),
        point.getY(),
        rotate);

    return canvas.getImage();
  }

  public static void main(String[] args) {
    BufferedImage image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
//    PlotCanvas.writeImage("doc/images/polarPlotDemo.png", image);
  }
}
