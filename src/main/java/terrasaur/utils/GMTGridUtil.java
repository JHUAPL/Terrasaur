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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import nom.tam.fits.FitsException;
import terrasaur.fits.FitsUtil;
import spice.basic.LatitudinalCoordinates;
import spice.basic.Matrix33;
import spice.basic.SpiceException;
import spice.basic.Vector3;

/**
 * This class takes a 3D field as input, uses GMT to create a uniform grid on a local plane, and
 * then returns the field values at these uniform grid points.
 * 
 * @author nairah1
 *
 */
public class GMTGridUtil {

  private List<Vector3> pointsList;
  private List<Double> field;
  private Matrix33 rotation;
  private Vector3 translation;
  private Vector3 uz;
  private int halfSize;
  private int nX, nY;
  private double groundSampleDistance;
  private String additionalGMTArgs;
  private List<Vector3> globalXYZ;
  private List<Vector3> evaluateXYZ;
  private boolean evaluateAtCustomPoints;

  /**
   * Specify the dimensions and grid spacing of the 2D grid used by GMT. nX and nY must be odd and
   * equal.
   * 
   * @param nX
   * @param nY
   * @param groundSampleDistance
   */
  public GMTGridUtil(int nX, int nY, double groundSampleDistance) {
    this.nX = nX;
    this.nY = nY;
    this.halfSize = (nX - 1) / 2;
    checkDimensions();
    this.groundSampleDistance = groundSampleDistance;
    this.evaluateAtCustomPoints = false;
    this.additionalGMTArgs = "";
  }

  /**
   * Specify the dimensions and grid spacing of the 2D grid used by GMT.
   * 
   * @param halfSize grid dimensions are (2*halfSize +1)x(2*halfSize +1)
   * @param groundSampleDistance
   */
  public GMTGridUtil(int halfSize, double groundSampleDistance) {
    this.halfSize = halfSize;
    this.nX = halfSize * 2 + 1;
    this.nY = halfSize * 2 + 1;
    this.groundSampleDistance = groundSampleDistance;
    this.evaluateAtCustomPoints = false;
    this.additionalGMTArgs = "";
  }

  private void checkDimensions() {
    if (nX != nY) {
      throw new IllegalArgumentException(
          String.format("GMTGridUtil: nX (%d) and nY (%d) must be equal!\n", nX, nY));
    }
    if (nX % 2 != 1) {
      throw new IllegalArgumentException(String.format("GMTGridUtil: nX (%d) must be odd!\n", nX));
    }
  }

  public void setGMTArgs(String args) {
    additionalGMTArgs = args;
  }

  /**
   * Specify the 3D coordinates for the input field
   * 
   * @param x
   * @param y
   * @param z
   */
  public void setXYZ(double[] x, double[] y, double[] z) {
    pointsList = new ArrayList<>();
    for (int i = 0; i < x.length; i++) {
      pointsList.add(new Vector3(x[i], y[i], z[i]));
    }

    translation = new Vector3();
    for (Vector3 point : pointsList)
      translation = translation.add(point);
    translation = translation.scale(1. / pointsList.size());

    try {
      calculateTransformation();
    } catch (SpiceException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  /**
   * Specify the 3D coordinates to evaluate the gridded field. If not called, the grid points from
   * the local plane will be used.
   * 
   * @param x
   * @param y
   * @param z
   */
  public void setEvaluationXYZ(double[] x, double[] y, double[] z) {
    evaluateXYZ = new ArrayList<>();
    for (int i = 0; i < x.length; i++) {
      evaluateXYZ.add(new Vector3(x[i], y[i], z[i]));
    }
    evaluateAtCustomPoints = true;
  }

  public double[][] getRotation() {
    return rotation.toArray();
  }

  public double[] getPlaneNormal() {
    return uz.toArray();
  }

  /**
   * Return a 4x4 transformation matrix. The top left 3x3 matrix is the rotation matrix. The top
   * three entries in the right hand column are the translation vector. The bottom row is always 0 0
   * 0 1.
   * <p>
   * local coordinate system to global:
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mtxv(point).sub(translation);
   * }
   * </pre>
   * <p>
   * global coordinate system to local:
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mxv(point.add(translation));
   * }
   * </pre>
   * 
   * @return
   */
  public double[][] getTransformation() {
    double[][] retArray = MatrixUtils.createRealIdentityMatrix(4).getData();

    try {
      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
          retArray[i][j] = rotation.getElt(i, j);
        }
        retArray[i][3] = -translation.getElt(i);
      }
    } catch (SpiceException e) {
      e.printStackTrace();
    }
    return (retArray);
  }

  /**
   * <p>
   * local coordinate system to global:
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mtxv(point).sub(translation);
   * }
   * </pre>
   * <p>
   * global coordinate system to local:
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mxv(point.add(translation));
   * }
   * </pre>
   * 
   * @return
   */
  public Pair<Vector3, Matrix33> getTransformationAsPair() {
    return Pair.create(new Vector3(translation).negate(), new Matrix33(rotation));
  }

  /**
   * Set the rotation matrix to transform between global and local coordinates. If not called, a
   * rotation matrix will be found from a transformation that moves the input XYZ coordinates
   * closest to a best fit plane. Call this AFTER {@link #setXYZ(double[], double[], double[])}.
   * 
   */
  public void setRotation(double[][] rotation) {
    try {
      this.rotation = new Matrix33(rotation);
      calculateTransformation();
    } catch (SpiceException e) {
      e.printStackTrace();
    }
  }

  public double[] getTranslation() {
    return translation.toArray();
  }

  /**
   * Set the translation vector to transform from local to global coordinates. If not called,
   * translation vector will be set to the centroid of the input XYZ coordinates. Call this AFTER
   * {@link #setXYZ(double[], double[], double[])}.
   */
  public void setTranslation(double[] translation) {
    if (translation != null) {
      try {
        this.translation = new Vector3(translation);
        calculateTransformation();
      } catch (SpiceException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Specify input field values at the coordinates supplied to
   * {@link #setXYZ(double[], double[], double[])}
   * 
   * @param fArray
   */
  public void setField(double[] fArray) {
    field = new ArrayList<>();
    for (double f : fArray)
      field.add(f);
  }

  /**
   * If called, set the field to the height above plane. XYZ points are rotated to the local
   * coordinate system and height above the plane (Z coordinate) is stored as the field to
   * interpolate.
   */
  public void setFieldToHeight() {
    List<Vector3> transformed = globalToLocal(pointsList);
    double[] heights = new double[transformed.size()];
    for (int i = 0; i < heights.length; i++) {
      try {
        heights[i] = transformed.get(i).getElt(2);
      } catch (SpiceException e) {
        e.printStackTrace();
      }
    }
    setField(heights);
  }

  /**
   * Field positions from the pointsList are transformed to a local plane coordinate system and
   * regridded with GMTSurface. These points are then transformed back to the global coordinate
   * system.
   * <p>
   * GMTSurface is run twice. Once with the field value substituted for z, and one with the
   * transformed x, y, z to allow transforming back from the local plane to global coordinates.
   * 
   * @return a double array of dimensions[7][nX][nY]. First six indices are Lat, Lon, Radius, X, Y,
   *         and Z. Last index is the field value at that position.
   * @throws SpiceException
   * @throws IOException
   * @throws InterruptedException
   * @throws FitsException
   */
  public double[][][] regridField()
      throws SpiceException, IOException, InterruptedException, FitsException {

    File tmpDir = new File(String.format("GMT-%d", System.currentTimeMillis()));
    if (!tmpDir.exists())
      tmpDir.mkdirs();
    tmpDir.deleteOnExit();

    List<Vector3> transformed = globalToLocal(pointsList);
    double xmin = Double.MAX_VALUE;
    double xmax = -xmin;
    double ymin = Double.MAX_VALUE;
    double ymax = -xmin;
    for (Vector3 point : transformed) {
      try {
        double x = point.getElt(0);
        double y = point.getElt(1);
        if (x > xmax)
          xmax = x;
        if (x < xmin)
          xmin = x;
        if (y > ymax)
          ymax = y;
        if (y < ymin)
          ymin = y;
      } catch (SpiceException e) {
        e.printStackTrace();
      }
    }

    System.out.printf("Data extents %f/%f/%f/%f\n", xmin, xmax, ymin, ymax);

    ArrayList<Vector3> inputField = new ArrayList<>();
    for (int i = 0; i < transformed.size(); i++) {
      Vector3 point = transformed.get(i);
      Double value = field.get(i);
      inputField.add(new Vector3(point.getElt(0), point.getElt(1), value));
    }

    xmax = (evaluateAtCustomPoints ? (halfSize + 1) : halfSize) * groundSampleDistance;
    xmin = -xmax;
    ymin = xmin;
    ymax = xmax;

    // create a random 8 character string
    String name = RandomStringUtils.randomAlphabetic(8);
    String inputGMT = new File(tmpDir, name + "_gmt-input.bin").getPath();
    String outputNetCDF = new File(tmpDir, name + "_surface-output.grd").getPath();
    String outputFITS = new File(tmpDir, name + "_surface-output.fits").getPath();
    writeBinaryGMTInput(inputField, inputGMT);

    String command = String.format("GMTSurface %s %12.8f %f/%f/%f/%f %s %s %s", inputGMT,
        groundSampleDistance, xmin, xmax, ymin, ymax, outputNetCDF, outputFITS, additionalGMTArgs);

    ProcessUtils.runProgramAndWait(command, null, true);

    DescriptiveStatistics xStats = new DescriptiveStatistics();
    DescriptiveStatistics yStats = new DescriptiveStatistics();

    List<Vector3> surfaceField = new ArrayList<>();
    PiecewiseBicubicSplineInterpolatingFunction interpolator =
        readGMTFits(outputFITS, surfaceField, xStats, yStats);

    if (globalXYZ == null) {
      if (evaluateAtCustomPoints) {
        globalXYZ = evaluateXYZ;
      } else {
        name = "height";
        inputGMT = new File(tmpDir, name + "_gmt-input.bin").getPath();
        outputNetCDF = new File(tmpDir, name + "_surface-output.grd").getPath();
        outputFITS = new File(tmpDir, name + "_surface-output.fits").getPath();
        writeBinaryGMTInput(transformed, inputGMT);

        command = String.format("GMTSurface %s %12.8f %f/%f/%f/%f %s %s %s", inputGMT,
            groundSampleDistance, xmin, xmax, ymin, ymax, outputNetCDF, outputFITS,
            additionalGMTArgs);
        ProcessUtils.runProgramAndWait(command, null, false);

        List<Vector3> surfaceXYZ = new ArrayList<>();
        readGMTFits(outputFITS, surfaceXYZ, xStats, yStats);

        globalXYZ = localToGlobal(surfaceXYZ);
      }
    }

    if (evaluateAtCustomPoints) {
      surfaceField = new ArrayList<>();
      List<Vector3> transformedEvaluationPoints = globalToLocal(evaluateXYZ);
      for (Vector3 transformedEvaluationPoint : transformedEvaluationPoints) {
        double x = transformedEvaluationPoint.getElt(0);
        if (x < xStats.getMin()) {
          System.err.printf("Warning: x value %g outside range [%g, %g], setting to %g\n", x,
              xStats.getMin(), xStats.getMax(), xStats.getMin());
          x = xStats.getMin();
        }
        if (x > xStats.getMax()) {
          System.err.printf("Warning: x value %g outside range [%g, %g], setting to %g\n", x,
              xStats.getMin(), xStats.getMax(), xStats.getMax());
          x = xStats.getMax();
        }

        double y = transformedEvaluationPoint.getElt(1);
        if (y < yStats.getMin()) {
          System.err.printf("Warning: y value %g outside range [%g, %g], setting to %g\n", y,
              yStats.getMin(), yStats.getMax(), yStats.getMin());
          y = yStats.getMin();
        }
        if (y > yStats.getMax()) {
          System.err.printf("Warning: y value %g outside range [%g, %g], setting to %g\n", y,
              yStats.getMin(), yStats.getMax(), yStats.getMax());
          y = yStats.getMax();
        }

        double z = interpolator.value(x, y);
        surfaceField.add(new Vector3(x, y, z));
      }
    }

    /*-
     0 - latitude (degrees)
     1 - longitude (degrees)
     2 - radius
     3 - vertex x
     4 - vertex y
     5 - vertex z
     6 - interpolated field value at vertex
     */
    double[][][] returnArray = new double[7][nX][nY];

    for (int i = 0; i < globalXYZ.size(); i++) {
      LatitudinalCoordinates lc = new LatitudinalCoordinates(globalXYZ.get(i));
      int m = i / nX;
      int n = i % nY;

      returnArray[0][m][n] = Math.toDegrees(lc.getLatitude());
      returnArray[1][m][n] = Math.toDegrees(lc.getLongitude());
      if (returnArray[1][m][n] < 0)
        returnArray[1][m][n] += 360;
      returnArray[2][m][n] = lc.getRadius();
      returnArray[3][m][n] = globalXYZ.get(i).getElt(0);
      returnArray[4][m][n] = globalXYZ.get(i).getElt(1);
      returnArray[5][m][n] = globalXYZ.get(i).getElt(2);
      returnArray[6][m][n] = surfaceField.get(i).getElt(2); // Z coordinate is the regridded field
    }

    return returnArray;
  }

  /**
   * Finds the points in each bin and returns statistics using the supplied refValue rather than the
   * mean.
   * 
   * @param refValue
   * @return
   * @throws SpiceException
   */
  public Map<Pair<Integer, Integer>, DescriptiveStatistics> getStats(double[][] refValue)
      throws SpiceException {
    List<Vector3> transformed = globalToLocal(pointsList);

    ArrayList<Vector3> inputField = new ArrayList<>();
    for (int i = 0; i < transformed.size(); i++) {
      Vector3 point = transformed.get(i);
      Double value = field.get(i);
      inputField.add(new Vector3(point.getElt(0), point.getElt(1), value));
    }

    double xmin = -halfSize * groundSampleDistance;
    double ymin = -halfSize * groundSampleDistance;

    Map<Pair<Integer, Integer>, DescriptiveStatistics> binnedPoints = new HashMap<>();
    for (int i = 0; i < inputField.size(); i++) {
      Vector3 point = inputField.get(i);

      double x = (point.getElt(0) - xmin) / groundSampleDistance;
      double y = (point.getElt(1) - ymin) / groundSampleDistance;

      int m = (int) (Math.signum(x) * Math.floor(Math.abs(x)));
      if (m < 0 || m >= nX)
        continue;
      int n = (int) (Math.signum(y) * Math.floor(Math.abs(y)));
      if (n < 0 || n >= nY)
        continue;

      Pair<Integer, Integer> pair = Pair.create(m, n);
      DescriptiveStatistics stats = binnedPoints.get(pair);
      if (stats == null) {
        stats = new DescriptiveStatistics();
        binnedPoints.put(pair, stats);
      }
      double residual = point.getElt(2) - refValue[m][n];
      stats.addValue(residual);
    }
    return binnedPoints;
  }

  public void calculateTransformation() throws SpiceException {
    int numPts = pointsList.size();

    double[][] points = new double[3][numPts];
    for (int i = 0; i < numPts; i++) {
      Vector3 translated = pointsList.get(i).sub(translation);
      for (int j = 0; j < 3; j++)
        points[j][i] = translated.getElt(j);
    }

    // Follow the same logic as Mapola.fitPlaneToMapola()

    RealMatrix pointMatrix = new Array2DRowRealMatrix(points, false);

    // Now do SVD on this matrix
    SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
    RealMatrix u = svd.getU();

    // uz points normal to the plane and equals the eigenvector
    // corresponding to the smallest eigenvalue of the V matrix
    uz = new Vector3(u.getColumn(2)).hat();

    setRotationFromUz(uz);
  }

  /**
   * Set the rotation matrix given the "up" vector. Call this AFTER
   * {@link #setXYZ(double[], double[], double[])}.
   * 
   * @param uz
   * @throws SpiceException
   */
  public void setRotationFromUz(Vector3 uz) throws SpiceException {
    Vector3 ux, uy;
    // Make sure uz points away from the asteroid rather than towards it
    // by looking at the dot product of uz and the centroid. If dot product
    // is negative, reverse uz.
    if (translation.dot(uz) <= 0.0)
      uz = uz.negate();

    uz = uz.hat();

    // new code for ux, uy, uz. Based on Bob Gaskell code in COMMON/ORIENT.f
    if (uz.getElt(2) > 0.9998D) {
      // z closest to pointing north (i.e. at north pole)
      uz = new Vector3(0, 0, 1);
      uy = new Vector3(0, 1, 0);
    } else if (uz.getElt(2) < -0.9998D) {
      // z closest to pointing south (i.e. at south pole)
      uz = new Vector3(0, 0, -1);
      uy = new Vector3(0, 1, 0);
    } else {
      // initial y vector to be orthogonal to z
      uy = new Vector3(-uz.getElt(1), uz.getElt(0), 0).hat();
    }

    ux = uy.cross(uz);
    uy = uz.cross(ux);

    rotation = new Matrix33(ux, uy, uz);
  }

  /**
   * Transform points in the local coordinate system to global
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mtxv(point).add(translation);
   * }
   * </pre>
   * 
   * @return
   */
  public List<Vector3> localToGlobal(List<Vector3> points) {
    ArrayList<Vector3> transformed = new ArrayList<>();
    for (Vector3 point : points) {
      Vector3 transformedPoint = rotation.mtxv(point).add(translation);
      transformed.add(transformedPoint);
    }

    return transformed;
  }

  /**
   * Transform points in the global coordinate system to local
   * 
   * <pre>
   * {@code
   * transformedPoint = rotation.mxv(point.sub(translation));
   * }
   * </pre>
   * 
   * @return
   */
  public List<Vector3> globalToLocal(List<Vector3> points) {
    ArrayList<Vector3> transformed = new ArrayList<>();
    for (Vector3 point : points) {
      Vector3 transformedPoint = rotation.mxv(point.sub(translation));
      transformed.add(transformedPoint);
    }

    return transformed;
  }

  private void writeBinaryGMTInput(Collection<Vector3> points, String filename) {
    try (DataOutputStream os =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
      for (Vector3 point : points) {
        for (int i = 0; i < 3; i++)
          BinaryUtils.writeDoubleAndSwap(os, point.getElt(i));
        // GMTSurface expects 4 column
        BinaryUtils.writeDoubleAndSwap(os, 1.);
      }

    } catch (IOException | SpiceException e) {
      e.printStackTrace();
    }
  }

  private static PiecewiseBicubicSplineInterpolatingFunction readGMTFits(String fitsFile,
      List<Vector3> points, DescriptiveStatistics xStats, DescriptiveStatistics yStats)
      throws FitsException, IOException {
    int[] axes = new int[3];

    // load data from fits file
    double[][][] data = FitsUtil.loadFits(fitsFile, axes);

    // indices into x,y,z components of position vector of fits file data array
    int xIndex = 3;
    int yIndex = 4;
    int zIndex = 5;

    int numCols = axes[1];
    int numRows = axes[2];

    double[] x = new double[numCols];
    double[] y = new double[numRows];
    double[][] z = new double[numCols][numRows];

    xStats.clear();
    yStats.clear();

    for (int n = 0; n < numCols; ++n) {
      for (int m = 0; m < numRows; ++m) {
        if (m == 0) {
          x[n] = data[xIndex][m][n];
          xStats.addValue(x[n]);
        }
        if (n == 0) {
          y[m] = data[yIndex][m][n];
          yStats.addValue(y[m]);
        }
        z[n][m] = data[zIndex][m][n];
        Vector3 thisPoint = new Vector3(x[n], y[m], z[n][m]);
        points.add(thisPoint);

        // System.out.printf("%d %d %s\n", m, n, thisPoint);
      }
    }

    PiecewiseBicubicSplineInterpolator interpolator = new PiecewiseBicubicSplineInterpolator();
    return interpolator.interpolate(x, y, z);
  }


}
