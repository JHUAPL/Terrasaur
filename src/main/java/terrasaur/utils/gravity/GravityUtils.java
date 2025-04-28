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
package terrasaur.utils.gravity;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.utils.gravity.GravityOptions.EVALUATION;

public class GravityUtils {

  private final static Logger logger = LogManager.getLogger(GravityUtils.class);

  /**
   * Modify potential due to gravity from external body.
   * <p>
   * TODO: add estimate for Hill Sphere radius, spherical harmonic expansion for primary
   * 
   * @param plateResults self-gravity acceleration and potential
   * @param externalMass mass of perturbing body, kg
   * @param externalXYZ position in body fixed coordinates of perturbing body, km
   * @param gravConst gravitational constant, m^3/kgs^2
   * @return updated acceleration and potential
   */
  public static List<GravityResult> addExternalBody(List<GravityResult> plateResults,
      double externalMass, double[] externalXYZ, double gravConst) {
    double totalArea = 0;
    for (GravityResult gr : plateResults)
      totalArea += gr.getArea();

    logger.info(String.format(
        "Adding contribution from external body.  Mass %.3e kg, position %.3e %.3e %.3e km, total area %.3e km^2\n",
        externalMass, externalXYZ[0], externalXYZ[1], externalXYZ[2], totalArea));

    // first find potential and acceleration at body center
    double R = new Vector3D(externalXYZ).getNorm() * 1e3; // meters

    // results are in units of J/kg and m/s^2
    // GM is in SI units
    final double GM = gravConst * externalMass;
    double gMag = GM / R / R;
    double[] gCenter = new double[3];
    double pCenter = -GM / R;
    for (int i = 0; i < 3; i++)
      gCenter[i] = gMag * externalXYZ[i] * 1e3 / R;

    List<GravityResult> updatedPlateResults = new ArrayList<>();
    for (GravityResult gr : plateResults) {
      Vector3D bodyToPoint = new Vector3D(gr.getXYZ()).subtract(new Vector3D(externalXYZ));
      double dist = bodyToPoint.getNorm() * 1e3; // meters
      double potential = -GM / dist;

      double[] acc = gr.getAcc();

      double[] bodyToPointArray = bodyToPoint.toArray();
      for (int i = 0; i < 3; i++)
        acc[i] += potential / dist * (bodyToPointArray[i] * 1e3 / dist) - gCenter[i];
      potential -= pCenter;
      potential += gr.getPotential();
      updatedPlateResults.add(new GravityResult(gr.getIndex(), gr.getXYZ(), potential, acc));
    }

    return updatedPlateResults;
  }

  public static String buildCommand(GravityOptions options) {

    StringBuilder sb = new StringBuilder();

    sb.append("gravity ");
    sb.append(String.format("-d %.16e ", options.density()));
    sb.append(String.format("-r %.16e ", options.rotation()));
    sb.append(String.format("--%s ", options.algorithm().name().toLowerCase()));
    sb.append(String.format("--%s ", options.evaluation().commandString));
    if (options.evaluation() == EVALUATION.FILE)
      sb.append(String.format("%s ", options.fieldPointsFile().get()));
    sb.append(String.format("--start-index %d ", options.startIndex().get()));
    sb.append(
        String.format("--end-index %d ", options.startIndex().get() + options.numPlates().get()));
    sb.append(String.format("--suffix %s ", options.suffix()));
    sb.append(String.format("--output-folder %s ", options.outputFolder().get()));
    sb.append(String.format("-gravConst %.16e ", options.gravConstant()));
    sb.append(options.plateModelFile());

    return sb.toString();

  }

}
