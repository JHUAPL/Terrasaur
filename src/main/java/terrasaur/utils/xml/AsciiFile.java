package terrasaur.utils.xml;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for writing to ASCII file in various ways. Includes a static method for reading an ascii
 * file into List<String>
 *
 * @author espirrc1
 */
public class AsciiFile {

  private FileOutputStream fstreamOut;
  private PrintStream printStream;

  public AsciiFile(String outFname) {
    fstreamOut = null;
    try {
      fstreamOut = new FileOutputStream(outFname);
      printStream = new PrintStream(fstreamOut);
    } catch (IOException e) {
      System.err.println("Cannot open file for writing:" + outFname);
      System.exit(1);
    }
  }

  /**
   * Print the double array to the ascii file. Uses default formatting.
   *
   * @param arrayD
   */
  public void ArrayDToFile(double[] arrayD) {
    String line;
    String terminator = "\n";

    for (int ii = 0; ii < arrayD.length; ii++) {
      line = Double.toString(arrayD[ii]);
      printStream.print(line + terminator);
    }
  }

  /**
   * Print a given string as a record to the file. Use this when it becomes cumbersome or redundant
   * to form the List <String> before writing the list to a file. This way I can dynamically form
   * the string in a loop and write each string as it is created.
   *
   * @param record
   * @param termType
   */
  public void streamSToFile(String record, int termType) {
    String terminator;

    switch (termType) {
      case -1:
        terminator = "";
      case 0:
        terminator = "\r\n";
        break;
      case 1:
        terminator = "\n";
        break;
      default:
        terminator = "\n";
    }
    printStream.print(record + terminator);
  }

  /**
   * Print each string in the string list to the PrintStream. Terminate each string with \r\n. Note:
   * List should be backed by LinkedList in order to preserve ordering of strings.
   *
   * @param stringList
   * @param termType
   */
  public void lStringToFile(List<String> stringList, int termType) {

    String terminator;

    switch (termType) {
      case 0:
        terminator = "\r\n";
        break;
      case 1:
        terminator = "\n";
        break;
      default:
        terminator = "\n";
    }
    for (String line : stringList) {
      printStream.print(line + terminator);
    }
  }

  public void closeFile() {
    printStream.close();
  }

  public static List<String> readFileasStrList(String path)
      throws FileNotFoundException, IOException {
    List<String> fileContent = new ArrayList<String>();
    String thisLine = null;
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {

      while ((thisLine = br.readLine()) != null) {
        fileContent.add(thisLine);
      }
    }

    return fileContent;
  }
}
