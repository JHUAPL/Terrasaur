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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TableHDU;
import nom.tam.util.BufferedFile;
import nom.tam.util.Cursor;

/**
 * Utility class containing generic static routines for working with FITS files. Refer to AltwgFits
 * for static routines pertaining to ALTWG fit image files.
 * 
 * @author kahneg1
 * @version 1.0
 * 
 */
public class FitsUtil {
  // private static final String PLANE = "PLANE";
  // public static final String COMMENT = "COMMENT";

  public static double[][][] loadFits(String filename, int[] axes)
      throws FitsException, IOException {
    Fits f = new Fits(filename);
    BasicHDU hdu = f.getHDU(0);
    int[] axes2 = hdu.getAxes();
    if (axes2.length != 3) {
      throw new IOException("FITS file has incorrect dimensions");
    }

    axes[0] = axes2[0];
    axes[1] = axes2[1];
    axes[2] = axes2[2];

    Object data = hdu.getData().getData();
    f.getStream().close();

    if (data instanceof float[][][]) {
      return float2double((float[][][]) data);
    } else {// assume double[][][]
      return (double[][][]) data;
    }
  }

  /**
   * Return image dimensions in this order: [numPlanes][iSize][jSize]
   * 
   * @param filename
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static int[] getAxes(File filename) throws FitsException, IOException {
    Fits f = new Fits(filename);
    BasicHDU hdu = f.getHDU(0);
    int[] axes = hdu.getAxes();
    f.close();
    return axes;

  }

  /**
   * Extract plane from fits file. Throws error if file not found or if planeIndex is not in the
   * number of planes of fits file.
   * 
   * @param fitsFile
   * @param planeIndex - specify using Java array indexing (counter starts at 0).
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static double[][] getPlaneFromFits(File fitsFile, int planeIndex, int[] axes)
      throws FitsException, IOException {

    Fits f = new Fits(fitsFile);
    BasicHDU hdu = f.getHDU(0);
    axes = hdu.getAxes();
    if (axes.length != 3) {
      throw new IOException(
          "FITS file has incorrect dimensions! This was assumed to be 3D fits file!");
    }

    // fits axes specifies image dimensions in this order: [numPlanes][iSize][jSize]
    // check that planeIndex is valid
    int numPlanes = axes[0];
    if ((planeIndex < 0)) {
      String errMesg = "ERROR! Planeindex cannot be less than 0!";
      throw new RuntimeException(errMesg);
    }

    if ((planeIndex > numPlanes)) {
      String errMesg = "ERROR! desired planeindex:" + planeIndex + " is greater than " + numPlanes
          + " number of planes in file.";
      throw new RuntimeException(errMesg);
    }

    Object data = hdu.getData().getData();
    f.getStream().close();

    double[][][] dataD = null;
    if (data instanceof float[][][]) {
      dataD = float2double((float[][][]) data);
    } else {// assume double[][][]
      dataD = (double[][][]) data;
    }
    double[][] planeData = new double[axes[1]][axes[2]];
    planeData = dataD[planeIndex];
    return planeData;
  }

  /**
   * Find the row and column in the FITS data corresponding to an XYZ coordinate
   * 
   * @param fitsData
   * @param xyz
   * @return 2D int array containing {row, col}. Row is second index into FITS cube, column is third
   *         index. This point will have magnitude within 1e-3 units and direction exactly the same
   *         as xyz.
   */
  public static int[] findInFits(double[][][] fitsData, double[] xyz) {

    // check xyz vector. If any components are NaN then return null;
    for (int ii = 0; ii < 3; ii++) {
      if (xyz[ii] == Double.NaN) {
        return null;
      }
    }

    Vector3D xyzV = new Vector3D(xyz);
    double mag2 = xyzV.getNorm();

    for (int ii = 0; ii < fitsData[1].length; ii++) {
      for (int jj = 0; jj < fitsData[0][1].length; jj++) {
        Vector3D fits1P =
            new Vector3D(fitsData[4][ii][jj], fitsData[5][ii][jj], fitsData[6][ii][jj]);

        // find angular separation between fits1 P vector and fits2 P vector
        double radSep = Vector3D.angle(fits1P, xyzV);

        if (radSep == 0D) {
          // compute magnitude of fits1 P vector and fits2 P vector.
          double mag1 = fits1P.getNorm();
          double diffD = Math.abs(mag1 - mag2);
          if (diffD < 0.001D) {
            int[] rowCol = new int[2];
            rowCol[0] = ii;
            rowCol[1] = jj;
            return rowCol;
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns true if HeaderTag is part of the general header keywords that all fits files should
   * have. Used to filter out keywords that should be handled by the API and NOT set by the user.
   * 
   * @param keyword
   * @return
   */
  public static boolean isGeneralHeaderKey(HeaderTag keyword) {
    String name = keyword.name();
    switch (name) {

      case "SIMPLE":
      case "BITPIX":
      case "NAXIS":
      case "NAXIS1":
      case "NAXIS2":
      case "NAXIS3":
      case "EXTEND":
        return true;

      default:
        return false;
    }
  }

  /**
   * Generic method for creating a fits file with an N-plane image array and fits headers.
   * 
   * @param data -contains N X M X P image array that will be written as a data cube of size N X M
   *        and P planes.
   * @param outfile
   * @param newHeaderCards - HeaderCards containing information for generating fits headers
   * @throws FitsException
   * @throws IOException
   */
  public static void saveFits(double[][][] data, String outfile, List<HeaderCard> newHeaderCards)
      throws FitsException, IOException {
    float[][][] dataF = double2float(data);

    Fits f = new Fits();
    BasicHDU<?> hdu = FitsFactory.hduFactory(dataF);

    // Add any tags passed in as the third argument of this function
    if (newHeaderCards != null) {
      for (HeaderCard hc : newHeaderCards) {
        if (hc.getKey().toUpperCase().equals("COMMENT"))
          hdu.getHeader().insertComment(hc.getComment());
        else
          hdu.getHeader().addLine(hc);
      }
    }

    f.addHDU(hdu);
    System.out.println("writing to:" + outfile);
    BufferedFile bf = new BufferedFile(outfile, "rw");
    f.write(bf);
    bf.close();
    f.close();
  }

  /**
   * Generic method for creating a fits file with an 2D image array and fits headers.
   * 
   * @param data -contains N X M image array that will be written as a 2D image of size N X M
   * @param outfile
   * @param newHeaderCards - HeaderCards containing information for generating fits headers
   * @throws FitsException
   * @throws IOException
   */
  public static void save2DFits(double[][] data, String outfile, List<HeaderCard> newHeaderCards)
      throws FitsException, IOException {

    float[][] dataF = double2float2D(data);

    Fits f = new Fits();
    BasicHDU<?> hdu = FitsFactory.hduFactory(dataF);

    // Add any tags passed in as the third argument of this function
    if (newHeaderCards != null) {
      for (HeaderCard hc : newHeaderCards) {
        if (hc.getKey().toUpperCase().equals("COMMENT"))
          hdu.getHeader().insertComment(hc.getComment());
        else
          hdu.getHeader().addLine(hc);
      }
    }

    f.addHDU(hdu);
    System.out.println("writing to:" + outfile);
    BufferedFile bf = new BufferedFile(outfile, "rw");
    f.write(bf);
    bf.close();
    f.close();
  }


  /**
   * Assuming data consists of multiple 2D planes with the following ordering: Note that this
   * particular ordering is created so that the data is written properly to the fits file.
   * double[][][] data = new double[numPlanes][iSize][jSize]
   * 
   * Flip each 2D plane along the i dimension, i.e. do a vertical flip on each image. This is done
   * because some fits readers have a default data orientation that is different from the fits
   * writer orientation.
   * 
   * The display orientation is defined by the PDS4 XML label but some applications may not be using
   * the XML file to properly display the data cube. Hence it would be better to flip the data so
   * the application displays it in the "correct" orientation.
   * 
   * @param data
   * @return
   */
  public static double[][][] flipVertical(double[][][] data) {

    // retrieve lengths of each dimension
    int numPlanes = data.length;
    int numI = data[1].length;
    int numJ = data[0][1].length;

    double[][][] newData = new double[numPlanes][numI][numJ];

    for (int k = 0; k < numPlanes; k++) {
      for (int i = 0; i < numI / 2; i++) {
        for (int j = 0; j < numJ; j++) {
          newData[k][i][j] = data[k][numI - i - 1][j];
          newData[k][numI - i - 1][j] = data[k][i][j];
        }
      }
    }
    return newData;
  }

  public static double[][][] xyz2llrxyz(double[][][] indata) {
    int numPlanes = indata.length;
    if (numPlanes != 3) {
      System.out.println("Error: cube must contain exactly 3 planes");
      return null;
    }
    int numRows = indata[0].length;
    int numCols = indata[0][0].length;
    double[][][] outdata = new double[6][numRows][numCols];
    double[] pt = new double[3];
    for (int i = 0; i < numRows; ++i)
      for (int j = 0; j < numCols; ++j) {
        pt[0] = indata[0][i][j];
        pt[1] = indata[1][i][j];
        pt[2] = indata[2][i][j];

        Vector3D v = new Vector3D(pt);

        outdata[0][i][j] = Math.toDegrees(v.getAlpha());
        outdata[1][i][j] = Math.toDegrees(v.getDelta());
        outdata[2][i][j] = v.getNorm();
        outdata[3][i][j] = pt[0];
        outdata[4][i][j] = pt[1];
        outdata[5][i][j] = pt[2];
      }
    return outdata;
  }

  public static double[][][] llr2llrxyz(double[][][] indata) {
    int numPlanes = indata.length;
    if (numPlanes != 3) {
      System.out.println("Error: cube must contain exactly 3 planes");
      return null;
    }
    int numRows = indata[0].length;
    int numCols = indata[0][0].length;
    double[][][] outdata = new double[6][numRows][numCols];
    for (int i = 0; i < numRows; ++i)
      for (int j = 0; j < numCols; ++j) {
        double lat = indata[0][i][j];
        double lon = indata[1][i][j];
        double rad = indata[2][i][j];

        double[] pt =
            new Vector3D(Math.toRadians(lon), Math.toRadians(lat)).scalarMultiply(rad).toArray();

        outdata[0][i][j] = indata[0][i][j];
        outdata[1][i][j] = indata[1][i][j];
        outdata[2][i][j] = indata[2][i][j];
        outdata[3][i][j] = pt[0];
        outdata[4][i][j] = pt[1];
        outdata[5][i][j] = pt[2];
      }
    return outdata;
  }

  public static float[][][] double2float(double[][][] indata) {
    int numPlanes = indata.length;
    int numRows = indata[0].length;
    int numCols = indata[0][0].length;
    float[][][] outdata = new float[numPlanes][numRows][numCols];
    for (int k = 0; k < numPlanes; ++k)
      for (int i = 0; i < numRows; ++i)
        for (int j = 0; j < numCols; ++j) {
          outdata[k][i][j] = (float) indata[k][i][j];
        }
    return outdata;
  }

  public static float[][] double2float2D(double[][] indata) {
    int numRows = indata.length;
    int numCols = indata[0].length;
    float[][] outdata = new float[numRows][numCols];
    for (int i = 0; i < numRows; ++i)
      for (int j = 0; j < numCols; ++j) {
        outdata[i][j] = (float) indata[i][j];
      }
    return outdata;
  }

  public static double[][][] float2double(float[][][] indata) {
    int numPlanes = indata.length;
    int numRows = indata[0].length;
    int numCols = indata[0][0].length;
    double[][][] outdata = new double[numPlanes][numRows][numCols];
    for (int k = 0; k < numPlanes; ++k)
      for (int i = 0; i < numRows; ++i)
        for (int j = 0; j < numCols; ++j) {
          outdata[k][i][j] = indata[k][i][j];
        }
    return outdata;
  }

  public static double[][][] crop(double[][][] indata, int cropAmount) {
    int numPlanes = indata.length;
    int numRows = indata[0].length;
    int numCols = indata[0][0].length;
    double[][][] outdata =
        new double[numPlanes][numRows - 2 * cropAmount][numCols - 2 * cropAmount];
    for (int k = 0; k < numPlanes; ++k)
      for (int i = 0; i < numRows - 2 * cropAmount; ++i)
        for (int j = 0; j < numCols - 2 * cropAmount; ++j) {
          outdata[k][i][j] = indata[k][i + cropAmount][j + cropAmount];
        }
    return outdata;
  }

  /**
   * Update a single HeaderCard in a list of HeaderCards. Search on the keyword in the list and
   * replace with the new HeaderCard. This method has been updated to accept a HeaderCard as input
   * as this is the most generalized form. This allows the datatype of the HeaderCard value to be
   * preserved or even to change. I.e. the original HeaderCard value could be a double and this lets
   * one change the datatype to a string. NOTE: Does NOTHING if keyword is not found in the list! To
   * append the keyword when it is not found in the list, use updateOrAppendCard().
   * 
   * @param headers
   * @param newHeaderCard
   * @return
   */
  public static List<HeaderCard> updateCard(List<HeaderCard> headers, HeaderCard newHeaderCard) {

    List<HeaderCard> newHeaders = new ArrayList<HeaderCard>();

    // find kitsKey in the list.
    for (HeaderCard thisCard : headers) {
      if ((newHeaderCard.getKey()).equals(thisCard.getKey())) {
        newHeaders.add(newHeaderCard);
      } else {
        newHeaders.add(thisCard);
      }
    }

    return newHeaders;
  }

  /**
   * Update a single HeaderCard in a list of HeaderCards. Search on the keyword in the list and
   * replace with the new value and new comment(Optional. Comment not updated if comment string is
   * null). If keyword does NOT exist in the list then append it to the list.
   * 
   * @param headers
   * @param newHeaderCard
   * @return
   */
  public static List<HeaderCard> updateOrAppendCard(List<HeaderCard> headers,
      HeaderCard newHeaderCard) {

    List<HeaderCard> newHeaders = new ArrayList<HeaderCard>();

    // find kitsKey in the list.
    boolean cardFound = false;
    for (HeaderCard thisCard : headers) {
      if ((newHeaderCard.getKey()).equals(thisCard.getKey())) {
        newHeaders.add(newHeaderCard);
        cardFound = true;
      } else {
        newHeaders.add(thisCard);
      }
    }

    if (!cardFound) {
      newHeaders.add(newHeaderCard);
    }

    return newHeaders;

  }


  /**
   * Return the fits header as List&lt;HeaderCard&gt;. Each HeaderCard is equivalent to the FITS
   * Keyword = Value, comment line in a fits header.
   * 
   * @param fitsFile
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static List<HeaderCard> getFitsHeader(String fitsFile) throws FitsException, IOException {
    Fits inf = new Fits(fitsFile);
    BasicHDU inHdu = inf.getHDU(0);
    // Object data = inhdu.getData().getData();
    List<HeaderCard> inHeaders = new ArrayList<HeaderCard>();
    Cursor cursor = inHdu.getHeader().iterator();
    while (cursor.hasNext()) {
      HeaderCard hc = (HeaderCard) cursor.next();
      inHeaders.add(hc);
    }

    inf.getStream().close();
    return inHeaders;
  }

  /**
   * Return headercard in the fits header map given the key. Return null if headercard not found,
   * unless boolean failOnNull = true. If true, then method will throw a FitsException if headerCard
   * not found.
   * 
   * @param map
   * @param searchKey
   * @param failOnNull
   * @return
   * @throws FitsException
   */
  public static HeaderCard getCard(Map<String, HeaderCard> map, String searchKey,
      boolean failOnNull) throws FitsException {
    HeaderCard returnCard = null;
    if (map.containsKey(searchKey)) {
      returnCard = map.get(searchKey);
    } else {
      if (failOnNull) {
        String error = "ERROR! Could not parse fits Keyword:" + searchKey;
        throw new FitsException(error);
      } else {
        return null;
      }
    }
    return returnCard;
  }

  /**
   * Load fits header into Map&lt;String, HeaderCard&gt; where String is the string representation
   * of the fits keyword associated with the headerCard.
   * 
   * @param fitsFile
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static Map<String, HeaderCard> getFitsHeaderAsMap(String fitsFile)
      throws FitsException, IOException {
    Fits inf = new Fits(fitsFile);
    BasicHDU inHdu = inf.getHDU(0);

    // preserve order of keywords loaded from fits file
    Map<String, HeaderCard> inHeaders = new LinkedHashMap<String, HeaderCard>();
    Cursor cursor = inHdu.getHeader().iterator();
    while (cursor.hasNext()) {
      HeaderCard hc = (HeaderCard) cursor.next();
      inHeaders.put(hc.getKey(), hc);
    }

    inf.getStream().close();
    inf.close();
    return inHeaders;
  }

  /**
   * Generate a list of HeaderCards given a map. Useful for transforming a map of keywords loaded
   * from input file into a list of keywords for a fits output file. Warning: the list will be in
   * random order if the map class used did not preserve insertion order.
   * 
   * @param map
   * @return
   */
  public static List<HeaderCard> map2HeaderCards(Map<String, HeaderCard> map) {
    List<HeaderCard> headerCards = new ArrayList<HeaderCard>();
    for (String key : map.keySet()) {
      headerCards.add(map.get(key));
    }
    return headerCards;
  }

  /**
   * Return the Fits Table Header Data Unit given the path to a fits table file. Does not matter
   * whether table is Fits binary or ASCII.
   * 
   * @throws IOException
   * @throws FitsException
   */
  public static TableHDU TableHDUofFile(String fitsFile) throws FitsException, IOException {
    System.out.println("fits file:" + fitsFile);
    Fits tableFits = new Fits(fitsFile);
    int numHDUs = tableFits.getNumberOfHDUs();
    System.out.println("number of hdus:" + String.valueOf(numHDUs));
    TableHDU tableHDU = (TableHDU) tableFits.getHDU(1);

    int numRows = tableHDU.getNRows();
    int numCols = tableHDU.getNCols();

    System.out.println("numRows:" + String.valueOf(numRows));
    System.out.println("numCols:" + String.valueOf(numCols));

    return tableHDU;
  }
}
