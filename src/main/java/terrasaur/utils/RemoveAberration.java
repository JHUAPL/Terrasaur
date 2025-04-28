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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.jafama.FastMath;
import picante.math.functions.DifferentiableUnivariateFunction;
import picante.math.functions.DifferentiableUnivariateFunctions;
import picante.math.functions.UnivariateFunction;
import picante.math.intervals.IntervalSet;
import picante.math.intervals.UnwritableInterval;
import picante.roots.RootFinder;
import picante.roots.Stepper;
import picante.roots.Steppers;
import spice.basic.AberrationCorrection;
import spice.basic.AxisAndAngle;
import spice.basic.Body;
import spice.basic.Matrix33;
import spice.basic.PhysicalConstants;
import spice.basic.ReferenceFrame;
import spice.basic.SpiceException;
import spice.basic.StateRecord;
import spice.basic.TDBDuration;
import spice.basic.TDBTime;
import spice.basic.Vector3;

/**
 * Given a position vector including light time and aberration effects, solve for the geometric
 * position vector that would have been calculated with {@link AberrationCorrection}("NONE"). We
 * will also need to know
 * <ul>
 * <li>The geometric position and velocity of the target relative to the SSB</li>
 * <li>The geometric velocity of the observer relative to the SSB</li>
 * </ul>
 * 
 * This class assumes all necessary kernels have been loaded. The input position vector is assumed
 * to be a better estimate of the apparent position than the value calculated from the SPICE
 * kernels. The SPICE kernels are used to get an initial estimate of the aberration.
 * 
 * @author nairah1
 *
 */
public class RemoveAberration {

  private static final Logger logger = LogManager.getLogger();

  private Body target;
  private Body observer;
  private final ReferenceFrame J2000;
  private final AberrationCorrection NONE;
  private final Body SSB;

  public RemoveAberration(Body target, Body observer) throws SpiceException {
    this.target = target;
    this.observer = observer;
    J2000 = new ReferenceFrame("J2000");
    NONE = new AberrationCorrection("NONE");
    SSB = new Body(0);
  }

  /**
   * 
   * @param t ephemeris time
   * @param targetLTS observer to target vector calculated with LT+S. This must be in J2000.
   * @return a geometric vector from observer to target in the inertial frame at time t
   * @throws SpiceException
   */
  public Vector3 getGeometricPosition(TDBTime t, Vector3 targetLTS) throws SpiceException {

    TDBDuration lightTime = new TDBDuration(targetLTS.norm() / PhysicalConstants.CLIGHT);
    TDBTime tmlt = t.sub(lightTime);

    // geometric states of target and observer
    StateRecord targetState = new StateRecord(target, tmlt, J2000, NONE, SSB);
    StateRecord observerState = new StateRecord(observer, t, J2000, NONE, SSB);

    // estimate aberration magnitude
    StateRecord spiceLT =
        new StateRecord(target, t, J2000, new AberrationCorrection("LT"), observer);
    StateRecord spiceLTS =
        new StateRecord(target, t, J2000, new AberrationCorrection("LT+S"), observer);

    double angle = spiceLT.getPosition().sep(spiceLTS.getPosition());
    double step = angle / 10;

    DifferentiableUnivariateFunction func = dfunc(targetLTS, observerState.getVelocity(), step);

    RootFinder finder = RootFinder.create();
    Stepper stepper = Steppers.createConstant(step);

    IntervalSet boundingInterval =
        IntervalSet.create(new UnwritableInterval(-2 * angle, 2 * angle));

    IntervalSet angle0 = finder.locateValue(func, 0., boundingInterval, stepper);

    Vector3 geometricObsToTarget = new Vector3();
    if (angle0.size() > 0) {

      Vector3 axis = targetLTS.cross(observerState.getVelocity());
      Matrix33 m = new AxisAndAngle(axis, angle0.get(0).getBegin()).toMatrix();

      // this is target - observer
      Vector3 targetLT = m.mxv(targetLTS);

      // targetState was evaluated at t - lt. This is the observer position relative to the SSB.
      Vector3 observerPos = targetState.getPosition().sub(targetLT);
      targetState = new StateRecord(target, t, J2000, NONE, SSB);
      geometricObsToTarget = targetState.getPosition().sub(observerPos);
    } else {
      logger.error(this.getClass().getSimpleName() + ": no solution");
      System.exit(0);
    }

    return geometricObsToTarget;
  }

  /**
   * From https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/FORTRAN/req/abcorr.html:
   * 
   * Let r be the light time corrected vector from the observer to the object, and v be the velocity
   * of the observer with respect to the solar system barycenter. Let w be the angle between them.
   * The aberration angle phi is given by
   * 
   * <pre>
                 sin(phi) = v sin(w)
                            --------
                            c
   * </pre>
   * 
   * Let h be the vector given by the cross product
   * 
   * <pre>
                 h = r X v
   * </pre>
   * 
   * Rotate r by phi radians about h to obtain the apparent position of the object.
   * 
   * <p>
   * Also see <a href=
   * "https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/FORTRAN/spicelib/stelab.html">stelab.html</a>
   * 
   * @param targetLT
   * @param observerVelocity
   * @return
   * @throws SpiceException
   */
  public static Vector3 stelab(Vector3 targetLT, Vector3 observerVelocity) throws SpiceException {
    Vector3 axis = targetLT.cross(observerVelocity);
    double w = targetLT.sep(observerVelocity);
    double phi =
        FastMath.asin(observerVelocity.norm() * FastMath.sin(w) / PhysicalConstants.CLIGHT);
    Matrix33 m = new AxisAndAngle(axis, phi).toMatrix();
    Vector3 aberratedVector = m.mxv(targetLT);
    return aberratedVector;
  }

  private DifferentiableUnivariateFunction dfunc(Vector3 targetLTS, Vector3 observerVelocity,
      double step) {

    Vector3 axis = targetLTS.cross(observerVelocity);

    UnivariateFunction func = new UnivariateFunction() {

      /**
       * Rotate the targetLTS vector by angle. Treat this as a light-time corrected position.
       * Correct for the aberration due to this position and compare to targetLTS.
       * <p>
       * This function evaluates to zero if the two vectors agree. Rotate the LT+S vector by the
       * input angle to get the LT vector.
       */
      @Override
      public double evaluate(double angle) {
        try {
          Matrix33 m = new AxisAndAngle(axis, angle).toMatrix();
          Vector3 targetLT = m.mxv(targetLTS);
          Vector3 aberratedVector = stelab(targetLT, observerVelocity);
          double sign = Math.signum(aberratedVector.cross(targetLTS).dot(axis));

          return sign * aberratedVector.sep(targetLTS);
        } catch (SpiceException e) {
          logger.error(e.getLocalizedMessage(), e);
          return Double.NaN;
        }
      }

    };

    return DifferentiableUnivariateFunctions.quadraticApproximation(func, step);

  }

}
