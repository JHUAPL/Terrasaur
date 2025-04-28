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

import picante.math.coords.LatitudinalVector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Contains static list of string utilities.
 *
 * @author espirrc1
 */
public class StringUtil {

  // From
  // https://stackoverflow.com/questions/523871/best-way-to-concatenate-list-of-string-objects
  public static String concatStringsWSep(List<Object> strings, String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (Object s : strings) {
      if (s == null) sb.append(sep).append("NA");
      else sb.append(sep).append(s);
      sep = separator;
    }
    return sb.toString();
  }

  // From
  // https://stackoverflow.com/questions/523871/best-way-to-concatenate-list-of-string-objects
  public static String concatDToStringsWSep(
      List<Double> dataValues, String formatD, String separator, String nanString) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (Double dToConvert : dataValues) {

      if (dToConvert == null) sb.append(sep).append(nanString);
      else {

        // need to check string conversion for each value to see if it is valid
        // String formatD = "% 30.16f";
        String testConvert = String.format(formatD, dToConvert);
        double checkVal = StringUtil.parseSafeD(testConvert);
        if (Double.isNaN(checkVal)) {

          // System.out.println("Error converting dToConvert to string! string:" + testConvert);
          // System.out.println("Writing " + nanString + " instead.");
          sb.append(sep).append(nanString);

        } else {
          sb.append(sep).append(testConvert);
        }
      }
      sep = separator;
    }
    return sb.toString();
  }

  /**
   * Format a double according to the specified string format, then convert back to a double
   *
   * @param formatS - regex string describing format of rawD eg. "%10.3f"
   * @param rawD - double being formatted
   * @return
   */
  public static double formatD(String formatS, double rawD) {

    String doubleS = String.format(formatS, rawD);
    double newD = Double.parseDouble(doubleS);
    return newD;
  }

  /**
   * Convert string value to a double, formatted according to the specified string format.
   *
   * @param formatS
   * @param value
   * @return
   */
  public static double str2fmtD(String formatS, String value) {

    double dVal = parseSafeD(value);
    if (dVal != Double.NaN) {
      return formatD(formatS, dVal);
    } else {
      return Double.NaN;
    }
  }

  /**
   * convert from string to double. Does testing to determine if string represents a valid double.
   * If not then returns Double.NaN;
   *
   * @param myString
   * @return
   */
  public static double parseSafeD(String myString) {
    // final String Digits = "(\\p{Digit}+)";
    // final String HexDigits = "(\\p{XDigit}+)";
    // // an exponent is 'e' or 'E' followed by an optionally
    // // signed decimal integer.
    // final String Exp = "[eE][+-]?" + Digits;
    // final String fpRegex = ("[\\x00-\\x20]*" + // Optional leading "whitespace"
    // "[+-]?(" + // Optional sign character
    // "NaN|" + // "NaN" string
    // "Infinity|" + // "Infinity" string
    //
    // // A decimal floating-point string representing a finite positive
    // // number without a leading sign has at most five basic pieces:
    // // Digits . Digits ExponentPart FloatTypeSuffix
    // //
    // // Since this method allows integer-only strings as input
    // // in addition to strings of floating-point literals, the
    // // two sub-patterns below are simplifications of the grammar
    // // productions from the Java Language Specification, 2nd
    // // edition, section 3.10.2.
    //
    // // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
    // "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +
    //
    // // . Digits ExponentPart_opt FloatTypeSuffix_opt
    // "(\\.(" + Digits + ")(" + Exp + ")?)|" +
    //
    // // Hexadecimal strings
    // "((" +
    // // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
    // "(0[xX]" + HexDigits + "(\\.)?)|" +
    //
    // // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
    // "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +
    //
    // ")[pP][+-]?" + Digits + "))" +
    // "[fFdD]?))" +
    // "[\\x00-\\x20]*");// Optional trailing "whitespace"
    //
    // if (Pattern.matches(fpRegex, myString)) {
    // return Double.valueOf(myString); // Will not throw NumberFormatException
    // } else {
    // // Perform suitable alternative action
    // return Double.NaN;
    // }

    boolean throwException = false;
    return parseSafeDException(myString, throwException);
  }

  /**
   * Allow user to control error handling. If throwException is true then method will throw a
   * runtimeException when it cannot parse a valid double from myString.
   *
   * @param myString
   * @param throwException
   * @return
   */
  public static double parseSafeDException(String myString, boolean throwException) {

    final String Digits = "(\\p{Digit}+)";
    final String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final String Exp = "[eE][+-]?" + Digits;
    final String fpRegex =
        ("[\\x00-\\x20]*"
            + // Optional leading "whitespace"
            "[+-]?("
            + // Optional sign character
            "NaN|"
            + // "NaN" string
            "Infinity|"
            + // "Infinity" string

            // A decimal floating-point string representing a finite positive
            // number without a leading sign has at most five basic pieces:
            // Digits . Digits ExponentPart FloatTypeSuffix
            //
            // Since this method allows integer-only strings as input
            // in addition to strings of floating-point literals, the
            // two sub-patterns below are simplifications of the grammar
            // productions from the Java Language Specification, 2nd
            // edition, section 3.10.2.

            // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
            "((("
            + Digits
            + "(\\.)?("
            + Digits
            + "?)("
            + Exp
            + ")?)|"
            +

            // . Digits ExponentPart_opt FloatTypeSuffix_opt
            "(\\.("
            + Digits
            + ")("
            + Exp
            + ")?)|"
            +

            // Hexadecimal strings
            "(("
            +
            // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
            "(0[xX]"
            + HexDigits
            + "(\\.)?)|"
            +

            // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
            "(0[xX]"
            + HexDigits
            + "?(\\.)"
            + HexDigits
            + ")"
            + ")[pP][+-]?"
            + Digits
            + "))"
            + "[fFdD]?))"
            + "[\\x00-\\x20]*"); // Optional trailing
    // "whitespace"

    if (Pattern.matches(fpRegex, myString)) {
      return Double.valueOf(myString); // Will not throw NumberFormatException
    } else {
      // Perform suitable alternative action
      if (throwException) {
        String errMesg = "ERROR! Could not parse double from:" + myString;
        throw new RuntimeException(errMesg);
      } else {
        return Double.NaN;
      }
    }
  }

  public static Integer parseSafeInt(String numStr) {
    try {
      int num = Integer.parseInt(numStr);
      return num;
    } catch (NumberFormatException nfe) {
      // not a number
      String errMesg = "ERROR:" + numStr + "could not be" + "converted to an Integer!";
      throw new RuntimeException(errMesg);
    }
  }

  /**
   * Create and return current date time in format: yyyyMMdd'T'hhMMss. Used for ALTWG PDS naming
   * convention
   *
   * @return
   */
  public static String currDTime() {

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmms");
    Date date = new Date();
    String cDTime = dateFormat.format(date);
    return cDTime;
  }

  /**
   * pad with n spaces to the right of string
   *
   * @param s
   * @param n
   * @return
   */
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  /**
   * Pad with n spaces to left of string
   *
   * @param s
   * @param n
   * @return
   */
  public static String padLeft(String s, int n) {
    return String.format("%1$" + n + "s", s);
  }

  /** Pad with spaces to right of string until length is n. WARNING: Will not truncate if s > n */
  public static String padRightToN(String s, int n) {
    String newString;
    if (s.length() < n) {
      newString = padRight(s, n);
    } else {
      newString = s;
    }
    return newString;
  }

  /** Pad with spaces to left of string until length is n. WARNING: Will not truncate if s > n */
  public static String padLeftToN(String s, int n) {
    String newString;
    if (s.length() < n) {
      newString = padLeft(s, n);
    } else {
      newString = s;
    }
    return newString;
  }

  /**
   * Used to get current time in yyyy-mm-ddThh:mm:ss format Can be used for debugging statements or
   * to fill out time keywords
   *
   * @return
   */
  public static String timenow() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date date = new Date();
    String timenow = dateFormat.format(date);
    return timenow;
  }

  /**
   * Strip newline characters from incoming string, then break into a list of strings with length no
   * greater than n.
   *
   * @param originalString
   * @param n
   * @return
   */
  public static List<String> wrapString(String originalString, int n) {
    List<String> results = new ArrayList<>();

    String[] parts = originalString.replaceAll("\\r|\\n", " ").split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (String s : parts) {
      if (sb.length() + s.length() >= n) {
        results.add(sb.toString());
        sb = new StringBuilder();
      }
      // replace multiple spaces with a single space
      sb.append(s.trim().replaceAll("\\s{2,}", " "));
      sb.append(" ");
    }

    return results;
  }

  /**
   * convert a string array to a single string.
   *
   * @param originalString
   * @param insertNewLine insert newline characters at the end of each array element
   * @return
   */
  public static String flattenStringArray(String[] originalString, boolean insertNewLine) {
    StringBuilder sb = new StringBuilder();
    for (String s : originalString) {
      // replace multiple spaces with a single space
      sb.append(s.trim().replaceAll("\\s{2,}", " "));
      sb.append(insertNewLine ? "\n" : " ");
    }
    return sb.toString();
  }

  /**
   * Returns a string in the format 1200n08900
   *
   * @param lv
   * @return
   */
  public static String lvToString(LatitudinalVector lv) {
    int lat = (int) (Math.abs(Math.toDegrees(lv.getLatitude())) * 100 + 0.5);
    int lon = (int) (Math.abs(Math.toDegrees(lv.getLongitude())) * 100 + 0.5);

    return String.format("%04d%s%05d", lat, lv.getLatitude() > 0 ? "n" : "s", lon);
  }
}
