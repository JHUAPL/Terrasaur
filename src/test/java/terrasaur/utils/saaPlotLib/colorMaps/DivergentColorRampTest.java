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
