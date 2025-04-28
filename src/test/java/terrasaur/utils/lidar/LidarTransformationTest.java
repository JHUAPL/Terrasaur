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
package terrasaur.utils.lidar;

import static org.junit.Assert.assertTrue;
import java.io.File;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.BeforeClass;
import org.junit.Test;
import terrasaur.utils.ResourceUtils;

public class LidarTransformationTest {

  private static LidarTransformation testTransform;

  @BeforeClass
  public static void setup() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append(
        "\"translation\" : [ -2.7063546673442025e-06 , -3.2956184403440832e-06 , -3.1679187973381386e-06 ],");
    sb.append(
        "\"rotation\" : [ 9.9999990651910198e-01 , -3.6506246582060722e-04 , 1.7564641331158977e-04 , -1.5112749854652996e-04 ],");
    sb.append(
        "\"centerOfRotation\" : [ -2.1905955000000015e-02 , -7.5626499999999685e-04 , -1.4996199999999572e-03 ],");
    sb.append("\"startId\" : 0,");
    sb.append("\"stopId\" : 0,");
    sb.append("\"minErrorBefore\" : 9.2036295980522610e-14,");
    sb.append("\"maxErrorBefore\" : 4.6214268134231436e-03,");
    sb.append("\"rmsBefore\" : 4.9683609628854209e-04,");
    sb.append("\"meanErrorBefore\" : 3.1790339970386226e-04,");
    sb.append("\"stdBefore\" : 3.8181610106432639e-04,");
    sb.append("\"minErrorAfter\" : 5.3934126541711475e-09,");
    sb.append("\"maxErrorAfter\" : 4.6591641396458482e-03,");
    sb.append("\"rmsAfter\" : 4.9685848009884737e-04,");
    sb.append("\"meanErrorAfter\" : 3.2673464939622047e-04,");
    sb.append("\"stdAfter\" : 3.7431646788521823e-04");
    sb.append("}");

    testTransform = LidarTransformation.fromJSON(sb.toString());
  }

  @Test
  public void testReader() {
    String path = "/lidar/transformationfile.txt";
    File file = ResourceUtils.writeResourceToFile(path);
    LidarTransformation transform = LidarTransformation.fromJSON(file);

    assertTrue(Vector3D.angle(transform.getTranslation(), testTransform.getTranslation()) < 1e-5);
  }



}
