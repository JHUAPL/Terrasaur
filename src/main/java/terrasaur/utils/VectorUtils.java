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

import java.util.Random;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import net.jafama.FastMath;

public class VectorUtils {

  /**
   * Return a random unit vector.
   * 
   * @return
   */
  public static Vector3D randomVector() {
    Random r = new Random();
    double delta = FastMath.asin(2 * r.nextDouble() - 1.0);
    double alpha = FastMath.PI * (2 * r.nextDouble() - 1.0);
    return new Vector3D(alpha, delta);
  }

  /**
   * @param args three floating point numbers separated by commas.
   * @return
   */
  public static Vector3D stringToVector3D(String args) {
    String[] params = args.split(",");
    double[] array = new double[3];
    for (int i = 0; i < 3; i++)
      array[i] = Double.valueOf(params[i].trim()).doubleValue();
    return new Vector3D(array);
  }

}
