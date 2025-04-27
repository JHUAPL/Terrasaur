package terrasaur.apps;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkOBBTree;
import vtk.vtkOBJReader;
import vtk.vtkPolyData;

public class ValidateNormals implements TerrasaurTool {
  private static final Logger logger = LogManager.getLogger();

  private ValidateNormals() {}

  @Override
  public String shortDescription() {
    return "Check facet normal directions for an OBJ shape file.";
  }

  @Override
  public String fullDescription(Options options) {

    String footer =
        "\nThis program checks that the normals of the shape model are not pointing inward.\n";
    return TerrasaurTool.super.fullDescription(options, "", footer);
  }

  static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("origin")
            .hasArg()
            .desc(
                "If present, center of body in xyz coordinates.  "
                    + "Specify as three floating point values separated by commas.  Default is to use the centroid of "
                    + "the input shape model.")
            .build());
    options.addOption(
        Option.builder("obj").required().hasArg().desc("Shape model to validate.").build());
    options.addOption(
        Option.builder("output")
            .hasArg()
            .desc("Write out new OBJ file with corrected vertex orders for facets.")
            .build());
    options.addOption(
        Option.builder("numThreads")
            .hasArg()
            .desc("Number of threads to run.  Default is 1.")
            .build());
    return options;
  }

  private vtkPolyData polyData;
  private ThreadLocal<vtkOBBTree> threadLocalsearchTree;
  private double[] origin;

  public ValidateNormals(vtkPolyData polyData) {
    this.polyData = polyData;

    PolyDataStatistics stats = new PolyDataStatistics(polyData);
    origin = stats.getCentroid();

    threadLocalsearchTree = new ThreadLocal<>();
  }

  public vtkOBBTree getOBBTree() {
    vtkOBBTree searchTree = threadLocalsearchTree.get();
    if (searchTree == null) {
      searchTree = new vtkOBBTree();
      searchTree.SetDataSet(polyData);
      searchTree.SetTolerance(1e-12);
      searchTree.BuildLocator();
      threadLocalsearchTree.set(searchTree);
    }
    return searchTree;
  }

  public void setOrigin(double[] origin) {
    this.origin = origin;
  }

  private class FlippedNormalFinder implements Callable<List<Long>> {

    private static final DateTimeFormatter defaultFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private final long index0;
    private final long index1;

    public FlippedNormalFinder(long index0, long index1) {
      this.index0 = index0;
      this.index1 = index1;
    }

    @Override
    public List<Long> call() {

      logger.info("Thread {}: indices {} to {}", Thread.currentThread().threadId(), index0, index1);
      vtkIdList idList = new vtkIdList();
      vtkIdList cellIds = new vtkIdList();
      List<Long> flippedNormals = new ArrayList<>();

      final long startTime = Instant.now().getEpochSecond();
      final long numFacets = index1 - index0;
      for (int i = 0; i < numFacets; ++i) {

        if (i > 0 && i % (numFacets / 10) == 0) {
          double pctDone = i / (numFacets * .01);
          long elapsed = Instant.now().getEpochSecond() - startTime;
          long estimatedFinish = (long) (elapsed / (pctDone / 100) + startTime);
          String finish = defaultFormatter.format(Instant.ofEpochSecond(estimatedFinish));
          logger.info(
              String.format(
                  "Thread %d: read %d of %d facets. %.0f%% complete, projected finish %s",
                  Thread.currentThread().threadId(), index0 + i, index1, pctDone, finish));
        }

        long index = index0 + i;

        CellInfo ci = CellInfo.getCellInfo(polyData, index, idList);
        getOBBTree().IntersectWithLine(origin, ci.center().toArray(), null, cellIds);

        // count up all crossings of the surface between the origin and the facet.
        int numCrossings = 0;
        for (int j = 0; j < cellIds.GetNumberOfIds(); j++) {
          if (cellIds.GetId(j) == index) break;
          numCrossings++;
        }

        // if numCrossings is even, the radial and normal should point in the same direction. If it
        // is odd, the
        // radial and normal should point in opposite directions.
        boolean shouldBeOpposite = (numCrossings % 2 == 1);
        boolean isOpposite = (ci.center().dotProduct(ci.normal()) < 0);

        // XOR operator - true if both conditions are different
        if (isOpposite ^ shouldBeOpposite) flippedNormals.add(index);
      }

      return flippedNormals;
    }
  }

  public void flipNormals(Collection<Long> facets) {
    vtkCellArray cells = new vtkCellArray();
    for (long i = 0; i < polyData.GetNumberOfCells(); ++i) {
      vtkIdList idList = new vtkIdList();
      polyData.GetCellPoints(i, idList);
      if (facets.contains(i)) {
        long id0 = idList.GetId(0);
        long id1 = idList.GetId(1);
        long id2 = idList.GetId(2);
        idList.SetId(0, id0);
        idList.SetId(1, id2);
        idList.SetId(2, id1);
      }
      cells.InsertNextCell(idList);
    }
    polyData.SetPolys(cells);
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new ValidateNormals();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();

    // PolyDataUtil's OBJ reader messes with the normals - not reliable for a local obj
    vtkOBJReader smallBodyReader = new vtkOBJReader();
    smallBodyReader.SetFileName(cl.getOptionValue("obj"));
    smallBodyReader.Update();
    vtkPolyData polyData = new vtkPolyData();
    polyData.ShallowCopy(smallBodyReader.GetOutput());

    smallBodyReader.Delete();

    ValidateNormals app = new ValidateNormals(polyData);

    logger.info("Read {} facets from {}", polyData.GetNumberOfCells(), cl.getOptionValue("obj"));

    if (cl.hasOption("origin")) {
      String[] parts = cl.getOptionValue("origin").split(",");
      double[] origin = new double[3];
      for (int i = 0; i < 3; i++) origin[i] = Double.parseDouble(parts[i]);
      app.setOrigin(origin);
    }

    Set<Long> flippedNormals = new HashSet<>();

    int numThreads =
        cl.hasOption("numThreads") ? Integer.parseInt(cl.getOptionValue("numThreads")) : 1;
    try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
      List<Future<List<Long>>> futures = new ArrayList<>();

      long numFacets = polyData.GetNumberOfCells() / numThreads;
      for (int i = 0; i < numThreads; i++) {
        long fromIndex = i * numFacets;
        long toIndex = Math.min(polyData.GetNumberOfCells(), fromIndex + numFacets);

        FlippedNormalFinder fnf = app.new FlippedNormalFinder(fromIndex, toIndex);
        futures.add(executor.submit(fnf));
      }

      for (Future<List<Long>> future : futures) flippedNormals.addAll(future.get());

      executor.shutdown();
    }

    logger.info(
        "Found {} flipped normals out of {} facets",
        flippedNormals.size(),
        polyData.GetNumberOfCells());

    if (cl.hasOption("output")) {
      NavigableSet<Long> sorted = new TreeSet<>(flippedNormals);
      String header = "";
      if (!flippedNormals.isEmpty()) {
        header = "# The following indices were flipped from " + cl.getOptionValue("obj") + ":\n";
        StringBuilder sb = new StringBuilder("# ");
        for (Long index : sorted) {
          sb.append(String.format("%d", index));
          if (index < sorted.last()) sb.append(", ");
        }
        sb.append("\n");
        header += WordUtils.wrap(sb.toString(), 80, "\n# ", false);
        logger.info(header);
      }

      app.flipNormals(flippedNormals);
      PolyDataUtil.saveShapeModelAsOBJ(app.polyData, cl.getOptionValue("output"), header);
      logger.info("wrote OBJ file {}", cl.getOptionValue("output"));
    }

    logger.info("ValidateNormals done");
  }
}
