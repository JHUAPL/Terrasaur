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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Static methods to work with bits and bytes
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class BinaryUtils {

  static public float readFloatAndSwap(DataInputStream is) throws IOException {
    int intValue = is.readInt();
    intValue = ByteSwapper.swap(intValue);
    return Float.intBitsToFloat(intValue);
  }

  static public double readDoubleAndSwap(DataInputStream is) throws IOException {
    long longValue = is.readLong();
    longValue = ByteSwapper.swap(longValue);
    return Double.longBitsToDouble(longValue);
  }

  static public void writeFloatAndSwap(DataOutputStream os, float value) throws IOException {
    int intValue = Float.floatToRawIntBits(value);
    intValue = ByteSwapper.swap(intValue);
    os.writeInt(intValue);
  }

  static public void writeDoubleAndSwap(DataOutputStream os, double value) throws IOException {
    long longValue = Double.doubleToRawLongBits(value);
    longValue = ByteSwapper.swap(longValue);
    os.writeLong(longValue);
  }


  // This function is taken from
  // http://www.java2s.com/Code/Java/Language-Basics/Utilityforbyteswappingofalljavadatatypes.htm
  static public short swap(short value) {
    int b1 = value & 0xff;
    int b2 = (value >> 8) & 0xff;

    return (short) (b1 << 8 | b2 << 0);
  }

  // This function is taken from
  // http://www.java2s.com/Code/Java/Language-Basics/Utilityforbyteswappingofalljavadatatypes.htm
  static public int swap(int value) {
    int b1 = (value >> 0) & 0xff;
    int b2 = (value >> 8) & 0xff;
    int b3 = (value >> 16) & 0xff;
    int b4 = (value >> 24) & 0xff;

    return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
  }

  // This function is taken from
  // http://www.java2s.com/Code/Java/Language-Basics/Utilityforbyteswappingofalljavadatatypes.htm
  public static long swap(long value) {
    long b1 = (value >> 0) & 0xff;
    long b2 = (value >> 8) & 0xff;
    long b3 = (value >> 16) & 0xff;
    long b4 = (value >> 24) & 0xff;
    long b5 = (value >> 32) & 0xff;
    long b6 = (value >> 40) & 0xff;
    long b7 = (value >> 48) & 0xff;
    long b8 = (value >> 56) & 0xff;

    return b1 << 56 | b2 << 48 | b3 << 40 | b4 << 32 | b5 << 24 | b6 << 16 | b7 << 8 | b8 << 0;
  }

  /**
   * Byte swap a single float value. This function is taken from
   * http://www.java2s.com/Code/Java/Language-Basics/Utilityforbyteswappingofalljavadatatypes.htm
   * This method should NOT be used to read little endian data! Instead, use
   * LittleEndianDataInputStream!
   * 
   * @param value Value to byte swap.
   * @return Byte swapped representation.
   */
  public static float swap(float value) {
    int intValue = Float.floatToIntBits(value);
    intValue = swap(intValue);
    return Float.intBitsToFloat(intValue);
  }

}
