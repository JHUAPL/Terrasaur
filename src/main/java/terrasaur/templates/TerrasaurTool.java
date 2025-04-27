package terrasaur.templates;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import terrasaur.utils.AppVersion;
import terrasaur.utils.Log4j2Configurator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All classes in the apps folder should implement this interface. Calling the class without
 * arguments will invoke the fullDescription() method. Calling the class with -shortDescription will
 * invoke the shortDescription() method.
 *
 * @author nairah1
 */
public interface TerrasaurTool {

  /** Show required options first, followed by non-required. */
  class CustomHelpFormatter extends HelpFormatter {
    public CustomHelpFormatter() {
      setOptionComparator(
              (o1, o2) -> {
                if (o1.isRequired() && !o2.isRequired()) return -1;
                if (!o1.isRequired() && o2.isRequired()) return 1;
                return o1.getKey().compareToIgnoreCase(o2.getKey());
              });
    }
  }

  /**
   * @return One line description of this tool
   */
  String shortDescription();

  /**
   * @param options command line options
   * @param header String to print before argument list
   * @param footer String to print after argument list
   * @return Complete description of this tool.
   */
  default String fullDescription(Options options, String header, String footer) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println(AppVersion.getFullString() + "\n");
    HelpFormatter formatter = new CustomHelpFormatter();

    formatter.printHelp(
            pw,
            formatter.getWidth(),
            String.format("%s [options]", this.getClass().getSimpleName()),
            header,
            options,
            formatter.getLeftPadding(),
            formatter.getDescPadding(),
            footer);
    pw.flush();
    return sw.toString();
  }

  /**
   * @param options command line options
   * @return Complete description of this tool.
   */
  default String fullDescription(Options options) {
    return fullDescription(options, "", "");
  }

  /**
   * @param args arguments to parse
   * @param options set of options accepted by the program
   * @return command line formed by parsing arguments
   */
  default CommandLine parseArgs(String[] args, Options options) {
    // if no arguments, print the usage and exit
    if (args.length == 0) {
      System.out.println(fullDescription(options));
      System.exit(0);
    }

    // if -shortDescription is specified, print short description and exit.
    for (String arg : args) {
      if (arg.equals("-shortDescription")) {
        System.out.println(shortDescription());
        System.exit(0);
      }
    }

    // parse the arguments
    CommandLine cl = null;
    try {
      cl = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      System.out.println(fullDescription(options));
      System.exit(0);
    }

    return cl;
  }

  /**
   * @return options including -logFile and -logLevel
   */
  static Options defineOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder("logFile")
            .hasArg()
            .desc("If present, save screen output to log file.")
            .build());
    StringBuilder sb = new StringBuilder();
    for (Level l : Level.values()) sb.append(String.format("%s ", l.name()));
    options.addOption(
        Option.builder("logLevel")
            .hasArg()
            .desc(
                "If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim()
                    + ".  Default is INFO.")
            .build());
    return options;
  }

  /** Labels for startup messages. The enum order is the order in which they are printed. */
  enum MessageLabel {
    START("Start"),
    ARGUMENTS("arguments");
    public final String label;

    MessageLabel(String label) {
      this.label = label;
    }
  }

  /**
   * Generate startup messages. This is returned as a map. For example:
   *
   * <table>
   *     <tr>
   *         <th>Key</th>
   *         <th>Value</th>
   *     </tr>
   *     <tr>
   *         <td>
   *             Start
   *         </td>
   *         <td>
   *             MEGANESimulator [MMXTools version 25.01.28-b868ef6M] on nairah1-ml1
   *         </td>
   *     </tr>
   *     <tr>
   *         <td>arguments:</td>
   *         <td>-spice /project/sis/users/nairah1/MMX/spice/meganeLCp.mk -startTime 2026 JUN 20 00:00:00 -stopTime 2026 JUN 20 08:00:00 -delta 180 -outputCSV tmp.csv -numThreads 1 -obj /project/sis/users/nairah1/MMX/obj/Phobos-Ernst-800.obj -dbName tmp.db</td>
   *     </tr>
   * </table>
   *
   * @param cl Command line object
   * @return standard startup messages
   */
  default Map<MessageLabel, String> startupMessages(CommandLine cl) {

    Map<MessageLabel, String> startupMessages = new LinkedHashMap<>();

    String hostname = "unknown host";
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException ignored) {
    }

    Log4j2Configurator lc = Log4j2Configurator.getInstance();
    if (cl.hasOption("logLevel"))
      lc.setLevel(Level.valueOf(cl.getOptionValue("logLevel").toUpperCase().trim()));

    if (cl.hasOption("logFile")) lc.addFile(cl.getOptionValue("logFile"));

    StringBuilder sb =
        new StringBuilder(
            String.format(
                "%s [%s] on %s",
                getClass().getSimpleName(), AppVersion.getVersionString(), hostname));
    startupMessages.put(MessageLabel.START, sb.toString());
    sb = new StringBuilder();

    for (Option option : cl.getOptions()) {
      sb.append("-").append(option.getOpt()).append(" ");
      if (option.hasArgs()) {
        for (String arg : option.getValues()) sb.append(arg).append(" ");
      } else if (option.hasArg()) {
        sb.append(option.getValue()).append(" ");
      }
    }
    for (String arg : cl.getArgs()) {
      sb.append(arg).append(" ");
    }

    startupMessages.put(MessageLabel.ARGUMENTS, sb.toString());

    return startupMessages;
  }

}
