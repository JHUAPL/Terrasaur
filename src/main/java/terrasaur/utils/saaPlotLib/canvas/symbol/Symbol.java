package terrasaur.utils.saaPlotLib.canvas.symbol;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public abstract class Symbol {

  public static final double DEFAULT_SIZE = 8;

  protected double size;
  protected boolean fill;
  protected double rotate;

  public Symbol() {
    size = DEFAULT_SIZE;
    fill = false;
    rotate = 0;
  }

  public Symbol setFill(boolean fill) {
    this.fill = fill;
    return this;
  }

  public double getSize() {
    return size;
  }

  public Symbol setSize(double size) {
    this.size = size;
    return this;
  }

  public Symbol setRotate(double rotateDeg) {
    this.rotate = Math.toRadians(rotateDeg);
    return this;
  }

  public abstract void draw(Graphics2D g, double x, double y);

  public void drawError(Graphics2D g, double x, double y, Point2D xError, Point2D yError) {
    Graphics2D gg = (Graphics2D) g.create();

    if (xError != null) gg.draw(new Line2D.Double(xError.getX(), y, xError.getY(), y));

    if (yError != null) gg.draw(new Line2D.Double(x, yError.getX(), x, yError.getY()));

    gg.dispose();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (fill ? 1231 : 1237);
    result = prime * result + Double.hashCode(rotate);
    result = prime * result + Double.hashCode(size);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Symbol other = (Symbol) obj;
    if (fill != other.fill) return false;
    if (Double.doubleToLongBits(rotate) != Double.doubleToLongBits(other.rotate)) return false;
    return Double.doubleToLongBits(size) == Double.doubleToLongBits(other.size);
  }
}
