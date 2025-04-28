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
package terrasaur.utils.saaPlotLib.colorMaps.tables;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * These are colormaps from <a href=
 * "https://matplotlib.org/tutorials/colors/colormaps.html">https://matplotlib.org/tutorials/colors/colormaps.html</a>.
 * I used this python code to convert RGB to int:
 *
 * <pre>
 * {@code
 * import matplotlib
 * import matplotlib.cm as cm
 *
 * def from_cmap(name):
 * cmap = cm.get_cmap(name)
 *
 * print('private static int [] matplotlib_%s = {' % name)
 * for i in range(cmap.N):
 * hex=matplotlib.colors.rgb2hex(cmap(i)).replace('#','0x')
 * dec=int(hex,16)
 * print('%d,' % dec, end='')
 * print('};')
 * }
 * </pre>
 *
 * @author nairah1
 *
 */
public class Matplotlib extends ColorTable {

  private static final int[] viridis = {
    4456788, 4457046, 4523095, 4523353, 4589402, 4589660, 4590173, 4590430, 4656480, 4656737,
    4657251, 4657508, 4658021, 4723815, 4724328, 4724585, 4724842, 4725356, 4725613, 4725870,
    4726127, 4726640, 4726897, 4727155, 4727668, 4727925, 4728182, 4728439, 4728952, 4729209,
    4663930, 4664442, 4664699, 4664956, 4665213, 4599934, 4600446, 4600703, 4600960, 4535681,
    4536193, 4536450, 4471171, 4471427, 4471684, 4406660, 4406917, 4341637, 4341894, 4342150,
    4276871, 4277383, 4212104, 4212360, 4147080, 4147337, 4082057, 4082313, 4082826, 4017546,
    4017802, 3952522, 3952779, 3887499, 3887755, 3822475, 3822732, 3757452, 3757708, 3692684,
    3692940, 3627660, 3627917, 3562637, 3562893, 3497613, 3497869, 3432589, 3432845, 3367565,
    3367821, 3302542, 3302798, 3237518, 3237774, 3238030, 3172750, 3173006, 3107726, 3107982,
    3042702, 3042958, 3043214, 2977934, 2978190, 2912654, 2912910, 2913166, 2847886, 2848142,
    2782862, 2783118, 2783374, 2718094, 2718350, 2718606, 2653326, 2653582, 2588302, 2588558,
    2588814, 2523534, 2523790, 2523790, 2458510, 2458766, 2459022, 2393742, 2393998, 2328718,
    2328974, 2329229, 2263949, 2264205, 2264461, 2199181, 2199437, 2199693, 2199948, 2134668,
    2134668, 2134924, 2069644, 2069899, 2070155, 2070411, 2070667, 2070922, 2071178, 2005898,
    2006153, 2006409, 2072201, 2072456, 2072712, 2072968, 2072967, 2073223, 2139014, 2139270,
    2205061, 2205317, 2271109, 2271364, 2337155, 2402947, 2468738, 2468994, 2534785, 2600321,
    2666112, 2731903, 2797695, 2929022, 2994813, 3060604, 3126396, 3257723, 3323514, 3454585,
    3520377, 3651704, 3717495, 3848822, 3914613, 4045940, 4177011, 4242802, 4374129, 4505456,
    4636783, 4768110, 4899181, 5030508, 5161835, 5293162, 5424489, 5555560, 5686887, 5818213,
    5949540, 6080611, 6211938, 6343264, 6540127, 6671198, 6802524, 6933851, 7130458, 7261784,
    7393111, 7589974, 7721044, 7852371, 8048977, 8180304, 8377166, 8508237, 8705099, 8836425,
    9033032, 9164358, 9360965, 9492291, 9688897, 9820224, 10016830, 10213692, 10344763, 10541625,
    10672695, 10869558, 11066164, 11197490, 11394096, 11590959, 11722029, 11918891, 12115497,
    12246568, 12443430, 12640037, 12771107, 12967969, 13164576, 13295903, 13492509, 13689116,
    13820443, 14017050, 14213657, 14344985, 14541592, 14672664, 14869528, 15066137, 15197209,
    15394074, 15525147, 15721756, 15852829, 16049694, 16180768, 16311841, 16508707, 16639781
  };
  public static final Matplotlib VIRIDIS = new Matplotlib(viridis, FAMILY.LINEAR);

  private final int[] colors;

  private Matplotlib(int[] colors, ColorTable.FAMILY family) {
    super(family);
    this.colors = colors;
  }

  @Override
  public List<Color> getColors() {
    List<Color> colors = new ArrayList<>();
    for (int color : this.colors) colors.add(new Color(color));
    return colors;
  }
}
