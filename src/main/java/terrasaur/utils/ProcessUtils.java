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
package terrasaur.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Methods to run shell commands.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class ProcessUtils {

  private final static Logger logger = LogManager.getLogger(ProcessUtils.class);

  /**
   * Calls runProgramAndWait(program, null).
   * 
   * @param program
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean runProgramAndWait(String program) throws IOException, InterruptedException {
    return runProgramAndWait(program, null);
  }

  /**
   * Calls runProgramAndWait(program, workingDirectory, true).
   * 
   * @param program
   * @param workingDirectory
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean runProgramAndWait(String program, File workingDirectory)
      throws IOException, InterruptedException {
    return runProgramAndWait(program, workingDirectory, true);
  }

  /**
   * Run a shell command.
   * 
   * @param program command to run
   * @param workingDirectory working directory
   * @param printOutput if true, print output to screen
   * @return true on successful completion
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean runProgramAndWait(String program, File workingDirectory,
      boolean printOutput) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
    processBuilder.directory(workingDirectory);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();

    if (printOutput) {
      try (
          BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        logger.info(String.format("Output of running %s is:", program));
        while ((line = br.readLine()) != null) {
          logger.info(line);
        }
      }
    }

    int exitStatus = process.waitFor();
    logger.info("Program " + program + " finished with status: " + exitStatus);
    process.destroy();

    if (exitStatus != 0) {
      logger.warn("Terminating since subprogram failed.");
      System.exit(exitStatus);
    }

    return exitStatus == 0;
  }

}
