package terrasaur.templates;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Template for setting up an application.
 *
 * @author nairah1
 */
public class DefaultTerrasaurTool implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  /**
   * This doesn't need to be private, or even declared, but you might want to if you have other
   * constructors.
   */
  private DefaultTerrasaurTool() {}

  @Override
  public String shortDescription() {
    return "SHORT DESCRIPTION.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "TEXT APPEARING BEFORE COMMAND LINE OPTION SUMMARY";
    String footer = "\nTEXT APPENDED TO COMMAND LINE OPTION SUMMARY.\n";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("env")
            .hasArgs()
            .required()
            .desc("Print the named environment variable's value.  Can take multiple arguments.")
            .build());
    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new DefaultTerrasaurTool();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    for (String env : cl.getOptionValues("env"))
      logger.info(String.format("%s: %s", env, System.getenv(env)));

    logger.info("Finished");
  }
}
