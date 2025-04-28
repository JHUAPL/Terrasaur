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

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.PolyDataStatistics;
import terrasaur.utils.PolyDataUtil;
import vtk.vtkPolyData;

/**
 * PrintShapeModelStatistics program. Takes a shape model in OBJ format and prints out various
 * statistics about it. about this program.
 *
 * @author Eli Kahn
 * @version 1.0
 */
public class PrintShapeModelStatistics implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  private PrintShapeModelStatistics() {}

  @Override
  public String shortDescription() {
    return "Print statistics about a shape model.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "This program prints various statistics about a shape model in OBJ format.";
    String footer = "";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("objFile").required().hasArg().desc("Path to OBJ file.").build());
    return options;
  }

  public static void main(String[] args) throws Exception {
    TerrasaurTool defaultOBJ = new PrintShapeModelStatistics();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    String filename = cl.getOptionValue("objFile");

    NativeLibraryLoader.loadVtkLibraries();

    vtkPolyData polydata = PolyDataUtil.loadShapeModelAndComputeNormals(filename);

    PolyDataStatistics stat = new PolyDataStatistics(polydata);
    ArrayList<String> stats = stat.getShapeModelStats();
    for (String line : stats) {
      logger.info(line);
    }
  }
}
