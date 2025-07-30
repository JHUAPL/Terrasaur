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

import java.io.*;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.ICQUtils;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataUtil;
import vtk.vtkPolyData;

public class TriAx implements TerrasaurTool {

    private static final Logger logger = LogManager.getLogger();

    private TriAx() {}

    @Override
    public String shortDescription() {
        return "Generate a triaxial ellipsoid in ICQ format.";
    }

    @Override
    public String fullDescription(Options options) {

        String footer =
                "\nTriAx is an implementation of the SPC tool TRIAX, which generates a triaxial ellipsoid in ICQ format.\n";
        return TerrasaurTool.super.fullDescription(options, "", footer);
    }

    static Options defineOptions() {
        Options options = TerrasaurTool.defineOptions();
        options.addOption(Option.builder("A")
                .required()
                .hasArg()
                .desc("Long axis of the ellipsoid, arbitrary units (usually assumed to be km).")
                .build());
        options.addOption(Option.builder("B")
                .required()
                .hasArg()
                .desc("Medium axis of the ellipsoid, arbitrary units (usually assumed to be km).")
                .build());
        options.addOption(Option.builder("C")
                .required()
                .hasArg()
                .desc("Short axis of the ellipsoid, arbitrary units (usually assumed to be km).")
                .build());
        options.addOption(Option.builder("Q")
                .required()
                .hasArg()
                .desc("ICQ size parameter.  This is conventionally but not necessarily a power of 2.")
                .build());
        options.addOption(Option.builder("saveOBJ")
                .desc("If present, save in OBJ format as well.  "
                        + "File will have the same name as ICQ file with an OBJ extension.")
                .build());
        options.addOption(Option.builder("output")
                .hasArg()
                .required()
                .desc("Name of ICQ file to write.")
                .build());

        return options;
    }

    static final int MAX_Q = 512;

    public static void main(String[] args) {
        TerrasaurTool defaultOBJ = new TriAx();

        Options options = defineOptions();

        CommandLine cl = defaultOBJ.parseArgs(args, options);

        Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
        for (MessageLabel ml : startupMessages.keySet()) logger.info("{} {}", ml.label, startupMessages.get(ml));

        int q = Integer.parseInt(cl.getOptionValue("Q"));
        String shapefile = cl.getOptionValue("output");

        double[] ax = new double[3];
        ax[0] = Double.parseDouble(cl.getOptionValue("A"));
        ax[1] = Double.parseDouble(cl.getOptionValue("B"));
        ax[2] = Double.parseDouble(cl.getOptionValue("C"));

        double[][][][] vec = new double[3][MAX_Q + 1][MAX_Q + 1][6];
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i <= q; i++) {
                for (int j = 0; j <= q; j++) {

                    double[] u = ICQUtils.xyf2u(q, i, j, f, ax);
                    double z = 1
                            / Math.sqrt(
                                    Math.pow(u[0] / ax[0], 2) + Math.pow(u[1] / ax[1], 2) + Math.pow(u[2] / ax[2], 2));

                    double[] v = new Vector3D(u).scalarMultiply(z).toArray();
                    for (int k = 0; k < 3; k++) {
                        vec[k][i][j][f] = v[k];
                    }
                }
            }
        }

        ICQUtils.writeICQ(q, vec, shapefile);

        if (cl.hasOption("saveOBJ")) {

            String basename = FilenameUtils.getBaseName(shapefile);
            String dirname = FilenameUtils.getFullPath(shapefile);
            if (dirname.isEmpty()) dirname = ".";
            File obj = new File(dirname, basename + ".obj");

            NativeLibraryLoader.loadVtkLibraries();
            try {
                vtkPolyData polydata = PolyDataUtil.loadShapeModel(shapefile);
                if (polydata == null) {
                    logger.error("Cannot read {}", shapefile);
                    System.exit(0);
                }

                polydata = PolyDataUtil.removeDuplicatePoints(polydata);
                polydata = PolyDataUtil.removeUnreferencedPoints(polydata);
                polydata = PolyDataUtil.removeZeroAreaFacets(polydata);

                PolyDataUtil.saveShapeModelAsOBJ(polydata, obj.getPath());
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
