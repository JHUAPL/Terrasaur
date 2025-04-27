package terrasaur.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import spice.basic.Body;
import spice.basic.CSPICE;
import spice.basic.DLADescriptor;
import spice.basic.DSK;
import spice.basic.DSKDescriptor;
import spice.basic.SpiceException;
import spice.basic.Surface;
import terrasaur.templates.TerrasaurTool;

public class DSK2OBJ implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Create an OBJ from a DSK.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer = "\nCreate an OBJ from a DSK.\n";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
    options.addOption(
        Option.builder("body")
            .hasArg()
            .desc(
                "If present, convert shape for named body.  Default is to use the first body in the DSK.")
            .build());
    options.addOption(
        Option.builder("dsk").hasArg().required().desc("Required.  Name of input DSK.").build());
    options.addOption(
        Option.builder("logFile")
            .hasArg()
            .desc("If present, save screen output to log file.")
            .build());
    StringBuilder sb = new StringBuilder();
    for (StandardLevel l : StandardLevel.values()) sb.append(String.format("%s ", l.name()));
    options.addOption(
        Option.builder("logLevel")
            .hasArg()
            .desc(
                "If present, print messages above selected priority.  Valid values are "
                    + sb.toString().trim()
                    + ".  Default is INFO.")
            .build());
    options.addOption(Option.builder("obj").hasArg().desc("Name of output OBJ.").build());
    options.addOption(
        Option.builder("printBodies")
            .desc("If present, print bodies and surface ids in DSK.")
            .build());
    options.addOption(
        Option.builder("surface")
            .hasArg()
            .desc(
                "If present, use specified surface id.  Default is to use the first surface id for the body.")
            .build());
    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new DSK2OBJ();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    System.loadLibrary("JNISpice");

    String dskName = cl.getOptionValue("dsk");
    File dskFile = new File(dskName);
    if (!dskFile.exists()) {
      logger.warn("Input DSK " + dskName + "does not exist!");
      System.exit(0);
    }

    try {
      DSK dsk = DSK.openForRead(dskName);
      Body[] bodies = dsk.getBodies();

      if (cl.hasOption("printBodies")) {
        logger.info("found bodies and surface ids:");
        for (int i = 0; i < bodies.length; i++) {
          Body b = bodies[i];
          Surface[] surfaces = dsk.getSurfaces(b);
          StringBuilder sb = new StringBuilder();
          sb.append(String.format("%d) %s", i, b.getName()));
          for (Surface s : surfaces) sb.append(String.format(" %d", s.getIDCode()));
          logger.info(sb.toString());
        }
      }

      Body b = cl.hasOption("body") ? new Body(cl.getOptionValue("body")) : bodies[0];
      boolean missingBody = true;
      for (Body body : bodies) {
        if (b.equals(body)) {
          missingBody = false;
          break;
        }
      }
      if (missingBody) {
        logger.warn(String.format("Body %s not found in DSK!  Valid bodies are:", b.getName()));
        for (Body body : bodies) logger.warn(body.getName());
        System.exit(0);
      }

      Surface[] surfaces = dsk.getSurfaces(b);
      Surface s =
          cl.hasOption("surface")
              ? new Surface(Integer.parseInt(cl.getOptionValue("surface")), b)
              : surfaces[0];
      boolean missingSurface = true;
      for (Surface surface : surfaces) {
        if (s.equals(surface)) {
          missingSurface = false;
          break;
        }
      }
      if (missingSurface) {
        logger.warn(
            String.format(
                "Surface %d for body %s not found in DSK!  Valid surfaces are:",
                s.getIDCode(), b.getName()));
        for (Surface surface : surfaces) logger.warn(Integer.toString(surface.getIDCode()));
        System.exit(0);
      }

      DLADescriptor dladsc = dsk.beginBackwardSearch();
      boolean found = true;
      while (found) {

        DSKDescriptor dskdsc = dsk.getDSKDescriptor(dladsc);

        if (b.getIDCode() == dskdsc.getCenterID() && s.getIDCode() == dskdsc.getSurfaceID()) {

          // number of plates and vertices
          int[] np = new int[1];
          int[] nv = new int[1];
          CSPICE.dskz02(dsk.getHandle(), dladsc.toArray(), nv, np);

          double[][] vertices = CSPICE.dskv02(dsk.getHandle(), dladsc.toArray(), 1, nv[0]);
          int[][] plates = CSPICE.dskp02(dsk.getHandle(), dladsc.toArray(), 1, np[0]);

          if (cl.hasOption("obj")) {
            try (PrintWriter pw = new PrintWriter(cl.getOptionValue("obj"))) {
              for (double[] v : vertices) {
                pw.printf("v %20.16f %20.16f %20.16f\r\n", v[0], v[1], v[2]);
              }
              for (int[] p : plates) {
                pw.printf("f %d %d %d\r\n", p[0], p[1], p[2]);
              }
              logger.info(
                  String.format(
                      "Wrote %d vertices and %d plates to %s for body %d surface %d",
                      nv[0],
                      np[0],
                      cl.getOptionValue("obj"),
                      dskdsc.getCenterID(),
                      dskdsc.getSurfaceID()));
            } catch (FileNotFoundException e) {
              logger.warn(e.getLocalizedMessage());
            }
          }
        }

        found = dsk.hasPrevious(dladsc);
        if (found) {
          dladsc = dsk.getPrevious(dladsc);
        }
      }

    } catch (SpiceException e) {
      logger.warn(e.getLocalizedMessage());
    }
  }
}
