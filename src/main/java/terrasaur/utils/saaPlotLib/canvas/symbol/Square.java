package terrasaur.utils.saaPlotLib.canvas.symbol;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class Square extends Symbol {

  public Square() {
    super();
  }

  @Override
  public void draw(Graphics2D g, double x, double y) {
    Graphics2D gg = (Graphics2D) g.create();

    Path2D.Double p = new Path2D.Double();
    p.moveTo(x - size / 2, y + size / 2);
    p.lineTo(x + size / 2, y + size / 2);
    p.lineTo(x + size / 2, y - size / 2);
    p.lineTo(x - size / 2, y - size / 2);
    p.closePath();

    AffineTransform at = AffineTransform.getRotateInstance(rotate, x, y);
    Shape s = at.createTransformedShape(p);

    gg.draw(s);

    if (fill)
      gg.fill(s);

    gg.dispose();
  }

}
