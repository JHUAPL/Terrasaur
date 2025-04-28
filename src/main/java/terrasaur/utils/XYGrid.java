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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import terrasaur.smallBodyModel.SmallBodyModel;
import spice.basic.Plane;
import spice.basic.SpiceException;
import spice.basic.Vector3;
import vtk.vtkPolyData;

/**
 * Class representing a uniformly spaced grid in two dimensions.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class XYGrid {

  class XYGridPoint {
    private final int i;
    private final int j;
    private final double x, y, height;
    private final Vector3 pointOnPlane;

    public XYGridPoint(int i, int j, double x, double y, Vector3 pointOnPlane, double height) {
      this.i = i;
      this.j = j;
      this.x = x;
      this.y = y;
      this.pointOnPlane = pointOnPlane;
      this.height = height;
    }

    public double getHeight() {
      return height;
    }

    public Vector3 getPointOnPlane() {
      return pointOnPlane;
    }

  }

  private ArrayList<XYGridPoint> gridPoints;

  private final Plane plane;
  private final Vector3 xAxis;
  private final Vector3 yAxis;

  private final double resolution;

  private final int nx, ny;

  public int getNx() {
    return nx;
  }

  public int getNy() {
    return ny;
  }

  private final SmallBodyModel smallBodyModel;

  private double xmin, xmax, ymin, ymax;

  /**
   * 
   * @param plane reference plane. The X and Y axes are defined by
   *        {@link Plane#getSpanningVectors()}.
   * @param resolution (in same units as polyData, usually km)
   * @param radius half extent of grid: number of grid points in each dimension = (2 * ((int)
   *        (radius / resolution)) + 1)
   * @param polyData shape model
   * @throws SpiceException
   */
  public XYGrid(Plane plane, double resolution, double radius, vtkPolyData polyData)
      throws SpiceException {
    this.plane = plane;
    Vector3[] spanningVectors = plane.getSpanningVectors();
    this.xAxis = spanningVectors[0];
    this.yAxis = spanningVectors[1];
    this.resolution = resolution;
    this.smallBodyModel = new SmallBodyModel(polyData);

    nx = 2 * ((int) (radius / resolution)) + 1;
    ny = 2 * ((int) (radius / resolution)) + 1;
  }

  /**
   * Return a vector shifted by an integral number of grid points in X and Y.
   * 
   * @param inVector initial point
   * @param xShift number of grid points to move in X
   * @param yShift number of grid points to move in Y
   * @return
   */
  public Vector3 shift(Vector3 inVector, int xShift, int yShift) {
    double x = xShift * resolution;
    double y = yShift * resolution;

    Vector3 outVector = inVector.add(xAxis.scale(x).add(yAxis.scale(y)));
    return outVector;
  }

  // private vtkPolyData rayBundlePolyData;
  // private vtkCellArray rayBundleCells;
  // private vtkPoints rayBundlePoints;
  // private vtkDoubleArray rayBundleSuccessArray;

  // public void writeVTKDebugFile(String filename)
  // {
  // vtkPolyDataWriter writer=new vtkPolyDataWriter();
  // writer.SetInputData(rayBundlePolyData);
  // writer.SetFileName(filename);
  // writer.SetFileTypeToBinary();
  // writer.Update();
  // }

  /**
   * 
   * @return heights[ny][nx]
   */
  public double[][] getHeightGrid() {
    double[][] heights = new double[ny][nx];
    for (XYGridPoint gridPoint : gridPoints)
      heights[gridPoint.i][gridPoint.j] = gridPoint.height;
    return heights;
  }


  /**
   * Write out grid coordinates and height.
   * 
   * @param file File to write
   */
  public void writeXYZTable(File file) {
    try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
      PrintStream printStream = new PrintStream(outputStream);
      for (XYGridPoint gridPoint : gridPoints) {
        if (Double.isNaN(gridPoint.height))
          continue;

        // write values in meters
        printStream.printf("%16.8e %16.8e %16.8e\n", 1e3 * gridPoint.x, 1e3 * gridPoint.y,
            1e3 * gridPoint.height);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Build a 2D array of heights above a reference plane
   * 
   * @param point Center of the array
   * @throws SpiceException
   */
  public void buildHeightGrid(Vector3 point) throws SpiceException {
    // for debugging
    // rayBundlePolyData=new vtkPolyData();
    // rayBundleCells=new vtkCellArray();
    // rayBundlePoints=new vtkPoints();
    // rayBundleSuccessArray=new vtkDoubleArray();
    // rayBundleSuccessArray.SetName("success");
    //
    // rayBundlePolyData.SetPoints(rayBundlePoints);
    // rayBundlePolyData.SetLines(rayBundleCells);
    // rayBundlePolyData.GetCellData().AddArray(rayBundleSuccessArray);

    Vector3 featurePosition = plane.project(point);

    gridPoints = new ArrayList<XYGridPoint>();

    double[] intersectPoint = new double[3];
    double[] origin3D = {0., 0., 0.};
    Vector3 pointOnShape = new Vector3();
    for (int j = 0; j < ny; j++) {
      int yShift = j - ny / 2; // ny is always odd
      for (int i = 0; i < nx; i++) {
        int xShift = i - nx / 2; // nx is always odd
        double x = xShift * resolution;
        double y = yShift * resolution;

        Vector3 pointOnPlane = featurePosition.add(xAxis.scale(x).add(yAxis.scale(y)));
        double[] direction = pointOnPlane.hat().toArray();
        long cellID = smallBodyModel.computeRayIntersection(origin3D, direction, pointOnPlane.norm(),
            intersectPoint);

        // Ray ray = new Ray(new Vector3(origin3D), pointOnPlane.hat());
        // double success = 0;
        double height = Double.NaN;

        // vtkLine line=new vtkLine();
        // int id0=rayBundlePoints.InsertNextPoint(ray.getVertex().toArray());
        // int id1=rayBundlePoints.InsertNextPoint(pointOnPlane.add(ray.getVertex()).toArray());

        if (cellID > 0) {
          pointOnShape.assign(intersectPoint);
          Vector3 projectedPoint = plane.project(pointOnShape);
          height = pointOnShape.sub(projectedPoint).norm();
          if (pointOnShape.norm() < projectedPoint.norm())
            height *= -1;
          // success = 1.;
          // id1=rayBundlePoints.InsertNextPoint(pointOnShape.add(ray.getVertex()).toArray());
        }

        // line.GetPointIds().SetId(0, id0);
        // line.GetPointIds().SetId(1, id1);
        //
        // rayBundleCells.InsertNextCell(line);
        // rayBundleSuccessArray.InsertNextValue(success);

        gridPoints.add(new XYGridPoint(i, j, x, y, pointOnPlane, height));
      }
    }

    xmin = Double.MAX_VALUE;
    xmax = -Double.MAX_VALUE;
    ymin = Double.MAX_VALUE;
    ymax = -Double.MAX_VALUE;
    for (XYGridPoint gridPoint : gridPoints) {
      if (Double.isNaN(gridPoint.height))
        continue;

      if (xmin > gridPoint.x)
        xmin = gridPoint.x;
      if (xmax < gridPoint.x)
        xmax = gridPoint.x;
      if (ymin > gridPoint.y)
        ymin = gridPoint.y;
      if (ymax < gridPoint.y)
        ymax = gridPoint.y;
    }
  }
}
