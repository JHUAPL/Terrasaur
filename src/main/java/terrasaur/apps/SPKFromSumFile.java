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
package terrasaur.apps;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.intervals.UnwritableInterval;
import spice.basic.*;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import terrasaur.utils.math.MathConversions;

public class SPKFromSumFile implements TerrasaurTool {

    private final static Logger logger = LogManager.getLogger();

    @Override
    public String shortDescription() {
        return "Given three or more sumfiles, create an input file for MKSPK.";
    }

    @Override
    public String fullDescription(Options options) {
        String header = "";
        String footer = """
                Given three or more sumfiles, fit a parabola to the spacecraft
                trajectory in J2000 and create an input file for MKSPK.
                """;
        return TerrasaurTool.super.fullDescription(options, header, footer);
    }

    private Body observer;
    private Body target;
    private ReferenceFrame J2000;
    private ReferenceFrame bodyFixed;
    private NavigableMap<Double, SumFile> sumFiles;
    private Map<SumFile, Double> weightMap;
    private NavigableMap<Double, String> sumFilenames;
    private UnwritableInterval interval;

    private SPKFromSumFile(){}

    private SPKFromSumFile(Body observer, Body target, ReferenceFrame bodyFixed, Map<String, Double> weightMap,
                           double extend) throws SpiceException {
        this.observer = observer;
        this.target = target;
        this.bodyFixed = bodyFixed;
        this.J2000 = new ReferenceFrame("J2000");

        this.sumFiles = new TreeMap<>();
        this.weightMap = new HashMap<>();
        this.sumFilenames = new TreeMap<>();

        for (String filename : weightMap.keySet()) {
            SumFile s = SumFile.fromFile(new File(filename));
            double tdb = new TDBTime(s.utcString()).getTDBSeconds();
            this.sumFiles.put(tdb, s);
            this.weightMap.put(s, weightMap.get(filename));
            this.sumFilenames.put(tdb, filename);
        }

        this.interval = new UnwritableInterval(this.sumFiles.firstKey(), this.sumFiles.lastKey() + extend);
    }

    /**
     * @param basename        base name for MKSPK input files
     * @param comments        comments to include in SPK
     * @param degree          polynomial degree to use for fitting position
     * @param velocity        User-supplied velocity (if null, use derivative of calculated fit to position)
     * @param velocityIsJ2000 if true, user-supplied velocity is in J2000 frame
     * @return command to run MKSPK
     */
    public String writeMKSPKFiles(String basename, List<String> comments, int degree, final Vector3 velocity,
                                  boolean velocityIsJ2000) throws SpiceException {

        String commentFile = basename + "-comments.txt";
        String setupFile = basename + ".setup";
        String inputFile = basename + ".inp";

        try (PrintWriter pw = new PrintWriter(commentFile)) {
            StringBuilder sb = new StringBuilder();
            if (!comments.isEmpty()) {
                for (String comment : comments)
                    sb.append(comment).append("\n");
                sb.append("\n");
            }
            sb.append(String.format("This SPK for %s was generated by fitting a parabola to each component of the " + "SCOBJ vector from " + "the following sumfiles:\n", target));
            for (String sumFile : sumFilenames.values()) {
                sb.append(String.format("\t%s\n", sumFile));
            }
            sb.append("The SCOBJ vector was transformed to J2000 and an aberration correction ");
            sb.append(String.format("was applied to find the geometric position relative to %s before the parabola " + "fit.  ", target.getName()));
            sb.append(String.format("The period covered by this SPK is %s to %s.",
                    new TDBTime(interval.getBegin()).toUTCString("ISOC", 3),
                    new TDBTime(interval.getEnd()).toUTCString("ISOC", 3)));

            String allComments = sb.toString();
            for (String comment : allComments.split("\\r?\\n"))
                pw.println(WordUtils.wrap(comment, 80));
        } catch (FileNotFoundException e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        int numTXT = KernelDatabase.ktotal("TEXT");
        File lsk = null;
        for (int i = 0; i < numTXT; i++) {
            String filename = KernelDatabase.getFileName(i, "TEXT");
            if (filename.toLowerCase().endsWith(".tls")) lsk = new File(filename);
        }

        Map<String, String> map = new TreeMap<>();
        map.put("INPUT_DATA_TYPE", "'STATES'");
        map.put("OUTPUT_SPK_TYPE", "13"); // hermite polynomial, unevenly spaced in time
        map.put("OBJECT_ID", String.format("%d", observer.getIDCode()));
        map.put("CENTER_ID", String.format("%d", target.getIDCode()));
        map.put("COMMENT_FILE", String.format("'%s'", commentFile));
        map.put("REF_FRAME_NAME", "'J2000'");
        map.put("PRODUCER_ID", "'Hari.Nair@jhuapl.edu'");
        map.put("DATA_ORDER", "'EPOCH X Y Z VX VY VZ'");
        map.put("DATA_DELIMITER", "' '");
        map.put("LEAPSECONDS_FILE", String.format("'%s'", lsk));
        map.put("TIME_WRAPPER", "'# ETSECONDS'");
        map.put("POLYNOM_DEGREE", "7");
        map.put("SEGMENT_ID", "'SPK_STATES_13'");
        map.put("LINES_PER_RECORD", "1");
        try (PrintWriter pw = new PrintWriter(setupFile)) {
            pw.println("\\begindata");
            for (String key : map.keySet()) {
                pw.printf("%s = %s\n", key, map.get(key));
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        RemoveAberration ra = new RemoveAberration(target, observer);

        WeightedObservedPoints x = new WeightedObservedPoints();
        WeightedObservedPoints y = new WeightedObservedPoints();
        WeightedObservedPoints z = new WeightedObservedPoints();
        Map<Double, Vector3> geometricMap = new HashMap<>();
        for (Double t : sumFiles.keySet()) {
            SumFile sumFile = sumFiles.get(t);
            double weight = weightMap.get(sumFile);
            TDBTime tdb = new TDBTime(t);
            Matrix33 bodyFixedToJ2000 = bodyFixed.getPositionTransformation(J2000, tdb);
            Vector3 scObjJ2000 = bodyFixedToJ2000.mxv(MathConversions.toVector3(sumFile.scobj()));
            Vector3 geometricScPos = ra.getGeometricPosition(tdb, scObjJ2000);
            geometricMap.put(t, geometricScPos);
            x.add(weight, t, geometricScPos.getElt(0));
            y.add(weight, t, geometricScPos.getElt(1));
            z.add(weight, t, geometricScPos.getElt(2));
        }

        // fit a polynomial to the geometric positions in J2000
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        double[] xCoeff = fitter.fit(x.toList());
        double[] yCoeff = fitter.fit(y.toList());
        double[] zCoeff = fitter.fit(z.toList());

        PolynomialFunction xPos = new PolynomialFunction(xCoeff);
        PolynomialFunction xVel = xPos.polynomialDerivative();
        PolynomialFunction yPos = new PolynomialFunction(yCoeff);
        PolynomialFunction yVel = yPos.polynomialDerivative();
        PolynomialFunction zPos = new PolynomialFunction(zCoeff);
        PolynomialFunction zVel = zPos.polynomialDerivative();

        logger.info("Polynomial fitting coefficients for geometric position of {} relative to {} in J2000:",
                observer.getName(), target.getName());
        StringBuilder xMsg = new StringBuilder(String.format("X = %e ", xCoeff[0]));
        StringBuilder yMsg = new StringBuilder(String.format("Y = %e ", yCoeff[0]));
        StringBuilder zMsg = new StringBuilder(String.format("Z = %e ", zCoeff[0]));
        for (int i = 1; i <= degree; i++) {
            xMsg.append(xCoeff[i] < 0 ? "- " : "+ ").append(String.format("%e ", Math.abs(xCoeff[i]))).append("t").append(i > 1 ? "^" + i : "").append(" ");
            yMsg.append(yCoeff[i] < 0 ? "- " : "+ ").append(String.format("%e ", Math.abs(yCoeff[i]))).append("t").append(i > 1 ? "^" + i : "").append(" ");
            zMsg.append(zCoeff[i] < 0 ? "- " : "+ ").append(String.format("%e ", Math.abs(zCoeff[i]))).append("t").append(i > 1 ? "^" + i : "").append(" ");
        }
        logger.info(xMsg);
        logger.info(yMsg);
        logger.info(zMsg);

        logger.debug("");
        logger.debug("NOTE: comparing aberration correction=LT+S positions from sumfile with aberration " +
                "correction=NONE for fit.");
        for (Double t : sumFiles.keySet()) {
            TDBTime tdb = new TDBTime(t);
            SumFile sumFile = sumFiles.get(t);
            Vector3 j2000Pos = new Vector3(xPos.value(t), yPos.value(t), zPos.value(t));
            Matrix33 bodyFixedToJ2000 = bodyFixed.getPositionTransformation(J2000, tdb);
            Vector3D bfPos = MathConversions.toVector3D(bodyFixedToJ2000.mtxv(j2000Pos));
            // comparing LT+S with NONE here
            logger.debug("UTC Date:      {}", sumFile.utcString());
            logger.debug("Sumfile SCOBJ: {}", sumFile.scobj());
            logger.debug("Fit:           {}", bfPos);
            logger.debug("residual (m):  {}", sumFile.scobj().subtract(bfPos).scalarMultiply(1000));
            logger.debug("");
        }

        try (PrintWriter pw = new PrintWriter(inputFile)) {
            for (double t = interval.getBegin(); t < interval.getEnd(); t++) {

                if (velocity == null) {
                    double vx = -xVel.value(t);
                    double vy = -yVel.value(t);
                    double vz = -zVel.value(t);
                    pw.printf("%.16e %.16e %.16e %.16e %.16e %.16e %.16e\n", t, -xPos.value(t), -yPos.value(t),
                            -zPos.value(t), vx, vy, vz);
                } else {
                    Vector3 thisVelocity = new Vector3(velocity);
                    if (!velocityIsJ2000) {
                        TDBTime tdb = new TDBTime(t);
                        thisVelocity = bodyFixed.getPositionTransformation(J2000, tdb).mxv(velocity);
                    }
                    double vx = thisVelocity.getElt(0);
                    double vy = thisVelocity.getElt(1);
                    double vz = thisVelocity.getElt(2);
                    pw.printf("%.16e %.16e %.16e %.16e %.16e %.16e %.16e\n", t, -xPos.value(t), -yPos.value(t),
                            -zPos.value(t), vx, vy, vz);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getLocalizedMessage(), e);
        }


        try (PrintWriter pw = new PrintWriter(basename + ".csv")) {
            pw.println("# Note: fit quantities are without light time or aberration corrections");
            pw.println("# SCOBJ");
            pw.println("# UTC, TDB, SumFile, SPICE (body fixed) x, y, z, SCOBJ (body fixed) x, y, z, SCOBJ (J2000) x," +
                    " y, z, SCOBJ (Geometric J2000) x, y, z, Fit SCOBJ (body fixed) x, y, z, Fit SCOBJ (Geometric " +
                    "J2000) x, y, z");
            for (Double t : sumFiles.keySet()) {
                SumFile sumFile = sumFiles.get(t);
                pw.printf("%s,", sumFile.utcString());
                pw.printf("%.3f, ", t);
                pw.printf("%s, ", FilenameUtils.getBaseName(sumFilenames.get(t)));

                TDBTime tdb = new TDBTime(t);
                StateRecord sr = new StateRecord(observer, tdb, bodyFixed, new AberrationCorrection("LT+S"), target);

                pw.printf("%s, ", sr.getPosition().getElt(0));
                pw.printf("%s, ", sr.getPosition().getElt(1));
                pw.printf("%s, ", sr.getPosition().getElt(2));

                pw.printf("%s, ", sumFile.scobj().getX());
                pw.printf("%s, ", sumFile.scobj().getY());
                pw.printf("%s, ", sumFile.scobj().getZ());

                Matrix33 j2000ToBodyFixed = J2000.getPositionTransformation(bodyFixed, tdb);
                Vector3 scobjJ2000 = j2000ToBodyFixed.mtxv(MathConversions.toVector3(sumFile.scobj()));
                pw.printf("%s, ", scobjJ2000.getElt(0));
                pw.printf("%s, ", scobjJ2000.getElt(1));
                pw.printf("%s, ", scobjJ2000.getElt(2));

                Vector3 geometricScPos = geometricMap.get(t);
                pw.printf("%s, ", geometricScPos.getElt(0));
                pw.printf("%s, ", geometricScPos.getElt(1));
                pw.printf("%s, ", geometricScPos.getElt(2));

                Vector3 fitScOBJJ2000 = new Vector3(xPos.value(t), yPos.value(t), zPos.value(t));
                Vector3 fitScOBJ = j2000ToBodyFixed.mxv(fitScOBJJ2000);
                pw.printf("%s, ", fitScOBJ.getElt(0));
                pw.printf("%s, ", fitScOBJ.getElt(1));
                pw.printf("%s, ", fitScOBJ.getElt(2));

                pw.printf("%s, ", fitScOBJJ2000.getElt(0));
                pw.printf("%s, ", fitScOBJJ2000.getElt(1));
                pw.printf("%s, ", fitScOBJJ2000.getElt(2));
                pw.println();
            }
            pw.println("\n# Velocity");
            pw.println("# UTC, TDB, SumFile, SPICE (body fixed) x, y, z, SPICE (J2000) x, y, z, Fit (body fixed) x, " +
                    "y, z, Fit (J2000) x, y, z");
            for (Double t : sumFiles.keySet()) {
                SumFile sumFile = sumFiles.get(t);
                pw.printf("%s,", sumFile.utcString());
                pw.printf("%.3f, ", t);
                pw.printf("%s, ", FilenameUtils.getBaseName(sumFilenames.get(t)));

                TDBTime tdb = new TDBTime(t);

                Matrix66 j2000ToBodyFixed = J2000.getStateTransformation(bodyFixed, tdb);
                StateRecord sr = new StateRecord(observer, tdb, J2000, new AberrationCorrection("NONE"), target);
                Vector3 velJ2000 = sr.getVelocity();
                Vector3 velBodyFixed = j2000ToBodyFixed.mxv(sr.getStateVector()).getVector3(1);

                pw.printf("%s, ", velBodyFixed.getElt(0));
                pw.printf("%s, ", velBodyFixed.getElt(1));
                pw.printf("%s, ", velBodyFixed.getElt(2));

                pw.printf("%s, ", velJ2000.getElt(0));
                pw.printf("%s, ", velJ2000.getElt(1));
                pw.printf("%s, ", velJ2000.getElt(2));

                velJ2000 = new Vector3(xVel.value(t), yVel.value(t), zVel.value(t));
                if (velocity != null) {
                    Vector3 thisVelocity = new Vector3(velocity);
                    if (!velocityIsJ2000) {
                        thisVelocity = bodyFixed.getPositionTransformation(J2000, tdb).mxv(velocity);
                    }
                    double vx = thisVelocity.getElt(0);
                    double vy = thisVelocity.getElt(1);
                    double vz = thisVelocity.getElt(2);
                    velJ2000 = new Vector3(vx, vy, vz);
                }

                StateVector stateJ2000 = new StateVector(new Vector3(xPos.value(t), yPos.value(t), zPos.value(t)),
                        velJ2000);
                velBodyFixed = j2000ToBodyFixed.mxv(stateJ2000).getVector3(1);

                pw.printf("%s, ", velBodyFixed.getElt(0));
                pw.printf("%s, ", velBodyFixed.getElt(1));
                pw.printf("%s, ", velBodyFixed.getElt(2));

                pw.printf("%s, ", velJ2000.getElt(0));
                pw.printf("%s, ", velJ2000.getElt(1));
                pw.printf("%s, ", velJ2000.getElt(2));
                pw.println();
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        return String.format("mkspk -setup %s -input %s -output %s.bsp", setupFile, inputFile, basename);
    }

    private static Options defineOptions() {
        Options options = TerrasaurTool.defineOptions();
        options.addOption(Option.builder("degree").hasArg().desc("Degree of polynomial used to fit sumFile locations" + "." + "  Default is 2.").build());
        options.addOption(Option.builder("extend").hasArg().desc("Extend SPK past the last sumFile by <arg> seconds. "
                + " Default is zero.").build());
        options.addOption(Option.builder("frame").hasArg().desc("Name of body fixed frame.  This will default to the "
                + "target's body fixed frame.").build());
        options.addOption(Option.builder("logFile").hasArg().desc("If present, save screen output to log file.").build());
        StringBuilder sb = new StringBuilder();
        for (StandardLevel l : StandardLevel.values())
            sb.append(String.format("%s ", l.name()));
        options.addOption(Option.builder("logLevel").hasArg().desc("If present, print messages above selected " +
                "priority.  Valid values are " + sb.toString().trim() + ".  Default is INFO.").build());
        options.addOption(Option.builder("observer").required().hasArg().desc("Required.  SPICE ID for the observer.").build());
        options.addOption(Option.builder("sumFile").hasArg().required().desc("""
                File listing sumfiles to read.  This is a text file,
                one per line.  You can include an optional weight
                after each filename.  The default weight is 1.0.
                Lines starting with # are ignored.

                Example:

                D717506120G0.SUM
                D717506126G0.SUM
                D717506127G0.SUM
                D717506128G0.SUM
                D717506129G0.SUM
                D717506131G0.SUM
                # Weight this last image less than the others
                D717506132G0.SUM 0.25
                """).build());
        options.addOption(Option.builder("spice").required().hasArgs().desc("Required.  SPICE metakernel file " +
                "containing body fixed frame and spacecraft kernels.  Can specify more than one kernel, separated by "
                + "whitespace.").build());
        options.addOption(Option.builder("target").required().hasArg().desc("Required.  SPICE ID for the target.").build());
        options.addOption(Option.builder("velocity").hasArgs().desc("Spacecraft velocity relative to target in the " + "body fixed frame.  If present, use this fixed velocity in the MKSPK input file.  Default is to " + "take the derivative of the fit position.  Specify as three floating point values in km/sec," + "separated by whitespace.").build());
        options.addOption(Option.builder("velocityJ2000").desc("If present, argument to -velocity is in J2000 frame. "
                + " Ignored if -velocity is not set.").build());        return options;
    }

    public static void main(String[] args) throws SpiceException {
        TerrasaurTool defaultOBJ = new SPKFromSumFile();

        Options options = defineOptions();

        CommandLine cl = defaultOBJ.parseArgs(args, options);

        Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
        for (MessageLabel ml : startupMessages.keySet())
            logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

        NativeLibraryLoader.loadSpiceLibraries();


        final double extend = cl.hasOption("extend") ? Double.parseDouble(cl.getOptionValue("extend")) : 0;

        for (String kernel : cl.getOptionValues("spice"))
            KernelDatabase.load(kernel);

        Body observer = new Body(cl.getOptionValue("observer"));
        Body target = new Body(cl.getOptionValue("target"));

        ReferenceFrame bodyFixed = null;
        if (cl.hasOption("frame")) {
            bodyFixed = new ReferenceFrame(cl.getOptionValue("frame"));
        } else {
            String keyword = String.format("OBJECT_%d_FRAME", target.getIDCode());
            if (KernelPool.exists(keyword)) {
                bodyFixed = new ReferenceFrame(KernelPool.getCharacter(keyword)[0]);
            } else {
                logger.error("No keyword {} in kernel pool and -frame was not specified!", keyword);
            }
        }

        final int degree = cl.hasOption("degree") ? Integer.parseInt(cl.getOptionValue("degree")) : 2;
        Vector3 velocity = null;
        if (cl.hasOption("velocity")) {
            String[] parts = cl.getOptionValues("velocity");
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                velocity = new Vector3(x, y, z);
            }
        }
        boolean velocityJ2000 = cl.hasOption("velocityJ2000");

        Map<String, Double> weightMap = new HashMap<>();
        String first = null;
        String last = null;
        try {
            List<String> lines = FileUtils.readLines(new File(cl.getOptionValue("sumFile")), Charset.defaultCharset());
            for (String line : lines) {
                if (line.strip().startsWith("#")) continue;
                String[] parts = line.strip().split("\\s+");
                double weight = 1.0;
                if (parts.length > 1) weight = Double.parseDouble(parts[1]);
                weightMap.put(parts[0], weight);
                if (first == null) first = parts[0];
                last = parts[0];
            }
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        SPKFromSumFile app = new SPKFromSumFile(observer, target, bodyFixed, weightMap, extend);
        String basename = String.format("%s_%s", FilenameUtils.getBaseName(first), FilenameUtils.getBaseName(last));
        List<String> comments = new ArrayList<>();
        String command = app.writeMKSPKFiles(basename, comments, degree, velocity, velocityJ2000);

        logger.info("Generate new SPK:\n\t{}", command);

        logger.info("Finished.");
    }

}
