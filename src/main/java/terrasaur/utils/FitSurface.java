package terrasaur.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.jafama.FastMath;

/**
 * Based on IDL's sfit routine. Fit a 2D polynomial to an input set of points. The fitting function
 * is <br>
 * f(x,y) = &#931;k<sub>i,j</sub>x<sup>i</sup>y<sup>j</sup> <br>
 * 
 * Input points are assumed to be in a local coordinate system where X and Y are the coordinates in
 * the reference plane and Z is the height above the plane. This may require transforming the
 * original set of points to a local coordinate system with {@link FitPlane} before using this
 * class.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class FitSurface implements MultivariateFunction {

  private final static Logger logger = LogManager.getLogger(FitSurface.class);

  private List<Vector3D> points;
  private int degree;
  private double[][] coefficients;

  /**
   * 
   * @param points points to fit
   * @param degree degree of fitting polynomial (i.e. degree 3 means highest term in the polynomial
   *        is x<sup>3</sup>y<sup>3</sup>)
   */
  public FitSurface(List<Vector3D> points, int degree) {
    this.points = points;
    this.degree = degree;
    fit();
  }

  /**
   * 
   * @param x in local coordinates
   * @param y in local coordinates
   * @return function value
   */
  public double value(double x, double y) {
    List<Double> terms = new ArrayList<>();
    for (int i = 0; i <= degree; i++) {
      double xi = FastMath.pow(x, i);
      for (int j = 0; j <= degree; j++) {
        double yj = FastMath.pow(y, j);
        terms.add(coefficients[i][j] * xi * yj);
      }
    }

    Collections.sort(terms, new Comparator<Double>() {
      @Override
      public int compare(Double o1, Double o2) {
        return Double.compare(Math.abs(o1), Math.abs(o2));
      }
    });
    double f = 0;
    for (Double term : terms)
      f += term;
    return f;
  }

  @Override
  public double value(double[] arg0) {
    return value(arg0[0], arg0[1]);
  }

  private void fit() {
    int n2 = (degree + 1) * (degree + 1);

    RealMatrix x = MatrixUtils.createRealMatrix(points.size(), 1);
    RealMatrix y = MatrixUtils.createRealMatrix(points.size(), 1);
    RealMatrix z = MatrixUtils.createRealMatrix(points.size(), 1);

    for (int i = 0; i < points.size(); i++) {
      Vector3D v = points.get(i);
      x.setEntry(i, 0, v.getX());
      y.setEntry(i, 0, v.getY());
      z.setEntry(i, 0, v.getZ());
    }

    if (points.size() < n2) {
      logger.warn("{} points supplied, need at least {} for fitting degree {}", points.size(), n2,
          degree);
    }

    RealMatrix ut = MatrixUtils.createRealMatrix(points.size(), n2);

    for (int ip = 0; ip < points.size(); ip++) {
      for (int i = 0; i <= degree; i++) {
        double xi = Math.pow(x.getEntry(ip, 0), i);
        for (int j = 0; j <= degree; j++) {
          double yj = Math.pow(y.getEntry(ip, 0), j);
          ut.setEntry(ip, i * (degree + 1) + j, xi * yj);
        }
      }
    }

    RealMatrix kk = ut.multiply(MatrixUtils.inverse(ut.transpose().multiply(ut)));
    RealMatrix kx1 = kk.transpose().multiply(z);
    coefficients = new double[degree + 1][degree + 1];
    for (int i = 0; i <= degree; i++) {
      for (int j = 0; j <= degree; j++) {
        coefficients[i][j] = kx1.getEntry(i * (degree + 1) + j, 0);
      }
    }
  }


}
