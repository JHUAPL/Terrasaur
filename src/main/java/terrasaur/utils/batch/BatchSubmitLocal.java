package terrasaur.utils.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains concrete methods for submitting batch jobs to local machine. So even if batchType says
 * "GRID*" will default to local.
 * 
 * @author espirrc1
 *
 */
public class BatchSubmitLocal implements BatchSubmitI {

  private static Logger logger = LogManager.getLogger(BatchSubmitLocal.class);

  private List<String> commandList;
  private BatchType batchType;
  private int cores;
  private boolean showOutput = true;
  // private String gridQueue = null;

  public BatchSubmitLocal(List<String> commandList, BatchType batchType) {
    this.commandList = commandList;
    this.batchType = batchType;
    cores = Runtime.getRuntime().availableProcessors();
  }

  /**
   * STDOUT from commandlist will not be printed.
   */
  public void noScreenOutput() {
    showOutput = false;
  }

  /**
   * Does not apply in local mode.
   * 
   * @param gridQueue
   */
  public void setQueue(String gridQueue) {
    logger.warn(
        "Warning: trying to set a queue while running local mode, does" + " not do anything.");
  }

  public void limitCores(int limit) {
    cores = Math.min(limit, Runtime.getRuntime().availableProcessors());
    logger.info("limiting number of local cores to:" + cores);
  }

  public String printInfo() {
    return "batchType:" + this.batchType;
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
  public boolean runBatchSubmitinDir(String workingDir) throws InterruptedException, IOException {

    // evaluate workingDir. If empty string then set to null;
    workingDir = emptyToNull(workingDir);

    switch (batchType) {
      case GNU_PARALLEL:
        return runBatchSubmitProgramParallel(commandList);

      case LOCAL_SEQUENTIAL:
        return runBatchSubmitProgramLocalSequential(commandList, workingDir);

      case GRID_ENGINE:
      case GRID_ENGINE_8:
      case GRID_ENGINE_6:
      case GRID_ENGINE_4:
      case GRID_ENGINE_3:
      case GRID_ENGINE_2:
      default:
        throw new RuntimeException("Can't submit " + batchType.toString() + " in "
            + "BatchSubmitLocal class. Please fix pipe config file.");
    }
  }

  public boolean runProgramAndWait(String program) throws IOException, InterruptedException {
    return runProgramAndWait(program, null);
  }

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
    if (showOutput) {
      logger.printf(Level.INFO, "Output of running %s is:", program);
      while ((line = br.readLine()) != null) {
        System.out.println(line);
      }
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
