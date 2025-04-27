package terrasaur.utils;

import java.util.Random;
import org.junit.Test;

public class Binary16Test {

  @Test
  public void TestBinary16() {
    Random r = new Random();

    for (int i = 0; i < 30; i++) {
      // generate a random number between -1e6 and 1e6
      float f = (float) (Math.pow(10, -12 * r.nextDouble() + 6) * Math.pow(-1, r.nextInt()));
      int fromf = Binary16.fromFloat(f);
      float tof = Binary16.toFloat(fromf);

      // System.out.printf("%16.6g %8d %16.6g\n", f, fromf, tof);
    }
  }

}
