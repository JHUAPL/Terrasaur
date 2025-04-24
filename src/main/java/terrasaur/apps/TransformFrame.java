package terrasaur.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.KernelDatabase;
import spice.basic.Matrix33;
import spice.basic.ReferenceFrame;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.TDBTime;
import spice.basic.Vector3;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.NativeLibraryLoader;
import terrasaur.utils.SPICEUtil;

public class TransformFrame implements TerrasaurTool {
  private static final Logger logger = LogManager.getLogger();


  @Override
  public String shortDescription() {
      return "Transform coordinates between reference frames.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer = "\nThis program transforms coordinates between reference frames.\n";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private NavigableMap<TDBTime, Vector3> pointsIn;
  private NavigableMap<TDBTime, Vector3> pointsOut;

  public TransformFrame() {}

  public void setPoints(NavigableMap<TDBTime, Vector3> pointsIn) {
    this.pointsIn = pointsIn;
  }

  public void transformCoordinates(String inFrame, String outFrame) {
    try {
      ReferenceFrame from = new ReferenceFrame(inFrame);
      ReferenceFrame to = new ReferenceFrame(outFrame);
      pointsOut = new TreeMap<>(SPICEUtil.tdbComparator);
      for (TDBTime t : pointsIn.keySet()) {
        Matrix33 transform = from.getPositionTransformation(to, t);
        pointsOut.put(t, transform.mxv(pointsIn.get(t)));
      }
    } catch (SpiceException e) {
      logger.error(e.getLocalizedMessage());
    }
  }

  public void write(String outFile) {

    try (PrintWriter pw = new PrintWriter(outFile)) {
      for (TDBTime t : pointsOut.keySet()) {
        Vector3 v = pointsOut.get(t);
        pw.printf("%.6f,%.6e,%.6e,%.6e\n", t.getTDBSeconds(), v.getElt(0), v.getElt(1),
            v.getElt(2));
      }
    } catch (FileNotFoundException | SpiceException e) {
      logger.error(e.getLocalizedMessage());
    }

  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(Option.builder("inFile").required().hasArg()
            .desc("Required.  Text file containing comma separated t, x, y, z values.  Time is ET.")
            .build());
    options.addOption(Option.builder("inFrame").required().hasArg()
            .desc("Required.  Name of inFile reference frame.").build());
    options.addOption(Option.builder("outFile").required().hasArg()
            .desc("Required.  Name of output file.  It will be in the same format as inFile.").build());
    options.addOption(Option.builder("outFrame").required().hasArg()
            .desc("Required.  Name of outFile reference frame.").build());
    options.addOption(Option.builder("spice").required().hasArg().desc(
                    "Required.  Name of SPICE metakernel containing kernels needed to make the frame transformation.")
            .build());
    options.addOption(Option.builder("logFile").hasArg()
            .desc("If present, save screen output to log file.").build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values())
      sb.append(String.format("%s ", l.name()));
    options.addOption(Option.builder("logLevel").hasArg()
            .desc("If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim() + ".  Default is INFO.")
            .build());    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new TransformFrame();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    TransformFrame tf = new TransformFrame();
    NavigableMap<TDBTime, Vector3> map = new TreeMap<>(SPICEUtil.tdbComparator);
    try {
      File f = new File(cl.getOptionValue("inFile"));
      List<String> lines = FileUtils.readLines(f, Charset.defaultCharset());
      for (String line : lines) {
        String trim = line.trim();
        if (trim.isEmpty() || trim.startsWith("#"))
          continue;
        String[] parts = trim.split(",");
        double et = Double.parseDouble(parts[0].trim());
        if (et > 0) {
          TDBTime t = new TDBTime(et);
          Vector3 v = new Vector3(Double.parseDouble(parts[1].trim()),
              Double.parseDouble(parts[2].trim()), Double.parseDouble(parts[3].trim()));
          map.put(t, v);
        }
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage());
    }

    tf.setPoints(map);

    NativeLibraryLoader.loadSpiceLibraries();
    try {
      KernelDatabase.load(cl.getOptionValue("spice"));
    } catch (SpiceErrorException e) {
      logger.error(e.getLocalizedMessage());
    }

    tf.transformCoordinates(cl.getOptionValue("inFrame"), cl.getOptionValue("outFrame"));
    tf.write(cl.getOptionValue("outFile"));
  }

}
