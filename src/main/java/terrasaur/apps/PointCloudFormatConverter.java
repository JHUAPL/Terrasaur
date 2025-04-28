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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import picante.math.vectorspace.UnwritableVectorIJK;
import spice.basic.Vector3;
import terrasaur.enums.FORMATS;
import terrasaur.smallBodyModel.BoundingBox;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.*;
import vtk.vtkCellArray;
import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

public class PointCloudFormatConverter implements TerrasaurTool {

  private static final Logger logger = LogManager.getLogger();

  @Override
  public String shortDescription() {
    return "Convert an input point cloud to a new format.";
  }

  @Override
  public String fullDescription(Options options) {
    String header = "";
    String footer =
        """
            This program converts an input point cloud to a new format.

            Supported input formats are ASCII, BIN3 (x,y,z), BIN4 (x, y, z, w), BIN7 (t, x, y, z, s/c x, y, z), FITS, ICQ, OBJ, PLY, and VTK.  Supported output formats are ASCII, BIN3, OBJ, and VTK.

            ASCII format is white spaced delimited x y z coordinates.  BINARY files must contain double precision x y z coordinates.""";
    return TerrasaurTool.super.fullDescription(options, header, footer);
  }

  private FORMATS inFormat;
  private FORMATS outFormat;
  private vtkPoints pointsXYZ;
  // private List<Double> receivedIntensity;
  private vtkPolyData polyData;
  private Vector3 center;
  private int halfSize;
  private double groundSampleDistance;
  private double clip;
  private String additionalGMTArgs;
  private double mapRadius;

  public static vtkPoints readPointCloud(String filename) {
    PointCloudFormatConverter pcfc = new PointCloudFormatConverter(filename, FORMATS.VTK);
    pcfc.read(filename, false);
    return pcfc.getPoints();
  }

  private PointCloudFormatConverter() {}

  public PointCloudFormatConverter(FORMATS inFormat, String outFilename) {
    this(inFormat, FORMATS.formatFromExtension(outFilename));
  }

  public PointCloudFormatConverter(String inFilename, FORMATS outFormat) {
    this(FORMATS.formatFromExtension(inFilename), outFormat);
  }

  public PointCloudFormatConverter(String inFilename, String outFilename) {
    this(FORMATS.formatFromExtension(inFilename), FORMATS.formatFromExtension(outFilename));
  }

  public PointCloudFormatConverter(FORMATS inFormat, FORMATS outFormat) {
    this.inFormat = inFormat;
    this.outFormat = outFormat;
    this.pointsXYZ = new vtkPoints();
    this.polyData = null;

    this.center = null;
    this.mapRadius = Math.sqrt(2);
    this.halfSize = -1;
    this.groundSampleDistance = -1;
    this.clip = 1;
    this.additionalGMTArgs = "";
  }

  public PointCloudFormatConverter setPoints(vtkPoints pointsXYZ) {
    this.pointsXYZ = pointsXYZ;
    return this;
  }

  public vtkPoints getPoints() {
    return pointsXYZ;
  }

  public void setClip(Double clip) {
    this.clip = clip;
  }

  public void setCenter(double[] centerPt) {
    center = new Vector3(centerPt);
  }

  public void setMapRadius(double mapRadius) {
    this.mapRadius = mapRadius;
  }

  public PointCloudFormatConverter setHalfSize(int halfSize) {
    this.halfSize = halfSize;
    return this;
  }

  public PointCloudFormatConverter setGroundSampleDistance(double groundSampleDistance) {
    this.groundSampleDistance = groundSampleDistance;
    return this;
  }

  public PointCloudFormatConverter setGMTArgs(String args) {
    this.additionalGMTArgs = args;
    return this;
  }

  public void read(String inFile, boolean inLLR) {
    switch (inFormat) {
      case ASCII:
        try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
          String line = br.readLine();
          while (line != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
              String[] parts = line.split("\\s+");
              if (inLLR) {
                double lon = Math.toRadians(Double.parseDouble(parts[0].trim()));
                double lat = Math.toRadians(Double.parseDouble(parts[1].trim()));
                double range = Double.parseDouble(parts[2].trim());
                double[] xyz = new Vector3D(lon, lat).scalarMultiply(range).toArray();
                pointsXYZ.InsertNextPoint(xyz);
              } else {
                double[] xyz = new double[3];
                xyz[0] = Double.parseDouble(parts[0].trim());
                xyz[1] = Double.parseDouble(parts[1].trim());
                xyz[2] = Double.parseDouble(parts[2].trim());
                pointsXYZ.InsertNextPoint(xyz);
              }
            }
            line = br.readLine();
          }
        } catch (IOException e) {
          logger.error(e.getLocalizedMessage(), e);
        }
        break;
      case BIN3:
      case BIN4:
      case BIN7:
        try (DataInputStream dis =
            new DataInputStream(new BufferedInputStream(new FileInputStream(inFile)))) {
          while (dis.available() > 0) {
            if (inFormat == FORMATS.BIN7) {
              // skip time field
              BinaryUtils.readDoubleAndSwap(dis);
            }
            if (inLLR) {
              double lon = Math.toRadians(BinaryUtils.readDoubleAndSwap(dis));
              double lat = Math.toRadians(BinaryUtils.readDoubleAndSwap(dis));
              double range = BinaryUtils.readDoubleAndSwap(dis);
              double[] xyz = new Vector3D(lon, lat).scalarMultiply(range).toArray();
              pointsXYZ.InsertNextPoint(xyz);
            } else {
              double[] xyz = new double[3];
              xyz[0] = BinaryUtils.readDoubleAndSwap(dis);
              xyz[1] = BinaryUtils.readDoubleAndSwap(dis);
              xyz[2] = BinaryUtils.readDoubleAndSwap(dis);
              pointsXYZ.InsertNextPoint(xyz);
            }
          }
        } catch (IOException e) {
          logger.error(e.getLocalizedMessage(), e);
        }
      case ICQ:
      case OBJ:
      case PLT:
      case PLY:
      case VTK:
        try {
          polyData = PolyDataUtil.loadShapeModel(inFile);
          pointsXYZ.DeepCopy(polyData.GetPoints());
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
        }
        break;
      case FITS:
        try {
          polyData = PolyDataUtil.loadFITShapeModel(inFile);
          pointsXYZ.DeepCopy(polyData.GetPoints());
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
        }
        break;
      default:
        break;
    }

    if (clip != 1) {
      BoundingBox bbox = new BoundingBox();
      for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
        double[] point = pointsXYZ.GetPoint(i);
        bbox.update(new UnwritableVectorIJK(point[0], point[1], point[2]));
      }
      BoundingBox clipped = bbox.getScaledBoundingBox(clip);
      vtkPoints clippedPoints = new vtkPoints();
      for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
        if (clipped.contains(pointsXYZ.GetPoint(i)))
          clippedPoints.InsertNextPoint(pointsXYZ.GetPoint(i));
      }
      pointsXYZ = clippedPoints;
      polyData = null;
    }
  }

  public void write(String outFile, boolean outLLR) {
    switch (outFormat) {
      case ASCII:
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))) {
          for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
            double[] thisPoint = pointsXYZ.GetPoint(i);
            if (outLLR) {
              Vector3D v = new Vector3D(thisPoint);
              out.printf(
                  "%f %f %f\n",
                  Math.toDegrees(v.getAlpha()), Math.toDegrees(v.getDelta()), v.getNorm());
            } else {
              out.printf("%f %f %f\n", thisPoint[0], thisPoint[1], thisPoint[2]);
            }
          }
        } catch (IOException e) {
          logger.error(e.getLocalizedMessage(), e);
        }
        break;
      case BIN3:
        try (DataOutputStream os =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {

          for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
            double[] thisPoint = pointsXYZ.GetPoint(i);
            if (outLLR) {
              Vector3D v = new Vector3D(thisPoint);

              BinaryUtils.writeDoubleAndSwap(os, Math.toDegrees(v.getAlpha()));
              BinaryUtils.writeDoubleAndSwap(os, Math.toDegrees(v.getDelta()));
              BinaryUtils.writeDoubleAndSwap(os, v.getNorm());
            } else {
              for (int ii = 0; ii < 3; ii++) BinaryUtils.writeDoubleAndSwap(os, thisPoint[ii]);
            }
          }
        } catch (IOException e) {
          logger.error(e.getLocalizedMessage(), e);
        }

        break;
      case OBJ:
        if (polyData != null) {
          try {
            PolyDataUtil.saveShapeModelAsOBJ(polyData, outFile);
          } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
          }
        } else {
          if (halfSize < 0 || groundSampleDistance < 0) {
            System.out.printf(
                "Must supply -halfSize and -groundSampleDistance for %s output\n",
                    outFormat);
            return;
          }

          final double radius = mapRadius * halfSize * groundSampleDistance;
          vtkPoints vtkPoints = pointsXYZ;
          if (center != null) {
            vtkPoints = new vtkPoints();
            for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
              Vector3 pt = new Vector3(pointsXYZ.GetPoint(i));
              if (center.sub(new Vector3(pt)).norm() > radius) continue;
              vtkPoints.InsertNextPoint(pt.toArray());
            }
          }

          PointCloudToPlane pctp = new PointCloudToPlane(vtkPoints, halfSize, groundSampleDistance);
          pctp.getGMU().setFieldToHeight();
          pctp.getGMU().setGMTArgs(additionalGMTArgs);
          try {
            double[][][] regridField = pctp.getGMU().regridField();
            vtkPolyData griddedXYZ = PolyDataUtil.loadLocalFitsLLRModelN(regridField);
            PolyDataUtil.saveShapeModelAsOBJ(griddedXYZ, outFile);
          } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
          }
        }
        break;
      case VTK:
        if (polyData == null) {
          polyData = new vtkPolyData();
          polyData.SetPoints(pointsXYZ);
        }

        vtkCellArray cells = new vtkCellArray();
        vtkFloatArray albedo = new vtkFloatArray();
        albedo.SetName("albedo");
        polyData.SetPolys(cells);
        polyData.GetPointData().AddArray(albedo);

        for (int i = 0; i < pointsXYZ.GetNumberOfPoints(); i++) {
          vtkIdList idList = new vtkIdList();
          idList.InsertNextId(i);
          cells.InsertNextCell(idList);
          albedo.InsertNextValue(0.5f);
        }

        vtkPolyDataWriter writer = new vtkPolyDataWriter();
        writer.SetInputData(polyData);
        writer.SetFileName(outFile);
        writer.SetFileTypeToBinary();
        writer.Update();
        break;
      default:
        break;
    }
  }

  private static Options defineOptions() {
    Options options = TerrasaurTool.defineOptions();
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
    options.addOption(
        Option.builder("inputFormat")
            .hasArg()
            .desc(
                "Format of input file.  If not present format will be inferred from inputFile extension.")
            .build());
    options.addOption(
        Option.builder("inputFile")
            .required()
            .hasArg()
            .desc("Required.  Name of input file.")
            .build());
    options.addOption(
        Option.builder("outputFormat")
            .hasArg()
            .desc(
                "Format of output file.  If not present format will be inferred from outputFile extension.")
            .build());
    options.addOption(
        Option.builder("outputFile")
            .required()
            .hasArg()
            .desc("Required.  Name of output file.")
            .build());
    options.addOption(
        Option.builder("inllr")
            .desc(
                "Only used with ASCII or BINARY formats.  If present, input values are assumed to be lon, lat, rad.  Default is x, y, z.")
            .build());
    options.addOption(
        Option.builder("outllr")
            .desc(
                "Only used with ASCII or BINARY formats.  If present, output values will be lon, lat, rad.  Default is x, y, z.")
            .build());
    options.addOption(
        Option.builder("centerXYZ")
            .hasArg()
            .desc(
                "Only used to generate OBJ output.  Center output shape on supplied coordinates.  Specify XYZ coordinates as three floating point numbers separated"
                    + " by commas.")
            .build());
    options.addOption(
        Option.builder("centerLonLat")
            .hasArg()
            .desc(
                "Only used to generate OBJ output.  Center output shape on supplied lon,lat.  Specify lon,lat in degrees as floating point numbers separated"
                    + " by a comma.  Shape will be centered on the point closest to this lon,lat pair.")
            .build());
    options.addOption(
        Option.builder("halfSize")
            .hasArg()
            .desc(
                "Only used to generate OBJ output.  Used with -groundSampleDistance to resample to a uniform grid.  Grid dimensions are (2*halfSize+1)x(2*halfSize+1).")
            .build());
    options.addOption(
        Option.builder("groundSampleDistance")
            .hasArg()
            .desc(
                "Used with -halfSize to resample to a uniform grid.  Spacing between grid points.  Only used to generate OBJ output.  "
                    + "Units are the same as the input file, usually km.")
            .build());
    options.addOption(
        Option.builder("mapRadius")
            .hasArg()
            .desc(
                "Only used to generate OBJ output.  Used with -centerXYZ to resample to a uniform grid.  Only include points within "
                    + "mapRadius*groundSampleDistance*halfSize of centerXYZ.  Default value is sqrt(2).")
            .build());
    options.addOption(
        Option.builder("gmtArgs")
            .hasArg()
            .longOpt("gmt-args")
            .desc(
                "Only used to generate OBJ output.  Pass additional options to GMTSurface.  May be used multiple times, use once per additional argument.")
            .build());
    options.addOption(
        Option.builder("clip")
            .hasArg()
            .desc(
                "Shrink bounding box to a relative size of <arg> and clip any points outside of it.  Default is 1 (no clipping).")
            .build());
    return options;
  }

  public static void main(String[] args) {
    TerrasaurTool defaultOBJ = new PointCloudFormatConverter();

    Options options = defineOptions();

    CommandLine cl = defaultOBJ.parseArgs(args, options);

    Map<MessageLabel, String> startupMessages = defaultOBJ.startupMessages(cl);
    for (MessageLabel ml : startupMessages.keySet())
      logger.info(String.format("%s %s", ml.label, startupMessages.get(ml)));

    NativeLibraryLoader.loadVtkLibraries();
    NativeLibraryLoader.loadSpiceLibraries();

    String inFile = cl.getOptionValue("inputFile");
    String outFile = cl.getOptionValue("outputFile");
    boolean inLLR = cl.hasOption("inllr");
    boolean outLLR = cl.hasOption("outllr");

    FORMATS inFormat =
        cl.hasOption("inputFormat")
            ? FORMATS.valueOf(cl.getOptionValue("inputFormat").toUpperCase())
            : FORMATS.formatFromExtension(cl.getOptionValue("inputFile"));
    FORMATS outFormat =
        cl.hasOption("outputFormat")
            ? FORMATS.valueOf(cl.getOptionValue("outputFormat").toUpperCase())
            : FORMATS.formatFromExtension(cl.getOptionValue("outputFile"));

    PointCloudFormatConverter pcfc = new PointCloudFormatConverter(inFormat, outFormat);

    if (cl.hasOption("centerXYZ")) {
      String[] params = cl.getOptionValue("centerXYZ").split(",");
      double[] array = new double[3];
      for (int i = 0; i < 3; i++) array[i] = Double.parseDouble(params[i].trim());
      pcfc.setCenter(array);
    }

    if (cl.hasOption("clip")) {
      pcfc.setClip(Double.valueOf(cl.getOptionValue("clip")));
    }

    if (cl.hasOption("gmtArgs")) {
      StringBuilder gmtArgs = new StringBuilder();
      for (String arg : cl.getOptionValues("gmtArgs")) gmtArgs.append(String.format("%s ", arg));
      pcfc.setGMTArgs(gmtArgs.toString());
    }

    pcfc.read(inFile, inLLR);

    if (cl.hasOption("centerLonLat")) {
      String[] params = cl.getOptionValue("centerLonLat").split(",");
      Vector3D lcDir =
          new Vector3D(
              Math.toRadians(Double.parseDouble(params[0].trim())),
              Math.toRadians(Double.parseDouble(params[1].trim())));
      double[] center = null;
      double minSep = Double.MAX_VALUE;
      vtkPoints vtkPoints = pcfc.getPoints();
      for (int i = 0; i < vtkPoints.GetNumberOfPoints(); i++) {
        double[] pt = vtkPoints.GetPoint(i);
        double sep = Vector3D.angle(lcDir, new Vector3D(pt));
        if (sep < minSep) {
          minSep = sep;
          center = pt;
        }
      }
      pcfc.setCenter(center);
    }

    pcfc.setMapRadius(
        cl.hasOption("mapRadius") ? Double.parseDouble(cl.getOptionValue("mapRadius")) : Math.sqrt(2));

    if (cl.hasOption("halfSize") && cl.hasOption("groundSampleDistance")) {
      // resample on a uniform XY grid
      pcfc.setHalfSize(Integer.parseInt(cl.getOptionValue("halfSize")));
      pcfc.setGroundSampleDistance(Double.parseDouble(cl.getOptionValue("groundSampleDistance")));
    }

    pcfc.write(outFile, outLLR);
  }
}
