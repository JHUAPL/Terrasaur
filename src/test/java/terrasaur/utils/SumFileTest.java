package terrasaur.utils;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;

public class SumFileTest {

  @Test
  public void testFrustum() throws IOException {

    File file = ResourceUtils.writeResourceToFile("/M605862153F5.SUM");

    List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());

    SumFile sumFile = SumFile.fromLines(lines);

    assertEquals(Vector3D.angle(sumFile.frustum1(),
        new Vector3D(0.4395120989622179, 0.898199076601014, -0.008217886523401116)), 0, 1E-10);
    assertEquals(Vector3D.angle(sumFile.frustum2(),
        new Vector3D(0.43798867001719116, 0.8968954485311167, 0.06119215097329732)), 0, 1E-10);
    assertEquals(Vector3D.angle(sumFile.frustum3(),
        new Vector3D(0.3761137519676121, 0.926529032684429, -0.009077288896014253)), 0, 1E-10);
    assertEquals(Vector3D.angle(sumFile.frustum4(),
        new Vector3D(0.37459032302258627, 0.9252254046145307, 0.060332748600675515)), 0, 1E-10);

  }

}
