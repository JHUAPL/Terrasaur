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
package terrasaur.apps;

import com.google.common.io.LittleEndianDataInputStream;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;
import terrasaur.altwg.pipeline.NameConvention;
import terrasaur.altwg.pipeline.NamingFactory;
import terrasaur.altwg.pipeline.ProductNamer;
import terrasaur.enums.AltwgDataType;
import terrasaur.enums.FitsHeaderType;
import terrasaur.enums.PlaneInfo;
import terrasaur.enums.SrcProductType;
import terrasaur.fits.FitsData;
import terrasaur.fits.FitsData.FitsDataBuilder;
import terrasaur.fits.FitsHdr;
import terrasaur.fits.FitsHdr.FitsHdrBuilder;
import terrasaur.fits.HeaderTag;
import terrasaur.fits.ProductFits;
import terrasaur.fits.UnitDir;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.Binary16;
import terrasaur.utils.BinaryUtils;
import terrasaur.utils.xml.AsciiFile;

/**
 * Maplet2FITS program. See the usage string for more information about this program.
 *
 * @author Eli Kahn
 * @version 1.0
 */
public class Maplet2FITS implements TerrasaurTool {
  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Convert a Gaskell maplet to FITS format.";
  }

  @Override
  public String fullDescription(Options options) {
    String header =
        """
This program converts a maplet file in Gaskell maplet format to a FITS file.
The program assumes the Gaskell scale is in units of km.
By default the generated FITS cube will contain these 10 planes:
1. latitude
2. longitude
3. radius
4. x position
5. y position
6. z position
7. height
8. albedo
9. sigma
10. quality
If the --exclude-position option is provided, then only the height, albedo, sigma
and quality planes are saved out.""";
    String footer = "";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  public static class HazardParams {

    public final boolean noHazard;
    public final double initialValue;

    private HazardParams(boolean noHazard, double initialValue) {
      this.noHazard = noHazard;
      this.initialValue = initialValue;
    }
  }

  /**
   * Generate HazardParams object that contains attributes needed for generating the Hazard plane.
   *
   * @param noHazard
   * @param initialVal
   * @return
   */
  public static HazardParams getHazardParam(boolean noHazard, double initialVal) {
    return new HazardParams(noHazard, initialVal);
  }

  /**
   * Self-contained function call to generate ALTWG FITS files from a given maplet file. Note the
   * boolean that determines whether to use the outfile string "as-is" or replace the filename with
   * one using the ALTWG naming convention.
   *
   * @param mapletFile
   * @param outfile
   * @param productType
   * @param excludePosition
   * @param sigmasFile
   * @param qualityFile
   * @param namingConvention
   * @throws IOException
   * @throws FitsException
   */
  public static void run(
      String mapletFile,
      String outfile,
      AltwgDataType productType,
      boolean excludePosition,
      String fitsConfigFile,
      String sigmasFile,
      String sigsumFile,
      String qualityFile,
      String namingConvention,
      boolean swapBytes,
      double scalFactor,
      double sigmaScale,
      String mapName,
      HazardParams hazParam)
      throws IOException, FitsException {

    // sanity check. If no naming convention specified then outfile should be fully qualified path
    // to an output file, NOT a directory
    if (namingConvention.isEmpty()) {
      File outFname = new File(outfile);
      if (outFname.isDirectory()) {
        String errMesg =
            "ERROR! No naming convention specified but output file:"
                + outfile
                + " is a directory! Must be a full path to an output file!";
        throw new RuntimeException(errMesg);
      }
    }

    DataInputStream is =
        new DataInputStream(new BufferedInputStream(new FileInputStream(mapletFile)));
    DataInput sigmaInput = null;
    BufferedInputStream sigmabis = null;
    if (sigmasFile != null) {
      System.out.println("Parsing " + sigmasFile + " for sigma values.");
      Path filePath = Paths.get(sigmasFile);
      if (!Files.exists(filePath)) {
        System.out.println(
            "WARNING! sigmas file:"
                + filePath.toAbsolutePath()
                + " not found! Sigmas will default to 0!");
      } else {
        sigmabis =
            new BufferedInputStream(new FileInputStream(filePath.toAbsolutePath().toString()));
        sigmaInput =
            swapBytes ? new LittleEndianDataInputStream(sigmabis) : new DataInputStream(sigmabis);
      }
    }
    DataInput qualityInput = null;
    BufferedInputStream qualbis = null;
    if (qualityFile != null) {
      System.out.println("Parsing " + qualityFile + " for quality values.");
      Path filePath = Paths.get(qualityFile);
      if (!Files.exists(filePath)) {
        System.out.println(
            "WARNING! quality file:"
                + filePath.toAbsolutePath()
                + " not found! Quality values will default to 0!");
      } else {
        qualbis =
            new BufferedInputStream(new FileInputStream(filePath.toAbsolutePath().toString()));
        qualityInput =
            swapBytes ? new LittleEndianDataInputStream(qualbis) : new DataInputStream(qualbis);
      }
    }

    double[] V = new double[3];
    double[] ux = new double[3];
    double[] uy = new double[3];
    double[] uz = new double[3];

    /*
     * Copied from Bob Gaskell READ_MAP.f code:
     *
     * bytes 1-2 height/hscale (integer*2 msb) byte 3 relative "albedo" (1-199) (byte)
     *
     * If there is missing data at any point, both height and albedo are set to zero.
     *
     * The map array is read row by row from the upper left (i,j = -qsz). Rows are increasing in the
     * Uy direction with spacing = scale Columns are increasing in the Ux direction with spacing =
     * scale Heights are positive in the Uz direction with units = scale
     */

    // use the first 4 bytes of the maplet header to store intensity min & dynamic range.
    float intensityMin = Binary16.toFloat(BinaryUtils.swap(is.readShort()));
    float intensityRange = Binary16.toFloat(BinaryUtils.swap(is.readShort()));

    // advancing byte pointer past some headers. Unused, per Bob's WRITE_MAP.f
    is.readByte();
    is.readByte();

    float scale = is.readFloat();
    short halfsize = BinaryUtils.swap(is.readShort());

    // x,y,z position uncertainty unit vector * 255.
    // per Bob's WRITE_MAP.f
    is.readByte();
    is.readByte();
    is.readByte();

    V[0] = is.readFloat();
    V[1] = is.readFloat();
    V[2] = is.readFloat();
    ux[0] = is.readFloat();
    ux[1] = is.readFloat();
    ux[2] = is.readFloat();
    uy[0] = is.readFloat();
    uy[1] = is.readFloat();
    uy[2] = is.readFloat();
    uz[0] = is.readFloat();
    uz[1] = is.readFloat();
    uz[2] = is.readFloat();
    float hscale = is.readFloat();

    // magnitude of position uncertainty
    is.readFloat();

    // byte 72 of the maplet header is the version number. OLA uses version numbers < 0 and SPC
    // maplets have
    // version numbers > 0. A version number of 0 is Bob Gaskell's original maplet format.
    byte b = is.readByte();
    logger.info("byte is:" + b);
    // boolean isOLAMaplet = (is.readByte() < 0);
    boolean isOLAMaplet = (b < 0);
    if (isOLAMaplet) {
      logger.info("byte72 of maplet header indicates this is an OLA maplet.");
    } else {
      logger.info("byte72 of maplet header indicates this is an SPC maplet.");
    }

    logger.info(String.format("V : [%f %f %f]", V[0], V[1], V[2]));
    logger.info(String.format("ux: [%f %f %f]", ux[0], ux[1], ux[2]));
    logger.info(String.format("uy: [%f %f %f]", uy[0], uy[1], uy[2]));
    logger.info(String.format("uz: [%f %f %f]", uz[0], uz[1], uz[2]));
    logger.info("halfsize: " + halfsize);
    logger.info("scale: " + scale);
    logger.info("hscale: " + hscale);
    logger.info("AltwgProductType:" + productType.toString());

    int totalsize = 2 * halfsize + 1;
    int numPlanes = 10;
    if (excludePosition) {
      numPlanes = 4;
      if (!hazParam.noHazard) {
        numPlanes = numPlanes + 1;
      }
    }
    double[][][] data = new double[numPlanes][totalsize][totalsize];
    double[][][] llrData = new double[numPlanes][totalsize][totalsize];

    for (int i = -halfsize; i <= halfsize; ++i)
      for (int j = -halfsize; j <= halfsize; ++j) {

        double h = is.readShort() * hscale * scale;

        int n = 0;
        int llrIndex = 0;
        double[] p = {
          V[0] + i * scale * ux[0] + j * scale * uy[0] + h * uz[0],
          V[1] + i * scale * ux[1] + j * scale * uy[1] + h * uz[1],
          V[2] + i * scale * ux[2] + j * scale * uy[2] + h * uz[2]
        };
        LatitudinalVector lv = CoordConverters.convertToLatitudinal(new UnwritableVectorIJK(p));
        if (!excludePosition) {
          data[n++][i + halfsize][j + halfsize] = Math.toDegrees(lv.getLatitude());
          data[n++][i + halfsize][j + halfsize] = Math.toDegrees(lv.getLongitude());
          data[n++][i + halfsize][j + halfsize] = lv.getRadius();
          data[n++][i + halfsize][j + halfsize] = p[0];
          data[n++][i + halfsize][j + halfsize] = p[1];
          data[n++][i + halfsize][j + halfsize] = p[2];
        } else {
          llrData[llrIndex++][i + halfsize][j + halfsize] = Math.toDegrees(lv.getLatitude());
          llrData[llrIndex++][i + halfsize][j + halfsize] = Math.toDegrees(lv.getLongitude());
          llrData[llrIndex++][i + halfsize][j + halfsize] = lv.getRadius();
        }

        data[n++][i + halfsize][j + halfsize] = h;

        double albedo = is.readUnsignedByte();

        if (isOLAMaplet) {
          albedo = albedo / 199. * intensityRange + intensityMin;
        } else {
          albedo = albedo / 100.0D;
        }
        data[n++][i + halfsize][j + halfsize] = albedo;

        // sigmas default to 0 unless a SIGMAS file was specified
        float sigmaVal = getSigma(sigmaInput, sigmaScale);

        data[n++][i + halfsize][j + halfsize] = sigmaVal;

        // quality defaults to 0 unless a quality file was specified
        float qualVal = getQuality(qualityInput);

        data[n++][i + halfsize][j + halfsize] = qualVal;

        if ((excludePosition) && (!hazParam.noHazard)) {
          // NFT MLN; includes Hazard plane, initialized to initial value
          data[n++][i + halfsize][j + halfsize] = hazParam.initialValue;
        }
      }

    is.close();
    if (sigmabis != null) sigmabis.close();
    if (qualbis != null) qualbis.close();

    String sigmaSum = null;
    if (sigsumFile != null) {
      sigmaSum = parseSigsumFile(sigsumFile);
    }

    // Map<String, HeaderCard> headerValues = new LinkedHashMap<String, HeaderCard>();

    // create new fits header builder
    FitsHdrBuilder hdrBuilder = FitsHdr.getBuilder();

    if (fitsConfigFile != null) {
      // initialize header cards with values from configfile
      hdrBuilder = FitsHdr.configHdrBuilder(fitsConfigFile, hdrBuilder);
    }

    if (sigmaSum != null) {
      // able to parse sigma summary value. Show this in header builder
      String hdrTag = HeaderTag.SIGMA.toString();
      // update sigma summary value
      hdrBuilder.setVCbyHeaderTag(HeaderTag.SIGMA, sigmaSum, HeaderTag.SIGMA.comment());

      // headerValues.put(HeaderTag.SIGMA.toString(),
      // new HeaderCard(HeaderTag.SIGMA.toString(), sigmaSum, HeaderTag.SIGMA.comment()));

      // define this SIGMA as a measurement of height uncertainty

      hdrBuilder.setVCbyHeaderTag(
          HeaderTag.SIG_DEF, "height uncertainty", HeaderTag.SIG_DEF.comment());

      // headerValues.put(HeaderTag.SIG_DEF.toString(),
      // new HeaderCard(HeaderTag.SIG_DEF.toString(), "Uncertainty", HeaderTag.SIG_DEF.comment()));
      //// "Definition of the SIGMA uncertainty metric"));

    }

    // set the mapletFile as the DATASRC
    hdrBuilder.setVbyHeaderTag(HeaderTag.DATASRCF, new File(mapletFile).getName());

    // set the MAP_NAME
    hdrBuilder.setVbyHeaderTag(HeaderTag.MAP_NAME, mapName);

    // create list describing the planes in the datacube
    List<PlaneInfo> planeList = new ArrayList<>();

    // determine SrcProductType from header builder
    String dataSource = SrcProductType.UNKNOWN.toString();
    if (hdrBuilder.containsKey(HeaderTag.DATASRC.toString())) {
      HeaderCard srcCard = hdrBuilder.getCard(HeaderTag.DATASRC.toString());
      dataSource = srcCard.getValue();
    }

    // use scalFactor to determine GSD
    double gsd = scale * scalFactor;

    // assume all maplets are local, not global.
    boolean isGlobal = false;

    // create FitsData object. Stores data and relevant information about the data
    FitsDataBuilder fitsDataB = new FitsDataBuilder(data, isGlobal);
    FitsData fitsData =
        fitsDataB
            .setV(V)
            .setAltProdType(productType)
            .setDataSource(dataSource)
            .setU(ux, UnitDir.UX)
            .setU(uy, UnitDir.UY)
            .setU(uz, UnitDir.UZ)
            .setScale(scale)
            .setGSD(gsd)
            .build();

    if (excludePosition) {

      // define SIG_DEF using just "Uncertainty" for NFT
      hdrBuilder.setVCbyHeaderTag(HeaderTag.SIG_DEF, "Uncertainty", HeaderTag.SIG_DEF.comment());

      // change comment for DQUAL_2
      hdrBuilder.setCbyHeaderTag(HeaderTag.DQUAL_2, "Data Quality Metric: mean residual [m]");

      planeList.add(PlaneInfo.HEIGHT);
      // call this plane ALBEDO even if OLA is the source
      planeList.add(PlaneInfo.ALBEDO);
      planeList.add(PlaneInfo.SIGMA);
      planeList.add(PlaneInfo.QUALITY);
      if (!hazParam.noHazard) {
        planeList.add(PlaneInfo.HAZARD);
      }

      // create llrData object. Stores lat,lon,radius information. Needed to fill out fits header
      FitsDataBuilder llrDataB = new FitsDataBuilder(llrData, isGlobal);
      FitsData llrFitsData =
          llrDataB
              .setV(V)
              .setAltProdType(productType)
              .setDataSource(dataSource)
              .setU(ux, UnitDir.UX)
              .setU(uy, UnitDir.UY)
              .setU(uz, UnitDir.UZ)
              .setScale(scale)
              .setGSD(gsd)
              .build();

      // fill out fits header with information in llrFitsData
      hdrBuilder.setByFitsData(llrFitsData);

      saveNFTFits(hdrBuilder, fitsData, planeList, namingConvention, outfile);

    } else {

      planeList.add(PlaneInfo.LAT);
      planeList.add(PlaneInfo.LON);
      planeList.add(PlaneInfo.RAD);
      planeList.add(PlaneInfo.X);
      planeList.add(PlaneInfo.Y);
      planeList.add(PlaneInfo.Z);
      planeList.add(PlaneInfo.HEIGHT);
      planeList.add(PlaneInfo.ALBEDO);
      planeList.add(PlaneInfo.SIGMA);
      planeList.add(PlaneInfo.QUALITY);

      saveDTMFits(
          hdrBuilder, fitsData, planeList, namingConvention, productType, isGlobal, outfile);
    }
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("input-map").required().hasArg().desc("input maplet file").build());
    options.addOption(
        Option.builder("output-fits").required().hasArg().desc("output FITS file").build());
    options.addOption(
        Option.builder("exclude-position")
            .desc(
                "Only save out the height, albedo, sigma, quality, and hazard planes to the output file. Used for creating NFT MLNs.")
            .build());
    options.addOption(
        Option.builder("noHazard")
            .desc(
                "Only used in conjunction with -exclude-position. If present then the NFT MLN will NOT include a Hazard plane initially filled with all ones.")
            .build());
    options.addOption(
        Option.builder("hazardVal")
            .hasArg()
            .desc(
                "Only used in conjunction with -exclude-position. If present then will use the specified value.")
            .build());
    options.addOption(
        Option.builder("lsb")
            .desc(
                """
         By default the sigmas and quality binary files are assumed to be in big-endian floating
          point format. Pass this argument if you know your sigma and quality binary files are
          in little-endian format. For example, products created by SPC executables are OS
          dependent and intel Linux OSes use little-endian.""")
            .build());
    options.addOption(
        Option.builder("scalFactor")
            .hasArg()
            .desc(
                "Enter scale factor used to convert scale to ground sample distance in mm i.e. for SPC maplets the scale factor is 1000000 (km to mm).  Set to 1.0e6 by default.")
            .build());
    options.addOption(
        Option.builder("sigmas-file")
            .hasArg()
            .desc(
                "Path to binary sigmas file containing sigma values per pixel, in same order as the maplet file. If this option is omitted, the sigma plane is set to all zeros.")
            .build());
    options.addOption(
        Option.builder("sigsum-file")
            .hasArg()
            .desc(
                "Path to ascii sigma summary file, containing the overall sigma value of the maplet.")
            .build());
    options.addOption(
        Option.builder("sigmaScale")
            .hasArg()
            .desc(
                "Scale sigmas from sigmas-file by <value>. Only applicable if -sigmas-file is used.  Defaults to 1 if not specified.")
            .build());
    options.addOption(
        Option.builder("mapname")
            .hasArg()
            .desc("Sets the MAP_NAME fits keyword to <mapname>. Default is 'Non-NFT DTM'")
            .build());
    options.addOption(
        Option.builder("quality-file")
            .hasArg()
            .desc(
                "Path to binary quality file containing quality values. If this option is omitted, the quality plane is set to all zeros.")
            .build());
    options.addOption(
        Option.builder("configFile")
            .hasArg()
            .desc(
                """
          Path to fits configuration file that contains keywords and values which should be included
          in the fits header. The fits header will always be fully populated with all keywords
          that are required by the ALTWG SIS. The values may not be populated or UNK if they cannot
          be derived from the data itself. This configuration file is a way to populate those values.""")
            .build());
    options.addOption(
        Option.builder("namingConvention")
            .hasArg()
            .desc(
                """
          Renames the output fits file per the naming convention specified by the string.
          Currently supports 'altproduct', 'dartproduct', and 'altnftmln' conventions.
          NOTE that -exclude-position does not automatically choose the 'altnftmln'
          naming convention! 'ALTWG NFT MLN naming convention (altnftmln) MUST BE EXPLICITLY SPECIFIED
          The renamed file is placed in the path specified by -output-fits""")
            .build());
    return options;
  }

  public static void main(String[] args) throws FitsException, IOException {
    TerrasaurTool defaultOBJ = new Maplet2FITS();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    boolean excludePosition = cl.hasOption("exclude-position");
    boolean noHazard = cl.hasOption("noHazard");
    String namingConvention = cl.getOptionValue("namingConvention", "noneused");
    boolean swapBytes = cl.hasOption("lsb");
    String sigmasFile = cl.hasOption("sigmas-file") ? cl.getOptionValue("sigmas-file") : null;
    String sigsumFile = cl.hasOption("sigsum-file") ? cl.getOptionValue("sigsum-file") : null;
    String qualityFile = cl.hasOption("quality-file") ? cl.getOptionValue("quality-file") : null;
    String fitsConfigFile = cl.hasOption("configFile") ? cl.getOptionValue("configFile") : null;
    String mapName = cl.getOptionValue("mapname", "Non-NFT DTM");

    double scalFactor =
        Double.parseDouble(cl.getOptionValue("scalFactor", "1e6").replaceAll("[dD]", "e"));
    double sigmaScale =
        Double.parseDouble(cl.getOptionValue("sigmaScale", "1.0").replaceAll("[dD]", "e"));
    double hazardVal =
        Double.parseDouble(cl.getOptionValue("hazardVal", "1.0").replaceAll("[dD]", "e"));
    AltwgDataType altwgProduct = AltwgDataType.NA;

    if (sigsumFile != null) logger.info("using {} to parse for global uncertainty.", sigsumFile);

    if (!namingConvention.isEmpty()) {
      if (excludePosition) {
        altwgProduct = AltwgDataType.NFTDTM;
      } else {
        altwgProduct = AltwgDataType.DTM;
      }
    }

    HazardParams hazParams = getHazardParam(noHazard, hazardVal);

    String mapletFile = cl.getOptionValue("input-map");
    String outfile = cl.getOptionValue("output-fits");
    logger.info("altwgProductType:{}", altwgProduct.toString());
    run(
        mapletFile,
        outfile,
        altwgProduct,
        excludePosition,
        fitsConfigFile,
        sigmasFile,
        sigsumFile,
        qualityFile,
        namingConvention,
        swapBytes,
        scalFactor,
        sigmaScale,
        mapName,
        hazParams);
  }

  public static String parseSigsumFile(String sigsumFile) throws IOException {
    if (!Files.exists(Paths.get(sigsumFile))) {
      System.out.println(
          "Warning! Sigmas summary file:"
              + sigsumFile
              + " does not exist! Not "
              + "able to parse sigma summary.");
      return null;
    } else {
      List<String> content = FileUtils.readLines(new File(sigsumFile), Charset.defaultCharset());
      String firstLine = content.get(0);

      if (firstLine != null) {

        // first assume format is <mapletfilename> <sigma summary> <blah>
        // this is the SPC format
        // try to parse a double from this
        String[] array = firstLine.split(" +");
        boolean foundFirst = false;
        if (array.length > 1) {
          System.out.println("Assuming sigma summary file of form:");
          System.out.println("<maplet Filename> <sigma summary> <blah>");
          for (String s : array) {
            if (foundFirst) {
              // second cell after finding the first non-empty cell should be the sigma summary
              // value!
              double thisD = Double.parseDouble(s.replaceAll("[dD]", "e"));
              return Double.isNaN(thisD) ? null : Double.toString(thisD);
            }
            if (!s.isEmpty()) {
              // loop through array until you find first non-empty string.
              // This should be the maplet filename
              foundFirst = true;
            }
          }
        } else {

          // assume format is mean on first line, median on second line
          if (content.size() > 1) {
            System.out.println("Assuming sigma summary file of form:");
            System.out.println("<mean value>");
            System.out.println("<median value>");
            System.out.println("Will use median value");
            return content.get(1).replaceAll("\\s+", "");
          } else {
            // first line is not null but there is only one line
            System.out.println("Assuming first value in first line contains sigma summary!");
            return content.get(0).replaceAll("\\s+", "");
          }
        }

        // did not find first non-empty string or did not find sigma
        System.out.println("WARNING: Could not parse sigma summary file!");
        return null;

        /*
         * firstLine = firstLine.replace(" ", ""); double thisD = StringUtil.parseSafeD(firstLine);
         * if (Double.isNaN(thisD)) { Pattern p = Pattern.compile("^\\s*(\\d+\\.?\\d+)\\s*.*");
         * Matcher m = p.matcher(firstLine); if (m.matches()) { return m.group(1); } else { return
         * null; } } else { return Double.toString(thisD); }
         */
      } else {
        return null;
      }
    }
  }

  /**
   * Given a maplet, return the command call to Maplet2FITS to turn it into a fits file. Allows one
   * to specify filenames of sigma, sigma summary, and quality files. Set string variables to empty
   * string if you do not wish to include them in the command call. Method generates the full set of
   * fits file planes, i.e. NOT an NFT Fits file.
   *
   * @param mapletFile - path to maplet
   * @param configFile - configuration file to use
   * @param lsb - if True assume sigma and quality files are in little-endian
   * @param sigmaFile - pointer to sigma file. Leave as empty string if no file exists/needed
   *     (defaults to 0).
   * @param sigmaScaleF - sigma scale factor to be used with sigma values read from sigma file.
   * @param sigsumFile - pointer to simga summary file. Leave as empty string if no file
   *     exists/needed (defaults to 0)
   * @param qualityFile - pointer to quality file. Leave as empty string if no file exists/needed
   *     (defaults to 0).
   * @return
   */
  public static String getCmd(
      File mapletFile,
      String outFits,
      String configFile,
      boolean altwgNaming,
      boolean lsb,
      String sigmaFile,
      String sigsumFile,
      String sigmaScaleF,
      String qualityFile) {

    StringBuilder toolexe = new StringBuilder("Maplet2FITS");
    if (!configFile.isEmpty()) {
      toolexe.append(" -configFile ");
      toolexe.append(configFile);
    }

    if (altwgNaming) {
      toolexe.append(" -altwgNaming");
    }

    if (lsb) {
      toolexe.append(" -lsb");
    }

    Path thisFile;
    if (!sigmaFile.isEmpty()) {
      thisFile = Paths.get(sigmaFile);
      if (Files.exists(thisFile)) {
        toolexe.append(" -sigmas-file ");
        toolexe.append(thisFile.toAbsolutePath());
      } else {
        logger.warn("Could not find sigmas file:{}", sigmaFile);
      }
    }

    if (!sigmaScaleF.isEmpty()) {
      toolexe.append(" -sigmaScale ");
      toolexe.append(sigmaScaleF);
    }

    if (!sigsumFile.isEmpty()) {
      thisFile = Paths.get(sigsumFile);
      if (Files.exists(thisFile)) {
        toolexe.append(" -sigsum-file ");
        toolexe.append(thisFile.toAbsolutePath());
      } else {
        logger.warn("Could not find sigma summary file:{}", sigsumFile);
      }
    }

    if (!qualityFile.isEmpty()) {
      thisFile = Paths.get(qualityFile);
      if (Files.exists(thisFile)) {
        toolexe.append(" -quality-file ");
        toolexe.append(thisFile.toAbsolutePath());
      } else {
        logger.warn("Could not find quality file:{}", qualityFile);
      }
    }

    toolexe.append(" ");
    toolexe.append(mapletFile.getAbsolutePath());
    toolexe.append(" ");
    toolexe.append(outFits);

    return toolexe.toString();
  }

  private static float getSigma(DataInput dataIn, double sigmaScale) throws IOException {
    float floatVal = 0f;
    if (dataIn != null) {
      floatVal = dataIn.readFloat();
    }
    floatVal = floatVal * (float) sigmaScale;
    return floatVal;
  }

  private static float getQuality(DataInput dataIn) throws IOException {
    float floatVal = 0f;
    if (dataIn != null) {
      floatVal = dataIn.readFloat();
    }
    return floatVal;
  }

  private static void saveNFTFits(
      FitsHdrBuilder hdrBuilder,
      FitsData fitsData,
      List<PlaneInfo> planeList,
      String namingConvention,
      String outfile)
      throws FitsException, IOException {

    File crossrefFile = null;

    NameConvention nameConvention = NameConvention.parseNameConvention(namingConvention);
    if (nameConvention != NameConvention.NONEUSED) {

      String outNFTFname = outfile;
      Path outPath = Paths.get(outNFTFname);

      // save old filename
      String oldFilename = outPath.getFileName().toString();

      // hardcode for now. NFT is not a nominal Toolkit product.
      AltwgDataType productType = null;
      boolean isGlobal = false;

      // generate new NFT output filename based on naming convention
      ProductNamer productNamer = NamingFactory.parseNamingConvention(namingConvention);
      String newbaseName = productNamer.productbaseName(hdrBuilder, productType, isGlobal);

      String newFilename = newbaseName + ".fits";

      // replace outfile with new nft filename and write FITS file to it.
      outNFTFname = outNFTFname.replace(oldFilename, newFilename);

      // save new PDS name in cross-reference file, for future reference
      crossrefFile = new File(outNFTFname + ".crf");
      AsciiFile crfFile = new AsciiFile(crossrefFile.getAbsolutePath());
      crfFile.streamSToFile(newbaseName, 0);
      crfFile.closeFile();

      outfile = outNFTFname;
    }

    FitsHeaderType hdrType = FitsHeaderType.NFTMLN;
    ProductFits.saveNftFits(fitsData, planeList, outfile, hdrBuilder, hdrType, crossrefFile);
  }

  private static void saveDTMFits(
      FitsHdrBuilder hdrBuilder,
      FitsData fitsData,
      List<PlaneInfo> planeList,
      String namingConvention,
      AltwgDataType productType,
      boolean isGlobal,
      String outfile)
      throws FitsException, IOException {

    // Use a different static method to create the ALTWG product. This allows me to differentiate
    // between different kinds of fits header types.
    FitsHeaderType hdrType = FitsHeaderType.DTMLOCALALTWG;

    File crossrefFile;
    String outFitsFname = outfile;

    // possible renaming of output file
    NameConvention nameConvention = NameConvention.parseNameConvention(namingConvention);
    File[] outFiles =
        NamingFactory.getBaseNameAndCrossRef(
            nameConvention, hdrBuilder, productType, isGlobal, outfile);

    // check if cross-ref file is not null. If so then output file was renamed to naming convention.
    crossrefFile = outFiles[1];
    if (crossrefFile != null) {

      // rename fitsFile per naming convention and create a cross-reference file.
      String baseOutputName = outFiles[0].toString();

      // determine whether original outfile is a directory
      File outFname = new File(outfile);
      String outputFolder = outFname.getAbsoluteFile().getParent();
      if (outFname.isDirectory()) {
        outputFolder = outfile;

        // cannot create cross-reference file if original outfile was a directory. make it null;
        crossrefFile = null;
      }
      outFitsFname = new File(outputFolder, baseOutputName + ".fits").getAbsolutePath();
    }
    ProductFits.saveDataCubeFits(
        fitsData, planeList, outFitsFname, hdrBuilder, hdrType, crossrefFile);
  }
}
