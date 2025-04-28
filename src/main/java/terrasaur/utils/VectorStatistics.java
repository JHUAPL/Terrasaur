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
package terrasaur.utils;

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import picante.math.vectorspace.UnwritableVectorIJK;
import spice.basic.Vector3;

/**
 * Class which stores a {@link DescriptiveStatistics} object for each dimension in a collection of
 * {@link Vector3D} objects
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class VectorStatistics {

  private final DescriptiveStatistics xStats;
  private final DescriptiveStatistics yStats;
  private final DescriptiveStatistics zStats;

  private boolean printMedian;

  /**
   * Toggle printing median statistics in {@link #toString()}. Default is true. Setting to false can
   * save some time.
   */
  public void setPrintMedian(boolean printMedian) {
    this.printMedian = printMedian;
  }

  public VectorStatistics() {
    xStats = new DescriptiveStatistics();
    yStats = new DescriptiveStatistics();
    zStats = new DescriptiveStatistics();
    printMedian = true;
  }

  /**
   * Add a vector to the statistics
   * 
   * @param v vector to add
   */
  public void add(Vector3D v) {
    xStats.addValue(v.getX());
    yStats.addValue(v.getY());
    zStats.addValue(v.getZ());
  }

  /**
   * Add a vector to the statistics
   *
   * @param v vector to add
   */
  public void add(UnwritableVectorIJK v) {
    xStats.addValue(v.getI());
    yStats.addValue(v.getJ());
    zStats.addValue(v.getK());
  }

  /**
   * Add a vector to the statistics
   *
   * @param v vector to add
   */
  public void add(Vector3 v) {
    double[] values = v.toArray();
    xStats.addValue(values[0]);
    yStats.addValue(values[1]);
    zStats.addValue(values[2]);
  }

  /**
   * @return a {@link Vector3D} where the X component is the mean of the X components, Y is the mean
   *         of the Y components, and Z is the mean of the Z components.
   */
  public Vector3D getMean() {
    return new Vector3D(xStats.getMean(), yStats.getMean(), zStats.getMean());
  }

  /**
   * @return a {@link Vector3D} where the X component is the min of the X components, Y is the min
   *         of the Y components, and Z is the min of the Z components.
   */
  public Vector3D getMin() {
    return new Vector3D(xStats.getMin(), yStats.getMin(), zStats.getMin());
  }

  /**
   * @return a {@link Vector3D} where the X component is the max of the X components, Y is the max
   *         of the Y components, and Z is the max of the Z components.
   */
  public Vector3D getMax() {
    return new Vector3D(xStats.getMax(), yStats.getMax(), zStats.getMax());
  }

  /**
   * @return a {@link Vector3D} where the X component is the std of the X components, Y is the std
   *         of the Y components, and Z is the std of the Z components.
   */
  public Vector3D getStandardDeviation() {
    return new Vector3D(xStats.getStandardDeviation(), yStats.getStandardDeviation(),
        zStats.getStandardDeviation());
  }

  @Override
  public String toString() {
    StringBuilder outBuffer = new StringBuilder();
    outBuffer.append("VectorStatistics:\n");
    outBuffer.append(String.format("n: [%d,%d,%d]\n", xStats.getN(), yStats.getN(), zStats.getN()));
    outBuffer.append(
        String.format("min: [%f,%f,%f]\n", xStats.getMin(), yStats.getMin(), zStats.getMin()));
    outBuffer.append(
        String.format("max: [%f,%f,%f]\n", xStats.getMax(), yStats.getMax(), zStats.getMax()));
    outBuffer.append(
        String.format("mean: [%f,%f,%f]\n", xStats.getMean(), yStats.getMean(), zStats.getMean()));
    outBuffer.append(String.format("std dev: [%f,%f,%f]\n", xStats.getStandardDeviation(),
        yStats.getStandardDeviation(), zStats.getStandardDeviation()));
    if (printMedian) {
      try {
        // No catch for MIAE because actual parameter is valid below
        outBuffer.append(String.format("median: [%f,%f,%f]\n", xStats.getPercentile(50),
            yStats.getPercentile(50), zStats.getPercentile(50)));
      } catch (MathIllegalStateException ex) {
        outBuffer.append("median: unavailable\n");
      }
    }
    outBuffer.append(String.format("skewness: [%f,%f,%f]\n", xStats.getSkewness(),
        yStats.getSkewness(), zStats.getSkewness()));
    outBuffer.append(String.format("kurtosis: [%f,%f,%f]\n", xStats.getKurtosis(),
        yStats.getKurtosis(), zStats.getKurtosis()));
    return outBuffer.toString();
  }

}
