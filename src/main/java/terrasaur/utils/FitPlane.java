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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.Pair;
import terrasaur.utils.math.RotationUtils;

/**
 * Transform points in a global coordinate system to a local one.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class FitPlane {

  private Rotation rotation;
  private Vector3D translation;

  private FitPlane() {

  }

  public FitPlane(List<Vector3D> points) {
    Vector3D center = new Vector3D(0, 0, 0);
    for (Vector3D p : points)
      center = center.add(p);
    center = center.scalarMultiply(1. / points.size());

    double[][] array = new double[3][points.size()];
    for (int i = 0; i < points.size(); i++) {
      Vector3D p = points.get(i);
      array[0][i] = p.getX() - center.getX();
      array[1][i] = p.getY() - center.getY();
      array[2][i] = p.getZ() - center.getZ();
    }

    RealMatrix pointMatrix = new Array2DRowRealMatrix(array, false);
    SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
    RealMatrix u = svd.getU();

    Vector3D zAxis = new Vector3D(u.getColumn(2)).normalize();
    if (zAxis.dotProduct(center) < 0)
      zAxis = zAxis.negate();

    Vector3D xAxis = new Vector3D(u.getColumn(0)).normalize();

    rotation = RotationUtils.KprimaryIsecondary(zAxis, xAxis);
    translation = center;
  }

  /**
   * Get the transform to convert points in global coordinates to local coordinates. <br>
   * <code>
   * <pre>
   * Pair&lt;RotationMatrixIJK, VectorIJK&gt; p = getTransform();
   * VectorIJK local = p.getKey().mxv(VectorIJK.subtract(global, p.getValue()))
   * <pre>
   * </code> <br>
   * 
   * @return
   */
  public Pair<Rotation, Vector3D> getTransform() {
    return new Pair<Rotation, Vector3D>(rotation, translation);
  }

  /**
   * Transform a vector in the global coordinate system to the local coordinate system
   * 
   * @param global
   * @return
   */
  public Vector3D globalToLocal(Vector3D global) {
    return globalToLocal(Arrays.asList(global)).get(0);
  }

  /**
   * Transform a {@link List} of vectors in the global coordinate system to the local coordinate
   * system
   * 
   * @param global
   * @return
   */
  public List<Vector3D> globalToLocal(List<Vector3D> global) {
    Pair<Rotation, Vector3D> p = getTransform();
    List<Vector3D> local = new ArrayList<>();
    for (Vector3D v : global) {
      local.add(p.getKey().applyTo(v.subtract(p.getValue())));
    }
    return local;
  }

  /**
   * Transform a vector in the global coordinate system to the local coordinate system
   * 
   * @param local
   * @return
   */
  public Vector3D localToGlobal(Vector3D local) {
    return localToGlobal(Arrays.asList(local)).get(0);
  }

  /**
   * Transform a vector in the global coordinate system to the local coordinate system
   * 
   * @param local
   * @return
   */
  public List<Vector3D> localToGlobal(List<Vector3D> local) {
    Pair<Rotation, Vector3D> p = getTransform();
    List<Vector3D> global = new ArrayList<>();
    for (Vector3D v : local) {
      global.add(p.getKey().applyInverseTo(v).add(p.getValue()));
    }
    return global;
  }

  /**
   * Return a plane with the normal pointing in the opposite direction
   * 
   * @return
   */
  public FitPlane reverseNormal() {
    FitPlane fp = new FitPlane();
    Vector3D zAxis = rotation.applyInverseTo(Vector3D.PLUS_K).negate();
    Vector3D xAxis = rotation.applyInverseTo(Vector3D.PLUS_I);

    fp.translation = translation;
    fp.rotation = RotationUtils.KprimaryIsecondary(zAxis, xAxis);

    return fp;
  }

}
