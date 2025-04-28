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

/**
 * Class to read/write floating point numbers into two bytes. Uses the 16-bit base 2 format
 * (binary16) from IEEE 754-2008.
 * <p>
 * From <a href=
 * "https://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java/6162687">Stack
 * Overflow</a>
 * 
 * @author nairah1
 *
 */
public class Binary16 {

  /**
   * Calculate a floating point value from the lower 16 bits of a 32 bit integer. The upper 16 bits
   * are ignored.
   * 
   * @param hbits
   * @return
   */
  // ignores the higher 16 bits
  public static float toFloat(int hbits) {
    int mant = hbits & 0x03ff; // 10 bits mantissa
    int exp = hbits & 0x7c00; // 5 bits exponent
    if (exp == 0x7c00) // NaN/Inf
      exp = 0x3fc00; // -> NaN/Inf
    else if (exp != 0) // normalized value
    {
      exp += 0x1c000; // exp - 15 + 127
      if (mant == 0 && exp > 0x1c400) // smooth transition
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
    } else if (mant != 0) // && exp==0 -> subnormal
    {
      exp = 0x1c400; // make it normal
      do {
        mant <<= 1; // mantissa * 2
        exp -= 0x400; // decrease exp by 1
      } while ((mant & 0x400) == 0); // while not normal
      mant &= 0x3ff; // discard subnormal bit
    } // else +/-0 -> +/-0
    return Float.intBitsToFloat( // combine all parts
        (hbits & 0x8000) << 16 // sign << ( 31 - 15 )
            | (exp | mant) << 13); // value << ( 23 - 10 )
  }

  /**
   * Calculate a 16 bit representation of a floating point value. The upper 16 bits of the result
   * are 0.
   * 
   * 
   * @param fval
   * @return
   */
  // returns all higher 16 bits as 0 for all results
  public static int fromFloat(float fval) {
    int fbits = Float.floatToIntBits(fval);
    int sign = fbits >>> 16 & 0x8000; // sign only
    int val = (fbits & 0x7fffffff) + 0x1000; // rounded value

    if (val >= 0x47800000) // might be or become NaN/Inf
    { // avoid Inf due to rounding
      if ((fbits & 0x7fffffff) >= 0x47800000) { // is or must become NaN/Inf
        if (val < 0x7f800000) // was value but too large
          return sign | 0x7c00; // make it +/-Inf
        return sign | 0x7c00 | // remains +/-Inf or NaN
            (fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
      }
      return sign | 0x7bff; // unrounded not quite Inf
    }
    if (val >= 0x38800000) // remains normalized value
      return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
    if (val < 0x33000000) // too small for subnormal
      return sign; // becomes +/-0
    val = (fbits & 0x7fffffff) >>> 23; // tmp exp for subnormal calc
    return sign | ((fbits & 0x7fffff | 0x800000) // add subnormal bit
        + (0x800000 >>> val - 102) // round depending on cut off
        >>> 126 - val); // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
  }

}
