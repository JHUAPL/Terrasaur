package terrasaur.utils.saaPlotLib.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import terrasaur.utils.saaPlotLib.canvas.DiscreteDataPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet;
import terrasaur.utils.saaPlotLib.data.DiscreteDataSet.PLOTTYPE;
import terrasaur.utils.saaPlotLib.data.HistogramDataSet;
import terrasaur.utils.saaPlotLib.util.ImmutableLegendEntry;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class BarPlotDemo {

    public static BufferedImage plotGaussianHistogram() {
        PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).title("Gaussian Histogram").build();

        config = ImmutablePlotConfig.builder().from(config).legendPosition(new Point2D.Double(config.leftMargin() + 50, config.topMargin() + 50)).legendFont(new Font(Font.MONOSPACED, Font.BOLD, 18)).gridColor(Color.LIGHT_GRAY).build();

        AxisX xLowerAxis = new AxisX(-4, 4, "X Axis");

        double binSize = 0.2;
        List<Double> binBoundaries = new ArrayList<>();
        binBoundaries.add(xLowerAxis.getRange().getMin() - binSize / 2);
        for (double x = xLowerAxis.getRange().getMin(); x <= xLowerAxis.getRange().getMax(); x += binSize) {
            binBoundaries.add(x + binSize / 2);
        }
        binBoundaries.add(xLowerAxis.getRange().getMax() + binSize / 2);

        HistogramDataSet data = new HistogramDataSet("Histogram", binBoundaries);
        int npts = 10000;
        for (int i = 0; i < npts; i++)
            data.add(new Random().nextGaussian());

        data = data.getNormalized("Normalized");

        AxisY yLeftAxis = new AxisY(0, data.getYStats().getMax(), "% of total", x -> String.format("%.1f", 100 * x));
        DiscreteDataPlot canvas = new DiscreteDataPlot(config);
        canvas.setAxes(xLowerAxis, yLeftAxis);

        canvas.drawAxes();
        data.setPlotType(PLOTTYPE.BAR);
        canvas.plot(data);

        List<Double> yValues = data.getYValues();
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (double y : yValues) stats.addValue(y);
        canvas.addToLegend(ImmutableLegendEntry.builder().name(String.format("N       %d", stats.getN())).color(data.getColor()).build());
        canvas.addToLegend(ImmutableLegendEntry.builder().name(String.format("min     %g", stats.getMin())).color(data.getColor()).build());
        canvas.addToLegend(ImmutableLegendEntry.builder().name(String.format("max     %g", stats.getMax())).color(data.getColor()).build());
        canvas.addToLegend(ImmutableLegendEntry.builder().name(String.format("mean    %g", stats.getMean())).color(data.getColor()).build());
        canvas.addToLegend(ImmutableLegendEntry.builder().name(String.format("std dev %g", stats.getStandardDeviation())).color(data.getColor()).build());
        canvas.drawGrid();
        canvas.drawLegend();

        // float[] dash = { 10.0f };
        // data.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
        // dash, 0.0f));
        // data.setColor(Color.RED);
        // data.setPlotType(PLOTTYPE.LINE);
        // canvas.plot(data);

        return canvas.getImage();
    }

    public static BufferedImage plotCosX() {

        PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).title("Bar Plot Example").build();

        config = ImmutablePlotConfig.builder().from(config).legendPosition(new Point2D.Double(config.leftMargin() + 50, config.topMargin() + 50)).legendFont(new Font("Helvetica", Font.BOLD, 36)).gridColor(Color.LIGHT_GRAY).build();

        AxisX xLowerAxis = new AxisX(0, 2 * Math.PI, "X Axis");
        AxisY yLeftAxis = new AxisY(-1.5, 1.5, "Y Axis");

        DiscreteDataPlot canvas = new DiscreteDataPlot(config);
        canvas.setAxes(xLowerAxis, yLeftAxis);

        DiscreteDataSet data = new DiscreteDataSet("cos X");
        for (int i = 0; i < 50; i++) {
            double x = 2 * Math.PI * i / 49.;
            double y = Math.cos(x);
            data.add(x, y);
        }

        canvas.drawAxes();
        data.setPlotType(PLOTTYPE.BAR);
        canvas.plot(data);
        canvas.addToLegend(data.getLegendEntry());
        canvas.drawGrid();
        canvas.drawLegend();

        return canvas.getImage();
    }

    public static void main(String[] args) {
        BufferedImage image;
        // image = plotCosX();
        // PlotCanvas.showJFrame(image);
        image = plotGaussianHistogram();
        PlotUtils.addCreationDate(image);
        PlotCanvas.showJFrame(image);
         PlotCanvas.writeImage("doc/images/barPlotDemo.png", image);
    }

}
