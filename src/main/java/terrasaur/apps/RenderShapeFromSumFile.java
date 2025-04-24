package terrasaur.apps;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import net.jafama.FastMath;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.DateTime;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import terrasaur.smallBodyModel.LocalModelCollection;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.RotationUtils;
import terrasaur.utils.saaPlotLib.util.StringFunctions;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;

public class RenderShapeFromSumFile implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Render a simulated camera image given a shape model and sumFile.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer = "\nRender a simulated camera image given a shape model and sumFile.\n";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private RenderShapeFromSumFile() {}

  private String globalOBJname;
  private Double scale;
  private Rotation rotation;

  /** Sun position in body fixed coordinates */
  private Vector3D sunXYZ;

  /** Set camera position in body fixed coordinates */
  private Vector3D cameraXYZ;

  private Rotation cameraToBodyFixed;

  /** set instantaneous field of view in radians/pixel */
  private double ifov;

  private int nPixelsX, nPixelsY;
  private double centerX, centerY;
  private int subPixel;

  private ThreadLocal<SmallBodyModel> sbm;
  // key is cell index, value is albedo
  private Map<Long, Double> albedoMap;
  // key is resolution, value is local shape model
  private NavigableMap<Double, LocalModelCollection> lmcMap;

  // key is field name, value is pair of comment and metadata value
  private NavigableMap<String, Map.Entry<String, String>> metadata;

  public RenderShapeFromSumFile(String globalOBJname, Double scale, Rotation rotation) {
    this.globalOBJname = globalOBJname;
    this.scale = scale;
    this.rotation = rotation;

    subPixel = 2;

    sbm = new ThreadLocal<>();
    albedoMap = new HashMap<>();
    lmcMap = new TreeMap<>();

    metadata = new TreeMap<>();
  }

  private void loadAlbedoFile(String albedoFile) {
    try {
      List<String> lines = FileUtils.readLines(new File(albedoFile), Charset.defaultCharset());
      for (String line : lines) {
        String trimLine = line.strip();
        if (trimLine.isEmpty() || trimLine.startsWith("#")) continue;

        String[] parts = trimLine.split(",");
        long index = Long.parseLong(parts[0].trim());
        albedoMap.put(index, Double.parseDouble(parts[1].trim()));
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }

  private SmallBodyModel getGlobalModel() {
    if (sbm.get() == null) {
      try {
        vtkPolyData model = PolyDataUtil.loadShapeModel(globalOBJname);
        if (scale != null || rotation != null) {
          PolyDataStatistics stats = new PolyDataStatistics(model);
          Vector3D center = new Vector3D(stats.getCentroid());

          vtkPoints points = model.GetPoints();
          for (int i = 0; i < points.GetNumberOfPoints(); i++) {
            Vector3D thisPoint = new Vector3D(points.GetPoint(i));
            if (scale != null)
              thisPoint = thisPoint.subtract(center).scalarMultiply(scale).add(center);
            if (rotation != null)
              thisPoint = rotation.applyTo(thisPoint.subtract(center)).add(center);
            points.SetPoint(i, thisPoint.toArray());
          }
        }
        sbm.set(new SmallBodyModel(model));
      } catch (Exception e) {
        logger.error(e.getLocalizedMessage());
      }
    }
    return sbm.get();
  }

  public void addMetaData(String key, String comment, String value) {
    metadata.put(key, new AbstractMap.SimpleEntry<>(comment, value));
  }

  public void setSubPixel(int subPixel) {
    this.subPixel = subPixel;
  }

  /**
   * Return a unit 3D vector in the body fixed frame given pixel coordinates x and y. (0,0) is the
   * upper left corner with X increasing to the right and Y increasing down.
   *
   * @param ix pixel x value
   * @param iy pixel y value
   * @return look direction
   */
  public Vector3D pixelToBodyFixed(double ix, double iy) {

    double[] xyz = new double[3];
    xyz[0] = FastMath.sin(ifov * (ix - centerX));
    xyz[1] = FastMath.sin(ifov * (iy - centerY));
    xyz[2] = FastMath.sqrt(1 - xyz[0] * xyz[0] - xyz[1] * xyz[1]);

    return cameraToBodyFixed.applyTo(new Vector3D(xyz));
  }

  private static class Brightness {
    private final double incidence;
    private final double emission;
    private final double phase;
    private final double brightness;
    private final double range;
    private final double facetX;
    private final double facetY;
    private final double facetZ;
    private final double normalX;
    private final double normalY;
    private final double normalZ;

    private Brightness(
        double incidence,
        double emission,
        double phase,
        double brightness,
        double range,
        Vector3D facet,
        Vector3D normal) {
      this.incidence = Math.toDegrees(incidence);
      this.emission = Math.toDegrees(emission);
      this.phase = Math.toDegrees(phase);
      this.brightness = brightness;
      this.range = range;
      this.facetX = facet.getX();
      this.facetY = facet.getY();
      this.facetZ = facet.getZ();
      this.normalX = normal.getX();
      this.normalY = normal.getY();
      this.normalZ = normal.getZ();
    }

    private double[] values() {
      return new double[] {
        this.brightness,
        this.incidence,
        this.emission,
        this.phase,
        this.range,
        this.facetX,
        this.facetY,
        this.facetZ,
        this.normalX,
        this.normalY,
        this.normalZ
      };
    }
  }

  /**
   * @param pf Photometric function
   * @param intersect cell id of intersection point
   * @param intersectPoint XYZ coordinates of intersection point
   * @param isDefault true if this is the default model, false if local
   * @return Brightness structure
   */
  private Brightness getBrightness(
      PhotometricFunction pf,
      SmallBodyModel sbm,
      long intersect,
      Vector3D intersectPoint,
      boolean isDefault) {

    Vector3D facetToCamera = cameraXYZ.subtract(intersectPoint);

    CellInfo ci = CellInfo.getCellInfo(sbm.getSmallBodyPolyData(), intersect, new vtkIdList());
    Vector3D normal = ci.normal();

    double emission = Vector3D.angle(facetToCamera, normal);
    double distFromCamera = facetToCamera.getNorm();

    // speeds up calculation along the limb. Need to combine all pixels in the ifov.
    double kmPerPixel =
        ifov * distFromCamera / Math.abs(FastMath.cos(Math.min(Math.toRadians(60), emission)));

    double sum = 0;

    Set<Long> cells = sbm.findClosestCellsWithinRadius(intersectPoint.toArray(), kmPerPixel / 2);
    cells.add(intersect);
    double incidence = 0;
    double phase = 0;
    for (long cell : cells) {

      ci = CellInfo.getCellInfo(sbm.getSmallBodyPolyData(), cell, new vtkIdList(), true);
      facetToCamera = cameraXYZ.subtract(ci.center());
      normal = new Vector3D(sbm.getCellNormals().GetTuple3(cell));
      emission = Vector3D.angle(facetToCamera, normal);
      incidence = 0;
      phase = 0;

      if (sunXYZ != null) {
        incidence = Vector3D.angle(sunXYZ, normal);
        phase = Vector3D.angle(facetToCamera, sunXYZ);

        Vector3D sunToFacet = ci.center().subtract(sunXYZ);

        // check for shadowing
        double[] sunIntersectPoint = new double[3];
        long sunIntersect =
            sbm.computeRayIntersection(sunXYZ.toArray(), sunToFacet.toArray(), sunIntersectPoint);
        if (sunIntersect != cell) {
          // don't allow points in shadow to have a 0 value
          sum += .001;
          continue;
        }
      }
      double albedo = (isDefault && albedoMap.containsKey(cell)) ? albedoMap.get(cell) : 1;
      sum +=
          albedo
              * pf.getValue(
                  FastMath.cos(incidence), FastMath.cos(emission), FastMath.toDegrees(phase));
    }

    logger.printf(
        Level.DEBUG,
        "Thread %d lat/lon %.2f/%.2f, %s, sum %f, cells %d, %.2f",
        Thread.currentThread().getId(),
        Math.toDegrees(intersectPoint.getDelta()),
        Math.toDegrees(intersectPoint.getAlpha()),
        intersectPoint.toString(),
        sum,
        cells.size(),
        sum / cells.size());

    return new Brightness(
        incidence, emission, phase, sum / cells.size(), distFromCamera, facetToCamera, normal);
  }

  class BrightnessCalculator implements Callable<Map<Integer, Brightness>> {

    Collection<Integer> pixelIndices;
    PhotometricFunction pf;

    private BrightnessCalculator(Collection<Integer> pixelIndices, PhotometricFunction pf) {
      this.pixelIndices = pixelIndices;
      this.pf = pf;
    }

    @Override
    public Map<Integer, Brightness> call() throws Exception {

      logger.info("Thread {}: starting", Thread.currentThread().getId());

      int xPixels = subPixel * nPixelsX;

      SmallBodyModel globalModel = getGlobalModel();

      Map<Integer, Brightness> brightness = new HashMap<>();
      double[] intersectPoint = new double[3];
      double[] cameraXYZArray = cameraXYZ.toArray();
      for (Integer index : pixelIndices) {
        int j = index / xPixels;
        int i = index % xPixels;
        Vector3D pixelDir = pixelToBodyFixed(((double) i) / subPixel, ((double) j) / subPixel);

        long intersect =
            globalModel.computeRayIntersection(cameraXYZArray, pixelDir.toArray(), intersectPoint);

        if (intersect > -1) {

          Vector3D intersectPt3D = new Vector3D(intersectPoint);

          // resolution in m/pixel
          double resolution = ifov * intersectPt3D.distance(cameraXYZ) * 1e3;

          // if no ceiling entry exists, stick with the global model
          Entry<Double, LocalModelCollection> lmcEntry = lmcMap.ceilingEntry(resolution);
          if (lmcEntry != null) {

            LocalModelCollection lmc = lmcEntry.getValue();
            double[] localIntersectPoint = new double[3];

            SmallBodyModel localModel = lmc.get(intersectPt3D);
            if (localModel != null) {
              long localIntersect =
                  localModel.computeRayIntersection(
                      cameraXYZArray, pixelDir.toArray(), localIntersectPoint);
              if (localIntersect != -1) {
                break;
              } else {
                logger.debug(
                    String.format(
                        "Thread %d: No intersection with local model for pixel (%d,%d): lat/lon %.2f/%.2f, using global intersection %d %s",
                        Thread.currentThread().getId(),
                        i,
                        j,
                        Math.toDegrees(intersectPt3D.getDelta()),
                        Math.toDegrees(intersectPt3D.getAlpha()),
                        intersect,
                        intersectPt3D));
              }
            }
          }

          boolean isDefault = lmcEntry == null;
          Brightness b = getBrightness(pf, globalModel, intersect, intersectPt3D, isDefault);
          brightness.put(j * xPixels + i, b);
        }
      }

      logger.info("Thread {}: finished", Thread.currentThread().getId());

      return brightness;
    }
  }

  public double[][][] getFits(PhotometricFunction pf, int numThreads) {

    int xPixels = subPixel * nPixelsX;
    int yPixels = subPixel * nPixelsY;

    Map<Integer, Brightness> brightness = new HashMap<>();
    try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {

      List<Integer> indices = IntStream.range(0, yPixels * xPixels).boxed().toList();

      int numPixels = indices.size();

      Set<BrightnessCalculator> callables = new HashSet<>();
      for (int i = 0; i < numThreads; i++) {
        int fromIndex = i * numPixels / numThreads;
        int toIndex = Math.min(numPixels, fromIndex + numPixels / numThreads);
        callables.add(new BrightnessCalculator(indices.subList(fromIndex, toIndex), pf));
      }

      Set<Future<Map<Integer, Brightness>>> futures = new HashSet<>();
      for (BrightnessCalculator callable : callables) futures.add(executor.submit(callable));

      for (Future<Map<Integer, Brightness>> future : futures) {
        try {
          Map<Integer, Brightness> values = future.get();
          brightness.putAll(values);
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
        }
      }
      executor.shutdown();
    }

    double[][][] img = new double[11][yPixels][xPixels];
    for (int i = 0; i < xPixels; i++) {
      for (int j = 0; j < yPixels; j++) {
        Brightness pixel = brightness.get(j * xPixels + i);
        if (pixel == null) continue;
        double[] pixels = pixel.values();
        for (int k = 0; k < img.length; k++) {
          img[k][yPixels - j - 1][i] = pixels[k];
        }
      }
    }
    return img;
  }

  public BufferedImage getImage(PhotometricFunction pf, int numThreads) {

    int xPixels = subPixel * nPixelsX;
    int yPixels = subPixel * nPixelsY;
    BufferedImage image = new BufferedImage(xPixels, yPixels, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
    g.setComposite(AlphaComposite.Src);
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());

    Map<Integer, Brightness> brightness = new HashMap<>();
    double maxBrightness = -Double.MAX_VALUE;

    try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {

      List<Integer> indices = IntStream.range(0, yPixels * xPixels).boxed().toList();

      int numPixels = indices.size();

      Set<BrightnessCalculator> callables = new HashSet<>();
      for (int i = 0; i < numThreads; i++) {
        int fromIndex = i * numPixels / numThreads;
        int toIndex = Math.min(numPixels, fromIndex + numPixels / numThreads);
        callables.add(new BrightnessCalculator(indices.subList(fromIndex, toIndex), pf));
      }

      Set<Future<Map<Integer, Brightness>>> futures = new HashSet<>();
      for (BrightnessCalculator callable : callables) futures.add(executor.submit(callable));

      for (Future<Map<Integer, Brightness>> future : futures) {
        try {
          Map<Integer, Brightness> brightnessMap = future.get();
          for (Brightness b : brightnessMap.values())
            if (maxBrightness < b.brightness) maxBrightness = b.brightness;
          brightness.putAll(future.get());
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
        }
      }
      executor.shutdown();
    }

    /*-
    double[] intersectPoint = new double[3];

    Map<Integer, Double> brightness = new HashMap<>();

    double[] cameraXYZArray = cameraXYZ.toArray();
    for (int i = 0; i < xPixels; i++) {
      for (int j = 0; j < yPixels; j++) {
        Vector3D pixelDir = pixelToBodyFixed(((double) i) / scale, ((double) j) / scale);

        int intersect =
            sbm.computeRayIntersection(cameraXYZArray, pixelDir.toArray(), intersectPoint);
        if (intersect > 0) {
          brightness.put(i * xPixels + j,
              getBrightness(pf, intersect, new Vector3D(intersectPoint)));
        }
      }
    }
    */
    if (brightness.isEmpty()) {
      logger.info("No intersections with shape model found!");
    } else {
      for (int j = 0; j < yPixels; j++) {
        for (int i = 0; i < xPixels; i++) {
          if (!brightness.containsKey(j * xPixels + i)) continue;
          double value = brightness.get(j * xPixels + i).brightness;

          int grey = value < 0.01 ? 1 : (int) (255 * value / maxBrightness);

          image.setRGB(i, j, new Color(grey, grey, grey).getRGB());
        }
      }
    }

    BufferedImage img = new BufferedImage(nPixelsX, nPixelsY, BufferedImage.TYPE_INT_RGB);
    img.createGraphics()
        .drawImage(image.getScaledInstance(nPixelsX, nPixelsY, Image.SCALE_SMOOTH), 0, 0, null);

    return img;
  }

  public SumFile loadSumFile(String filename) {

    SumFile sumFile = SumFile.fromFile(new File(filename));

    addMetaData("image.utc", "Imaging date.  Taken from sumfile", sumFile.utcString());

    nPixelsX = sumFile.imageWidth();
    nPixelsY = sumFile.imageHeight();

    centerX = 0.5 * (nPixelsX - 1);
    centerY = 0.5 * (nPixelsY - 1);

    // let's assume square pixels
    ifov = sumFile.horizontalResolution();

    // put the sun far away
    sunXYZ = sumFile.sunDirection().scalarMultiply(1e8);
    cameraXYZ = sumFile.scobj().negate();

    cameraToBodyFixed = sumFile.getBodyFixedToCamera().revert();

    double[] intersectPoint = new double[3];
    Vector3D boresight = sumFile.boresight();
    long intersect =
        getGlobalModel()
            .computeRayIntersection(cameraXYZ.toArray(), boresight.toArray(), intersectPoint);
    if (intersect > 0) {
      Vector3D nadirPt = new Vector3D(intersectPoint);
      double lat = nadirPt.getDelta();
      double lon = nadirPt.getAlpha();
      Vector3D normal = new Vector3D(getGlobalModel().getCellNormals().GetTuple3(intersect));
      double inc = Vector3D.angle(sunXYZ, normal);
      Vector3D toCamera = cameraXYZ.subtract(nadirPt);
      double ems = Vector3D.angle(toCamera, normal);
      double phs = Vector3D.angle(sunXYZ, toCamera);
      double range = cameraXYZ.subtract(nadirPt).getNorm();

      addMetaData("image.cell", "Index of center pixel cell", Long.toString(intersect));
      addMetaData("image.lat", "Center latitude", StringFunctions.toDegreesLat("%.2f ").apply(lat));
      addMetaData(
          "image.lon", "Center longitude", StringFunctions.toDegreesELon("%.2f ").apply(lon));
      addMetaData(
          "image.inc", "Center incidence in degrees", String.format("%.2f", Math.toDegrees(inc)));
      addMetaData(
          "image.ems",
          "Center emission in degrees (may not be zero if facet is tilted)",
          String.format("%.2f", Math.toDegrees(ems)));
      addMetaData(
          "image.phs", "Center phase in degrees", String.format("%.2f", Math.toDegrees(phs)));
      addMetaData("image.range", "Center point range in m", String.format("%.3f", range * 1e3));
      addMetaData(
          "image.resolution",
          "Center point resolution in m/pixel",
          String.format("%.3f", ifov * range * 1e3));
    }
    return sumFile;
  }

  /**
   * load a local model file
   *
   * @param lmcName local model filename
   */
  public void loadLocalModels(String lmcName) {
    LocalModelCollection lmc = new LocalModelCollection(128, scale, rotation);
    try {
      List<String> lines = FileUtils.readLines(new File(lmcName), Charset.defaultCharset());

      Double localResolution = null;

      for (String line : lines) {
        String strippedLine = line.strip();
        if (strippedLine.isEmpty() || strippedLine.startsWith("#")) continue;
        String[] parts = strippedLine.split(",");

        if (localResolution == null) {
          localResolution = Double.valueOf(parts[0].strip());
          logger.debug("Loading {} with a resolution of {} m/pixel", lmcName, localResolution);
        } else {
          double lat = Math.toRadians(Double.parseDouble(parts[0]));
          double lon = Math.toRadians(Double.parseDouble(parts[1]));
          String filename = parts[2].strip();
          lmc.addModel(lat, lon, filename);
        }
      }
      this.lmcMap.put(localResolution, lmc);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage());
    }
  }

  /**
   * Write a metadata file containing information about the simulated image.
   *
   * @param metadataFile file to write
   * @param arguments command line arguments
   */
  public void writeMetadata(File metadataFile, String arguments) {
    try (PrintWriter pw = new PrintWriter(metadataFile)) {

      pw.printf("# Created %s by %s\n", Instant.now().toString(), AppVersion.getVersionString());
      pw.printf("# arguments: %s\n\n", arguments);

      String lastSection = null;
      for (String key : metadata.keySet()) {
        String thisSection = key.substring(0, key.indexOf("."));
        if (lastSection == null) lastSection = thisSection;
        if (!lastSection.equals(thisSection)) {
          pw.println();
          lastSection = thisSection;
        }
        Map.Entry<String, String> value = metadata.get(key);
        String[] comments = value.getKey().split("\\r?\\n");
        for (String comment : comments) if (!comment.trim().isEmpty()) pw.printf("# %s\n", comment);
        pw.printf("%s = %s\n", key, value.getValue());
      }

    } catch (FileNotFoundException e) {
      logger.log(Level.ERROR, e.getLocalizedMessage(), e);
    }
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("albedoFile")
            .hasArg()
            .desc(
                """
                    Name of albedo file.  This is a CSV file with facet
                    index in the first column and albedo (in the range 0
                    to 1) in the second.  Additional columns are
                    ignored.  Albedo for facets not specified will be
                    set to 1.  Lines starting with # or blank
                    lines are ignored.  This file applies only to the
                    default shape model, not any local ones."""
                    .replaceAll("\\s+", " ")
                    .strip())
            .build());
    options.addOption(
        Option.builder("localModels")
            .hasArgs()
            .desc(
                """
                    File containing local shape models, one per line.
                    The first line of the file should contain the
                    coarsest resolution in m/pixel where these models
                    should be used.  Usually this can be about half the
                    resolution of the next coarser model.  For example,
                    if the global model has a resolution of 1 m/pixel
                    the local model file should be used for resolutions
                    better than 0.5 m/pixel.  Format of each remaining
                    line is lat, lon, filename as comma separated
                    values.  Lat and lon are in degrees.  This option
                    may be specified multiple times to load multiple
                    sets of models.  Lines starting with # or blank
                    lines are ignored."""
                    .replaceAll("\\s+", " ")
                    .strip())
            .build());
    options.addOption(
        Option.builder("logFile")
            .hasArg()
            .desc("If present, save screen output to log file.")
            .build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values()) sb.append(String.format("%s ", l.name()));
    options.addOption(
        Option.builder("logLevel")
            .hasArg()
            .desc(
                "If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim()
                    + ".  Default is INFO.")
            .build());
    options.addOption(
        Option.builder("model")
            .required()
            .hasArg()
            .desc(
                "Required.  Default shape model filename.  Supported formats are OBJ, PLT, PLY, STL, or VTK format.")
            .build());
    options.addOption(
        Option.builder("numThreads")
            .hasArg()
            .desc("Number of threads to run in parallel when generating the image.  Default is 2.")
            .build());
    options.addOption(
        Option.builder("photo")
            .hasArg()
            .desc(PhotometricFunction.getOptionString().trim() + "  Default is OREX.")
            .build());
    options.addOption(
        Option.builder("output")
            .required()
            .hasArg()
            .desc("Required.  Name of image file to write.  Valid extensions are fits or png.")
            .build());
    options.addOption(
        Option.builder("rotateModel")
            .hasArg()
            .desc(
                """
                    If present, rotate shape model.  Specify by an angle
                    (degrees) and a 3 element rotation axis vector (XYZ)
                    separated by commas.
                    """
                    .replaceAll("\\s+", " ")
                    .strip())
            .build());
    options.addOption(
        Option.builder("scaleModel")
            .hasArg()
            .desc("If present, factor to scale shape model.  The center is unchanged.")
            .build());
    options.addOption(
        Option.builder("subPixel")
            .hasArg()
            .desc(
                """
                    Generate the simulated image a factor of subPixel
                    (must be an integer) larger than the dimensions in
                    the sum file.  The simulated image is them reduced
                    in size to the dimensions in the sum file.  The
                    default is 2.
                    """
                    .replaceAll("\\s+", " ")
                    .strip())
            .build());
    options.addOption(
        Option.builder("sumFile")
            .required()
            .hasArg()
            .desc("Required.  Name of sum file to read.")
            .build());
    return options;
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new RenderShapeFromSumFile();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();

    Double scale =
        cl.hasOption("scaleModel") ? Double.parseDouble(cl.getOptionValue("scaleModel")) : null;
    Rotation rotation =
        cl.hasOption("rotateModel")
            ? RotationUtils.stringToRotation(cl.getOptionValue("rotateModel"))
            : null;

    RenderShapeFromSumFile app =
        new RenderShapeFromSumFile(cl.getOptionValue("model"), scale, rotation);
    SumFile sumFile = app.loadSumFile(cl.getOptionValue("sumFile"));

    if (cl.hasOption("albedoFile")) app.loadAlbedoFile(cl.getOptionValue("albedoFile"));

    if (cl.hasOption("localModels"))
      for (String localModel : cl.getOptionValues("localModels")) app.loadLocalModels(localModel);

    if (cl.hasOption("subPixel")) app.setSubPixel(Integer.parseInt(cl.getOptionValue("subPixel")));

    PhotometricFunction pf = PhotometricFunction.OREX1;
    if (cl.hasOption("photo"))
      pf = PhotometricFunction.getPhotometricFunction(cl.getOptionValue("photo"));
    int numThreads =
        cl.hasOption("numThreads") ? Integer.parseInt(cl.getOptionValue("numThreads")) : 2;

    String outputFilename = cl.getOptionValue("output");
    String dirname = FilenameUtils.getPath(outputFilename);
    if (dirname.trim().isEmpty()) dirname = ".";
    String basename = FilenameUtils.getBaseName(outputFilename);
    String extension = FilenameUtils.getExtension(outputFilename);

    if (extension.equalsIgnoreCase("png")) {
      BufferedImage image = app.getImage(pf, numThreads);
      File png = new File(dirname, basename + "." + extension);
      File metadataFile = new File(dirname, basename + ".txt");

      ImageIO.write(image, "PNG", png);
      app.writeMetadata(metadataFile, startupMessages.get(MessageLabel.ARGUMENTS));
      logger.info("Wrote {}", outputFilename);
    } else if (extension.equalsIgnoreCase("fits")) {
      Fits fits = new Fits();
      ImageHDU imageHDU = (ImageHDU) Fits.makeHDU(app.getFits(pf, numThreads));
      Header header = imageHDU.getHeader();
      header.addValue(
          DateTime.TIMESYS_UTC, app.metadata.get("image.utc").getValue(), "Time from the SUM file");
      header.addValue("TITLE", sumFile.picnm(), "Title of SUM file");
      header.addValue("PLANE1", "brightness", "from 0 to 1");
      header.addValue("PLANE2", "incidence", "degrees");
      header.addValue("PLANE3", "emission", "degrees");
      header.addValue("PLANE4", "phase", "degrees");
      header.addValue("PLANE5", "range", "kilometers");
      header.addValue("PLANE6", "facetX", "kilometers");
      header.addValue("PLANE7", "facetY", "kilometers");
      header.addValue("PLANE8", "facetZ", "kilometers");
      header.addValue("PLANE9", "normalX", "X component of unit normal");
      header.addValue("PLANE10", "normalY", "Y component of unit normal");
      header.addValue("PLANE11", "normalZ", "Z component of unit normal");
      header.addValue("MMFL", sumFile.mmfl(), "From SUM file");
      header.addValue("SCOBJ", sumFile.scobj().toString(), "From SUM file");
      header.addValue("CX", sumFile.cx().toString(), "From SUM file");
      header.addValue("CY", sumFile.cy().toString(), "From SUM file");
      header.addValue("CZ", sumFile.cz().toString(), "From SUM file");
      header.addValue("SZ", sumFile.sz().toString(), "From SUM file");
      header.addValue("KMAT1", sumFile.kmat1().toString(), "From SUM file");
      header.addValue("KMAT2", sumFile.kmat2().toString(), "From SUM file");
      header.addValue("DIST", sumFile.distortion().toString(), "From SUM file");
      header.addValue("SIGVSO", sumFile.sig_vso().toString(), "From SUM file");
      header.addValue("SIGPTG", sumFile.sig_ptg().toString(), "From SUM file");
      fits.addHDU(imageHDU);
      fits.write(outputFilename);
      fits.close();
      logger.info("wrote {}", outputFilename);
    } else {
      logger.error("Unsupported output file type: {}", outputFilename);
    }
  }
}
