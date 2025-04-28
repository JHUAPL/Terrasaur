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
package terrasaur.smallBodyModel;


import picante.math.intervals.Interval;
import picante.math.intervals.UnwritableInterval;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;

/**
 * The BoundingBox class is a data structure for storing the bounding box enclosing the a
 * 3-dimensional box-shaped region.
 * 
 * @author kahneg1
 * @version 1.0
 *
 */
public class BoundingBox {
  private Interval xRange;
  private Interval yRange;
  private Interval zRange;

  /**
   * Return a BoundingBox with each range set to {@link Interval#ALL_DOUBLES}.
   */
  public BoundingBox() {
    this(Interval.ALL_DOUBLES, Interval.ALL_DOUBLES, Interval.ALL_DOUBLES);
  }

  /**
   * Return a bounding box with its X limits set to elements 0 and 1, Y limits set to elements 2 and
   * 3, and Z limits set to elements 4 and 5 of the input array.
   * 
   * @param bounds
   */
  public BoundingBox(double[] bounds) {
    this(new Interval(bounds[0], bounds[1]), new Interval(bounds[2], bounds[3]),
        new Interval(bounds[4], bounds[5]));
  }

  /**
   * Return a BoundingBox with the supplied dimensions.
   * 
   * @param xRange
   * @param yRange
   * @param zRange
   */
  public BoundingBox(UnwritableInterval xRange, UnwritableInterval yRange,
                     UnwritableInterval zRange) {
    this.xRange = new Interval(xRange);
    this.yRange = new Interval(yRange);
    this.zRange = new Interval(zRange);
  }

  /**
   * Return a new BoundingBox with the same center as this instance but with each side's length
   * scaled by scale.
   * 
   * @param scale
   * @return
   */
  public BoundingBox getScaledBoundingBox(double scale) {
    double center = xRange.getMiddle();
    double length = xRange.getLength() * scale;
    Interval newXRange = new Interval(center - length / 2, center + length / 2);

    center = yRange.getMiddle();
    length = yRange.getLength() * scale;
    Interval newYRange = new Interval(center - length / 2, center + length / 2);

    center = zRange.getMiddle();
    length = zRange.getLength() * scale;
    Interval newZRange = new Interval(center - length / 2, center + length / 2);

    return new BoundingBox(newXRange, newYRange, newZRange);
  }

  /** Set the X dimension */
  public void setXRange(UnwritableInterval range) {
    this.xRange.setTo(range);
  }

  /** Set the Y dimension */
  public void setYRange(UnwritableInterval range) {
    this.yRange.setTo(range);
  }

  /** Set the Z dimension */
  public void setZRange(UnwritableInterval range) {
    this.zRange.setTo(range);
  }

  /** Return the X dimension */
  public UnwritableInterval getXRange() {
    return new UnwritableInterval(xRange);
  }

  /** Return the Y dimension */
  public UnwritableInterval getYRange() {
    return new UnwritableInterval(yRange);
  }

  /** Return the Z dimension */
  public UnwritableInterval getZRange() {
    return new UnwritableInterval(zRange);
  }

  /**
   * Expand the bounding box to contain the supplied point if it is not already contained.
   * 
   * @param point
   */
  public void update(UnwritableVectorIJK point) {
    xRange.set(Math.min(point.getI(), xRange.getBegin()), Math.max(point.getI(), xRange.getEnd()));
    yRange.set(Math.min(point.getJ(), yRange.getBegin()), Math.max(point.getJ(), yRange.getEnd()));
    zRange.set(Math.min(point.getK(), zRange.getBegin()), Math.max(point.getK(), zRange.getEnd()));
  }

  /**
   * Check if this instance intersects the other. The intersection test in each dimension is
   * {@link UnwritableInterval#closedIntersects(UnwritableInterval)}.
   * 
   * @param other
   * @return
   */
  public boolean intersects(BoundingBox other) {
    return xRange.closedIntersects(other.xRange) && yRange.closedIntersects(other.yRange)
        && zRange.closedIntersects(other.zRange);
  }

  /**
   * @return the length of the largest side of the box
   */
  public double getLargestSide() {
    return Math.max(xRange.getLength(), Math.max(yRange.getLength(), zRange.getLength()));
  }

  /**
   * @return the center of the box
   */
  public VectorIJK getCenterPoint() {
    return new VectorIJK(xRange.getMiddle(), yRange.getMiddle(), zRange.getMiddle());
  }

  /**
   * @return the diagonal length of the box
   */
  public double getDiagonalLength() {
    VectorIJK vec = new VectorIJK(xRange.getLength(), yRange.getLength(), zRange.getLength());
    return vec.getLength();
  }

  /**
   * Returns whether or not the given point is contained in the box. The contains test in each
   * dimension is {@link UnwritableInterval#closedContains(double)}.
   * 
   * @param pt
   * @return
   */
  public boolean contains(UnwritableVectorIJK pt) {
    return xRange.closedContains(pt.getI()) && yRange.closedContains(pt.getJ())
        && zRange.closedContains(pt.getK());
  }

  /**
   * Returns whether or not the given point is contained in the box. The contains test in each
   * dimension is {@link UnwritableInterval#closedContains(double)}.
   * 
   * @param pt
   * @return
   */
  public boolean contains(double[] pt) {
    return contains(new VectorIJK(pt));
  }

  /**
   * expand the X range by length on each side.
   * 
   * @param length
   */
  public void expandX(double length) {
    xRange.set(xRange.getBegin() - length, xRange.getEnd() + length);
  }

  /**
   * expand the Y range by length on each side.
   * 
   * @param length
   */
  public void expandY(double length) {
    yRange.set(yRange.getBegin() - length, yRange.getEnd() + length);
  }

  /**
   * expand the Z range by length on each side.
   * 
   * @param length
   */
  public void expandZ(double length) {
    zRange.set(zRange.getBegin() - length, zRange.getEnd() + length);
  }

  /**
   * Increase the size of the bounding box by adding (subtracting) to each side a specified
   * percentage of the bounding box diagonal
   * 
   * @param fractionOfDiagonalLength must be positive
   */
  public void increaseSize(double fractionOfDiagonalLength) {
    if (fractionOfDiagonalLength > 0.0) {
      double size = fractionOfDiagonalLength * getDiagonalLength();
      xRange.set(xRange.getBegin() - size, xRange.getEnd() + size);
      yRange.set(yRange.getBegin() - size, yRange.getEnd() + size);
      zRange.set(zRange.getBegin() - size, zRange.getEnd() + size);
    }
  }

  @Override
  public String toString() {
    return "xmin: " + xRange.getBegin() + " xmax: " + xRange.getEnd() + " ymin: "
        + yRange.getBegin() + " ymax: " + yRange.getEnd() + " zmin: " + zRange.getBegin()
        + " zmax: " + zRange.getEnd();
  }

  @Override
  public boolean equals(Object obj) {
    BoundingBox b = (BoundingBox) obj;
    return xRange.equals(b.xRange) && yRange.equals(b.yRange) && zRange.equals(b.zRange);
  }

  @Override
  public Object clone() {
    return new BoundingBox(xRange, yRange, zRange);
  }
}
