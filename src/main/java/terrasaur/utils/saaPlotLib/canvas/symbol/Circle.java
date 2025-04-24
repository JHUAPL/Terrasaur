package terrasaur.utils.saaPlotLib.canvas.symbol;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

public class Circle extends Symbol {

  public Circle() {
    super();
  }

  @Override
  public void draw(Graphics2D g, double x, double y) {
    Graphics2D gg = (Graphics2D) g.create();

    Shape shape = new Ellipse2D.Double(x - size / 2, y - size / 2, size, size);
    gg.draw(shape);
    if (fill)
      gg.fill(shape);

    gg.dispose();
  }

}
