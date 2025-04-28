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
package terrasaur.utils.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains concrete methods for submitting batch jobs to Sun Open Grid Grid Computing software in
 * addition to supporting submission of jobs to local sequential or local parallel.
 * 
 * This class assumes that the Sun Open Grid Engine does exist and is able to accept jobs, hence no
 * need to check for existence of $SGE_ROOT environment variable.
 * 
 * @author espirrc1
 *
 */
public class BatchSubmitOpenGrid implements BatchSubmitI {

  private static Logger logger = LogManager.getLogger(BatchSubmitOpenGrid.class);

  private List<String> commandList;
  private BatchType batchType;
  private int cores;
  private boolean showOutput = true;
  private String gridQueue = null;

  public BatchSubmitOpenGrid(List<String> commandList, BatchType batchType) {
    this.commandList = commandList;
    this.batchType = batchType;
    cores = Runtime.getRuntime().availableProcessors();
  }

  /**
   * STDOUT from commandlist will not be printed.
   */
  @Override
  public void noScreenOutput() {
    showOutput = false;
  }

  /**
   * Set the grid queue to use when calling the grid engine.
   * 
   * @param queueName
   */
  @Override
  public void setQueue(String queueName) {
    this.gridQueue = queueName;
  }

  @Override
  public void limitCores(int limit) {
    cores = Math.min(limit, Runtime.getRuntime().availableProcessors());
    logger.info("limiting number of local cores to:" + cores);
  }

  @Override
  public String printInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("batchType:" + this.batchType);
    if (gridQueue != null) {
      sb.append("queue:" + gridQueue);
    }
    return sb.toString();
  }

  /**
   * Run batch submission in the specified working directory. Uses current working directory if
   * workingDir is null.
   * 
   * @param workingDir
   * @return
   * @throws InterruptedException
   * @throws IOException
   */
  @Override
  public boolean runBatchSubmitinDir(String workingDir) throws InterruptedException, IOException {

    // evaluate workingDir. If empty string then set to null;
    workingDir = emptyToNull(workingDir);

    logger.info("Running:" + batchType.toString());

    switch (batchType) {
      case GRID_ENGINE:
        return runBatchSubmitProgramGridEngine(commandList, workingDir);

      case GRID_ENGINE_45:
      case GRID_ENGINE_35:
      case GRID_ENGINE_33:
      case GRID_ENGINE_32:
      case GRID_ENGINE_23:
      case GRID_ENGINE_17:
      case GRID_ENGINE_16:
      case GRID_ENGINE_10:
      case GRID_ENGINE_8:
      case GRID_ENGINE_6:
      case GRID_ENGINE_4:
      case GRID_ENGINE_3:
      case GRID_ENGINE_2:
        return runBatchSubmitProgramGridEngineLimitSlots(commandList, workingDir, batchType);

      case GNU_PARALLEL:
        return runBatchSubmitProgramParallel(commandList);

      case LOCAL_SEQUENTIAL:
        return runBatchSubmitProgramLocalSequential(commandList, workingDir);

      default:
        String errMesg =
            "ERROR! Batch type:" + batchType.toString() + " not supported by runBatchSubmitinDir!";
        throw new RuntimeException(errMesg);

    }
  }

  @Override
  public boolean runProgramAndWait(String program) throws IOException, InterruptedException {
    return runProgramAndWait(program, null);
  }


  @Override
  public boolean runProgramAndWait(String program, File workingDirectory)
      throws IOException, InterruptedException {

    // return runAndWait(program, workingDirectory, showOutput);
    ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
    processBuilder.directory(workingDirectory);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();

    InputStream is = process.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;

    // disable output for now. See if this
    if (showOutput) {
      logger.printf(Level.INFO, "Output of running %s is:", program);
      while ((line = br.readLine()) != null) {
        System.out.println(line);
      }
      System.out.flush();
    } else {
      logger.printf(Level.INFO, "Output of running %s disabled.", program);
    }
    int exitStatus = process.waitFor();
    logger.info("Program " + program + " finished with status: " + exitStatus);
    br.close();
    process.destroy();

    if (exitStatus != 0) {
      logger.error("Terminating since subprogram failed.");
      System.exit(exitStatus);
    }
    return exitStatus == 0;
  }

  /**
   * Static method that can also be used when one does not need to specify batchType. It will be run
   * on machine that is executing the code. User takes responsibility for errors if the program
   * itself is expecting a distributed process to exist.
   * 
   * @param program
   * @param workingDirectory
   * @param showOutput
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean runAndWait(String program, File workingDirectory, boolean showOutput)
      throws IOException, InterruptedException {

    return false;
  }

  /**
   * Evaluate working directory string. Set to null if empty.
   * 
   * @param workingDir
   * @return
   */
  private String emptyToNull(String workingDir) {
    if (workingDir != null) {
      if (workingDir.length() < 1) {
        return null;
      } else {
      }
    }
    return workingDir;
  }

  private boolean runBatchSubmitProgramGridEngine(List<String> commandList, String workingDir)
      throws InterruptedException, IOException {

    workingDir = emptyToNull(workingDir);

    // Create a text file for input to qsub making use of qsub's job array
    // option. Use workingDir if not null
    File tempDir = null;
    if (workingDir != null) {
      tempDir = new File(workingDir);
    }
    File temp = File.createTempFile("altwg-batch-list", ".bash", tempDir);

    FileWriter ofs = new FileWriter(temp);
    BufferedWriter out = new BufferedWriter(ofs);
    out.write("CURRENTDATE=`date +\"%Y-%m-%d %T\"`\n");
    out.write("echo $CURRENTDATE The node running this job is $HOSTNAME\n");
    for (int i = 1; i <= commandList.size(); ++i)
      out.write("if [ $SGE_TASK_ID -eq " + i + " ]; then \n " + commandList.get(i - 1)
          + "\n exit $?\nfi\n");
    out.close();

    String batchSubmitCommand;

    String queue = "";
    if ((gridQueue != null) && (gridQueue.length() > 1)) {
      // use grid queue name explicitly
      queue = " -q " + gridQueue + " ";
    }

    if (workingDir != null) {

      // user specified working directory
      batchSubmitCommand = "qsub " + queue + "-S /bin/bash -V -wd " + workingDir + " -sync y -t 1-"
          + commandList.size() + " " + temp.getAbsolutePath();

    } else {
      // null working directory. Assume want to work in current working directory
      batchSubmitCommand = "qsub " + queue + "-S /bin/bash -V -cwd -sync y -t 1-"
          + commandList.size() + " " + temp.getAbsolutePath();

    }

    boolean success = runProgramAndWait(batchSubmitCommand);

    if (success) {
      // If no error, delete qsub output and error files
      File[] filelist = new File(".").listFiles();
      for (File f : filelist) {
        if (f.getName().startsWith(temp.getName()))
          f.delete();
      }
    }

    return success;
  }

  /**
   * Call the grid engine using the parallel environment that has been configured for it. The
   * batchType enumeration should contain a number specifying the number of CPUs needed per job.
   * Useful when trying to run memory intensive programs such as DistributedGravity.java.
   * 
   * @param commandList
   * @return
   * @throws InterruptedException
   * @throws IOException
   */
  private boolean runBatchSubmitProgramGridEngineLimitSlots(List<String> commandList,
      String workingDir, BatchType batchType) throws InterruptedException, IOException {

    // evaluate workingDir. If empty string then set to null;
    workingDir = emptyToNull(workingDir);

    // Create a text file for input to qsub making use of qsub's job array
    // option
    File tempDir = null;
    if (workingDir != null) {
      tempDir = new File(workingDir);
    }
    File temp = File.createTempFile("altwg-batch-list", ".bash", tempDir);

    FileWriter ofs = new FileWriter(temp);
    BufferedWriter out = new BufferedWriter(ofs);
    out.write("CURRENTDATE=`date +\"%Y-%m-%d %T\"`\n");
    out.write("echo $CURRENTDATE The node running this job is $HOSTNAME\n");
    for (int i = 1; i <= commandList.size(); ++i)
      out.write("if [ $SGE_TASK_ID -eq " + i + " ]; then \n " + commandList.get(i - 1)
          + "\n exit $?\nfi\n");
    out.close();

    // default to using 6 cpus per job.
    String cpus = "6";
    // String batchString = batchType.toString();
    int numSlots = BatchType.slotPerJob(batchType);
    if (numSlots == 999) {
      String errMesg = "ERROR! batchType:" + batchType.toString() + " not supported by"
          + " BatchType.slotsPerJob()!";
      throw new RuntimeException(errMesg);
    }
    cpus = Integer.toString(numSlots);

    // check to see if grid queue is specified
    StringBuilder batchCmd = new StringBuilder("qsub");
    if ((gridQueue != null) && (gridQueue.length() > 1)) {

      // use grid queue name explicitly
      batchCmd.append(" -q " + gridQueue);
    }

    // Try to use parallel processing environment
    batchCmd.append(" -pe ocmp");
    batchCmd.append(" " + cpus);

    batchCmd.append(" -S /bin/bash");
    batchCmd.append(" -V");

    if (workingDir != null) {
      batchCmd.append(" -wd " + workingDir);
    } else {
      batchCmd.append(" -cwd ");
    }
    batchCmd.append(" -sync y");
    batchCmd.append(" -t 1-" + commandList.size());
    batchCmd.append(" " + temp.getAbsolutePath());

    // batchSubmitCommand = "qsub -pe ocmp " + cpus + " -S /bin/bash -V -cwd -sync y -t 1-" +
    // commandList.size() + " "
    // + temp.getAbsolutePath();
    // batchSubmitCommand = "qsub -pe ocmp " + cpus + " -S /bin/bash -V -wd " + workingDir + " -sync
    // y -t 1-" + commandList.size() + " "
    // + temp.getAbsolutePath();

    String batchSubmitCommand = batchCmd.toString();
    boolean success = runProgramAndWait(batchSubmitCommand);

    if (success) {
      // If no error, delete qsub output and error files
      File[] filelist = new File(".").listFiles();
      for (File f : filelist) {
        if (f.getName().startsWith(temp.getName()))
          f.delete();
      }
    }

    return success;
  }

  private boolean runBatchSubmitProgramParallel(List<String> commandList)
      throws InterruptedException, IOException {
    // Create a text file with all the commands that should be run, one per
    // line
    File temp = File.createTempFile("altwg-batch-list", ".tmp", null);
    BatchSubmitI.saveList(commandList, temp.getAbsolutePath());

    // Now submit all these batches GNU Parallel
    String batchSubmitCommand = "parallel -v -a " + temp.getAbsolutePath();

    return runProgramAndWait(batchSubmitCommand);
  }

  private boolean runBatchSubmitProgramLocalSequential(List<String> commandList, String workingDir)
      throws IOException, InterruptedException {

    // evaluate workingDir. If empty string then set to null;
    workingDir = emptyToNull(workingDir);

    boolean successful = true;
    for (String command : commandList) {
      if (workingDir != null) {
        File workingFile = new File(workingDir);
        if (!runProgramAndWait(command, workingFile))
          successful = false;
      } else {
        if (!runProgramAndWait(command))
          successful = false;
      }
    }

    return successful;
  }

}
