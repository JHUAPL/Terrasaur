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

import static org.junit.Assert.assertEquals;
import java.io.File;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import spice.basic.AberrationCorrection;
import spice.basic.Body;
import spice.basic.CSPICE;
import spice.basic.KernelDatabase;
import spice.basic.ReferenceFrame;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.StateRecord;
import spice.basic.TDBTime;
import spice.basic.Vector3;

public class RemoveAberrationTest {

  private static final Logger logger = LogManager.getLogger();

  @BeforeClass
  public static void setup() throws SpiceErrorException {
    NativeLibraryLoader.loadSpiceLibraries();

    File lsk = ResourceUtils.writeResourceToFile("/resources/kernels/lsk/naif0012.tls");
    File spk = ResourceUtils.writeResourceToFile("/resources/kernels/spk/de432s.bsp");

    if (!lsk.exists()) {
      logger.error("LSK file {} does not exist!", lsk.getAbsolutePath());
    }
    if (!spk.exists()) {
      logger.error("SPK file {} does not exist!", spk.getAbsolutePath());
    }

    KernelDatabase.load(lsk.getAbsolutePath());
    KernelDatabase.load(spk.getAbsolutePath());

    Log4j2Configurator.getInstance();
  }


  @Test
  public void test01() throws SpiceException {

    // https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/FORTRAN/spicelib/stelab.html

    // light time corrected vector
    double[] pos = {201738.725087, -260893.141602, -147722.589056};
    Vector3 posV = new Vector3(pos);

    // observer velocity wrt SSB
    double[] vel = {28.611751, 5.7275129, 2.4830453};
    Vector3 velV = new Vector3(vel);

    double[] corrected = {201765.929516, -260876.818077, -147714.262441};
    Vector3 correctedV = RemoveAberration.stelab(posV, velV);

    assertEquals(correctedV.sep(new Vector3(corrected)), 0, 1e-12);
  }

  @Test
  public void test02() throws SpiceException {

    Body target = new Body("MOON");
    Body obs = new Body("EARTH");
    Body ssb = new Body(0);
    TDBTime et = new TDBTime("July 4 2004");
    ReferenceFrame j2000 = new ReferenceFrame("J2000");
    StateRecord sr = new StateRecord(target, et, j2000, new AberrationCorrection("LT"), obs);

    Vector3 pos = sr.getPosition();

    sr = new StateRecord(obs, et, j2000, new AberrationCorrection("NONE"), ssb);
    Vector3 vel = sr.getVelocity();

    Vector3 corrected = RemoveAberration.stelab(pos, vel);

    logger.printf(Level.DEBUG, "Uncorrected position vector (LT):    %f %f %f", pos.getElt(0),
        pos.getElt(1), pos.getElt(2));
    logger.printf(Level.DEBUG, "Velocity vector:                     %f %f %f", vel.getElt(0),
        vel.getElt(1), vel.getElt(2));
    logger.printf(Level.DEBUG, "Corrected position vector:           %f %f %f", corrected.getElt(0),
        corrected.getElt(1), corrected.getElt(2));

    corrected = new Vector3(CSPICE.stelab(pos.toArray(), vel.toArray()));
    logger.printf(Level.DEBUG, "Corrected position vector (stelab):  %f %f %f", corrected.getElt(0),
        corrected.getElt(1), corrected.getElt(2));

    sr = new StateRecord(target, et, j2000, new AberrationCorrection("LT+S"), obs);
    corrected = sr.getPosition();
    logger.printf(Level.DEBUG, "Corrected position vector (LT+S):    %f %f %f", corrected.getElt(0),
        corrected.getElt(1), corrected.getElt(2));

    assertEquals(RemoveAberration.stelab(pos, vel).sep(corrected), 0, 1e-12);
  }

  @Test
  public void test03() throws SpiceException {
    Body target = new Body("MOON");
    Body obs = new Body("EARTH");
    TDBTime et = new TDBTime("July 4 2004");
    ReferenceFrame j2000 = new ReferenceFrame("J2000");

    RemoveAberration ra = new RemoveAberration(target, obs);

    StateRecord sr = new StateRecord(target, et, j2000, new AberrationCorrection("LT+S"), obs);
    Vector3 corrected = sr.getPosition();
    logger.printf(Level.DEBUG, "Corrected position vector (LT+S):        %f %f %f",
        corrected.getElt(0), corrected.getElt(1), corrected.getElt(2));

    sr = new StateRecord(target, et, j2000, new AberrationCorrection("NONE"), obs);
    Vector3 uncorrected = sr.getPosition();
    logger.printf(Level.DEBUG, "Uncorrected position vector (NONE):      %f %f %f",
        uncorrected.getElt(0), uncorrected.getElt(1), uncorrected.getElt(2));

    uncorrected = ra.getGeometricPosition(et, corrected);
    logger.printf(Level.DEBUG, "Uncorrected position vector (Estimated): %f %f %f",
        uncorrected.getElt(0), uncorrected.getElt(1), uncorrected.getElt(2));

    assertEquals(sr.getPosition().sep(uncorrected), 0, 1e-8);
  }
}
