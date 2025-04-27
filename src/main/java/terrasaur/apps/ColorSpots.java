package terrasaur.apps;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.vectorspace.VectorIJK;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkCell;
import vtk.vtkIdList;
import vtk.vtkObject;
import vtk.vtkPoints;
import vtk.vtkPolyData;

/**
 * Read a file containing (x, y, z, value), or (lat, lon, value) along with an OBJ and write out
 * facet, color
 *
 * @author nairah1
 */
public class ColorSpots implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  private ColorSpots() {}

  @Override
  public String shortDescription() {
    return "Assign values to facets in a shape model from an input dataset.";
  }

  @Override
  public String fullDescription(Options options) {

    String header = "";
    String footer =
        """

                This program reads an OBJ file along with a CSV file containing locations and values and writes the \
                mean value and standard deviation for each facet within a specified distance of an input point to standard \
                out.  Latitude and longitude are specified in degrees.  Longitude is east longitude.  Units of x, y, z, and \
                radius are the same as the units in the supplied OBJ file.
                """;
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private enum FORMAT {
    LL,
    LLR,
    XYZ
  }

  private enum FIELD {
    MIN,
    MAX,
    MEDIAN,
    N,
    RMS,
    SUM,
    STD,
    VARIANCE
  }

  private vtkPolyData polyData;
  private SmallBodyModel smallBodyModel;

  public ColorSpots(vtkPolyData polyData) {
    this.polyData = polyData;
    this.smallBodyModel = new SmallBodyModel(polyData);
  }

  private long getXYZ(double lat, double lon, double[] pt) {
    double[] origin = {0., 0., 0.};
    Vector3D lookDir = new Vector3D(lon, lat);

    return smallBodyModel.computeRayIntersection(origin, lookDir.toArray(), pt);
  }

  private ArrayList<double[]> readCSV(String filename, FORMAT format) {

    ArrayList<double[]> returnArray = new ArrayList<>();

    try (Reader in = new FileReader(filename)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
      for (CSVRecord record : records) {
        double[] values = new double[4];
        values[3] = Double.NaN;
        if (format == FORMAT.LL) {
          double lon = Math.toRadians(Double.parseDouble(record.get(0).trim()));
          double lat = Math.toRadians(Double.parseDouble(record.get(1).trim()));

          if (getXYZ(lat, lon, values) < 0) continue;
          try {
            values[3] = Double.parseDouble(record.get(2));
          } catch (NumberFormatException e) {
            continue;
          }
        } else {
          if (format == FORMAT.LLR) {
            double lon = Math.toRadians(Double.parseDouble(record.get(0).trim()));
            double lat = Math.toRadians(Double.parseDouble(record.get(1).trim()));
            double rad = Double.parseDouble(record.get(2).trim());
            Vector3D xyz = new Vector3D(lon, lat).scalarMultiply(rad);
            values[0] = xyz.getX();
            values[1] = xyz.getY();
            values[2] = xyz.getZ();
          } else if (format == FORMAT.XYZ) {
            values[0] = Double.parseDouble(record.get(0).trim());
            values[1] = Double.parseDouble(record.get(1).trim());
            values[2] = Double.parseDouble(record.get(2).trim());
          }
          smallBodyModel.findClosestCell(values);
          try {
            values[3] = Double.parseDouble(record.get(3).trim());
          } catch (NumberFormatException e) {
            continue;
          }
        }
        returnArray.add(values);
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    return returnArray;
  }

  public TreeMap<Long, DescriptiveStatistics> getStatsFast(
      ArrayList<double[]> valuesList, double radius, boolean weight, boolean atVertices) {
    return atVertices
        ? getStatsVertex(valuesList, radius, weight)
        : getStatsFacet(valuesList, radius, weight);
  }

  private TreeMap<Long, DescriptiveStatistics> getStatsVertex(
      ArrayList<double[]> valuesList, double radius, boolean weight) {

    TreeMap<Long, DescriptiveStatistics> statMap = new TreeMap<>();
    for (long i = 0; i < smallBodyModel.getSmallBodyPolyData().GetNumberOfPoints(); i++) {
      DescriptiveStatistics stats = new DescriptiveStatistics();
      statMap.put(i, stats);
    }
    // double[] xyz = new double[3];

    for (double[] values : valuesList) {
      Vector3D xyz = new Vector3D(values[0], values[1], values[2]);
      double value = values[3];

      vtkIdList pointIDs = new vtkIdList();
      smallBodyModel.getPointLocator().FindPointsWithinRadius(radius, xyz.toArray(), pointIDs);
      // pointIDs.InsertNextId(smallBodyModel.getPointLocator().FindClosestPoint(xyz));

      for (int i = 0; i < pointIDs.GetNumberOfIds(); i++) {
        long pointID = pointIDs.GetId(i);
        DescriptiveStatistics stats = statMap.get(pointID);

        Vector3D p = new Vector3D(smallBodyModel.getSmallBodyPolyData().GetPoint(pointID));
        double dist = p.distance(xyz);

        // cell center can be farther than radius as long as one point is closer than //
        // radius
        if (dist < radius) {
          double thisValue = value;
          if (weight) thisValue *= (1 - dist / radius);
          stats.addValue(thisValue);

          // if (thisValue < 0)
          // System.out.printf("Cell %d dist %f radius %f xyz %f %f %f value %f thisValue %e\n",
          // cellID, dist, radius,
          // xyz[0], xyz[1], xyz[2],
          // value, thisValue);
        }
      } // point loop
    } // values loop

    return statMap;
  }

  private TreeMap<Long, DescriptiveStatistics> getStatsFacet(
      ArrayList<double[]> valuesList, double radius, boolean weight) {

    TreeMap<Long, DescriptiveStatistics> statMap = new TreeMap<>();
    for (long i = 0; i < smallBodyModel.getSmallBodyPolyData().GetNumberOfCells(); i++) {
      DescriptiveStatistics stats = new DescriptiveStatistics();
      statMap.put(i, stats);
    }

    for (double[] values : valuesList) {
      Vector3D xyz = new Vector3D(values[0], values[1], values[2]);
      double value = values[3];

      Set<Long> cellIDs = smallBodyModel.findClosestCellsWithinRadius(xyz.toArray(), radius);
      // cellIDs.add(smallBodyModel.findClosestCell(xyz));

      for (Long cellID : cellIDs) {
        DescriptiveStatistics stats = statMap.get(cellID);

        TriangularFacet tf = PolyDataUtil.getFacet(polyData, cellID);
        Vector3D p = MathConversions.toVector3D(tf.getCenter());
        double dist = p.distance(xyz);

        // cell center can be farther than radius as long as one point is closer than //
        // radius
        if (dist < radius) {
          double thisValue = value;
          if (weight) thisValue *= (1 - dist / radius);
          stats.addValue(thisValue);

          // if (thisValue < 0)
          // System.out.printf("Cell %d dist %f radius %f xyz %f %f %f value %f thisValue %e\n",
          // cellID, dist, radius,
          // xyz[0], xyz[1], xyz[2],
          // value, thisValue);
        }
      } // cell loop
    } // values loop

    return statMap;
  }

  public TreeMap<Integer, DescriptiveStatistics> getStats(
      ArrayList<double[]> valuesList, double radius) {

    // for each value, store indices of closest cells and distances
    TreeMap<Integer, ArrayList<Pair<Long, Double>>> closestCells = new TreeMap<>();
    for (int i = 0; i < valuesList.size(); i++) {
      double[] values = valuesList.get(i);
      Vector3D xyz = new Vector3D(values);

      TreeSet<Long> sortedCellIDs =
          new TreeSet<>(smallBodyModel.findClosestCellsWithinRadius(values, radius));
      sortedCellIDs.add(smallBodyModel.findClosestCell(values));

      ArrayList<Pair<Long, Double>> distances = new ArrayList<>();
      for (long cellID : sortedCellIDs) {
        TriangularFacet tf = PolyDataUtil.getFacet(polyData, cellID);
        Vector3D p = MathConversions.toVector3D(tf.getCenter());
        double dist = p.distance(xyz);
        distances.add(Pair.create(cellID, dist));
      }
      closestCells.put(i, distances);
    }

    TreeMap<Integer, DescriptiveStatistics> statMap = new TreeMap<>();
    for (int cellID = 0; cellID < polyData.GetNumberOfCells(); cellID++) {
      DescriptiveStatistics stats = statMap.get(cellID);
      if (stats == null) {
        stats = new DescriptiveStatistics();
        statMap.put(cellID, stats);
      }

      for (int i = 0; i < valuesList.size(); i++) {
        double[] values = valuesList.get(i);

        ArrayList<Pair<Long, Double>> distances = closestCells.get(i);
        for (Pair<Long, Double> pair : distances) {

          if (pair.getFirst().intValue() < cellID) continue;

          if (pair.getFirst().intValue() > cellID) break;

          if (pair.getFirst().intValue() == cellID) {
            double dist = pair.getSecond();
            if (dist < radius) {
              double thisValue = (1 - dist / radius) * values[3];
              stats.addValue(thisValue);
            }
          }
        }
      }
    } // cell loop

    return statMap;
  }

  public static void main(String[] args) {
    // run the VTK garbage collector every 30 seconds
    vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetScheduleTime(30, TimeUnit.SECONDS);
    vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetAutoGarbageCollection(true);
    // vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetDebug(true);

    try {
      ColorSpotsMain(args);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetAutoGarbageCollection(false);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("additionalFields")
            .hasArg()
            .desc(
                "Specify additional fields to write out. Allowed values are min, max, median, n, rms, sum, std, variance. "
                    + "More than one field may be specified in a comma separated list (e.g. "
                    + "-additionalFields sum,median,rms).  Additional fields will be written out after the mean and std columns.")
            .build());
    options.addOption(
        Option.builder("allFacets")
            .desc(
                "Report values for all facets in OBJ shape model, even if facet is not within searchRadius "
                    + "of any points. Prints NaN if facet not within searchRadius.  Default is to only "
                    + "print facets which have contributions from input points.")
            .build());
    options.addOption(
        Option.builder("info")
            .required()
            .hasArg()
            .desc(
                "Required.  Name of CSV file containing value to plot."
                    + "  Default format is lon, lat, radius, value.  See -xyz and -llOnly options for alternate formats.")
            .build());
    options.addOption(
        Option.builder("llOnly").desc("Format of -info file is lon, lat, value.").build());
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
        Option.builder("normalize")
            .desc(
                "Report values per unit area (divide by total area of facets within search ellipse).")
            .build());
    options.addOption(
        Option.builder("noWeight")
            .desc("Do not weight points by distance from facet/vertex.")
            .build());
    options.addOption(
        Option.builder("obj")
            .required()
            .hasArg()
            .desc("Required.  Name of shape model to read.")
            .build());
    options.addOption(
        Option.builder("outFile")
            .hasArg()
            .desc("Specify output file to store the output.")
            .build());
    options.addOption(
        Option.builder("searchRadius")
            .hasArg()
            .desc(
                "Each facet will be colored using a weighted average of all points within searchRadius of the facet/vertex.  "
                    + "If not present, set to sqrt(2)/2 * mean facet edge length.")
            .build());
    options.addOption(
        Option.builder("writeVertices")
            .desc(
                "Convert output from a per facet to per vertex format. Each line will be of the form"
                    + " x, y, z, value, sigma where x, y, z are the vector components of vertex V. "
                    + " Default is to only report facetID, facet_value, facet_sigma.")
            .build());
    options.addOption(
        Option.builder("xyz").desc("Format of -info file is x, y, z, value.").build());
    return options;
  }

  public static void ColorSpotsMain(String[] args) throws Exception {

    TerrasaurTool defaultOBJ = new ColorSpots();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();

    final boolean writeVerts = cl.hasOption("writeVertices");
    final boolean allFacets = cl.hasOption("allFacets");
    final boolean normalize = cl.hasOption("normalize") && !writeVerts;
    final boolean weight = !cl.hasOption("noWeight");
    FORMAT format = FORMAT.LLR;
    for (Option option : cl.getOptions()) {
      if (option.getOpt().equals("xyz")) {
        format = FORMAT.XYZ;
      }
      if (option.getOpt().equals("llOnly")) {
        format = FORMAT.LL;
      }
    }

    vtkPolyData polyData = PolyDataUtil.loadShapeModelAndComputeNormals(cl.getOptionValue("obj"));

    double radius;
    if (cl.hasOption("searchRadius")) {
      radius = Double.parseDouble(cl.getOptionValue("searchRadius"));
    } else {
      PolyDataStatistics stats = new PolyDataStatistics(polyData);
      radius = stats.getMeanEdgeLength() * Math.sqrt(2) / 2;
      logger.info("Using search radius of " + radius);
    }

    ColorSpots cs = new ColorSpots(polyData);
    ArrayList<double[]> infoValues = cs.readCSV(cl.getOptionValue("info"), format);
    TreeMap<Long, DescriptiveStatistics> statMap =
        cs.getStatsFast(infoValues, radius, weight, writeVerts);

    double totalArea = 0;
    if (normalize) {
      for (int facet = 0; facet < polyData.GetNumberOfCells(); facet++) {
        vtkCell cell = polyData.GetCell(facet);
        vtkPoints points = cell.GetPoints();
        double[] pt0 = points.GetPoint(0);
        double[] pt1 = points.GetPoint(1);
        double[] pt2 = points.GetPoint(2);

        TriangularFacet tf =
            new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
        double area = tf.getArea();

        totalArea += area;
        points.Delete();
        cell.Delete();
      }
    }

    ArrayList<FIELD> fields = new ArrayList<>();
    if (cl.hasOption("additionalFields")) {
      for (String s : cl.getOptionValue("additionalFields").trim().toUpperCase().split(",")) {
        for (FIELD f : FIELD.values()) {
          if (f.name().equalsIgnoreCase(s)) fields.add(f);
        }
      }
    }

    TreeMap<Long, ArrayList<Double>> map = new TreeMap<>();
    long numPoints = (writeVerts ? polyData.GetNumberOfPoints() : polyData.GetNumberOfCells());
    for (long index = 0; index < numPoints; index++) {
      DescriptiveStatistics stats = statMap.get(index);
      ArrayList<Double> values = new ArrayList<>();
      if (stats != null) {
        values.add(stats.getMean());
        values.add(stats.getStandardDeviation());
        for (FIELD f : fields) {
          if (f == FIELD.MIN) values.add(stats.getMin());
          if (f == FIELD.MAX) values.add(stats.getMax());
          if (f == FIELD.MEDIAN) values.add(stats.getPercentile(50));
          if (f == FIELD.N) values.add((double) stats.getN());
          if (f == FIELD.RMS) values.add(Math.sqrt(stats.getSumsq() / stats.getN()));
          if (f == FIELD.STD) values.add(stats.getStandardDeviation());
          if (f == FIELD.SUM) values.add(stats.getSum());
          if (f == FIELD.VARIANCE) values.add(stats.getVariance());
        }
      } else {
        values.add(Double.NaN);
        values.add(Double.NaN);
        for (FIELD f : fields) {
          if (f == FIELD.MIN) values.add(Double.NaN);
          if (f == FIELD.MAX) values.add(Double.NaN);
          if (f == FIELD.MEDIAN) values.add(Double.NaN);
          if (f == FIELD.N) values.add(Double.NaN);
          if (f == FIELD.RMS) values.add(Double.NaN);
          if (f == FIELD.STD) values.add(Double.NaN);
          if (f == FIELD.SUM) values.add(Double.NaN);
          if (f == FIELD.VARIANCE) values.add(Double.NaN);
        }
      }
      map.put(index, values);
    }

    ArrayList<String> returnList;

    if (writeVerts) {
      returnList = writeVertices(map, polyData, allFacets);
    } else {
      returnList = writeFacets(map, allFacets, normalize, totalArea);
    }

    if (cl.hasOption("outFile")) {
      try (PrintWriter pw = new PrintWriter(cl.getOptionValue("outFile"))) {
        for (String s : returnList) pw.println(s);
      }
    } else {
      for (String string : returnList) System.out.println(string);
    }
  }

  private static ArrayList<String> writeFacets(
      TreeMap<Long, ArrayList<Double>> map,
      boolean allFacets,
      boolean normalize,
      double totalArea) {

    ArrayList<String> returnList = new ArrayList<>();

    for (Long facet : map.keySet()) {
      ArrayList<Double> values = map.get(facet);
      Double value = values.get(0);
      Double sigma = values.get(1);
      if (allFacets || !value.isNaN()) {
        if (normalize) {
          value /= totalArea;
          sigma /= totalArea;
        }
        StringBuilder sb = new StringBuilder(String.format("%d, %e, %e", facet, value, sigma));
        for (int i = 2; i < values.size(); i++) {
          value = values.get(i);
          if (normalize) value /= totalArea;
          sb.append(String.format(", %e", value));
        }
        returnList.add(sb.toString());
      }
    }
    return returnList;
  }

  private static ArrayList<String> writeVertices(
      TreeMap<Long, ArrayList<Double>> map, vtkPolyData polyData, boolean allFacets) {

    ArrayList<String> returnList = new ArrayList<>();

    double[] thisPt = new double[3];
    for (Long vertex : map.keySet()) {
      ArrayList<Double> values = map.get(vertex);
      Double value = values.get(0);
      Double sigma = values.get(1);
      if (allFacets || !value.isNaN()) {
        // get vertex x,y,z values
        polyData.GetPoint(vertex, thisPt);
        StringBuilder sb =
            new StringBuilder(
                String.format("%e, %e, %e, %e, %e", thisPt[0], thisPt[1], thisPt[2], value, sigma));
        for (int i = 2; i < values.size(); i++) {
          value = values.get(i);
          sb.append(String.format(", %e", value));
        }
        returnList.add(sb.toString());
      }
    }
    return returnList;
  }
}
