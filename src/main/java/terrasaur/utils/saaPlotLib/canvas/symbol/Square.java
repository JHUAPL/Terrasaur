/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
