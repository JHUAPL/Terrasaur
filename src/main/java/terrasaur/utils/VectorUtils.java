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
