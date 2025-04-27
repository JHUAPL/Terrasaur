package terrasaur.apps;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.KernelDatabase;
import spice.basic.KernelPool;
import spice.basic.SCLK;
import spice.basic.SCLKTime;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.TDBTime;
import terrasaur.gui.TranslateTimeFX;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.NativeLibraryLoader;

/**
 * Translate time between formats.
 * 
 * @author nairah1
 *
 */
public class TranslateTime implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Convert between different time systems.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer = "\nConvert between different time systems.\n";
    return TerrasaurTool.super.fullDescription(options, header, footer);

  }

  private enum Types {
    JULIAN, SCLK, TDB, TDBCALENDAR, UTC
  }

  private Map<Integer, SCLK> sclkMap;

  private TranslateTime(){}

  public TranslateTime(Map<Integer, SCLK> sclkMap) {
    this.sclkMap = sclkMap;
  }

  private TDBTime tdb;

  public String toJulian() throws SpiceErrorException {
    return tdb.toString("JULIAND.######");
  }

  private SCLK sclkKernel;

  public SCLK getSCLKKernel() {
    return sclkKernel;
  }

  public void setSCLKKernel(int sclkID) {
    sclkKernel = sclkMap.get(sclkID);
    if (sclkKernel == null) {
      logger.error("SCLK {} is not loaded!", sclkID);
    }
  }

  public SCLKTime toSCLK() throws SpiceException {
    return new SCLKTime(sclkKernel, tdb);
  }

  public TDBTime toTDB() {
    return tdb;
  }

  public String toUTC() throws SpiceErrorException {
    return tdb.toUTCString("ISOC", 3);
  }

  public void setJulianDate(double julianDate) throws SpiceErrorException {
    tdb = new TDBTime(String.format("%.6f JDUTC", julianDate));
  }

  public void setSCLK(String sclkString) throws SpiceException {
    tdb = new TDBTime(new SCLKTime(sclkKernel, sclkString));
  }

  public void setTDB(double tdb) {
    this.tdb = new TDBTime(tdb);
  }

  public void setTDBCalendarString(String tdbString) throws SpiceErrorException {
    tdb = new TDBTime(String.format("%s TDB", tdbString));
  }

  public void setUTC(String utcStr) throws SpiceErrorException {
    tdb = new TDBTime(utcStr);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("logFile").hasArg()
            .desc("If present, save screen output to log file.").build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values())
      sb.append(String.format("%s ", l.name()));
    options.addOption(Option.builder("logLevel").hasArg()
            .desc("If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim() + ".  Default is INFO.")
            .build());
    options.addOption(Option.builder("sclk").hasArg().desc(
                    "SPICE id of the sclk to use.  Default is to use the first one found in the kernel pool.")
            .build());
    options.addOption(Option.builder("spice").required().hasArg()
            .desc("Required.  SPICE metakernel containing leap second and SCLK.").build());
    options.addOption(Option.builder("gui").desc("Launch a GUI.").build());
    options.addOption(Option.builder("inputDate").hasArgs().desc("Date to translate.").build());
    sb = new StringBuilder();
    for (Types system : Types.values()) {
      sb.append(String.format("%s ", system.name()));
    }
    options.addOption(Option.builder("inputSystem").hasArg().desc(
                    "Timesystem of inputDate.  Valid values are " + sb.toString().trim() + ".  Default is UTC.")
            .build());    return options;
  }

  public static void main(String[] args) throws SpiceException {
    TerrasaurTool defaultOBJ = new TranslateTime();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    // This is to avoid java crashing due to inability to connect to an X display
    if (!cl.hasOption("gui"))
      System.setProperty("java.awt.headless", "true");

    NativeLibraryLoader.loadSpiceLibraries();

    for (String kernel : cl.getOptionValues("spice"))
      KernelDatabase.load(kernel);

    LinkedHashMap<Integer, SCLK> sclkMap = new LinkedHashMap<>();
    String[] sclk_data_type = KernelPool.getNames("SCLK_DATA_*");
    for (String s : sclk_data_type) {
      String[] parts = s.split("_");
      int sclkID = -Integer.parseInt(parts[parts.length - 1]);
      sclkMap.put(sclkID, new SCLK(sclkID));
    }

    SCLK sclk = null;
    if (cl.hasOption("sclk")) {
      int sclkID = Integer.parseInt(cl.getOptionValue("sclk"));
      if (sclkMap.containsKey(sclkID))
        sclk = sclkMap.get(sclkID);
      else {
        logger.error("Cannot find SCLK {} in kernel pool!", sclkID);
        StringBuilder    sb = new StringBuilder();
        for (Integer id : sclkMap.keySet())
          sb.append(String.format("%d ", id));
        logger.error("Loaded IDs are {}", sb.toString());
      }
    } else {
      if (!sclkMap.values().isEmpty())
        // set the SCLK to the first one found
        sclk = sclkMap.values().stream().toList().get(0);
    }

    if (sclk == null) {
      logger.fatal("Cannot load SCLK");
      System.exit(0);
    }

    TranslateTime tt = new TranslateTime(sclkMap);

    if (cl.hasOption("gui")) {
      TranslateTimeFX.setTranslateTime(tt);
      TranslateTimeFX.setSCLKIDs(sclkMap.keySet());
      TranslateTimeFX.main(args);
      System.exit(0);
    } else {
      if (!cl.hasOption("inputDate")) {
        logger.fatal("Missing required option -inputDate!");
        System.exit(1);
      }
      tt.setSCLKKernel(sclk.getIDCode());
    }

    StringBuilder sb = new StringBuilder();
    for (String s : cl.getOptionValues("inputDate"))
      sb.append(String.format("%s ", s));
    String inputDate = sb.toString().trim();

    Types type =
        cl.hasOption("inputSystem") ? Types.valueOf(cl.getOptionValue("inputSystem").toUpperCase())
            : Types.UTC;

    switch (type) {
      case JULIAN:
        tt.setJulianDate(Double.parseDouble(inputDate));
        break;
      case SCLK:
        tt.setSCLK(inputDate);
        break;
      case TDB:
        tt.setTDB(Double.parseDouble(inputDate));
        break;
      case TDBCALENDAR:
        tt.setTDBCalendarString(inputDate);
        break;
      case UTC:
        tt.setUTC(inputDate);
        break;
    }

    System.out.printf("# input date %s (%s)\n", inputDate, type.name());
    System.out.printf("# UTC, TDB (Calendar), DOY, TDB, Julian Date, SCLK (%d)\n",
        sclk.getIDCode());

    String utcString = tt.toTDB().toUTCString("ISOC", 3);
    String tdbString = tt.toTDB().toString("YYYY-MM-DDTHR:MN:SC.### ::TDB");
    String doyString = tt.toTDB().toString("DOY");

    System.out.printf("%s, %s, %s, %.6f, %s, %s\n", utcString, tdbString, doyString,
        tt.toTDB().getTDBSeconds(), tt.toJulian(), tt.toSCLK().toString());

  }
}
