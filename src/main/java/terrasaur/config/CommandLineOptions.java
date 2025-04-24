package terrasaur.config;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.Log4j2Configurator;

public class CommandLineOptions {

  private static final Logger logger = LogManager.getLogger();

  /**
   * Configuration file to load
   *
   * @return
   */
  public static Option addConfig() {
    return Option.builder("config").hasArg().required().desc("Configuration file to load").build();
  }

  /**
   * Color ramp style. See {@link ColorRamp.TYPE} for valid values.
   *
   * @param defaultCRType
   * @return
   */
  public static Option addColorRamp(ColorRamp.TYPE defaultCRType) {
    StringBuilder sb = new StringBuilder();
    for (ColorRamp.TYPE t : ColorRamp.TYPE.values()) sb.append(String.format("%s ", t.name()));
    return Option.builder("colorRamp")
        .hasArg()
        .desc(
            "Color ramp style.  Valid values are "
                + sb.toString().trim()
                + ".  Default is "
                + defaultCRType.name()
                + ".  Run the ColorMaps application to see all supported color ramps.")
        .build();
  }

  /**
   * Return a color ramp type or the default value.
   *
   * @param cl
   * @param defaultCRType
   * @return
   */
  public static ColorRamp.TYPE getColorRamp(CommandLine cl, ColorRamp.TYPE defaultCRType) {
    ColorRamp.TYPE crType =
        cl.hasOption("colorRamp")
            ? ColorRamp.TYPE.valueOf(cl.getOptionValue("colorRamp").toUpperCase().strip())
            : defaultCRType;
    return crType;
  }

  /**
   * Hard lower limit for color bar. If the color bar minimum is set dynamically it will not be
   * lower than hardMin.
   *
   * @return
   */
  public static Option addHardMin() {
    return Option.builder("hardMin")
        .hasArg()
        .desc(
            "Hard lower limit for color bar. If the color bar minimum is set dynamically it will not be lower than hardMin.")
        .build();
  }

  /**
   * Hard upper limit for color bar. If the color bar maximum is set dynamically it will not be
   * higher than hardMax.
   *
   * @return
   */
  public static Option addHardMax() {
    return Option.builder("hardMax")
        .hasArg()
        .desc(
            "Hard upper limit for color bar. If the color bar maximum is set dynamically it will not be higher than hardMax.")
        .build();
  }

  /**
   * Get the hard minimum for the colorbar.
   *
   * @param cl
   * @return
   */
  public static double getHardMin(CommandLine cl) {
    return cl.hasOption("hardMin") ? Double.parseDouble(cl.getOptionValue("hardMin")) : Double.NaN;
  }

  /**
   * Get the hard maximum for the colorbar.
   *
   * @param cl
   * @return
   */
  public static double getHardMax(CommandLine cl) {
    return cl.hasOption("hardMax") ? Double.parseDouble(cl.getOptionValue("hardMax")) : Double.NaN;
  }

  /**
   * If present, save screen output to log file.
   *
   * @return
   */
  public static Option addLogFile() {
    return Option.builder("logFile")
        .hasArg()
        .desc("If present, save screen output to log file.")
        .build();
  }

  /**
   * If present, print messages above selected priority. See {@link StandardLevel} for valid values.
   *
   * @return
   */
  public static Option addLogLevel() {
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values()) sb.append(String.format("%s ", l.name()));
    return Option.builder("logLevel")
        .hasArg()
        .desc(
            "If present, print messages above selected priority.  Valid values are "
                + sb.toString().trim()
                + ".  Default is INFO.")
        .build();
  }

  /**
   * Set the logging level from the command line option.
   *
   * @param cl
   */
  public static void setLogLevel(CommandLine cl) {
    Log4j2Configurator lc = Log4j2Configurator.getInstance();
    if (cl.hasOption("logLevel"))
      lc.setLevel(Level.valueOf(cl.getOptionValue("logLevel").toUpperCase().trim()));
  }

  /**
   * Log to file named on the command line as well as others
   *
   * @param cl
   * @param others
   */
  public static void setLogFile(CommandLine cl, String... others) {
    Log4j2Configurator lc = Log4j2Configurator.getInstance();
    if (cl.hasOption("logFile")) lc.addFile(cl.getOptionValue("logFile"));
    for (String other : others) lc.addFile(other);
  }

  /**
   * Maximum number of simultaneous threads to execute.
   *
   * @return
   */
  public static Option addNumCPU() {
    return Option.builder("numCPU")
        .hasArg()
        .desc(
            "Maximum number of simultaneous threads to execute.  Default is numCPU value in configuration file.")
        .build();
  }

  /**
   * Directory to place output files. Default is the working directory.
   *
   * @return
   */
  public static Option addOutputDir() {
    return Option.builder("outputDir")
        .hasArg()
        .desc("Directory to place output files.  Default is the working directory.")
        .build();
  }

  /**
   * Set the output dir from the command line argument.
   *
   * @param cl
   * @return
   */
  public static String setOutputDir(CommandLine cl) {
    String path = cl.hasOption("outputDir") ? cl.getOptionValue("outputDir") : ".";
    File parent = new File(path);
    if (!parent.exists()) parent.mkdirs();
    return path;
  }

  /**
   * Minimum value to plot.
   *
   * @return
   */
  public static Option addPlotMin() {
    return Option.builder("plotMin").hasArg().desc("Min value to plot.").build();
  }

  /**
   * Get plot min from command line argument
   *
   * @param cl
   * @return
   */
  public static double getPlotMin(CommandLine cl) {
    return cl.hasOption("plotMin") ? Double.parseDouble(cl.getOptionValue("plotMin")) : Double.NaN;
  }

  /**
   * Maximum value to plot.
   *
   * @return
   */
  public static Option addPlotMax() {
    return Option.builder("plotMax").hasArg().desc("Max value to plot.").build();
  }

  /**
   * Get plot max from command line argument
   *
   * @param cl
   * @return
   */
  public static double getPlotMax(CommandLine cl) {
    return cl.hasOption("plotMax") ? Double.parseDouble(cl.getOptionValue("plotMax")) : Double.NaN;
  }
}
