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
package terrasaur.utils;

import java.awt.Color;
import org.immutables.value.Value;
import terrasaur.utils.ImmutableSBMTEllipseRecord.Builder;

@Value.Immutable
public abstract class SBMTEllipseRecord {

  abstract int id();

  abstract String name();

  public abstract double x();

  public abstract double y();

  public abstract double z();

  abstract double lat();

  abstract double lon();

  abstract double radius();

  abstract double slope();

  abstract double elevation();

  abstract double acceleration();

  abstract double potential();

  abstract double diameter();

  abstract double flattening();

  abstract double angle();

  abstract Color color();

  abstract String dummy();

  abstract String label();

  public static SBMTEllipseRecord fromString(String string) {
    String[] parts = string.split("\\s+");

    int id = Integer.parseInt(parts[0]);
    String name = parts[1];
    double x = Double.parseDouble(parts[2]);
    double y = Double.parseDouble(parts[3]);
    double z = Double.parseDouble(parts[4]);
    double lat = Double.parseDouble(parts[5]);
    double lon = Double.parseDouble(parts[6]);
    double radius = Double.parseDouble(parts[7]);
    double slope = Double.parseDouble(parts[8]);
    double elevation = Double.parseDouble(parts[9]);
    double acceleration = Double.parseDouble(parts[10]);
    double potential = Double.parseDouble(parts[11]);
    double diameter = Double.parseDouble(parts[12]);
    double flattening = Double.parseDouble(parts[13]);
    double angle = Double.parseDouble(parts[14]);
    String[] colorParts = parts[15].split(",");
    Color color = new Color(Integer.parseInt(colorParts[0]), Integer.parseInt(colorParts[1]),
        Integer.parseInt(colorParts[2]));
    String dummy = parts[16];
    StringBuilder label = new StringBuilder();
    for (int i = 17; i < parts.length; i++)
      label.append(parts[i] + " ");

    Builder record = ImmutableSBMTEllipseRecord.builder().id(id).name(name).x(x).y(y).z(z).lat(lat)
        .lon(lon).radius(radius).slope(slope).elevation(elevation).acceleration(acceleration)
        .potential(potential).diameter(diameter).flattening(flattening).angle(angle).color(color)
        .dummy(dummy).label(label.toString().replace("\"", "").trim());

    return record.build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%d\t", id()));
    sb.append(String.format("%s\t", name()));
    sb.append(String.format("%s\t", Double.valueOf(x()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(y()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(z()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(lat()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(lon()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(radius()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(slope()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(elevation()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(acceleration()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(potential()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(diameter()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(flattening()).toString()));
    sb.append(String.format("%s\t", Double.valueOf(angle()).toString()));
    sb.append(String.format("%d,%d,%d\t", color().getRed(), color().getGreen(), color().getBlue()));
    sb.append(String.format("%s\t", dummy()));
    sb.append(String.format("\"%s\" ", label()));
    return sb.toString();
  }

}
