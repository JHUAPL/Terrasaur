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
package terrasaur.fits;

import terrasaur.enums.AltwgDataType;
import terrasaur.enums.SrcProductType;

public class FitsData {

  private final double[][][] data;
  private final double[] V;
  private final double[] ux;
  private final double[] uy;
  private final double[] uz;
  private final double scale;
  private final double gsd;
  private final boolean hasV;
  private final boolean hasUnitv;
  private final boolean hasGsd;
  private final boolean isGlobal;
  private final boolean hasAltType;
  private final AltwgDataType altProd;
  private final String dataSource;

  private FitsData(FitsDataBuilder b) {
    this.data = b.data;
    this.V = b.V;
    this.ux = b.ux;
    this.uy = b.uy;
    this.uz = b.uz;
    this.scale = b.scale;
    this.gsd = b.gsd;
    this.hasV = b.hasV;
    this.hasUnitv = b.hasUnitv;
    this.hasGsd = b.hasGsd;
    this.hasAltType = b.hasAltType;
    this.isGlobal = b.isGlobal;
    this.altProd = b.altProd;
    this.dataSource = b.dataSource;
  }

  public AltwgDataType getAltProdType() {
    return this.altProd;
  }

  public String getSrcProdType() {
    return this.dataSource;
  }

  public double[][][] getData() {
    return this.data;
  }

  public boolean hasV() {
    return this.hasV;
  }

  public double[] getV() {
    return this.V;
  }

  public boolean hasUnitv() {
    return this.hasUnitv;
  }

  public double[] getUnit(UnitDir udir) {
    switch (udir) {
      case UX:
        return this.ux;

      case UY:
        return this.uy;

      case UZ:
        return this.uz;

      default:
        throw new RuntimeException();
    }
  }

  public double getScale() {
    return this.scale;
  }

  public boolean hasGsd() {
    return this.hasGsd;
  }

  public boolean hasAltType() {
    return this.hasAltType;
  }

  public boolean isGlobal() {
    return this.isGlobal;
  }

  public double getGSD() {
    if (this.hasGsd) {
      return this.gsd;
    } else {
      String errMesg = "ERROR! fitsData does not have gsd!";
      throw new RuntimeException(errMesg);
    }
  }


  public static class FitsDataBuilder {
    private final double[][][] data;
    private double[] V = null;
    private double[] ux = null;
    private double[] uy = null;
    private double[] uz = null;
    private boolean hasV = false;
    private boolean hasUnitv = false;
    private boolean hasGsd = false;
    private boolean isGlobal = false;
    private boolean hasAltType = false;
    private double scale = Double.NaN;
    private double gsd = Double.NaN;
    private AltwgDataType altProd = null;
    private String dataSource = SrcProductType.UNKNOWN.toString();

    /**
     * Constructor. isGlobal used to fill out fits keyword describing whether data is local or
     * global. May also be used for fits naming convention.
     * 
     * @param data
     * @param isGlobal
     */
    public FitsDataBuilder(double[][][] data, boolean isGlobal) {
      this.data = data;
      this.isGlobal = isGlobal;
    }

    public FitsDataBuilder setAltProdType(AltwgDataType altProd) {
      this.altProd = altProd;
      this.hasAltType = true;
      return this;
    }

    public FitsDataBuilder setDataSource(String dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public FitsDataBuilder setV(double[] V) {
      this.V = V;
      this.hasV = true;
      return this;
    }

    public FitsDataBuilder setU(double[] uvec, UnitDir udir) {
      switch (udir) {
        case UX:
          this.ux = uvec;
          this.hasUnitv = true;
          break;

        case UY:
          this.uy = uvec;
          this.hasUnitv = true;
          break;

        case UZ:
          this.uz = uvec;
          this.hasUnitv = true;
          break;

        default:
          throw new RuntimeException();

      }
      return this;
    }

    public FitsDataBuilder setScale(double scale) {
      this.scale = scale;
      return this;
    }

    public FitsDataBuilder setGSD(double gsd) {
      this.gsd = gsd;
      this.hasGsd = true;
      return this;
    }

    public FitsData build() {
      return new FitsData(this);
    }
  }
}
