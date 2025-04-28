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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PhotometricFunction {

  public abstract double getValue(double cosI, double cosE, double phaseDeg);

  public static PhotometricFunction OREX1 = new PhotometricFunction() {
    @Override
    public double getValue(double cosI, double cosE, double phaseDeg) {
      if (cosI < 0 || cosE < 0)
        return 0;
      double fls = Math.exp(((-0.000000990 * phaseDeg + 0.000269) * phaseDeg - 0.0436) * phaseDeg);
      return fls * cosI / (cosI + cosE);
    }
  };

  public static PhotometricFunction LommelSeeliger = new PhotometricFunction() {
    @Override
    public double getValue(double cosI, double cosE, double phaseDeg) {
      if (cosI < 0 || cosE < 0)
        return 0;
      return 2 * cosI / (cosI + cosE);
    }
  };

  public static PhotometricFunction Lunar = new PhotometricFunction() {
    @Override
    public double getValue(double cosI, double cosE, double phaseDeg) {
      if (cosI < 0 || cosE < 0)
        return 0;
      double phase0 = 60;
      double beta = Math.exp(-phaseDeg / phase0);
      return (1 - beta) * cosI + 2 * beta * cosI / (cosI + cosE);
    }
  };

  public static PhotometricFunction McEwen = new PhotometricFunction() {
    @Override
    public double getValue(double cosI, double cosE, double phaseDeg) {
      if (cosI < 0 || cosE < 0)
        return 0;
      double phase0 = 60;
      double beta = Math.exp(-phaseDeg / phase0);
      return (1 - beta) * cosI + beta * cosI / (cosI + cosE);
    }
  };

  public static PhotometricFunction NoPhase = new PhotometricFunction() {
    @Override
    public double getValue(double cosI, double cosE, double phaseDeg) {
      if (cosI < 0 || cosE < 0)
        return 0;
      double beta = 0.65;
      return (1 - beta) * cosI + beta * cosI / (cosI + cosE);
    }
  };

  public static Map<String, PhotometricFunction> getFuncMap() {
    Map<String, PhotometricFunction> map = new HashMap<>();
    map.put("OREX".toUpperCase(), OREX1);
    map.put("LommelSeeliger".toUpperCase(), LommelSeeliger);
    map.put("Lunar".toUpperCase(), Lunar);
    map.put("McEwen".toUpperCase(), McEwen);
    map.put("NoPhase".toUpperCase(), NoPhase);

    return map;
  }

  public static String getOptionString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Photometric function.  Supported values are ");
    List<String> names = new ArrayList<>(getFuncMap().keySet());
    for (int i = 0; i < names.size() - 1; i++)
      sb.append(String.format("%s, ", names.get(i)));
    sb.append(String.format("or %s.", names.get(names.size() - 1)));
    return sb.toString();
  }

  public static PhotometricFunction getPhotometricFunction(String funcName) {
    Map<String, PhotometricFunction> map = getFuncMap();
    return map.get(funcName.toUpperCase());
  }
}
