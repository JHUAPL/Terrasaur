package terrasaur.utils.saaPlotLib.canvas.symbol;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class Cross extends Symbol {

  public Cross() {
    super();
  }

  @Override
  public void draw(Graphics2D g, double x, double y) {
    Graphics2D gg = (Graphics2D) g.create();

    Shape shape = new Line2D.Double(x - size / 2, y, x + size / 2, y);

    for (int i = 0; i < 4; i++) {
      AffineTransform at = AffineTransform.getRotateInstance(i * Math.toRadians(90) + rotate, x, y);
      gg.draw(at.createTransformedShape(shape));
    }

    gg.dispose();
  }

}
