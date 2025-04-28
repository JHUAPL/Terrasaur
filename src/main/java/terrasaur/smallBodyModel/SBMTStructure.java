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
package terrasaur.smallBodyModel;

import java.awt.Color;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.immutables.value.Value;
import terrasaur.smallBodyModel.ImmutableSBMTStructure.Builder;

/**
 * 
 * <pre>
# SBMT Structure File
# type,point
# ------------------------------------------------------------------------------
# File consists of a list of structures on each line.
#
# Each line is defined by 17 columns with the following:
# &lt;id&gt; &lt;name&gt; &lt;centerXYZ[3]&gt; &lt;centerLLR[3]&gt; &lt;coloringValue[4]&gt; &lt;diameter&gt; &lt;flattening&gt; &lt;regularAngle&gt; &lt;colorRGB&gt; &lt;label&gt;*
#
#               id: Id of the structure
#             name: Name of the structure
#     centerXYZ[3]: 3 columns that define the structure center in 3D space
#     centerLLR[3]: 3 columns that define the structure center in lat,lon,radius
# coloringValue[4]: 4 columns that define the ellipse &#8220;standard&#8221; colorings. The
#                   colorings are: slope (NA), elevation (NA), acceleration (NA), potential (NA)
#         diameter: Diameter of (semimajor) axis of ellipse
#       flattening: Flattening factor of ellipse. Range: [0.0, 1.0]
#     regularAngle: Angle between the semimajor axis and the line of longitude
#                   as projected onto the surface
#         colorRGB: 1 column (of RGB values [0, 255] separated by commas with no
#                   spaces). This column appears as a single textual column.
#            label: Label of the structure
#
#
# Please note the following:
# - Each line is composed of columns separated by a tab character.
# - Blank lines or lines that start with '#' are ignored.
# - Angle units: degrees
# - Length units: kilometers
 * </pre>
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
@Value.Immutable
public abstract class SBMTStructure {

  abstract int id();

  abstract String name();

  public abstract Vector3D centerXYZ();

  abstract String slopeColoring();

  abstract String elevationColoring();

  abstract String accelerationColoring();

  abstract String potentialColoring();

  abstract double diameter();

  abstract double flattening();

  abstract double regularAngle();

  abstract Color rgb();

  abstract String label();

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%d\t", id()));
    sb.append(String.format("%s\t", name()));
    sb.append(String.format("%.16f\t", centerXYZ().getX()));
    sb.append(String.format("%.16f\t", centerXYZ().getY()));
    sb.append(String.format("%.16f\t", centerXYZ().getZ()));
    sb.append(String.format("%.16f\t", Math.toDegrees(centerXYZ().getDelta())));
    sb.append(String.format("%.16f\t", Math.toDegrees(centerXYZ().getAlpha())));
    sb.append(String.format("%.16f\t", centerXYZ().getNorm()));
    sb.append(String.format("%s\t", slopeColoring()));
    sb.append(String.format("%s\t", elevationColoring()));
    sb.append(String.format("%s\t", accelerationColoring()));
    sb.append(String.format("%s\t", potentialColoring()));
    sb.append(String.format("%f\t", diameter()));
    sb.append(String.format("%f\t", flattening()));
    sb.append(String.format("%f\t", regularAngle()));
    sb.append(String.format("%d,%d,%d\t", rgb().getRed(), rgb().getGreen(), rgb().getBlue()));
    sb.append(label());
    return sb.toString();
  }

  public static SBMTStructure fromString(String line) {
    String[] parts = line.split("\\s+");
    int id = Integer.parseInt(parts[0]);
    String name = parts[1];
    Vector3D centerXYZ = new Vector3D(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
        Double.parseDouble(parts[4]));
    String slopeColoring = parts[8];
    String elevationColoring = parts[9];
    String accelerationColoring = parts[10];
    String potentialColoring = parts[11];
    double diameter = Double.parseDouble(parts[12]);
    double flattening = Double.parseDouble(parts[13]);
    double regularAngle = Double.parseDouble(parts[14]);
    String[] colorParts = parts[15].split(",");
    Color rgb = new Color(Integer.parseInt(colorParts[0]), Integer.parseInt(colorParts[1]),
        Integer.parseInt(colorParts[2]));
    String label = parts[16];

    Builder builder = ImmutableSBMTStructure.builder();
    builder.id(id);
    builder.name(name);
    builder.centerXYZ(centerXYZ);
    builder.slopeColoring(slopeColoring);
    builder.elevationColoring(elevationColoring);
    builder.accelerationColoring(accelerationColoring);
    builder.potentialColoring(potentialColoring);
    builder.diameter(diameter);
    builder.flattening(flattening);
    builder.regularAngle(regularAngle);
    builder.rgb(rgb);
    builder.label(label);
    return builder.build();
  }


}
