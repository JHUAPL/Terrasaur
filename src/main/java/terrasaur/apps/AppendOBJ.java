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

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataUtil;
import vtk.vtkAppendPolyData;
import vtk.vtkObjectBase;
import vtk.vtkPolyData;

/**
 * AppendOBJ program. See the usage string for more information about this program.
 *
 * @author Eli Kahn
 * @version 1.0
 */
public class AppendOBJ implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Combine multiple shape files (OBJ or VTK format) into one.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "This program combines input shape models into a single shape model.";
    return TerrasaurTool.super.fullDescription(options, header, "");
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
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
        Option.builder("boundary")
            .desc("Only save out boundary. This option implies -vtk.")
            .build());
    options.addOption(
        Option.builder("decimate")
            .hasArg()
            .desc(
                "Reduce the number of facets in the output shape model.  The argument should be between 0 and 1.  "
                    + "For example, if a model has 100 facets and <arg> is 0.90, "
                    + "there will be approximately 10 facets after the decimation.")
            .build());
    options.addOption(
        Option.builder("input")
            .required()
            .hasArgs()
            .desc(
                "input file(s) to read.  Format is derived from the allowed extension: "
                    + "icq, llr, obj, pds, plt, ply, stl, or vtk.  Multiple files can be specified "
                    + "with a single -input option, separated by whitespace.  Alternatively, you may "
                    + "specify -input multiple times.")
            .build());
    options.addOption(
        Option.builder("output").required().hasArg().desc("output file to write.").build());
    options.addOption(
        Option.builder("vtk").desc("Save output file in VTK format rather than OBJ.").build());
    return options;
  }

  public static void main(String[] args) throws Exception {

    TerrasaurTool defaultOBJ = new AppendOBJ();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    boolean boundaryOnly = cl.hasOption("boundary");
    boolean vtkFormat = boundaryOnly || cl.hasOption("vtk");
    boolean decimate = cl.hasOption("decimate");
    double decimationPercentage =
        decimate ? Double.parseDouble(cl.getOptionValue("decimate")) : 1.0;

    NativeLibraryLoader.loadVtkLibraries();

    String outfile = cl.getOptionValue("output");
    String[] infiles = cl.getOptionValues("input");

    vtkAppendPolyData append = new vtkAppendPolyData();
    append.UserManagedInputsOn();
    append.SetNumberOfInputs(infiles.length);

    for (int i = 0; i < infiles.length; ++i) {
      logger.info("loading {} {} / {}", infiles[i], i + 1, infiles.length);

      vtkPolyData polydata = PolyDataUtil.loadShapeModel(infiles[i]);

      if (polydata == null) {
        logger.warn("Cannot load {}", infiles[i]);
      } else {
        if (boundaryOnly) {
          vtkPolyData boundary = PolyDataUtil.getBoundary(polydata);
          boundary.GetCellData().SetScalars(null);
          polydata.DeepCopy(boundary);
        }

        append.SetInputDataByNumber(i, polydata);
      }
      System.gc();
      vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false);
    }

    append.Update();

    vtkPolyData outputShape = append.GetOutput();
    if (decimate) PolyDataUtil.decimatePolyData(outputShape, decimationPercentage);

    if (vtkFormat) PolyDataUtil.saveShapeModelAsVTK(outputShape, outfile);
    else PolyDataUtil.saveShapeModelAsOBJ(append.GetOutput(), outfile);

    logger.info("Wrote " + outfile);
  }
}
