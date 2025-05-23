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

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public abstract class GravityOptions {

  public enum ALGORITHM {
    WERNER, CHENG
  };

  public enum EVALUATION {
    AVERAGE_VERTICES("--average-vertices"), CENTERS("--centers"), FILE("--file"), VERTICES(
        "--vertices");

    public final String commandString;

    private EVALUATION(String commandString) {
      this.commandString = commandString;
    }

  }

  /** Path to shape model file in OBJ or Gaskell PLT format */
  public abstract String plateModelFile();

  @Value.Default
  /** Density of shape model in g/cm^3 (default is 1) */
  public double density() {
    return 1.;
  }

  @Value.Default
  /** Rotation rate of shape model in radians/sec (default is 0) */
  public double rotation() {
    return 1.;
  }

  @Value.Default
  /**
   * gravitational constant to use. Units are (in g cm^3/s^2) (default is 6.67408e-11)
   */
  public double gravConstant() {
    return 6.67408e-11;
  }

  @Value.Default
  /** Algorithm for computing gravity {@link ALGORITHM#WERNER} or {@link ALGORITHM#CHENG} */
  public ALGORITHM algorithm() {
    return ALGORITHM.WERNER;
  }

  @Value.Default
  /** Where to evaluate gravity. Default is {@link EVALUATION#CENTERS} */
  public EVALUATION evaluation() {
    return EVALUATION.CENTERS;
  }

  /** name of file containing points for gravity evaluation */
  public abstract Optional<String> fieldPointsFile();

  /**
   * If {@link #evaluation()} is {@link EVALUATION#FILE}, then use this option to specify the
   * reference potential (in J/kg) which is needed for calculating elevation. If
   * {@link EVALUATION#FILE} is used but refPotential is not set then no elevation data is saved
   * out.
   */
  public abstract Optional<Double> refPotential();

  @Value.Default
  /**
   * If {@link #evaluation()} is {@link EVALUATION#FILE}, specify default column of the x coordinate
   * in the input file. Default is 0.
   */
  public int columnX() {
    return 0;
  }

  @Value.Default
  /**
   * If {@link #evaluation()} is {@link EVALUATION#FILE}, specify default column of the y coordinate
   * in the input file. Default is 1.
   */
  public int columnY() {
    return 1;
  }

  @Value.Default
  /**
   * If {@link #evaluation()} is {@link EVALUATION#FILE}, specify default column of the z coordinate
   * in the input file. Default is 2.
   */
  public int columnZ() {
    return 2;
  }

  /** Index of first plate to process. Useful for parallelizing large shape models. Default is 0. */
  public abstract Optional<Integer> startIndex();

  /**
   * Number of plates to process. Useful for parallelizing large shape models. Default is all
   * plates.
   */
  public abstract Optional<Integer> numPlates();

  @Value.Default
  /**
   * If specified, the suffix will be appended to all output files. This is needed when splitting
   * large shape models into multiple runs so that each run will be output to different files.
   */
  public String suffix() {
    return "";
  }

  /** Path to folder in which to place output files (default is current directory). */
  public abstract Optional<String> outputFolder();

}
