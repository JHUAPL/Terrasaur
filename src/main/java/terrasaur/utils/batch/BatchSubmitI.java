package terrasaur.utils.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public interface BatchSubmitI {

  public boolean runBatchSubmitinDir(String workingDir) throws InterruptedException, IOException;

  public boolean runProgramAndWait(String program) throws IOException, InterruptedException;

  public boolean runProgramAndWait(String program, File workingDirectory)
      throws IOException, InterruptedException;

  public void limitCores(int limit);

  public void setQueue(String queueName);

  public String printInfo();

  public void noScreenOutput();

  public static <T> void saveList(List<T> array, String filename) throws IOException {
    FileWriter fstream = new FileWriter(filename);
    BufferedWriter out = new BufferedWriter(fstream);

    String nl = System.getProperty("line.separator");

    for (T o : array)
      out.write(o.toString() + nl);

    out.close();
  }

  /**
   * Static method that can also be used when one does not need to specify batchType. It will be run
   * on machine that is executing the code. User takes responsibility for errors if the program
   * itself is expecting a distributed process to exist.
   * 
   * @param program
   * @param workingDirectory
   * @return STDOUT created by the program
   * @throws IOException
   * @throws InterruptedException
   */
  public static List<String> runAndReturn(String program, File workingDirectory)
      throws IOException, InterruptedException {

    List<String> stdOut = new ArrayList<String>();

    ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
    processBuilder.directory(workingDirectory);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();

    InputStream is = process.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;
    while ((line = br.readLine()) != null) {
      stdOut.add(line);
    }

    int exitStatus = process.waitFor();
    br.close();
    process.destroy();
    if (exitStatus != 0) {
      stdOut.add("Error! non-zero status returned!");
    }

    return stdOut;
  }

}
