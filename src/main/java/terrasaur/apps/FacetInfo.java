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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.CellInfo;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataStatistics;
import terrasaur.utils.PolyDataUtil;
import vtk.vtkIdList;
import vtk.vtkOBBTree;
import vtk.vtkPolyData;

public class FacetInfo implements TerrasaurTool {

    private static final Logger logger = LogManager.getLogger();

    /**
     * This doesn't need to be private, or even declared, but you might want to if you have other
     * constructors.
     */
    private FacetInfo() {}

    @Override
    public String shortDescription() {
        return "Print info about a facet.";
    }

    @Override
    public String fullDescription(Options options) {
        String header = "Prints information about facet(s).";
        String footer =
                """

This tool prints out facet center, normal, angle between center and
normal, and other information about the specified facet(s).""";

        return TerrasaurTool.super.fullDescription(options, header, footer);
    }

    private vtkPolyData polyData;
    private vtkOBBTree searchTree;
    private Vector3D origin;

    private FacetInfo(vtkPolyData polyData) {
        this.polyData = polyData;
        PolyDataStatistics stats = new PolyDataStatistics(polyData);
        origin = new Vector3D(stats.getCentroid());

        logger.info("Origin is at {}", origin);

        logger.info("Creating search tree");
        searchTree = new vtkOBBTree();
        searchTree.SetDataSet(polyData);
        searchTree.SetTolerance(1e-12);
        searchTree.BuildLocator();
    }

    /**
     * @param cellId id of this cell
     * @return Set of neighboring cells (ones which share a vertex with this one)
     */
    private NavigableSet<Long> neighbors(long cellId) {
        NavigableSet<Long> neighborCellIds = new TreeSet<>();

        vtkIdList vertexIdlist = new vtkIdList();
        CellInfo.getCellInfo(polyData, cellId, vertexIdlist);

        vtkIdList facetIdlist = new vtkIdList();
        for (long i = 0; i < vertexIdlist.GetNumberOfIds(); i++) {
            long vertexId = vertexIdlist.GetId(i);
            polyData.GetPointCells(vertexId, facetIdlist);
        }
        for (long i = 0; i < facetIdlist.GetNumberOfIds(); i++) {
            long id = facetIdlist.GetId(i);
            if (id == cellId) continue;
            neighborCellIds.add(id);
        }

        return neighborCellIds;
    }

    @Value.Immutable
    public abstract static class FacetInfoLine {

        public abstract long index();

        public abstract Vector3D radius();

        public abstract Vector3D normal();

        /**
         * @return facets between this and origin
         */
        public abstract NavigableSet<Long> interiorIntersections();

        /**
         * @return facets between this and infinity
         */
        public abstract NavigableSet<Long> exteriorIntersections();

        public static String getHeader() {
            return "# Index, "
                    + "Center Lat (deg), "
                    + "Center Lon (deg), "
                    + "Radius, "
                    + "Radial Vector, "
                    + "Normal Vector, "
                    + "Angle between radial and normal (deg), "
                    + "facets between this and origin, "
                    + "facets between this and infinity";
        }

        public String toCSV() {

            SphericalCoordinates spc = new SphericalCoordinates(radius());

            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%d, ", index()));
            sb.append(String.format("%.4f, ", 90 - Math.toDegrees(spc.getPhi())));
            sb.append(String.format("%.4f, ", Math.toDegrees(spc.getTheta())));
            sb.append(String.format("%.6f, ", spc.getR()));
            sb.append(String.format("%.6f %.6f %.6f, ", radius().getX(), radius().getY(), radius().getZ()));
            sb.append(String.format("%.6f %.6f %.6f, ", normal().getX(), normal().getY(), normal().getZ()));
            sb.append(String.format("%.3f, ", Math.toDegrees(Vector3D.angle(radius(), normal()))));
            sb.append(String.format("%d", interiorIntersections().size()));
            if (!interiorIntersections().isEmpty()) {
                for (long id : interiorIntersections()) sb.append(String.format(" %d", id));
            }
            sb.append(", ");
            sb.append(String.format("%d", exteriorIntersections().size()));
            if (!exteriorIntersections().isEmpty()) {
                for (long id : exteriorIntersections()) sb.append(String.format(" %d", id));
            }
            return sb.toString();
        }
    }

    private FacetInfoLine getFacetInfoLine(long cellId) {
        CellInfo ci = CellInfo.getCellInfo(polyData, cellId, new vtkIdList());

        vtkIdList cellIds = new vtkIdList();
        searchTree.IntersectWithLine(origin.toArray(), ci.center().toArray(), null, cellIds);

        // count up all crossings of the surface between the origin and the facet.
        NavigableSet<Long> insideIds = new TreeSet<>();
        for (long j = 0; j < cellIds.GetNumberOfIds(); j++) {
            if (cellIds.GetId(j) == cellId) continue;
            insideIds.add(cellIds.GetId(j));
        }

        Vector3D infinity = ci.center().scalarMultiply(1e9);

        cellIds = new vtkIdList();
        searchTree.IntersectWithLine(infinity.toArray(), ci.center().toArray(), null, cellIds);

        // count up all crossings of the surface between the infinity and the facet.
        NavigableSet<Long> outsideIds = new TreeSet<>();
        for (long j = 0; j < cellIds.GetNumberOfIds(); j++) {
            if (cellIds.GetId(j) == cellId) continue;
            outsideIds.add(cellIds.GetId(j));
        }

        return ImmutableFacetInfoLine.builder()
                .index(cellId)
                .radius(ci.center())
                .normal(ci.normal())
                .interiorIntersections(insideIds)
                .exteriorIntersections(outsideIds)
                .build();
    }

    private static Options defineOptions() {
        Options options = TerrasaurTool.defineOptions();
        options.addOption(Option.builder("facet")
                .required()
                .hasArgs()
                .desc("Facet(s) to query.  Separate multiple indices with whitespace.")
                .build());
        options.addOption(Option.builder("obj")
                .required()
                .hasArg()
                .desc("Shape model to validate.")
                .build());
        options.addOption(Option.builder("output")
                .required()
                .hasArg()
                .desc("CSV file to write.")
                .build());
        return options;
    }

    public static void main(String[] args) {
        TerrasaurTool defaultOBJ = new FacetInfo();

        Options options = defineOptions();

        CommandLine cl = defaultOBJ.parseArgs(args, options);

        Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
        for (MessageLabel ml : startupMessages.keySet()) logger.info("{} {}", ml.label, startupMessages.get(ml));

        NativeLibraryLoader.loadVtkLibraries();

        try {
            vtkPolyData polydata = PolyDataUtil.loadShapeModel(cl.getOptionValue("obj"));
            FacetInfo app = new FacetInfo(polydata);
            try (PrintWriter pw = new PrintWriter(cl.getOptionValue("output"))) {
                pw.println(FacetInfoLine.getHeader());
                for (long cellId : Arrays.stream(cl.getOptionValues("facet"))
                        .mapToLong(Long::parseLong)
                        .toArray()) {
                    pw.println(app.getFacetInfoLine(cellId).toCSV());

                    NavigableSet<Long> neighbors = app.neighbors(cellId);
                    for (long neighborCellId : neighbors)
                        pw.println(app.getFacetInfoLine(neighborCellId).toCSV());
                }
            }
            logger.info("Wrote {}", cl.getOptionValue("output"));
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        logger.info("Finished.");
    }
}
