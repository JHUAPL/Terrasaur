package terrasaur.apps;

import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.vectorspace.VectorIJK;
import terrasaur.smallBodyModel.SmallBodyModel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.mesh.TriangularFacet;
import vtk.vtkIdList;
import vtk.vtkPolyData;

/**
 * Check an OBJ file for the correct number of facets and vertices. Test for duplicate vertices,
 * unreferenced vertices, and zero area facets.
 *
 * <p>Even though this is named "ValidateOBJ" it will work with any format that can be read by
 * {@link PolyDataUtil#loadShapeModel(String)}.
 *
 * @author Hari.Nair@jhuapl.edu
 */
public class ValidateOBJ implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Check a closed shape file in OBJ format for errors.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer =
        """
                    This program checks that a shape model has the correct number of facets and vertices.  \
                    It will also check for duplicate vertices, vertices that are not referenced by any facet, and zero area facets.
                    """;
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private vtkPolyData polyData;
  private String validationMsg;

  private ValidateOBJ() {}

  public ValidateOBJ(vtkPolyData polyData) {
    this.polyData = polyData;
  }

  /**
   * @return {@link vtkPolyData#GetNumberOfCells()}
   */
  public long facetCount() {
    return polyData.GetNumberOfCells();
  }

  /**
   * @return {@link vtkPolyData#GetNumberOfPoints()}
   */
  public long vertexCount() {
    return polyData.GetNumberOfPoints();
  }

  /**
   * @return description of test result
   */
  public String getMessage() {
    return validationMsg;
  }

  /**
   * @return true if number of facets in the shape model satisfies 3*4^n where n is an integer
   */
  public boolean testFacets() {
    boolean meetsCondition = facetCount() % 3 == 0;

    if (meetsCondition) {
      long facet3 = facetCount() / 3;
      double logFacet3 = Math.log(facet3) / Math.log(4);
      if (Math.ceil(logFacet3) != Math.floor(logFacet3)) meetsCondition = false;
    }

    int n = (int) (Math.log(facetCount() / 3.) / Math.log(4.0) + 0.5);
    if (meetsCondition) {
      validationMsg =
          String.format(
              "Model has %d facets.  This satisfies f = 3*4^n with n = %d.", facetCount(), n);
    } else {
      validationMsg =
          String.format(
              "Model has %d facets.  This does not satisfy f = 3*4^n.  A shape model with %.0f facets has n = %d.",
              facetCount(), 3 * Math.pow(4, n), n);
    }

    return meetsCondition;
  }

  /**
   * @return true if number of vertices in the shape model satisfies v=f/2+2
   */
  public boolean testVertices() {
    boolean meetsCondition = (facetCount() + 4) / 2 == vertexCount();
    if (meetsCondition)
      validationMsg =
          String.format(
              "Model has %d vertices and %d facets.  This satisfies v = f/2+2.",
              vertexCount(), facetCount());
    else
      validationMsg =
          String.format(
              "Model has %d vertices and %d facets.  This does not satisfy v = f/2+2.  Number of vertices should be %d.",
              vertexCount(), facetCount(), facetCount() / 2 + 2);

    return meetsCondition;
  }

  /**
   * @return key is vertex id, value is a list of vertices within a hard coded distance of 1e-10.
   */
  public NavigableMap<Long, List<Long>> findDuplicateVertices() {

    SmallBodyModel sbm = new SmallBodyModel(polyData);

    double[] iPt = new double[3];

    NavigableMap<Long, List<Long>> map = new TreeMap<>();
    double tol = 1e-10;
    for (long i = 0; i < vertexCount(); i++) {
      polyData.GetPoint(i, iPt);

      List<Long> closestVertices = new ArrayList<>();
      for (Long id : sbm.findClosestVerticesWithinRadius(iPt, tol))
        if (id > i) closestVertices.add(id);
      if (!closestVertices.isEmpty()) map.put(i, closestVertices);

      if (map.containsKey(i) && !map.get(i).isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Duplicates for vertex %d: ", i + 1));
        for (Long dupId : map.get(i)) sb.append(String.format("%d ", dupId + 1));
        logger.debug(sb.toString());
      }
    }

    validationMsg = String.format("%d vertices have duplicates", map.size());

    return map;
  }

  /**
   * @return a list of vertex indices where one or more of the coordinates fail {@link
   *     Double#isFinite(double)}.
   */
  public List<Integer> findMalformedVertices() {
    double[] iPt = new double[3];
    NavigableSet<Integer> vertexIndices = new TreeSet<>();
    for (int i = 0; i < vertexCount(); i++) {
      polyData.GetPoint(i, iPt);
      for (int j = 0; j < 3; j++) {
        if (!Double.isFinite(iPt[j])) {
          logger.debug("Vertex {}: {} {} {}", i, iPt[0], iPt[1], iPt[2]);
          vertexIndices.add(i);
          break;
        }
      }
    }
    validationMsg = String.format("%d malformed vertices ", vertexIndices.size());
    return new ArrayList<>(vertexIndices);
  }

  /**
   * @return a list of vertex indices that are not referenced by any facet
   */
  public List<Long> findUnreferencedVertices() {
    NavigableSet<Long> vertexIndices = new TreeSet<>();
    for (long i = 0; i < polyData.GetNumberOfPoints(); i++) {
      vertexIndices.add(i);
    }

    vtkIdList idList = new vtkIdList();
    for (int i = 0; i < facetCount(); ++i) {
      polyData.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);

      vertexIndices.remove(id0);
      vertexIndices.remove(id1);
      vertexIndices.remove(id2);
    }

    if (!vertexIndices.isEmpty()) {
      double[] pt = new double[3];
      for (long id : vertexIndices) {
        polyData.GetPoint(id, pt);
        logger.debug("Unreferenced vertex {} [{}, {}, {}]", id + 1, pt[0], pt[1], pt[2]);
        // note OBJ vertices are numbered from 1 but VTK uses 0
      }
    }

    validationMsg = String.format("%d unreferenced vertices found", vertexIndices.size());

    return new ArrayList<>(vertexIndices);
  }

  /**
   * @return a list of facet indices where the facet has zero area
   */
  public List<Integer> findZeroAreaFacets() {
    List<Integer> zeroAreaFacets = new ArrayList<>();
    vtkIdList idList = new vtkIdList();
    double[] pt0 = new double[3];
    double[] pt1 = new double[3];
    double[] pt2 = new double[3];

    for (int i = 0; i < facetCount(); ++i) {
      polyData.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      polyData.GetPoint(id0, pt0);
      polyData.GetPoint(id1, pt1);
      polyData.GetPoint(id2, pt2);

      // would be faster to check if id0==id1||id0==id2||id1==id2 but there may be
      // duplicate vertices
      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
      double area = facet.getArea();
      if (area == 0) {
        zeroAreaFacets.add(i);
        logger.debug(
            "Facet {} has zero area.  Vertices are {} [{}, {}, {}], {} [{}, {}, {}],  and {} [{}, {}, {}]",
            i + 1,
            id0 + 1,
            pt0[0],
            pt0[1],
            pt0[2],
            id1 + 1,
            pt1[0],
            pt1[1],
            pt1[2],
            id2 + 1,
            pt2[0],
            pt2[1],
            pt2[2]);
      }
    }

    validationMsg = String.format("%d zero area facets found", zeroAreaFacets.size());
    return zeroAreaFacets;
  }

  /**
   * @return statistics on the angle between the facet radial and normal vectors
   */
  public DescriptiveStatistics normalStats() {
    DescriptiveStatistics stats = new DescriptiveStatistics();

    VectorStatistics cStats = new VectorStatistics();
    VectorStatistics nStats = new VectorStatistics();

    vtkIdList idList = new vtkIdList();
    double[] pt0 = new double[3];
    double[] pt1 = new double[3];
    double[] pt2 = new double[3];
    for (int i = 0; i < facetCount(); ++i) {
      polyData.GetCellPoints(i, idList);
      long id0 = idList.GetId(0);
      long id1 = idList.GetId(1);
      long id2 = idList.GetId(2);
      polyData.GetPoint(id0, pt0);
      polyData.GetPoint(id1, pt1);
      polyData.GetPoint(id2, pt2);

      // would be faster to check if id0==id1||id0==id2||id1==id2 but there may be
      // duplicate vertices
      TriangularFacet facet =
          new TriangularFacet(new VectorIJK(pt0), new VectorIJK(pt1), new VectorIJK(pt2));
      if (facet.getArea() > 0) {
        stats.addValue(facet.getCenter().getDot(facet.getNormal()));
        cStats.add(facet.getCenter());
        nStats.add(facet.getNormal());
      }
    }

    validationMsg =
        String.format(
            "Using %d non-zero area facets: Mean angle between radial and normal is %f degrees, "
                + "angle between mean radial and mean normal is %f degrees",
            stats.getN(),
            Math.toDegrees(Math.acos(stats.getMean())),
            Math.toDegrees(Vector3D.angle(cStats.getMean(), nStats.getMean())));

    return stats;
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("obj").required().hasArg().desc("Shape model to validate.").build());
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
    options.addOption(Option.builder("output").hasArg().desc("Filename for output OBJ.").build());
    options.addOption(
        Option.builder("removeDuplicateVertices")
            .desc("Remove duplicate vertices.  Use with -output to save OBJ.")
            .build());
    options.addOption(
        Option.builder("removeUnreferencedVertices")
            .desc("Remove unreferenced vertices.  Use with -output to save OBJ.")
            .build());
    options.addOption(
        Option.builder("removeZeroAreaFacets")
            .desc("Remove facets with zero area.  Use with -output to save OBJ.")
            .build());
    options.addOption(
        Option.builder("cleanup")
            .desc(
                "Combines -removeDuplicateVertices, -removeUnreferencedVertices, and -removeZeroAreaFacets.")
            .build());
    return options;
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new ValidateOBJ();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();
    vtkPolyData polyData = PolyDataUtil.loadShapeModel(cl.getOptionValue("obj"));

    if (polyData == null) {
      logger.error("Cannot read {}, exiting.", cl.getOptionValue("obj"));
      System.exit(0);
    }

    ValidateOBJ vo = new ValidateOBJ(polyData);

    logger.log(vo.testFacets() ? Level.INFO : Level.WARN, vo.getMessage());
    logger.log(vo.testVertices() ? Level.INFO : Level.WARN, vo.getMessage());

    DescriptiveStatistics stats = vo.normalStats();
    logger.log(stats.getMean() > 0 ? Level.INFO : Level.WARN, vo.getMessage());

    List<Integer> mfv = vo.findMalformedVertices();
    logger.log(!mfv.isEmpty() ? Level.WARN : Level.INFO, vo.getMessage());

    NavigableMap<Long, List<Long>> dv = vo.findDuplicateVertices();
    logger.log(!dv.isEmpty() ? Level.WARN : Level.INFO, vo.getMessage());

    List<Long> urv = vo.findUnreferencedVertices();
    logger.log(!urv.isEmpty() ? Level.WARN : Level.INFO, vo.getMessage());

    List<Integer> zaf = vo.findZeroAreaFacets();
    logger.log(!zaf.isEmpty() ? Level.WARN : Level.INFO, vo.getMessage());

    final boolean cleanup = cl.hasOption("cleanup");
    final boolean removeDuplicateVertices = cleanup || cl.hasOption("removeDuplicateVertices");
    final boolean removeUnreferencedVertices =
        cleanup || cl.hasOption("removeUnreferencedVertices");
    final boolean removeZeroAreaFacets = cleanup || cl.hasOption("removeZeroAreaFacets");

    if (removeDuplicateVertices) polyData = PolyDataUtil.removeDuplicatePoints(polyData);

    if (removeUnreferencedVertices) polyData = PolyDataUtil.removeUnreferencedPoints(polyData);

    if (removeZeroAreaFacets) polyData = PolyDataUtil.removeZeroAreaFacets(polyData);

    if (cl.hasOption("output")) {
      PolyDataUtil.saveShapeModelAsOBJ(polyData, cl.getOptionValue("output"));
      logger.info(String.format("Wrote OBJ file %s", cl.getOptionValue("output")));
    }
  }
}
