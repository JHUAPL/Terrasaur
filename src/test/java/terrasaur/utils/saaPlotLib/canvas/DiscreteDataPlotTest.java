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
