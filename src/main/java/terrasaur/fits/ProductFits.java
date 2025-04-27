package terrasaur.fits;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.io.FilenameUtils;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import terrasaur.enums.FitsHeaderType;
import terrasaur.enums.PlaneInfo;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;
import terrasaur.utils.DTMHeader;
import terrasaur.utils.xml.AsciiFile;

/**
 * Various static routines for working with fits file products produced or used by the toolkit.
 *
 * @author espirrc1
 */
public class ProductFits {

  public static final String PLANE = "PLANE";
  public static final String COMMENT = "COMMENT";

  /**
   * Open the fits file and extract fits keywords which start with "PLANE". The convention is that
   * these are the keywords which describe the planes in the fits datacube.
   *
   * @param fitsFile
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static List<HeaderCard> getPlaneHeaderCards(String fitsFile)
      throws FitsException, IOException {
    Fits inFits = new Fits(fitsFile);
    List<HeaderCard> planeHeaders = getPlaneHeaderCards(inFits);
    return planeHeaders;
  }

  /**
   * Open the fits object and extract fits keywords which start with "PLANE". The convention is that
   * these are the keywords which describe the planes in the fits datacube.
   *
   * @param inFitsFile
   * @return
   * @throws FitsException
   * @throws IOException
   */
  public static List<HeaderCard> getPlaneHeaderCards(Fits inFitsFile)
      throws FitsException, IOException {
    BasicHDU inHdu = inFitsFile.getHDU(0);
    List<HeaderCard> planeHeaders = getPlaneHeaderCards(inHdu);
    return planeHeaders;
  }

  /**
   * Parse the HeaderDataUnit (HDU) and extract fits keywords which start with "PLANE". The
   * convention is that these are the keywords which describe the planes in the fits datacube.
   *
   * @param inHdu
   * @return
   */
  public static List<HeaderCard> getPlaneHeaderCards(BasicHDU inHdu) {
    List<HeaderCard> planeHeaders = new ArrayList<HeaderCard>();
    Cursor cursor = inHdu.getHeader().iterator();
    while (cursor.hasNext()) {
      HeaderCard hc = (HeaderCard) cursor.next();
      if (hc.getKey().startsWith(PLANE)) planeHeaders.add(hc);
    }
    return planeHeaders;
  }

  /**
   * Parse fits header and determine min/max latitude and longitude. For global fits files will just
   * parse keywords that directly contain the min/max lat/lon values. For regional fits files will
   * parse the latlon corner keywords and determine min, max lat/lon values.
   *
   * @param fitsFile
   * @return Map&lt;String, Double&gt; where string is the .toString() of HeaderTags MINLON, MAXLON,
   *     MINLAT, MAXLAT.
   * @throws IOException
   * @throws FitsException
   */
  public static Map<String, Double> minMaxLLFromFits(File fitsFile)
      throws FitsException, IOException {

    System.out.println("Determining minmax lat lon from fits file:" + fitsFile.getAbsolutePath());

    // initialize output
    Map<String, Double> minMaxLL = new HashMap<String, Double>();

    Map<String, HeaderCard> fitsHeaders = FitsUtil.getFitsHeaderAsMap(fitsFile.getAbsolutePath());

    // check whether MINLON keyword exists.
    String keyword = HeaderTag.MINLON.toString();
    boolean failOnNull = false;
    HeaderCard thisCard = FitsUtil.getCard(fitsHeaders, keyword, failOnNull);
    if (thisCard == null) {

      // assume LatLon corner keywords exist
      // Map<String, HeaderCard> cornerCards = latLonCornersFromMap(fitsHeaders);

      // iterate through Lat corners and find min/max values
      double minLat = 999D;
      double maxLat = -999D;
      failOnNull = true;
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.LLCLAT.toString(), failOnNull);
      double thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLat = Math.min(minLat, thisValue);
      maxLat = Math.max(maxLat, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.ULCLAT.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLat = Math.min(minLat, thisValue);
      maxLat = Math.max(maxLat, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.URCLAT.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLat = Math.min(minLat, thisValue);
      maxLat = Math.max(maxLat, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.LRCLAT.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLat = Math.min(minLat, thisValue);
      maxLat = Math.max(maxLat, thisValue);

      // double check center lat. For polar observations the center latitude could be
      // either the minimum or maximum latitude
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.CLAT.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLat = Math.min(minLat, thisValue);
      maxLat = Math.max(maxLat, thisValue);

      minMaxLL.put(HeaderTag.MINLAT.toString(), minLat);
      minMaxLL.put(HeaderTag.MAXLAT.toString(), maxLat);

      // iterate through Lon corners and find min/max values
      double minLon = 999D;
      double maxLon = -999D;
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.LLCLNG.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLon = Math.min(minLon, thisValue);
      maxLon = Math.max(maxLon, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.ULCLNG.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLon = Math.min(minLon, thisValue);
      maxLon = Math.max(maxLon, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.URCLNG.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLon = Math.min(minLon, thisValue);
      maxLon = Math.max(maxLon, thisValue);

      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.LRCLNG.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLon = Math.min(minLon, thisValue);
      maxLon = Math.max(maxLon, thisValue);

      // double check center lon
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.CLON.toString(), failOnNull);
      thisValue = thisCard.getValue(Double.class, Double.NaN);
      minLon = Math.min(minLon, thisValue);
      maxLon = Math.max(maxLon, thisValue);

      minMaxLL.put(HeaderTag.MINLON.toString(), minLon);
      minMaxLL.put(HeaderTag.MAXLON.toString(), maxLon);

    } else {

      // assume min/max lat/lon keywords exist

      // already queried for MINLON
      double thisVal = thisCard.getValue(Double.class, Double.NaN);
      minMaxLL.put(HeaderTag.MINLON.toString(), thisVal);

      // get MAXLON
      failOnNull = true;
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.MAXLON.toString(), failOnNull);
      thisVal = thisCard.getValue(Double.class, Double.NaN);
      minMaxLL.put(HeaderTag.MAXLON.toString(), thisVal);

      // get MINLAT
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.MINLAT.toString(), failOnNull);
      thisVal = thisCard.getValue(Double.class, Double.NaN);
      minMaxLL.put(HeaderTag.MINLAT.toString(), thisVal);

      // get MAXLAT
      thisCard = FitsUtil.getCard(fitsHeaders, HeaderTag.MAXLAT.toString(), failOnNull);
      thisVal = thisCard.getValue(Double.class, Double.NaN);
      minMaxLL.put(HeaderTag.MAXLAT.toString(), thisVal);
    }

    return minMaxLL;
  }

  /**
   * Parse and extract list of PlaneInfo enumerations that describe the planes contained in the fits
   * file. Return empty list if none of the planes match.
   *
   * @param fitsFile
   * @return
   * @throws IOException
   * @throws FitsException
   */
  public static List<PlaneInfo> planesFromFits(String fitsFile) throws FitsException, IOException {

    List<PlaneInfo> planes = new ArrayList<PlaneInfo>();

    // extract header cards
    List<HeaderCard> planeCards = getPlaneHeaderCards(fitsFile);

    /*
     * planeCards are assumed to follow the fits keyword naming convention of PLANEN, where N goes
     * from 0 to the number of planes in the fits file - 1. They correspond directly to the order in
     * which the data is stored in the data cube. For example, PLANE0 = "Latitude" indicates that
     * the 0th plane in the fits datacube contains the latitude values.
     */
    for (HeaderCard thisPlane : planeCards) {
      PlaneInfo thisPlaneInfo = PlaneInfo.keyVal2Plane(thisPlane.getValue());
      if (thisPlaneInfo != null) {
        planes.add(thisPlaneInfo);
      }
    }
    return planes;
  }

  /**
   * Save data to a Fits Datacube. Assumes hdrBuilder is pre-loaded with all the keyword values that
   * can be known prior to this method. An example of a keyword value that cannot be known prior to
   * this method is the ALTWG filename. This can only be created within this method by using
   * metadata contained in the FitsHdrBuilder. The map information in fitsData will also be used to
   * populate hdrBuilder if it is available. If it is not available then the method assumes the
   * information is already in hdrBuilder. Ex. if fitsData was populated by reading a fits file then
   * it may not have vector to center or unit vectors describing the reference frame. However, that
   * information may be in the fits header, which should be captured by hdrBuilder.
   *
   * @param fitsData
   * @param planeList
   * @param outFname
   * @param hdrBuilder
   * @param hdrType
   * @param crossrefFile - if not null then method will create a cross-reference file. The
   *     cross-reference file allows the pipeline to cross reference an original filenaming
   *     convention with the mission specific one.
   * @throws FitsException
   * @throws IOException
   */
  public static void saveDataCubeFits(
      FitsData fitsData,
      List<PlaneInfo> planeList,
      String outFname,
      FitsHdrBuilder hdrBuilder,
      FitsHeaderType hdrType,
      File crossrefFile)
      throws FitsException, IOException {

    hdrBuilder.setByFitsData(fitsData);

    String fitsFname = outFname;

    if (crossrefFile != null) {
      // save outfile name in cross-reference file, for future reference
      String path = FilenameUtils.getFullPath(outFname);
      if (path.length() == 0) path = ".";
      String outBaseName =
          String.format("%s%s%s", path, File.pathSeparator, FilenameUtils.getBaseName(outFname));
      AsciiFile crfFile = new AsciiFile(crossrefFile.getAbsolutePath());
      crfFile.streamSToFile(outBaseName, 0);
      crfFile.closeFile();
    }

    // set date this product was produced
    hdrBuilder.setDateprod();

    DTMHeader fitsHeader = FitsHeaderFactory.getDTMHeader(hdrBuilder.build(), hdrType);
    fitsHeader.setData(fitsData);
    List<HeaderCard> headers = fitsHeader.createFitsHeader(PlaneInfo.planesToHeaderCard(planeList));

    System.out.println("saving to fits file:" + fitsFname);
    FitsUtil.saveFits(fitsData.getData(), fitsFname, headers);
  }

  /**
   * General method for saving 3D double array with fits header as defined in fitsHdrbuilder.
   * Assumes FitsHdrBuilder contains all the keywords that will be written to the fits header in the
   * order that they should be written.
   *
   * @param dataCube
   * @param hdrBuilder
   * @throws IOException
   * @throws FitsException
   */
  public static void saveDataCubeFits(
      double[][][] dataCube,
      FitsHdrBuilder hdrBuilder,
      List<HeaderCard> planeHeaders,
      String outFile)
      throws FitsException, IOException {

    FitsHdr fitsHdr = hdrBuilder.build();
    List<HeaderCard> headers = new ArrayList<HeaderCard>();

    for (String keyword : fitsHdr.fitsKV.keySet()) {
      headers.add(fitsHdr.fitsKV.get(keyword));
    }

    saveDataCubeFits(dataCube, headers, planeHeaders, outFile);
  }

  /**
   * General method for writing to a fits file a 3D double array with fits headers as defined in
   * List<HeaderCard> and planeKeywords as defined in separate List<HeaderCard>. plane keywords
   * defining the planes of the dataCube in a separate List<HeaderCard>
   *
   * @param dataCube
   * @param headers
   * @param planeKeywords - can be null. If so then assumes headers contains all the keyword
   *     information to be written to the file.
   * @param outFname
   * @throws IOException
   * @throws FitsException
   */
  public static void saveDataCubeFits(
      double[][][] dataCube,
      List<HeaderCard> headers,
      List<HeaderCard> planeKeywords,
      String outFname)
      throws FitsException, IOException {

    // append planeHeaders
    if (!planeKeywords.isEmpty()) {
      headers.addAll(planeKeywords);
    }
    FitsUtil.saveFits(dataCube, outFname, headers);
  }

  /**
   * Save NFT MLN.
   *
   * @param fitsData
   * @param planeList
   * @param outfile
   * @param hdrBuilder
   * @param hdrType
   * @param crossrefFile
   * @throws FitsException
   * @throws IOException
   */
  public static void saveNftFits(
      FitsData fitsData,
      List<PlaneInfo> planeList,
      String outfile,
      FitsHdrBuilder hdrBuilder,
      FitsHeaderType hdrType,
      File crossrefFile)
      throws FitsException, IOException {

    // public static void saveDataCubeFits(FitsData fitsData, List<PlaneInfo> planeList, String
    // outFname,
    // FitsHdrBuilder hdrBuilder, FitsHeaderType hdrType,
    // File crossrefFile) throws FitsException, IOException {

    String outNftFile = outfile;
    Path outPath = Paths.get(outfile);
    String nftFitsName = outPath.getFileName().toString();

    // need to replace the product name in the headers list. No need to change comments.
    hdrBuilder.setVbyHeaderTag(HeaderTag.PRODNAME, nftFitsName);

    // set date this product was produced. Uses NFT specific keyword
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    Date date = new Date();
    hdrBuilder.setVbyHeaderTag(HeaderTag.DATENPRD, dateFormat.format(date));

    FitsHdr fitsHeader = hdrBuilder.build();

    DTMHeader nftFitsHeader = FitsHeaderFactory.getDTMHeader(fitsHeader, FitsHeaderType.NFTMLN);
    nftFitsHeader.setData(fitsData);

    List<HeaderCard> headers =
        nftFitsHeader.createFitsHeader(PlaneInfo.planesToHeaderCard(planeList));

    System.out.println("Saving to " + outNftFile);
    FitsUtil.saveFits(fitsData.getData(), outNftFile, headers);
  }
}
