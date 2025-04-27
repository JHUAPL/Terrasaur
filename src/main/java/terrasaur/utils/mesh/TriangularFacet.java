package terrasaur.utils.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;

/**
 * Class representing a triangular facet with three vertices with cartesian coordinates.
 * 
 * @author nairah1
 *
 */
public class TriangularFacet {

  private final UnwritableVectorIJK vertex1;
  private final UnwritableVectorIJK vertex2;
  private final UnwritableVectorIJK vertex3;

  private UnwritableVectorIJK center;
  private UnwritableVectorIJK normal;

  private Double area;
  private Double meanEdgeLength;

  /**
   * Define a triangle. Vertices are in counterclockwise order. e.g.
   * 
   * <pre>
   *    1
   *   / \
   *  2---3
   * </pre>
   * 
   * The normal points in the direction of (v3-v2)x(v1-v2) so the order is important.
   * 
   * @param vertex1
   * @param vertex2
   * @param vertex3
   */
  public TriangularFacet(UnwritableVectorIJK vertex1, UnwritableVectorIJK vertex2,
      UnwritableVectorIJK vertex3) {
    this.vertex1 = vertex1;
    this.vertex2 = vertex2;
    this.vertex3 = vertex3;

    this.area = null;
    this.meanEdgeLength = null;
    this.center = null;
    this.normal = null;
  }

  public List<UnwritableVectorIJK> getVertices() {
    List<UnwritableVectorIJK> vertices = new ArrayList<>();
    vertices.add(vertex1);
    vertices.add(vertex2);
    vertices.add(vertex3);
    return Collections.unmodifiableList(vertices);
  }

  public UnwritableVectorIJK getVertex1() {
    return vertex1;
  }

  public UnwritableVectorIJK getVertex2() {
    return vertex2;
  }

  public UnwritableVectorIJK getVertex3() {
    return vertex3;
  }

  /**
   * return the average of the three vertices
   * 
   * @return
   */
  public UnwritableVectorIJK getCenter() {
    if (center == null) {
      double x = (vertex1.getI() + vertex2.getI() + vertex3.getI()) / 3;
      double y = (vertex1.getJ() + vertex2.getJ() + vertex3.getJ()) / 3;
      double z = (vertex1.getK() + vertex2.getK() + vertex3.getK()) / 3;
      center = new UnwritableVectorIJK(x, y, z);
    }
    return center;
  }

  /**
   * returns the unitized cross product of (v3-v2)x(v1-v2). Vertices are in counterclockwise order.
   * 
   * @return
   */
  public UnwritableVectorIJK getNormal() {
    if (normal == null) {
      UnwritableVectorIJK edge1 = VectorIJK.subtract(vertex3, vertex2);
      UnwritableVectorIJK edge2 = VectorIJK.subtract(vertex1, vertex2);
      normal = VectorIJK.cross(edge1, edge2).unitize();
    }
    return normal;
  }

  /**
   * Find area with <a href="https://en.wikipedia.org/wiki/Heron%27s_formula">Heron's formula</a>
   */
  private void calcArea() {
    VectorIJK v = VectorIJK.subtract(vertex2, vertex1);
    double a = v.getDot(v);
    v = VectorIJK.subtract(vertex3, vertex2);
    double b = v.getDot(v);
    v = VectorIJK.subtract(vertex1, vertex3);
    double c = v.getDot(v);
    area = (0.25 * Math.sqrt(Math.abs(4.0 * a * c - (a - b + c) * (a - b + c))));

    meanEdgeLength = (FastMath.sqrt(a) + FastMath.sqrt(b) + FastMath.sqrt(c)) / 3;
  }

  /**
   * Get the area of the triangle.
   * 
   * @return
   */
  public double getArea() {
    if (area == null) {
      calcArea();
    }
    return area.doubleValue();
  }

  /**
   * Get the mean length of the three edges.
   * 
   * @return
   */
  public double getMeanEdgeLength() {
    if (meanEdgeLength == null) {
      calcArea();
    }
    return meanEdgeLength.doubleValue();
  }

  /**
   * Check if the ray intersects the triangle. Algorithm is from
   * https://stackoverflow.com/questions/27381119/javafx-8-3d-scene-intersection-point
   * 
   * @param source observer position
   * @param ray look direction
   * @return
   */
  public boolean intersects(UnwritableVectorIJK source, UnwritableVectorIJK ray) {
    final double EPS = 1e-12;

    UnwritableVectorIJK a = getVertex1();
    UnwritableVectorIJK b = getVertex2();
    UnwritableVectorIJK c = getVertex3();

    UnwritableVectorIJK edge1 = VectorIJK.subtract(b, a);
    UnwritableVectorIJK edge2 = VectorIJK.subtract(c, a);
    UnwritableVectorIJK pvec = VectorIJK.cross(ray, edge2);
    double det = edge1.getDot(pvec);

    if (det <= -EPS || det >= EPS) {
      double inv_det = 1 / det;
      UnwritableVectorIJK tvec = VectorIJK.subtract(source, a);
      double u = tvec.getDot(pvec) * inv_det;
      if (u >= 0 && u <= 1) {
        UnwritableVectorIJK qvec = VectorIJK.cross(tvec, edge1);
        double v = ray.getDot(qvec) * inv_det;
        if (v >= 0 && u + v <= 1) {
          // double t = c.getDot(qvec) * inv_det;
          // System.out.printf("det: %f, t: %f, u: %f, v: %f\n", det, t, u, v);
          return true;
        }
      }
    }
    return false;

  }
}
