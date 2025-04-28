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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.fits.HeaderTag;
import terrasaur.fits.ProductFits;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.AppVersion;
import terrasaur.utils.JCommanderUsage;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataUtil;
import terrasaur.utils.ProcessUtils;
import vtk.vtkPolyData;

public class OBJ2DSK implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();



  @Override
  public String shortDescription() {
      return "Convert an OBJ shape file to SPICE DSK format.";
  }

  @Override
  public String fullDescription(Options options) {
    StringBuilder builder = new StringBuilder();
    Arguments arguments = new Arguments();
    JCommander jcommander = new JCommander(arguments);
    jcommander.setProgramName("OBJ2DSK");

    JCommanderUsage jcUsage = new JCommanderUsage(jcommander);
    jcUsage.setColumnSize(100);
    jcUsage.usage(builder, 4, arguments.commandDescription);
    return builder.toString();

  }

  private enum DSK_KEYS {
    SURF_NAME, CENTER_NAME, REFFRAME_NAME, NAIF_SURFNAME, NAIF_SURFCODE, NAIF_SURFBODY, METAK, COMMENTFILE
  }

  private static class Arguments {

    private final String commandDescription = AppVersion.getVersionString()
        + "\n\nConverts a triangular plate model in OBJ format to a SPICE DSK file.\n"
        + "The SPICE utility application 'mkdsk' must already be present on your PATH.\n";

    @Parameter(names = "-help", help = true)
    private boolean help;

    @Parameter(names = "--latMin", description = "<latMin> Minimum latitude of OBJ in degrees.",
        required = false, order = 0)
    private double latMin = -90D;

    @Parameter(names = "--latMax", description = "<latMax> Maximum latitude of OBJ in degrees.",
        required = false, order = 1)
    private double latMax = 90D;

    @Parameter(names = "--lonMin", description = "<lonMin> Minimum longitude of OBJ in degrees.",
        required = false, order = 2)
    private double lonMin = 0;

    @Parameter(names = "--lonMax", description = "<lonMax> Maximum longitude of OBJ in degrees.",
        required = false, order = 3)
    private double lonMax = 360D;

    @Parameter(names = "--fitsFile",
        description = "<filename> path to DTM fits file containing lat,lon"
            + " information as planes. Assumes PLANE1=latitude, PLANE2=longitude. Use in place of specifying lat/lon min/max values.",
        required = false, order = 4)
    private String fitsFile = "";

    @Parameter(names = "--fine-scale",
        description = "<fine-voxel-scale> Floating point value representing the "
            + " ratio of the spatial index's fine  voxel edge length to the average plate extent. "
            + " The 'extent' of a plate in a given coordinate direction is the difference between the maximum and minimum "
            + " values of that coordinate attained on the plate. Only required if mkdsk version is "
            + " lower than 66.",
        required = false, order = 11)
    double fineVoxScale = Double.NaN;

    @Parameter(names = "--coarse-scale",
        description = " <coarse-voxel-scale>"
            + " Integer value representing the ratio of the edge length of coarse voxels to"
            + " fine voxels. The number must be large enough that the total number of coarse"
            + " voxels is less than the value of MAXCGR, which is currently 1E5."
            + " Only required if mkdsk version is lower than 66.",
        required = false, order = 12)
    Integer coarseVoxScale = -999;

    @Parameter(names = "--useSetupFile",
        description = "<inputSetupFile> Use <inputSetupFile>"
            + " instead of the default setup file created by the tool.",
        required = false, order = 13)
    String inputSetup = "";

    @Parameter(names = "--writesetupFile",
        description = "<outputSetupFile> Write the setup file"
            + " to the specified path instead of writing it as a temporary file which gets deleted"
            + " after execution.",
        required = false, order = 14)
    String outsetupFname = "";

    @Parameter(names = "--keepTempFiles",
        description = "enable this to prevent setup files"
            + " from being deleted. Used for debugging purposes to see what is being sent"
            + " to mkdsk.",
        required = false, order = 15)
    boolean keepTmpFiles = false;

    @Parameter(names = "--mkFile",
        description = "<spice-meta-kernel-file> path to SPICE meta kernel file."
            + " Metakernel only needs to point to leap seconds kernel and a frames kernel that contains"
            + " the digital ID to CENTER_NAME and REF_FRAME_NAME lookup table."
            + " This argument is REQUIRED if user does NOT supply a setupFile!",
        required = false, order = 4)
    String mkFile = "";

    @Parameter(names = "--surfName",
        description = "<surfaceName> Allows user to modify the "
            + " SURFACE_NAME (name of the specific shape data set for the central body)"
            + " used in the default setup file created by the tool. This is a required"
            + " keyword in the setup file.",
        required = false, order = 5)
    String surfaceName = "BENNU";

    @Parameter(names = "--centName", description = "<centerName> Allows user to modify the "
        + " CENTER_NAME (central body name) used in the default setup file created by the tool. "
        + " Can also be an ID code.  This is a required keyword in the setup file.",
        required = false, order = 6)
    String centerName = "BENNU";

    @Parameter(names = "--refName", description = "<refFrameName> Allows user to modify the "
        + " REF_FRAME_NAME (reference frame name) used in the default setup file created by the tool. "
        + " This is a required keyword in the setup file.", required = false, order = 7)
    String refFrameName = "IAU_BENNU";

    @Parameter(names = "--naif_surfName",
        description = "<naif surface name> Allows user to add the "
            + " NAIF_SURFACE_NAME to the default setup file created by the tool. "
            + " This may be needed under certain conditions. Optional keyword. "
            + " Default is not to use it.",
        required = false, order = 8)
    String naifSurfName = "";

    @Parameter(names = "--naif_surfCode",
        description = "<integer ID surface code> Allows user to add the "
            + " NAIF_SURFACE_CODE to the default setup file created by the tool. "
            + " Allows the tool to associate this ID code to the NAIF_SURFACE_NAME. Optional keyword. "
            + " Default is not to use it.",
        required = false, order = 9)
    String naifSurfCode = "";

    @Parameter(names = "--naif_surfBody",
        description = "<integer body ID code> Allows user to add the "
            + " NAIF_SURFACE_BODY to the default setup file created by the tool. "
            + " This may be needed under certain conditions. Optional keyword."
            + " Default is not to use it.",
        required = false, order = 10)
    String naifSurfBody = "";

    @Parameter(names = "--cmtFile",
        description = "<commentFile> Specify the comment file"
            + " that mkdsk will add to the DSK. Comment file is an ASCII file containing"
            + " additional information about the DSK. Default is single space.",
        required = false, order = 11)
    String cmtFile = " ";

    @Parameter(names = "-shortDescription", hidden = true)
    private boolean shortDescription = false;

    @Parameter(
        description = " Versions of mkdsk that are V066 and higher will automatically calculate the\n"
            + "   voxel scales which optimize processing time without exceeding maximum array sizes.\n"
            + "   However, if you are using a version of mkdsk that is below v066 you can specify the\n"
            + "   FINE_VOXEL_SCALE and COARSE_VOXEL_SCALE via the '--fine-scale' and '--coarse-scale'\n"
            + "   optional arguments.\n"
            + "   Run 'mkdsk' by itself and note the 'Toolkit ver' version number. If it is below\n"
            + "   066 then you MUST specify the fine and coarse voxel scales.\n"
            + "   See SPICE documentation notes on recommended values.\n\n"
            + "Usage: OBJ2DSK [options] <input-obj-file> <output-dsk-file>\nWhere:\n"
            + "  <input-obj-file>\n" + "          input OBJ file\n" + "  <output-dsk-file>\n"
            + "          output dsk file\n"
            + "  NOTE: MUST set --metakFile if not supplying a custom setup file!")
    private List<String> files = new ArrayList<>();

  }

  private final Double fineVoxScale;
  private final Integer coarseVoxScale;

  public OBJ2DSK(double fineVoxScale, int coarseVoxScale) {
    this.fineVoxScale = fineVoxScale;
    this.coarseVoxScale = coarseVoxScale;
  }

  public OBJ2DSK() {
    this(Double.NaN, -1);
  }

  public static void main(String[] args) throws Exception {

    TerrasaurTool defaultObj = new OBJ2DSK();

    int i = 0;
    Map<String, Double> latLonMinMax = new HashMap<String, Double>();
    // String fitsFname = "";
    // String temp;
    // String inputSetup = "";
    // boolean keepTmpFiles = false;

    // check for -shortDescription before looking for required arguments
    for (String arg : args) {
      if (arg.equals("-shortDescription")) {
        System.out.println(defaultObj.shortDescription());
        System.exit(0);
      }
    }

    Arguments arg = new Arguments();

    JCommander command = new JCommander(arg);
    try {
      // @Deprecated
      // command = new JCommander(arg, args);
      command.parse(args);
    } catch (ParameterException ex) {
      System.out.println(defaultObj.fullDescription(null));
      System.out.println("Error parsing input arguments:");
      System.out.println(ex.getMessage());
      command = new JCommander(arg);
      System.exit(1);

    }

    if ((args.length < 1) || (arg.help)) {
      System.out.println(defaultObj.fullDescription(null));
      System.exit(0);
    }

    // This is to avoid java crashing due to inability to connect to an X display
    System.setProperty("java.awt.headless", "true");

    String spiceFile = "";
    String inFile = "";
    String outFile = "";
    if (arg.files.size() != 2) {
      String errMesg = "ERROR! Expecting 2 required inputs: input OBJ, output DSK";
      throw new RuntimeException(errMesg);
    } else {
      spiceFile = arg.mkFile;
      inFile = arg.files.get(0);
      outFile = arg.files.get(1);
    }

    boolean useFitsFile = false;
    boolean latLonSet = false;

    String fitsFname = arg.fitsFile;
    if (!fitsFname.isEmpty()) {
      useFitsFile = true;
      System.out.println("Will use lat,lons from " + fitsFname + " to set lat,lon bounds.");
      // load the fits header and parse for min, max lat, lon values
      latLonMinMax = ProductFits.minMaxLLFromFits(new File(fitsFname));

      if (latLonMinMax.size() < 4) {
        System.out.println("ERROR! Could not parse all min,max lat/lon corners!");
        System.out.println("Unable to create DSK for " + inFile + "!");
        System.exit(1);
      }
    } else {
      // parse lat,lon bounds. Some of these may be set to default values.
      latLonMinMax.put(HeaderTag.MINLAT.toString(), arg.latMin);
      latLonMinMax.put(HeaderTag.MAXLAT.toString(), arg.latMax);
      latLonMinMax.put(HeaderTag.MINLON.toString(), arg.lonMin);
      latLonMinMax.put(HeaderTag.MAXLON.toString(), arg.lonMax);
      latLonSet = true;
    }

    System.out.println("Using these lat, lon bounds:");
    for (String thisKey : latLonMinMax.keySet()) {
      System.out.println("key:" + thisKey + ", value:" + latLonMinMax.get(thisKey));
    }

    NativeLibraryLoader.loadVtkLibraries();


    OBJ2DSK obj2dsk;
    if ((Double.isNaN(arg.fineVoxScale)) || (arg.coarseVoxScale < 0)) {
      obj2dsk = new OBJ2DSK();
    } else {
      obj2dsk = new OBJ2DSK(arg.fineVoxScale, arg.coarseVoxScale);
    }

    // generate setup file if needed
    File setupFile = null;
    Map<DSK_KEYS, String> dskParams = new HashMap<DSK_KEYS, String>();
    dskParams.put(DSK_KEYS.SURF_NAME, arg.surfaceName);
    dskParams.put(DSK_KEYS.CENTER_NAME, arg.centerName);
    dskParams.put(DSK_KEYS.REFFRAME_NAME, arg.refFrameName);
    dskParams.put(DSK_KEYS.NAIF_SURFNAME, arg.naifSurfName);
    dskParams.put(DSK_KEYS.NAIF_SURFCODE, arg.naifSurfCode);
    dskParams.put(DSK_KEYS.NAIF_SURFBODY, arg.naifSurfBody);
    dskParams.put(DSK_KEYS.COMMENTFILE, arg.cmtFile);
    dskParams.put(DSK_KEYS.METAK, spiceFile);

    String outsetupFname = arg.outsetupFname;
    String inputSetup = arg.inputSetup;

    boolean keepTmpFiles = arg.keepTmpFiles;
    if (inputSetup.isEmpty()) {

      if (spiceFile.isEmpty()) {
        String errMesg = "ERROR! MUST supply path to SPICE metakernel via --mkFile!";
        throw new RuntimeException(errMesg);
      }

      System.out.println("Creating default setup file");
      setupFile = createSetup(latLonMinMax, obj2dsk.fineVoxScale, obj2dsk.coarseVoxScale, dskParams,
          outsetupFname);
      if (keepTmpFiles) {
        System.out.println("setup file created here:" + outsetupFname);
      } else {
        setupFile.deleteOnExit();
      }

    } else {

      // check that input setup file exists
      setupFile = new File(inputSetup);
      if (!setupFile.exists()) {
        String errMesg = "Custom setup file:" + inputSetup + " not found!";
        throw new RuntimeException(errMesg);
      }
      System.out.println("Using custom setup file:" + inputSetup);

    }
    obj2dsk.run(inFile, outFile, latLonMinMax, setupFile, outsetupFname, keepTmpFiles);

    /*
     * for (; i < args.length; ++i) { if (args[i].equals("-shortDescription")) {
     * System.out.println(defaultObj.shortDescription()); System.exit(0); } else if
     * (args[i].equals("--latMin")) { latLonMinMax.put(HeaderTag.MINLAT.toString(),
     * StringUtil.parseSafeD(args[++i])); latLonSet = true; } else if (args[i].equals("--latMax")) {
     * latLonMinMax.put(HeaderTag.MAXLAT.toString(), StringUtil.parseSafeD(args[++i])); latLonSet =
     * true; } else if (args[i].equals("--lonMin")) { latLonMinMax.put(HeaderTag.MINLON.toString(),
     * StringUtil.parseSafeD(args[++i])); latLonSet = true; } else if (args[i].equals("--lonMax")) {
     * latLonMinMax.put(HeaderTag.MAXLON.toString(), StringUtil.parseSafeD(args[++i])); latLonSet =
     * true; } else if (args[i].equals("--fits-file")) { fitsFname = args[++i]; useFitsFile = true;
     * } else if (args[i].equals("--fine-scale")) { temp = args[++i]; fineVoxScale =
     * StringUtil.parseSafeD(temp); if (fineVoxScale == Double.NaN) {
     * System.err.println("ERROR! value for fine-scale:" + temp);
     * System.err.println("Could not be converted into a double!");
     * System.out.println("Exiting program."); System.exit(1); } userVox = true;
     * 
     * } else if (args[i].equals("--coarse-scale")) {
     * 
     * temp = args[++i]; double tempD = StringUtil.parseSafeD(temp); if (tempD == Double.NaN) {
     * System.err.println("ERROR! value for coarse-scale:" + temp);
     * System.err.println("Could not be converted into a number!");
     * System.out.println("Exiting program."); System.exit(1); } coarseVoxScale = (int) tempD;
     * userVox = true;
     * 
     * } else if (args[i].equals("--writesetupFile")) { outsetupFname = args[++i]; } else if
     * (args[i].equals("--useSetupFile")) { inputSetup = args[++i]; keepTmpFiles = true; } else if
     * (args[i].equals("--keepTempFiles")) { keepTmpFiles = true; } else { break; } }
     * 
     * // There must be numRequiredArgs arguments remaining after the options. // Otherwise abort.
     * int numberRequiredArgs = 3; if (args.length - i != numberRequiredArgs) {
     * System.out.println(defaultObj.fullDescription()); System.exit(0); }
     * 
     * String current = new java.io.File(".").getAbsolutePath();
     * System.out.println("Running OBJ2DSK. Current dir:" + current);
     * 
     * String spicefile = args[i++]; String infile = args[i++]; String outfile = args[i++];
     */

  }

  public void run(String infile, String outfile, Map<String, Double> latLonMinMax, File setupFile,
      String outsetupFname, boolean keepTmpFiles) throws Exception {

    System.out.println("Running OBJ2DSK.");
    System.out.println("FINE_VOXEL_SCALE = " + Double.toString(fineVoxScale));
    System.out.println("COARSE_VOXEL_SCALE = " + Integer.toString(coarseVoxScale));

    // File setupFile = null;
    // if (inputSetup.length() < 1) {
    // System.out.println("Creating default setup file");
    // setupFile = createSetup(spicefile, latLonMinMax, this.fineVoxScale, this.coarseVoxScale,
    // outsetupFname);
    // if (keepTmpFiles) {
    // System.out.println("setup file created here:" + outsetupFname);
    // } else {
    // setupFile.deleteOnExit();
    // }
    // } else {
    // // check that input setup file exists
    // setupFile = new File(inputSetup);
    // if (!setupFile.exists()) {
    // String errMesg = "Custom setup file:" + inputSetup + " not found!";
    // throw new RuntimeException(errMesg);
    // }
    // System.out.println("Using custom setup file:" + inputSetup);
    // }

    vtkPolyData inpolydata = PolyDataUtil.loadShapeModelAndComputeNormals(infile);

    // We need to save out the OBJ file again in case it contains comment
    // lines since mkdsk does not support lines beginning with #
    // The OBJ file is saved to a temporary filename in order to preserve the
    // original OBJ. The temporary filename is deleted afterwards.
    File shapeModel = File.createTempFile("shapemodel-", null);
    shapeModel.deleteOnExit();
    PolyDataUtil.saveShapeModelAsOBJ(inpolydata, shapeModel.getAbsolutePath());

    // Delete dsk file if already exists since otherwise mkdsk will complain
    if (new File(outfile).isFile())
      new File(outfile).delete();

    String command = "mkdsk -setup " + setupFile.getAbsolutePath() + " -input "
        + shapeModel.getAbsolutePath() + " -output " + outfile;
    ProcessUtils.runProgramAndWait(command, null, false);
  }


  /**
   * Create the setup file for mkdsk executable.
   *
   * @param latLonCorners
   * @param fineVoxScale
   * @param coarseVoxScale
   * @param dskParams
   * @param setupFname
   * @return
   */
  private static File createSetup(Map<String, Double> latLonCorners, Double fineVoxScale,
      Integer coarseVoxScale, Map<DSK_KEYS, String> dskParams, String setupFname) {

    // evaluate latlon corners. Exit program if any are NaN.
    evaluateCorners(latLonCorners);

    File setupFile;

    if (setupFname.length() < 1) {
      setupFile = null;
      try {
        setupFile = File.createTempFile("setupfile-", null);
      } catch (IOException e) {
        String errMesg = "ERROR creating setupfile:" + setupFname;
        throw new RuntimeException(errMesg);
      }
    } else {
      setupFile = new File(setupFname);
    }
    System.out
        .println("Setup file for mkdsk created here:" + setupFile.getAbsolutePath().toString());

    // relativize the path to the metakernel file. Do this because mkdsk has a limit on the string
    // length to the metakernel. Get normalized absolute path to mkFile in case the user enters a
    // relative path string, e.x. ../../SPICE/spice-kernels.mk
    Path currDir = FileSystems.getDefault().getPath("").toAbsolutePath();
    Path mkFile = Paths.get(dskParams.get(DSK_KEYS.METAK)).toAbsolutePath().normalize();

    System.out.println("currDir:" + currDir.toString());
    System.out.println("mkFile:" + mkFile.toString());

    // mkFile path relative to currDir
    Path relPath = currDir.relativize(mkFile);
    String spicefile = relPath.toString();

    if (spicefile.length() > 80) {
      System.out.println("Error: pointer to SPICE metakernel kernel file may not be longer than"
          + " 80 characters.");
      System.out.println("The paths inside the metakernel file can be as long as 255 characters.");
      System.exit(1);
    }


    // create the content of setup file
    StringBuilder sb = new StringBuilder();
    sb.append("\\begindata\n");
    sb.append("COMMENT_FILE        = '" + dskParams.get(DSK_KEYS.COMMENTFILE) + "'\n");
    sb.append("LEAPSECONDS_FILE    = '" + spicefile + "'\n");
    sb.append("SURFACE_NAME        = '" + dskParams.get(DSK_KEYS.SURF_NAME) + "'\n");
    sb.append("CENTER_NAME         = '" + dskParams.get(DSK_KEYS.CENTER_NAME) + "'\n");
    sb.append("REF_FRAME_NAME      = '" + dskParams.get(DSK_KEYS.REFFRAME_NAME) + "'\n");
    // sb.append("SURFACE_NAME = 'BENNU'\n");
    // sb.append("CENTER_NAME = 'BENNU'\n");
    // sb.append("REF_FRAME_NAME = 'IAU_BENNU'\n");
    sb.append("START_TIME          = '1950-JAN-1/00:00:00'\n");
    sb.append("STOP_TIME           = '2050-JAN-1/00:00:00'\n");
    sb.append("DATA_CLASS          = 2\n");
    sb.append("INPUT_DATA_UNITS    = ( 'ANGLES    = DEGREES'\n");
    sb.append("                        'DISTANCES = KILOMETERS' )\n");
    sb.append("COORDINATE_SYSTEM   = 'LATITUDINAL'\n");
    String valueString = String.format("MINIMUM_LATITUDE    = %.5f\n",
        latLonCorners.get(HeaderTag.MINLAT.toString()));
    sb.append(valueString);

    // out.write("MAXIMUM_LATITUDE = 90\n");
    valueString = String.format("MAXIMUM_LATITUDE    = %.5f\n",
        latLonCorners.get(HeaderTag.MAXLAT.toString()));
    sb.append(valueString);

    // out.write("MINIMUM_LONGITUDE = -180\n");
    valueString = String.format("MINIMUM_LONGITUDE    = %.5f\n",
        latLonCorners.get(HeaderTag.MINLON.toString()));
    sb.append(valueString);

    // out.write("MAXIMUM_LONGITUDE = 180\n");
    valueString = String.format("MAXIMUM_LONGITUDE    = %.5f\n",
        latLonCorners.get(HeaderTag.MAXLON.toString()));
    sb.append(valueString);

    sb.append("DATA_TYPE           = 2\n");
    sb.append("PLATE_TYPE          = 3\n");

    String val;
    if (fineVoxScale > 0D) {
      val = fineVoxScale.toString();
      val = val.trim();
      sb.append("FINE_VOXEL_SCALE    = " + val + "\n");
    }
    if (coarseVoxScale > 0) {
      val = coarseVoxScale.toString();
      val = val.trim();
      sb.append("COARSE_VOXEL_SCALE  = " + val + "\n");
    }

    String naifSurf = dskParams.get(DSK_KEYS.NAIF_SURFNAME);
    String naifCode = dskParams.get(DSK_KEYS.NAIF_SURFCODE);
    String naifBody = dskParams.get(DSK_KEYS.NAIF_SURFBODY);
    if ((naifSurf.length() > 0) && (naifCode.length() > 0) && (naifBody.length() > 0)) {
      sb.append("NAIF_SURFACE_NAME +=" + "'" + dskParams.get(DSK_KEYS.NAIF_SURFNAME) + "'\n");
      sb.append("NAIF_SURFACE_CODE +=" + dskParams.get(DSK_KEYS.NAIF_SURFCODE) + "\n");
      sb.append("NAIF_SURFACE_BODY +=" + dskParams.get(DSK_KEYS.NAIF_SURFBODY) + "\n");

    } else {
      System.out.println("optional NAIF body keywords not set. Will not use them in setup file.");
    }

    sb.append("\\begintext\n");

    try {
      FileWriter os = new FileWriter(setupFile);
      BufferedWriter out = new BufferedWriter(os);
      out.write(sb.toString());
      out.close();

    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("ERROR creating setupfile for OBJ2DSK! Stopping with error!");
      System.exit(1);
    }
    return setupFile;

  }

  /**
   * Evaluate results of string parsing. Throw exception if any resolved to NaN
   * 
   * @param latLonCorners
   */
  private static void evaluateCorners(Map<String, Double> latLonCorners) {

    for (String key : latLonCorners.keySet()) {
      if (latLonCorners.get(key) == Double.NaN) {
        System.err.println("ERROR! Value for:" + key + " is NaN! Retry wiht proper string double.");
        System.err.println("Exiting program!");
        System.exit(1);
      }
    }
  }

}
