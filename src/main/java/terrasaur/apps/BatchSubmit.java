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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.batch.BatchSubmitFactory;
import terrasaur.utils.batch.BatchSubmitI;
import terrasaur.utils.batch.BatchType;
import terrasaur.utils.batch.GridType;

public class BatchSubmit implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();


  @Override
  public String shortDescription() {
    return "Run a command on a cluster.";
  }

  @Override
  public String fullDescription(Options options) {

    String footer = "\nRun a command on a cluster.\n";

    return TerrasaurTool.super.fullDescription(options, "", footer);

  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("command")
            .required()
            .hasArgs()
            .desc("Required.  Command(s) to run.")
            .build());

    StringBuilder sb = new StringBuilder();
    for (GridType type : GridType.values()) sb.append(String.format("%s ", type.name()));
    options.addOption(
        Option.builder("gridType")
            .hasArg()
            .desc("Grid type.  Valid values are " + sb + ". Default is LOCAL.")
            .build());

    options.addOption(
        Option.builder("workingDir")
            .hasArg()
            .desc("Working directory to run command.  Default is current directory.")
            .build());
    return options;
    }

  public static void main(String[] args) {

    TerrasaurTool defaultOBJ = new BatchSubmit();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    List<String> cmdList = Arrays.asList(cl.getOptionValues("command"));
    BatchType batchType = BatchType.GRID_ENGINE;
    GridType gridType =
        cl.hasOption("gridType") ? GridType.valueOf(cl.getOptionValue("gridType")) : GridType.LOCAL;

    BatchSubmitI submitter = BatchSubmitFactory.getBatchSubmit(cmdList, batchType, gridType);
    String workingDir = "";
    try {
      submitter.runBatchSubmitinDir(workingDir);
    } catch (InterruptedException | IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }

}
