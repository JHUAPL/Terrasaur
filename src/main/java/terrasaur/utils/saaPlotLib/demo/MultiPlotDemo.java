package terrasaur.utils.saaPlotLib.demo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.util.PlotUtils;

public class MultiPlotDemo {

  public static BufferedImage makePlot() {
    BufferedImage topImage = LinePlotDemo.makePlot();
    BufferedImage bottomImage = ActivityPlotDemo.makePlot();

    int width = 600;
    int height = 800;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(topImage, 0, 0, width, height / 2, 0, 0, topImage.getWidth(), topImage.getHeight(),
        null);
    g.drawImage(bottomImage, 0, height / 2, width, height, 0, 0, bottomImage.getWidth(),
        bottomImage.getHeight(), null);
    g.dispose();

    return image;
  }

  public static void main(String[] args) {
    BufferedImage image = makePlot();
    PlotUtils.addCreationDate(image);
    PlotCanvas.showJFrame(image);
    // PlotCanvas.writeImage("multiPlotDemo.png", image);
  }

}
