package terrasaur.utils.saaPlotLib.data;

import java.util.Random;
import org.junit.Ignore;
import org.junit.Test;

public class PointListTest {

    @Ignore
    @Test
    public void test01(){
        Random r = new Random();
        PointList pl = new PointList();
        for (int i = 0; i < 100; i++) {
            pl.add(r.nextGaussian(), r.nextGaussian());
        }

        System.out.println(pl);

        PointList subSet = pl.subSetX(0.5, 0.5);

        System.out.println(subSet.toString());

        System.out.println(subSet.getY(0.5));
        System.out.println(pl.getY(0.5));
    }

}
