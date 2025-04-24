package terrasaur.utils;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import spice.basic.Body;
import spice.basic.FOV;
import spice.basic.KernelPool;
import spice.basic.KernelVarDescriptor;
import spice.basic.Matrix33;
import spice.basic.Plane;
import spice.basic.Ray;
import spice.basic.RayPlaneIntercept;
import spice.basic.ReferenceFrame;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.SpiceQuaternion;
import spice.basic.TDBTime;
import spice.basic.Vector3;

/**
 * Class with various useful static utility functions.
 *
 */
public class SPICEUtil {

  public final static Comparator<TDBTime> tdbComparator = new Comparator<TDBTime>() {

    @Override
    public int compare(TDBTime o1, TDBTime o2) {
      int compare = 0;
      try {
        compare = Double.compare(o1.getTDBSeconds(), o2.getTDBSeconds());
      } catch (SpiceErrorException e) {
        e.printStackTrace();
      }
      return compare;
    }

  };

  /**
   * Return the body fixed frame for a given body
   * 
   * @param b
   * @return
   * @throws SpiceException
   */
  public static ReferenceFrame getBodyFixedFrame(Body b) throws SpiceException {

    String frameKey = "OBJECT_" + b.getIDCode() + "_FRAME";
    if (!KernelPool.exists(frameKey)) {
      frameKey = "OBJECT_" + b.getName() + "_FRAME";
      if (!KernelPool.exists(frameKey)) {
        return null;
      }
    }

    ReferenceFrame bodyFixed;

    switch (KernelPool.getDataType(frameKey)) {
      case KernelVarDescriptor.CHARACTER:
        bodyFixed = new ReferenceFrame(KernelPool.getCharacter(frameKey)[0]);
        break;
      case KernelVarDescriptor.NUMERIC:
        bodyFixed = new ReferenceFrame(KernelPool.getInteger(frameKey)[0]);
        break;
      default:
        bodyFixed = null;
    }

    return bodyFixed;
  }

  /**
   * Check if a ray from the instrument origin in a specified direction is in the FOV.
   * 
   * @param fov
   * @param directionVector specified in instrument frame
   * @return
   * @throws SpiceErrorException
   */
  public static List<Boolean> isInFOV(FOV fov, List<Vector3> directionVector)
      throws SpiceException {
    List<Boolean> isInFOV = new ArrayList<Boolean>();

    if (fov.getShape().equals("RECTANGLE") || fov.getShape().equals("POLYGON")) {
      Plane fovPlane = new Plane(fov.getBoresight(), fov.getBoresight());
      Vector3 origin = fovPlane.getPoint();
      Vector3 xAxis = fovPlane.getSpanningVectors()[0];
      Vector3 yAxis = fovPlane.getSpanningVectors()[1];

      Vector3[] boundaries = fov.getBoundary();
      Vector<Vector3> points = new Vector<Vector3>();
      for (Vector3 boundary : boundaries) {
        RayPlaneIntercept rpi =
            new RayPlaneIntercept(new Ray(new Vector3(0, 0, 0), boundary), fovPlane);
        points.add(rpi.getIntercept().sub(origin));
      }

      Path2D.Double shape = new Path2D.Double();
      shape.moveTo(xAxis.dot(points.get(0)), yAxis.dot(points.get(0)));
      for (int i = 1; i < points.size(); i++)
        shape.lineTo(xAxis.dot(points.get(i)), yAxis.dot(points.get(i)));
      shape.lineTo(xAxis.dot(points.get(0)), yAxis.dot(points.get(0)));

      for (Vector3 direction : directionVector) {
        RayPlaneIntercept rpi =
            new RayPlaneIntercept(new Ray(new Vector3(0, 0, 0), direction), fovPlane);
        Vector3 pointOnPlane = rpi.getIntercept().sub(origin);
        isInFOV.add(shape.contains(xAxis.dot(pointOnPlane), yAxis.dot(pointOnPlane)));
      }
    } else if (fov.getShape().equals("ELLIPSE")) {
      throw new SpiceErrorException("FOV shape ELLIPSE not supported.");
    } else if (fov.getShape().equals("CIRCLE")) {
      Vector3 boundary = fov.getBoundary()[0];
      double fovRadius = Math.acos(fov.getBoresight().hat().dot(boundary.hat()));
      for (Vector3 direction : directionVector) {
        double dist = Math.acos(fov.getBoresight().hat().dot(direction.hat()));
        isInFOV.add(dist < fovRadius);
      }
    } else {
      throw new SpiceErrorException("Unknown FOV shape: " + fov.getShape());
    }

    return isInFOV;
  }

  public static boolean isInFOV(FOV fov, Vector3 direction) throws SpiceException {
    List<Vector3> directionVector = new ArrayList<Vector3>();
    directionVector.add(direction);
    List<Boolean> isInFOV = isInFOV(fov, directionVector);
    return isInFOV.get(0);
  }

  public static Plane planeFromFacet(double[] a, double[] b, double[] c) {
    Vector3 va = new Vector3(a);
    Vector3 vab = new Vector3(b).sub(va);
    Vector3 vac = new Vector3(c).sub(va);

    Plane p = null;
    try {
      p = new Plane(va, vab, vac);
    } catch (SpiceException e) {
      e.printStackTrace();
    }
    return p;
  }

  /**
   * Return the inverse quaternion
   * 
   * @param q
   * @return
   */
  public static double[] invertQuaternion(double[] q) {
    try {
      SpiceQuaternion sq = new SpiceQuaternion(q);
      Matrix33 m = sq.toMatrix().invert();
      sq = new SpiceQuaternion(m);
      return sq.toArray();
    } catch (SpiceException e) {
      e.printStackTrace();
    }
    return null;
  }
}
