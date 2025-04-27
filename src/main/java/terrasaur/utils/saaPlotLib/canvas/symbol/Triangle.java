package terrasaur.utils.saaPlotLib.canvas.symbol;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class Triangle extends Symbol {

  public Triangle() {
    super();
  }

  @Override
  public void draw(Graphics2D g, double x, double y) {
    Graphics2D gg = (Graphics2D) g.create();

    final double side = 2 * size / Math.sqrt(3.);

    Path2D.Double p = new Path2D.Double();
    p.moveTo(x - side / 2, y + size / 2);
    p.lineTo(x + side / 2, y + size / 2);
    p.lineTo(x, y - size / 2);
    p.closePath();

    AffineTransform at = AffineTransform.getRotateInstance(rotate, x, y);
    Shape s = at.createTransformedShape(p);

    gg.draw(s);

    if (fill)
      gg.fill(s);

    gg.dispose();
  }

}
