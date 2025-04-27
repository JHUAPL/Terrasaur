package terrasaur.smallBodyModel;

import org.junit.Test;

public class SBMTStructureTest {

  @Test
  public void test() {

    String line =
        "1    default -261.61622569431825 2.0874520245281474  -1.3419589274656403 -0.2938864395072801 179.542843137706    261.6279951692062   NA  NA  NA  NA  0.4 1.0 0.0 255,0,255   \"\"";

    SBMTStructure s = SBMTStructure.fromString(line);

    System.out.println(line);
    System.out.println(s.toString());

  }
}
