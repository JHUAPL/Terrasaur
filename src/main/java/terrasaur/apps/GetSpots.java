package terrasaur.apps;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spice.basic.AberrationCorrection;
import spice.basic.Body;
import spice.basic.CSPICE;
import spice.basic.FOV;
import spice.basic.Instrument;
import spice.basic.LatitudinalCoordinates;
import spice.basic.Matrix33;
import spice.basic.Plane;
import spice.basic.PositionVector;
import spice.basic.ReferenceFrame;
import spice.basic.SCLK;
import spice.basic.SCLKTime;
import spice.basic.SpiceException;
import spice.basic.StateRecord;
import spice.basic.TDBTime;
import spice.basic.Vector3;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.CellInfo;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataUtil;
import terrasaur.utils.SPICEUtil;
import terrasaur.utils.math.MathConversions;
import vtk.vtkIdList;
import vtk.vtkPolyData;

/**
 *
 *
 * <h2>NAME</h2>
 *
 * GetSpots - find relevant OSIRIS-REx data for assigning values to facets in an OSIRIS-REx map
 * interchange (OBJ) file
 *
 * <h2>SYNOPSIS</h2>
 *
 * <code>GetSpots --spice <em>spicemetakernel</em> --obj <em>objfile</em> --instype
 * <em>instrumenttype</em> --sclk <em>sclkfile</em> --maxdist <em>distance</em></code>
 *
 * <h2>DESCRIPTION</h2>
 *
 * GetSpots identifies those times, listed in <em>sclkfile</em>, when the boresight of
 * <em>instrumenttype</em> intersects the surface of Bennu less than <em>distance</em> milliradians
 * from the center of individual facets in the OBJ file described in <em>objfile</em>. Needed spice
 * files for this calculation are listed in <em>spicemetakernel</em>.
 *
 * <p>At the time a three-dimensional map is made, a designated DSK file will be used to create an
 * OBJ file that act as a framework on which a map will be made. (This is done with the utility
 * {@link DSK2OBJ}.) The OBJ file identifies a set of vertices (in body centered Cartesian
 * coordinates) and groups these vertices to identify facets. Facet numbers correspond to their
 * ordinal position in the list of facet identifications. An ancillary file locates the center of
 * each facet in latitude (deg), longitude (deg), and range (km) from center of figure.
 *
 * <p>Mapmakers may wish to assign a value to a facet based on an algorithmic combination of one or
 * more observations that are in the proximity of the facet. In order to do this, they need to
 * understand which observations are candidates for their analysis. Knowing the distance and
 * position angle of the observation relative to the center of the facet is the discriminator for
 * determining which observations are candidates, and GetSpots provides this information.
 *
 * <p>For all observations taken by OSIRIS-REx, the unique value of a counter on the spacecraft when
 * the observation was made is known. This is called the sclk ("sklock") value. SPICE files provide
 * spacecraft ephemeris and orientation, Bennu position, orientation, and shape, and instrument
 * boresight and orientations. When combined with the sclk value, the intersection of the boresight
 * on the surface of Bennu can be calculated.
 *
 * <p>A simple text file of sclk values (one per line) acts as input to getspots. GetSpots can
 * determine which boresight values corresponding to the sclk values do not exceed the maximum
 * distance criteria for each facet, along with the position angle of the spot, and the fraction of
 * the facet covered by the spot. The position angle is valued from 0 to 359 degrees North being 0
 * with east being 90 degrees. Spots that do not completely intersect the surface of Bennu are
 * flagged. Any additional information on the input line following the sclk value is echoed to the
 * output unchanged, allowing other data useful in the subsequent analysis to be carried along in
 * the output.
 *
 * <h2>INPUTS</h2>
 *
 * <em>spicemetakernel</em> is a spice metakernel listing all required spice files (DSK, SPK, PCK,
 * etc.) needed to perform the analysis. Only files needed for this analysis are in the metakernel
 * (e.g. no ambiguity about which file to use.) Required contents of this metakernel is TBD.
 *
 * <p><em>instrumenttype</em> is a code that specifies the boresight and field of view of the
 * instrument to use in the analysis. This must be one of "OLA", "OTES", "OVIRS", "POLYCAM",
 * "MAPCAM", "SAMCAM", or "NAVCAM".
 *
 * <p><em>distance</em> is the maximum distance of the instrument boresight from the center of the
 * facet expressed in milliradians.
 *
 * <p><em>sclkfile</em> is a text file that contains the sclk values for one or more times of
 * observation by an instrument. Leading whitespace is ignored. One or more whitespace characters
 * must separate the sclkvalue from the rest of the line. The format is as follows:<br>
 * <code>
 * &nbsp;BEGIN<br>
 * &nbsp;sclkvalue [otherdata]<br>
 * &nbsp;sclkvalue [otherdata]<br>
 * &nbsp;.<br>
 * &nbsp;.<br>
 * &nbsp;END<br>
 * </code> Where:<br>
 * &nbsp;<code>sclkvalue</code> is a string (format TBD)<br>
 * &nbsp;<code>[otherdata]</code> includes all additional data (including whitespace) in the line up
 * to, but not including, the linefeed character.
 *
 * <h2>OUTPUT</h2>
 *
 * All output is written to standard output.
 *
 * <p>Output is a text file, with each line terminated by linefeeds.<br>
 * <code>
 * &nbsp;F1<br>
 * &nbsp;sclkvalue dist pos frac flag inc ems phs [otherdata]<br>
 * &nbsp;sclkvalue dist pos frac flag inc ems phs [otherdata]<br>
 * &nbsp;..<br>
 * &nbsp;F2<br>
 * &nbsp;sclkvalue dist pos frac flag inc ems phs [otherdata]<br>
 * &nbsp;sclkvalue dist pos frac flag inc ems phs [otherdata]<br>
 * &nbsp;F3<br>
 * &nbsp;F4<br>
 * &nbsp;..<br>
 * &nbsp;FN<br>
 * &nbsp;END<br>
 * </code> Where:<br>
 * &nbsp;F<em>N</em> is a facet identifier that identifies facet number <em>N</em>. Each sclkvalue
 * that meets the distance criteria for facet number <em>N</em> is listed sequentially after the
 * facet identifier. If no sclk values meet the distance criteria, then no values are listed, and
 * the next facet identifier follows on the next line. &nbsp; <code>sclkvalue</code> is the sclk
 * exactly as it appeared in the input file.<br>
 * &nbsp;<code>dist</code> is a real number that describes the distance of the spot from the center
 * of the facet in units of milliradians.<br>
 * &nbsp;<code>pos</code> is the position angle of the center of the instrument boresight
 * intersection on the surface measured in degrees from North, with East being 90 degrees.<br>
 * &nbsp;<code>frac</code> is a real number greater than or equal to 0.0 and less than or equal to
 * 100.0 that describes the fraction of the facet covered by the spot. If the value is 0.0, then the
 * spot does not cover any portion of the facet. If the value is 100.0 then the facet is entirely
 * covered by the spot.<br>
 * &nbsp;<code>flag</code> is 1 if any portion of the spot does not intersect the surface of Bennu,
 * otherwise it is 0. <br>
 * &nbsp;<code>inc</code> is the incidence angle in degrees<br>
 * &nbsp;<code>ems</code> is the emission angle in degrees<br>
 * &nbsp;<code>phs</code> is the phase angle in degrees<br>
 * &nbsp;<code>[otherdata]</code> is all textual data on the line following the sclk value, up to
 * the linefeed, unchanged.
 *
 * @author nairah1
 */
public class GetSpots implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  private GetSpots() {}

  @Override
  public String shortDescription() {
    return "find relevant OSIRIS-REx data for assigning values to facets in an OBJ file.";
  }

  @Override
  public String fullDescription(Options options) {
    String header =
        """
                    This program identifies those times when the boresight of instrumenttype intersects the surface
                    of Bennu less than a specified distance from the center of individual facets in shape model.
                    """;
    String footer =
        """
                    All output is written to standard output in the following format:
                     F1
                     sclkvalue dist pos frac flag inc ems phs [otherdata]
                     sclkvalue dist pos frac flag inc ems phs [otherdata]
                     ..
                     F2
                     sclkvalue dist pos frac flag inc ems phs [otherdata]
                     sclkvalue dist pos frac flag inc ems phs [otherdata]
                     ..
                     F3
                     F4
                     ..
                     FN
                     END
                    Where:
                     FN is a facet identifier that identifies facet number N. Each sclkvalue that meets the distance criteria for facet number N is listed sequentially\
                     after the facet identifier. If no sclk values meet the distance criteria, then no values are listed, and the next facet identifier follows on the \
                    next line.
                     sclkvalue is the sclk exactly as it appeared in the input file.
                     dist is a real number that describes the distance of the spot from the center of the facet in units of milliradians.
                     pos is the position angle of the center of the instrument boresight intersection on the surface measured in degrees from North, with East being 90 degrees.
                     frac is a real number greater than or equal to 0.0 and less than or equal to 100.0 that describes the fraction of the facet covered by the spot. \
                    If the value is 0.0, then the spot does not cover any portion of the facet. If the value is 100.0 then the facet is entirely covered by the spot.
                     flag is 1 if any portion of the spot does not intersect the surface of Bennu, otherwise it is 0.\s
                     inc is the incidence angle in degrees
                     ems is the emission angle in degrees
                     phs is the phase angle in degrees
                     [otherdata] is all textual data on the line following the sclk value, up to the linefeed, unchanged.
                    """;
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private String spicemetakernel;
  private String objfile;
  private String instrument;
  private String sclkfile;
  private int debugLevel;
  private double maxdist;
  private vtkPolyData polydata;
  private SmallBodyModel smallBodyModel;
  private HashMap<Integer, String> coverageMap;

  private int SC_ID;
  private String SC_ID_String;
  private ReferenceFrame BodyFixed;
  private String TARGET;
  private final Vector3 NORTH = new Vector3(0, 0, 1e6);
  private int instID;

  private PrintStream outputStream;

  public GetSpots(
      String spicemetakernel,
      String objfile,
      String instrument,
      String sclkfile,
      double maxdist,
      int debugLevel) {
    this.spicemetakernel = spicemetakernel;
    this.objfile = objfile;
    this.instrument = instrument;
    this.sclkfile = sclkfile;
    this.maxdist = maxdist;
    this.debugLevel = debugLevel;

    coverageMap = new HashMap<>();

    outputStream = System.out;
  }

  /**
   * Find facets covered by the FOV of the instrument. For each facet, find the distance and
   * position angle of the instrument boresight and the fraction of the facet covered by the FOV.
   *
   * @param line String where the first "word" is an sclk time
   * @throws SpiceException
   */
  private void findCoverage(String line) throws SpiceException {
    String[] parts = line.split(" ");
    if (parts.length == 0) return;

    SCLKTime sclkTime = new SCLKTime(new SCLK(SC_ID), parts[0]);
    TDBTime tdbTime = new TDBTime(sclkTime.getTDBSeconds());

    Instrument instrument = new Instrument(instID);
    FOV fov = new FOV(instrument);
    Matrix33 instrToBodyFixed =
        fov.getReferenceFrame().getPositionTransformation(BodyFixed, tdbTime);
    Vector3 bsightBodyFixed = instrToBodyFixed.mxv(fov.getBoresight());

    StateRecord sr =
        new StateRecord(
            new Body(SC_ID_String),
            tdbTime,
            BodyFixed,
            new AberrationCorrection("LT+S"),
            new Body(TARGET));
    Vector3 scposBodyFixed = sr.getPosition();

    PositionVector sunPos =
        new StateRecord(
                new Body("SUN"),
                tdbTime,
                BodyFixed,
                new AberrationCorrection("LT+S"),
                new Body(TARGET))
            .getPosition();

    double[] double3 = new double[3];
    long cellID =
        smallBodyModel.computeRayIntersection(
            scposBodyFixed.toArray(), bsightBodyFixed.hat().toArray(), double3);
    if (cellID == -1) return; // no boresight intersection

    Vector3 bsightIntersectVector = new Vector3(double3);

    if (debugLevel > 1) {
      LatitudinalCoordinates lc = new LatitudinalCoordinates(bsightIntersectVector);
      System.out.printf(
          "# %s %f %f %s\n",
          sclkTime,
          Math.toDegrees(lc.getLatitude()),
          Math.toDegrees(lc.getLongitude()),
          bsightIntersectVector);
    }

    // flag is 1 if any portion of the spot does not intersect the surface
    int flag = 0;
    Vector<Vector3> boundaryBodyFixed = new Vector<>();
    if (fov.getShape().equals("RECTANGLE") || fov.getShape().equals("POLYGON")) {
      for (Vector3 boundary : fov.getBoundary()) {
        boundaryBodyFixed.add(instrToBodyFixed.mxv(boundary));
      }
    } else if (fov.getShape().equals("CIRCLE")) {
      // bounds contains a single vector parallel to a ray that lies in the cone
      // that makes up the boundary of the FOV
      Vector3[] bounds = fov.getBoundary();

      for (int i = 0; i < 8; i++) {
        // not ideal, but check every 45 degrees along the perimeter of the circle for intersection
        // with the
        // surface
        Matrix33 rotateAlongPerimeter = new Matrix33(fov.getBoresight(), i * Math.toRadians(45));
        Vector3 perimeterVector = rotateAlongPerimeter.mxv(bounds[0]);
        boundaryBodyFixed.add(instrToBodyFixed.mxv(perimeterVector));
      }
    } else {
      // TODO: add ELLIPSE
      System.err.printf(
          "Instrument %s: Unsupported FOV shape %s\n", instrument.getName(), fov.getShape());
      System.exit(0);
    }

    // check all of the boundary vectors for surface intersections
    for (Vector3 vector : boundaryBodyFixed) {
      cellID =
          smallBodyModel.computeRayIntersection(
              scposBodyFixed.toArray(), vector.hat().toArray(), double3);
      if (cellID == -1) {
        flag = 1;
        break;
      }
    }

    vtkIdList idList = new vtkIdList();
    for (int i = 0; i < polydata.GetNumberOfCells(); ++i) {

      polydata.GetCellPoints(i, idList);
      double[] pt0 = polydata.GetPoint(idList.GetId(0));
      double[] pt1 = polydata.GetPoint(idList.GetId(1));
      double[] pt2 = polydata.GetPoint(idList.GetId(2));

      CellInfo ci = CellInfo.getCellInfo(polydata, i, idList);
      Vector3 facetNormal = MathConversions.toVector3(ci.normal());
      Vector3 facetCenter = MathConversions.toVector3(ci.center());

      // check that facet faces the observer
      Vector3 facetToSC = scposBodyFixed.sub(facetCenter);
      double emission = facetToSC.sep(facetNormal);
      if (emission > Math.PI / 2) continue;

      double dist =
          findDist(scposBodyFixed, bsightIntersectVector, facetCenter) * 1e3; // milliradians
      if (dist < maxdist) {

        Vector3 facetToSun = sunPos.sub(facetCenter);
        double incidence = facetToSun.sep(facetNormal);
        double phase = facetToSun.sep(facetToSC);

        Vector3 pt0v = new Vector3(pt0);
        Vector3 pt1v = new Vector3(pt1);
        Vector3 pt2v = new Vector3(pt2);

        Vector3 span1 = pt1v.sub(pt0v);
        Vector3 span2 = pt2v.sub(pt0v);

        Plane facetPlane = new Plane(pt0v, span1, span2);
        Vector3 localNorth = facetPlane.project(NORTH).sub(facetCenter);
        Vector3 bsightIntersectProjection =
            facetPlane.project(bsightIntersectVector).sub(facetCenter);

        // 0 = North, 90 = East
        double pos =
            Math.toDegrees(Math.acos(localNorth.hat().dot(bsightIntersectProjection.hat())));
        if (localNorth.cross(bsightIntersectProjection).dot(facetNormal) > 0) pos = 360 - pos;

        int nCovered = 0;
        if (SPICEUtil.isInFOV(fov, instrToBodyFixed.mtxv(pt0v.sub(scposBodyFixed)))) nCovered++;
        if (SPICEUtil.isInFOV(fov, instrToBodyFixed.mtxv(pt1v.sub(scposBodyFixed)))) nCovered++;
        if (SPICEUtil.isInFOV(fov, instrToBodyFixed.mtxv(pt2v.sub(scposBodyFixed)))) nCovered++;
        double frac;
        if (nCovered == 3) {
          frac = 1;
        } else {
          final double sep012 = span1.negate().sep(pt2v.sub(pt1v)); // angle at vertex 1
          final double sep021 = span2.negate().sep(pt1v.sub(pt2v)); // angle at vertex 2

          // check 0.5*nPts^2 points if they fall in FOV
          int nPts = 50;
          Vector<Vector3> pointsInFacet = new Vector<>();
          for (int ii = 0; ii < nPts; ii++) {
            Vector3 x = pt0v.add(span1.scale(ii / (nPts - 1.)));
            for (int jj = 0; jj < nPts; jj++) {
              Vector3 y = x.add(span2.scale(jj / (nPts - 1.)));

              // if outside the facet, angle 01y will be larger than angle 012
              if (span1.negate().sep(y.sub(pt1v)) > sep012) continue;
              // if outside the facet, angle 02y will be larger than angle 021
              if (span2.negate().sep(y.sub(pt2v)) > sep021) continue;
              pointsInFacet.add(instrToBodyFixed.mtxv(y.sub(scposBodyFixed)));
            }
          }

          if (pointsInFacet.isEmpty()) {
            frac = 0;
          } else {
            List<Boolean> isInFOV = SPICEUtil.isInFOV(fov, pointsInFacet);
            nCovered = 0;
            for (boolean b : isInFOV) if (b) nCovered++;
            frac = ((double) nCovered) / pointsInFacet.size();
          }
        }

        StringBuilder output =
            new StringBuilder(
                String.format(
                    "%s %.4f %5.1f %.1f %d %.1f %.1f %.1f",
                    sclkTime,
                    dist,
                    pos,
                    frac * 100,
                    flag,
                    Math.toDegrees(incidence),
                    Math.toDegrees(emission),
                    Math.toDegrees(phase)));
        for (int j = 1; j < parts.length; j++) output.append(String.format(" %s", parts[j]));
        output.append("\n");
        String coverage = coverageMap.get(i);
        if (coverage == null) {
          coverageMap.put(i, output.toString());
        } else {
          coverage += output;
          coverageMap.put(i, coverage);
        }
      }
    }
  }

  /**
   * Find the angular distance between pt1 and pt2 as seen from scPos. All coordinates are in the
   * body fixed frame.
   *
   * @param scPos Spacecraft position
   * @param pt1 Point 1
   * @param pt2 Point 2
   * @return distance between pt1 and pt2 in radians.
   */
  private double findDist(Vector3 scPos, Vector3 pt1, Vector3 pt2) {
    Vector3 scToPt1 = pt1.sub(scPos).hat();
    Vector3 scToPt2 = pt2.sub(scPos).hat();

    return Math.acos(scToPt1.dot(scToPt2));
  }

  public void printMap() {
    if (debugLevel > 0) {
      for (int i = 0; i < polydata.GetNumberOfCells(); ++i) {
        outputStream.printf("F%d\n", i + 1);
        String output = coverageMap.get(i);
        if (output != null) outputStream.print(coverageMap.get(i));
      }
    } else {
      List<Integer> list = new ArrayList<>(coverageMap.keySet());
      Collections.sort(list);
      for (Integer i : list) {
        outputStream.printf("F%d\n", i + 1);
        outputStream.print(coverageMap.get(i));
      }
    }
    outputStream.println("END");
  }

  public void process() throws Exception {
    boolean useNEAR = false;
    if (instrument.equalsIgnoreCase("OLA_LOW")) {
      // instID = -64400; // ORX_OLA_BASE
      // instID = -64401; // ORX_OLA_ART
      instID = -64403; // ORX_OLA_LOW
    }
    if (instrument.equalsIgnoreCase("OLA_HIGH")) {
      instID = -64402; // ORX_OLA_HIGH
    } else if (instrument.equalsIgnoreCase("OTES")) {
      instID = -64310; // ORX_OTES
    } else if (instrument.equalsIgnoreCase("OVIRS_SCI")) {
      // instID = -64320; // ORX_OVIRS <- no instrument kernel for this
      instID = -64321; // ORX_OVIRS_SCI
      // instID = -64322; // ORX_OVIRS_SUN
    } else if (instrument.equalsIgnoreCase("REXIS")) {
      instID = -64330; // ORX_REXIS
    } else if (instrument.equalsIgnoreCase("REXIS_SXM")) {
      instID = -64340; // ORX_REXIS_SXM
    } else if (instrument.equalsIgnoreCase("POLYCAM")) {
      instID = -64360; // ORX_OCAMS_POLYCAM
    } else if (instrument.equalsIgnoreCase("MAPCAM")) {
      instID = -64361; // ORX_OCAMS_MAPCAM
    } else if (instrument.equalsIgnoreCase("SAMCAM")) {
      instID = -64362; // ORX_OCAMS_SAMCAM
    } else if (instrument.equalsIgnoreCase("NAVCAM")) {
      // instID = -64070; // ORX_NAVCAM <- no frame kernel for this
      // instID = -64081; // ORX_NAVCAM1 <- no instrument kernel for this
      instID = -64082; // ORX_NAVCAM2 <- no instrument kernel for this
    } else if (instrument.equalsIgnoreCase("NIS_RECT")) {
      useNEAR = true;
      //      instID = -93021;
      instID = -93023; // relative to NEAR_NIS_BASE
    } else if (instrument.equalsIgnoreCase("NIS_SQUARE")) {
      useNEAR = true;
      //      instID = -93022;
      instID = -93024; // relative to NEAR_NIS_BASE
    }

    NativeLibraryLoader.loadVtkLibraries();
    polydata = PolyDataUtil.loadShapeModelAndComputeNormals(objfile);
    smallBodyModel = new SmallBodyModel(polydata);

    NativeLibraryLoader.loadSpiceLibraries();
    CSPICE.furnsh(spicemetakernel);

    if (useNEAR) {
      SC_ID = -93;
      SC_ID_String = "-93"; // "NEAR";
      TARGET = "2000433";
      BodyFixed = new ReferenceFrame("IAU_EROS");
    } else {
      SC_ID = -64;
      SC_ID_String = "-64"; // "ORX_SPACECRAFT";
      TARGET = "2101955";
      BodyFixed = new ReferenceFrame("IAU_BENNU");
    }

    List<String> sclkLines = FileUtils.readLines(new File(sclkfile), Charset.defaultCharset());
    boolean foundBegin = false;
    for (String line : sclkLines) {
      String trimLine = line.trim();
      if (trimLine.startsWith("#")) continue;
      if (trimLine.startsWith("BEGIN")) {
        foundBegin = true;
        continue;
      }
      if (foundBegin && !trimLine.startsWith("END")) {
        if (trimLine.startsWith("#")) continue;

        findCoverage(trimLine);
      }
    }
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("spice").required().hasArg().desc("SPICE metakernel").build());
    options.addOption(Option.builder("obj").required().hasArg().desc("Shape file").build());
    options.addOption(
        Option.builder("instype")
            .required()
            .hasArg()
            .desc(
                "one of OLA_LOW, OLA_HIGH, OTES, OVIRS_SCI, REXIS, REXIS_SXM, POLYCAM, MAPCAM, SAMCAM, or NAVCAM")
            .build());
    options.addOption(
        Option.builder("sclk")
            .required()
            .hasArg()
            .desc(
                """
file containing sclk values for instrument observation times.  All values between the strings BEGIN and END will be processed.
       For example:
            BEGIN
            3/605862045.24157
            END""")
            .build());
    options.addOption(
        Option.builder("maxdist")
            .required()
            .hasArg()
            .desc("maximum distance of boresight from facet center in milliradians")
            .build());
    options.addOption(
        Option.builder("all-facets")
            .desc(
                "Optional.  If present, entries for all facets will be output, even if there is no intersection.")
            .build());
    options.addOption(
        Option.builder("verbose")
            .hasArg()
            .desc(
                "Optional.  A level of 1 is equivalent to -all-facets.  A level of 2 or higher will print out the boresight intersection position at each sclk.")
            .build());
    return options;
  }

  public static void main(String[] args) {

    TerrasaurTool defaultOBJ = new GetSpots();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    String spicemetakernel = cl.getOptionValue("spice");
    String objfile = cl.getOptionValue("obj");
    String instrumenttype = cl.getOptionValue("instype");
    String sclkfile = cl.getOptionValue("sclk");
    double distance = Double.parseDouble(cl.getOptionValue("maxdist"));
    int debugLevel = Integer.parseInt(cl.getOptionValue("verbose", "0"));
    if (cl.hasOption("all-facets")) debugLevel = debugLevel == 0 ? 1 : debugLevel + 1;

    GetSpots gs =
        new GetSpots(spicemetakernel, objfile, instrumenttype, sclkfile, distance, debugLevel);
    try {
      gs.process();
      gs.printMap();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }
}
