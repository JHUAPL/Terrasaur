package terrasaur.utils.mesh;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.Statistics;
import picante.math.vectorspace.UnwritableRotationMatrixIJK;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import picante.surfaces.Surface;
import terrasaur.utils.octree.BoundingBox;
import terrasaur.utils.octree.Octree;

/**
 * Class representing a mesh made of triangular facets. Implements the {@link Surface} interface.
 * 
 * @author nairah1
 *
 */
public class TriangularMesh implements Surface {

  private static Logger logger = LogManager.getLogger(TriangularMesh.class);

  private int octreeLevel;
  private ThreadLocal<Octree> threadLocalOctree;

  /** list of 3D vertices in the order they were added */
  private List<UnwritableVectorIJK> vertexList;

  /** Lookup vertex index from 3D position */
  private Map<UnwritableVectorIJK, Integer> vertexIndexMap;

  /** List of facets in the order they were added */
  private List<TriangularFacet> facetList;

  /** Lookup facet index from facet */
  private Map<TriangularFacet, Integer> facetIndexMap;

  /** Lookup facets containing a vertex */
  private Map<UnwritableVectorIJK, List<TriangularFacet>> vertexFacetMap;

  /** Coordinates of mesh center */
  private UnwritableVectorIJK center;

  /** Facets in each level of the octree */
  private Map<Integer, Set<TriangularFacet>> facetOctreeMap;

  // "set" methods violate the Builder pattern, but this allows the color/albedo
  // map to change between renderings
  private Map<TriangularFacet, Double> albedoMap = Collections.emptyMap();
  private Map<TriangularFacet, Color> colorMap = Collections.emptyMap();

  /**
   * Get the stored albedo map
   * 
   * @return
   */
  public Map<TriangularFacet, Double> getAlbedoMap() {
    return albedoMap;
  }

  /**
   * Store an albedo map
   * 
   * @param albedoMap
   */
  public void setAlbedoMap(Map<TriangularFacet, Double> albedoMap) {
    this.albedoMap = albedoMap;
  }

  /**
   * Get the stored color map
   * 
   * @return
   */
  public Map<TriangularFacet, Color> getColorMap() {
    return colorMap;
  }

  /**
   * Store a color map
   * 
   * @param colorMap
   */
  public void setColorMap(Map<TriangularFacet, Color> colorMap) {
    this.colorMap = colorMap;
  }

  /**
   * Compare facets by distance from a center facet
   * 
   * @param center
   * @return
   */
  public final Comparator<TriangularFacet> FACET_DISTANCE_COMPARATOR(TriangularFacet center) {
    Comparator<TriangularFacet> comparator = new Comparator<TriangularFacet>() {

      @Override
      public int compare(TriangularFacet o1, TriangularFacet o2) {
        double dist1 = center.getCenter().getDistance(o1.getCenter());
        double dist2 = center.getCenter().getDistance(o2.getCenter());
        return Double.compare(dist1, dist2);
      }

    };
    return comparator;
  }

  /**
   * Compare facets by index
   */
  public final Comparator<TriangularFacet> FACET_INDEX_COMPARATOR =
      new Comparator<TriangularFacet>() {

        @Override
        public int compare(TriangularFacet o1, TriangularFacet o2) {
          int index1 = getFacetIndexMap().get(o1);
          int index2 = getFacetIndexMap().get(o2);
          return Integer.compare(index1, index2);
        }

      };

  /**
   * Compare vertices by distance from a center point
   * 
   * @param center
   * @return
   */
  public final Comparator<UnwritableVectorIJK> VERTEX_DISTANCE_COMPARATOR(
      UnwritableVectorIJK center) {
    Comparator<UnwritableVectorIJK> comparator = new Comparator<UnwritableVectorIJK>() {

      @Override
      public int compare(UnwritableVectorIJK o1, UnwritableVectorIJK o2) {
        double dist1 = center.getDistance(o1);
        double dist2 = center.getDistance(o2);
        return Double.compare(dist1, dist2);
      }

    };
    return comparator;
  }

  /** Bounding box containing the shape model */
  private BoundingBox boundingBox;

  /**
   * 
   * @param lines
   * @return
   */
  public static TriangularMesh fromLines(List<String> lines) {
    Builder builder = new Builder();
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.length() == 0) {
        continue;
      }
      if (trimmed.startsWith("#")) {
        continue;
      }
      String[] parts = trimmed.split("\\s+");
      if (parts[0].equalsIgnoreCase("v")) {
        builder.addVertex(new UnwritableVectorIJK(Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
      }
      if (parts[0].equalsIgnoreCase("f")) {
        int[] indices = new int[3];
        for (int i = 0; i < 3; i++) {
          String[] subparts = parts[i + 1].split("/");
          indices[i] = Integer.parseInt(subparts[0]) - 1;
        }
        builder.addFacet(indices[0], indices[1], indices[2]);
      }

    }
    logger.debug("read {} vertices and {} facets", builder.vertexList.size(),
        builder.facetIndexList.size());

    return builder.build();

  }

  /**
   * Read a shape model in Wavefront OBJ format.
   * 
   * @param objFile
   * @return
   */
  public static TriangularMesh readOBJ(String objFile) {
    return readOBJ(new File(objFile));
  }

  /**
   * Read a shape model in Wavefront OBJ format.
   * 
   * @param objFile
   * @return
   */
  public static TriangularMesh readOBJ(File objFile) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(objFile))) {
      String line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
      return null;
    }

    return fromLines(lines);
  }

  public void writeOBJ(String objFile) {

    try (PrintWriter pw = new PrintWriter(objFile)) {

      for (UnwritableVectorIJK v : vertexList) {
        pw.printf("v %f %f %f\n", v.getI(), v.getJ(), v.getK());
      }

      for (TriangularFacet f : facetList) {
        int index1 = getVertexIndexMap().get(f.getVertex1()) + 1;
        int index2 = getVertexIndexMap().get(f.getVertex2()) + 1;
        int index3 = getVertexIndexMap().get(f.getVertex3()) + 1;
        pw.printf("f %d %d %d\n", index1, index2, index3);
      }

    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage());
    }

  }

  public static class Builder {
    private List<UnwritableVectorIJK> vertexList;
    private List<UnwritableVectorIJK> facetIndexList;
    private List<TriangularFacet> facetList;

    private Map<UnwritableVectorIJK, Integer> vertexIndexMap;
    private Map<TriangularFacet, Integer> facetIndexMap;

    private Map<UnwritableVectorIJK, List<TriangularFacet>> vertexFacetMap;

    public Builder() {
      this.vertexList = new ArrayList<>();
      this.facetIndexList = new ArrayList<>();
      this.facetList = new ArrayList<>();

      this.vertexIndexMap = new HashMap<>();
      this.facetIndexMap = new HashMap<>();
      this.vertexFacetMap = new HashMap<>();
    }

    /**
     * Add a vertex to the mesh.
     * 
     * @param vertex
     */
    public void addVertex(UnwritableVectorIJK vertex) {
      vertexIndexMap.put(vertex, vertexList.size());
      vertexList.add(vertex);
    }

    /**
     * Add a facet to the mesh.
     * 
     * @param index1 index of vertex 1
     * @param index2 index of vertex 2
     * @param index3 index of vertex 3
     */
    public void addFacet(int index1, int index2, int index3) {
      facetIndexList.add(new UnwritableVectorIJK(index1, index2, index3));
      List<UnwritableVectorIJK> vertices = new ArrayList<>();
      vertices.add(vertexList.get(index1));
      vertices.add(vertexList.get(index2));
      vertices.add(vertexList.get(index3));

      TriangularFacet tf = new TriangularFacet(vertices.get(0), vertices.get(1), vertices.get(2));
      facetIndexMap.put(tf, facetList.size());
      facetList.add(tf);

      for (UnwritableVectorIJK vertex : vertices) {
        List<TriangularFacet> facets = vertexFacetMap.get(vertex);
        if (facets == null) {
          facets = new ArrayList<>();
          vertexFacetMap.put(vertex, facets);
        }
        facets.add(tf);
      }

    }

    /**
     * Return the mesh
     * 
     * @return
     */
    public TriangularMesh build() {
      TriangularMesh mesh = new TriangularMesh();
      mesh.vertexList = Collections.unmodifiableList(vertexList);
      mesh.facetList = Collections.unmodifiableList(facetList);

      Statistics.Builder builder = Statistics.builder();
      for (TriangularFacet f : facetList) {
        builder.accumulate(f.getMeanEdgeLength());
      }
      Statistics edgeStats = builder.build();

      mesh.vertexIndexMap = Collections.unmodifiableMap(vertexIndexMap);
      mesh.facetIndexMap = Collections.unmodifiableMap(facetIndexMap);

      mesh.vertexFacetMap = Collections.unmodifiableMap(vertexFacetMap);

      Statistics.Builder xBuilder = Statistics.builder();
      Statistics.Builder yBuilder = Statistics.builder();
      Statistics.Builder zBuilder = Statistics.builder();

      for (UnwritableVectorIJK v : vertexList) {
        xBuilder.accumulate(v.getI());
        yBuilder.accumulate(v.getJ());
        zBuilder.accumulate(v.getK());
      }
      Statistics xStats = xBuilder.build();
      Statistics yStats = yBuilder.build();
      Statistics zStats = zBuilder.build();

      mesh.center = new UnwritableVectorIJK(xStats.getMean(), yStats.getMean(), zStats.getMean());
      mesh.boundingBox = new BoundingBox(
          new UnwritableVectorIJK(xStats.getMinimumValue(), yStats.getMinimumValue(),
              zStats.getMinimumValue()),
          new UnwritableVectorIJK(xStats.getMaximumValue(), yStats.getMaximumValue(),
              zStats.getMaximumValue()));
      mesh.threadLocalOctree = new ThreadLocal<>();

      // this ratio should be about 10
      double maxSide = Math.max(mesh.boundingBox.getxRange().getLength(), Math
          .max(mesh.boundingBox.getyRange().getLength(), mesh.boundingBox.getzRange().getLength()));

      // octree levels above 10 require box indices to be long
      int octreeLevel = Math.min(10,
          (int) (Math.log(maxSide / (10 * edgeStats.getMean())) / Math.log(2.0) + 0.5));
      double boxSize = maxSide / Math.pow(2, octreeLevel);
      logger.printf(Level.DEBUG,
          "Octree level %d, Mean edge length %f, box size %f, box size/edge length %f", octreeLevel,
          edgeStats.getMean(), boxSize, boxSize / edgeStats.getMean());
      mesh.buildFacetOctreeMap(octreeLevel);
      return mesh;
    }
  }

  private Octree getOctree() {
    Octree octree = threadLocalOctree.get();
    if (octree == null) {
      octree = new Octree(boundingBox);
      threadLocalOctree.set(octree);
    }
    return octree;
  }

  /**
   * Return a list of vertices in the order they were added.
   * 
   * @return
   */
  public List<UnwritableVectorIJK> getVertexList() {
    return Collections.unmodifiableList(vertexList);
  }

  /**
   * return a list of facets in the order they were added
   * 
   * @return
   */
  public List<TriangularFacet> getFacetList() {
    return Collections.unmodifiableList(facetList);
  }

  /**
   * Return a mapping from vertex to list index. Add 1 to this index to get the corresponding OBJ
   * vertex number (which start from 1).
   * 
   * @return
   */
  public Map<UnwritableVectorIJK, Integer> getVertexIndexMap() {
    return vertexIndexMap;
  }

  /**
   * return a mapping from facet to list index
   * 
   * @return
   */
  public Map<TriangularFacet, Integer> getFacetIndexMap() {
    return facetIndexMap;
  }

  /**
   * Return a list of facets containing this vertex
   * 
   * @return
   */
  public Map<UnwritableVectorIJK, List<TriangularFacet>> getVertexFacetMap() {
    return vertexFacetMap;
  }

  /**
   * return the mean of all vertex positions
   * 
   * @return
   */
  public UnwritableVectorIJK getCenter() {
    return center;
  }

  /**
   * return a mapping of octree box index to {@link Set} of facets contained in the box
   * 
   * @return
   */
  public Map<Integer, Set<TriangularFacet>> getFacetOctreeMap() {
    return facetOctreeMap;
  }

  /**
   * Get the {@link BoundingBox} which encloses the mesh with its edges parallel to the coordinate
   * system axes.
   * 
   * @return
   */
  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  private void buildFacetOctreeMap(int level) {
    facetOctreeMap = new HashMap<>();
    octreeLevel = level;
    for (TriangularFacet facet : facetList) {
      for (UnwritableVectorIJK v : facet.getVertices()) {
        int index = getOctree().getIndex(v, level);
        Set<TriangularFacet> facets = facetOctreeMap.get(index);
        if (facets == null) {
          facets = new HashSet<>();
          facetOctreeMap.put(index, facets);
        }
        facets.add(facet);
      }
    }
  }

  /**
   * Return a set of facets sharing a vertex with this one
   * 
   * @return
   */
  public Set<TriangularFacet> getFacetNeighbors(TriangularFacet tf) {
    Set<TriangularFacet> facets = new TreeSet<>(FACET_DISTANCE_COMPARATOR(tf));

    for (UnwritableVectorIJK vertex : tf.getVertices()) {
      facets.addAll(vertexFacetMap.get(vertex));
    }
    return Collections.unmodifiableSet(facets);
  }

  /**
   * Return a contiguous set of facets with centers less than radius from this facet's center
   * 
   * @return
   */
  public Set<TriangularFacet> getFacetsWithinRadius(TriangularFacet tf, double radius) {
    Set<TriangularFacet> facets = new TreeSet<>(FACET_DISTANCE_COMPARATOR(tf));
    facets.add(tf);

    UnwritableVectorIJK center = tf.getCenter();

    while (true) {

      Set<TriangularFacet> currentFacets = new HashSet<>(facets);
      int facetSize = currentFacets.size();

      for (TriangularFacet facet : currentFacets) {
        for (TriangularFacet neighbor : getFacetNeighbors(facet)) {
          if (!facets.contains(neighbor)) {
            if (neighbor.getCenter().getDistance(center) < radius) {
              facets.add(neighbor);
            }
          }
        }
      }

      // we're done if we haven't added any more facets in this loop
      if (facets.size() == facetSize) {
        break;
      }

    }

    return Collections.unmodifiableSet(facets);
  }

  /**
   * Return a contiguous set of facets with centers within a specified radius of a point. The point
   * does not have to be on the shape model. This probably won't work well for points that are
   * inside the shape model.
   * 
   * @param point
   * @param radius
   * @return
   */
  public Set<TriangularFacet> getFacetsWithinRadius(UnwritableVectorIJK point, double radius) {

    Set<UnwritableVectorIJK> vertices = getVerticesWithinRadius(point, radius);
    Set<TriangularFacet> facets = new TreeSet<>(new Comparator<TriangularFacet>() {

      @Override
      public int compare(TriangularFacet o1, TriangularFacet o2) {
        double dist1 = point.getDistance(o1.getCenter());
        double dist2 = point.getDistance(o2.getCenter());
        return Double.compare(dist1, dist2);
      }

    });

    for (UnwritableVectorIJK v : vertices) {
      List<TriangularFacet> facetList = vertexFacetMap.get(v);
      for (TriangularFacet facet : facetList) {
        if (!facets.contains(facet)) {
          if (facet.getCenter().getDistance(point) < radius) {
            facets.add(facet);
          }
        }
      }
    }

    return facets;
  }

  /**
   * Return all vertices of a contiguous set of facets within a specified radius of a point. The
   * point does not have to be on the shape model. This probably won't work well for points that are
   * inside the shape model.
   * 
   * @param point
   * @param radius
   * @return
   */
  public Set<UnwritableVectorIJK> getVerticesWithinRadius(UnwritableVectorIJK point,
      double radius) {

    Set<UnwritableVectorIJK> vertices = new TreeSet<>(VERTEX_DISTANCE_COMPARATOR(point));

    NavigableSet<TriangularFacet> facets =
        getIntersections(getCenter(), VectorIJK.subtract(point, getCenter()));

    while (true) {

      Set<TriangularFacet> currentFacets = new HashSet<>(facets);
      int facetSize = currentFacets.size();

      for (TriangularFacet facet : currentFacets) {
        for (TriangularFacet neighbor : getFacetNeighbors(facet)) {
          for (UnwritableVectorIJK vertex : neighbor.getVertices()) {
            if (!vertices.contains(vertex)) {
              if (vertex.getDistance(point) < radius) {
                vertices.add(vertex);
                facets.add(neighbor);
              }
            }
          }
        }
      }

      // we're done if we haven't added any more facets in this loop
      if (facets.size() == facetSize) {
        break;
      }

    }

    return vertices;
  }

  /**
   * Apply the rotation about the center of the shape model
   * 
   * @param rotate
   * @return
   */
  public TriangularMesh rotate(UnwritableRotationMatrixIJK rotate) {
    Builder builder = new Builder();
    UnwritableVectorIJK center = getCenter();
    for (UnwritableVectorIJK v : vertexList) {
      VectorIJK rotatedVector = VectorIJK.add(rotate.mxv(VectorIJK.subtract(v, center)), center);
      builder.addVertex(rotatedVector);
    }

    for (TriangularFacet f : facetList) {
      builder.addFacet(vertexList.indexOf(f.getVertex1()), vertexList.indexOf(f.getVertex2()),
          vertexList.indexOf(f.getVertex3()));
    }

    return builder.build();
  }

  /**
   * Return a new Triangular mesh scaled by the specified value
   * 
   * @param scale
   * @return
   */
  public TriangularMesh scale(double scale) {

    Builder builder = new Builder();
    UnwritableVectorIJK center = getCenter();
    for (UnwritableVectorIJK v : vertexList) {
      VectorIJK scaledVector = VectorIJK.add(VectorIJK.subtract(v, center).scale(scale), center);
      builder.addVertex(scaledVector);
    }

    for (TriangularFacet f : facetList) {
      builder.addFacet(vertexList.indexOf(f.getVertex1()), vertexList.indexOf(f.getVertex2()),
          vertexList.indexOf(f.getVertex3()));
    }

    return builder.build();
  }

  /**
   * Create a mesh from the supplied facets
   * 
   * @param facets
   * @return
   */
  public TriangularMesh subset(Collection<TriangularFacet> facets) {

    Set<UnwritableVectorIJK> vertexSet = new HashSet<>();
    for (TriangularFacet facet : facets) {
      vertexSet.addAll(facet.getVertices());
    }

    List<UnwritableVectorIJK> vertexList = new ArrayList<>();
    vertexList.addAll(vertexSet);

    Builder builder = new Builder();
    for (UnwritableVectorIJK vertex : vertexList) {
      builder.addVertex(vertex);
    }
    for (TriangularFacet facet : facets) {
      builder.addFacet(vertexList.indexOf(facet.getVertex1()),
          vertexList.indexOf(facet.getVertex2()), vertexList.indexOf(facet.getVertex3()));
    }

    return builder.build();
  }

  /**
   * Return a new Triangular mesh translated by the specified vector
   * 
   * @param translate
   * @return
   */
  public TriangularMesh translate(UnwritableVectorIJK translate) {

    Builder builder = new Builder();
    for (UnwritableVectorIJK v : vertexList) {
      VectorIJK translatedVector = VectorIJK.add(v, translate);
      builder.addVertex(translatedVector);
    }

    for (TriangularFacet f : facetList) {
      builder.addFacet(vertexList.indexOf(f.getVertex1()), vertexList.indexOf(f.getVertex2()),
          vertexList.indexOf(f.getVertex3()));
    }

    return builder.build();
  }

  /**
   * Calculate facets intersected by the ray with its vertex at origin and pointing towards
   * direction. Facets are ordered by distance to origin. If the origin is on a facet, check that
   * the first intersection is not the facet containing the origin.
   * 
   * @param origin
   * @param direction
   * @return
   */
  public NavigableSet<TriangularFacet> getIntersections(UnwritableVectorIJK origin,
      UnwritableVectorIJK direction) {

    /*-
    		 List<TriangularFacet> list = facetList.parallelStream()
    		 .filter(f -> lookDir.getDirection().getDot(VectorIJK.subtract(f.getCenter(),
    		 getCenter())) < 0
    		 && f.intersects(lookDir))
    		 .collect(Collectors.toList());
    		 */

    // if the origin is inside the bounding box, do a check to ensure that any
    // intersections are along the specified direction
    final boolean outsideBox = !getOctree().getBoundingBox(0).closedContains(origin);

    // find the list of bounding boxes intersected by the ray
    Set<Integer> boxIndices = new HashSet<>();
    boxIndices.add(0);
    for (int level = 0; level < octreeLevel; level++) {
      Set<Integer> nextIndices = new HashSet<>();
      for (int index : boxIndices) {
        if (getOctree().getBoundingBox(index).intersects(origin, direction)) {
          nextIndices.add(index);
        }
      }
      boxIndices = new HashSet<>();
      for (int index : nextIndices) {
        boxIndices.addAll(getOctree().contains(index));
      }
    }
    // intersection with set of boxes containing facets
    boxIndices.retainAll(facetOctreeMap.keySet());

    // now collect all the facets intersected by the ray, sorted by distance to the
    // origin
    NavigableSet<TriangularFacet> intersectingFacets =
        new TreeSet<>(new Comparator<TriangularFacet>() {
          @Override
          public int compare(TriangularFacet o1, TriangularFacet o2) {
            return Double.compare(VectorIJK.subtract(o1.getCenter(), origin).getLength(),
                VectorIJK.subtract(o2.getCenter(), origin).getLength());
          }

        });

    // Set<TriangularFacet> candidates = new HashSet<>();
    for (int index : boxIndices) {
      Set<TriangularFacet> candidates = facetOctreeMap.get(index);
      for (TriangularFacet f : candidates) {
        if (f.intersects(origin, direction)) {
          if (outsideBox || VectorIJK.subtract(f.getCenter(), origin).getDot(direction) > 0) {
            intersectingFacets.add(f);
          }
        }
      }
      // candidates.addAll(facetOctreeMap.get(index));
    }

    // intersectingFacets.addAll(candidates.parallelStream().filter(f ->
    // f.intersects(origin, direction)).collect(Collectors.toSet()));

    return intersectingFacets;
  }

  /**
   * Draw a ray from the Sun to the facet center. If the nearest facet intersected by the ray is
   * this facet, it is not in shadow.
   * <p>
   * TODO: use finite size of sun, allow for partial shadowing.
   * 
   * @param f
   * @param sunPos sun position in the mesh coordinate system
   * @return 0 if not in shadow, 1 if shadowed
   */
  public double isInShadow(TriangularFacet f, UnwritableVectorIJK sunPos) {
    double shadow = 1;
    VectorIJK lookDir = VectorIJK.subtract(f.getCenter(), sunPos);
    if (f.getNormal().getDot(lookDir) < 0) {
      NavigableSet<TriangularFacet> list = getIntersections(sunPos, lookDir);
      if (list.size() > 0 && f.equals(list.first())) {
        shadow = 0.;
      }
    }
    return shadow;
  }

  @Override
  public VectorIJK computeOutwardNormal(UnwritableVectorIJK surfacePoint, VectorIJK buffer) {
    NavigableSet<TriangularFacet> list =
        getIntersections(getCenter(), VectorIJK.subtract(surfacePoint, getCenter()));
    if (list.size() > 0) {
      // note this is the closest facet to surfacePoint but may not be the last facet
      // intersected by the ray on its way out
      buffer.setTo(list.first().getNormal());
    }

    return buffer;
  }

  @Override
  public boolean intersects(UnwritableVectorIJK source, UnwritableVectorIJK ray) {

    // set of boxes intersected by the ray
    Set<Integer> boxIndices = getOctree().contains(0);
    for (int level = 1; level < octreeLevel; level++) {
      Set<Integer> nextIndices = new HashSet<>();
      for (int index : boxIndices) {
        if (getOctree().getBoundingBox(index).intersects(source, ray)) {
          nextIndices.add(index);
        }
      }
      boxIndices = new HashSet<>();
      for (int index : nextIndices) {
        boxIndices.addAll(getOctree().contains(index));
      }
    }

    // intersection with set of boxes containing facets
    boxIndices.retainAll(facetOctreeMap.keySet());
    for (int index : boxIndices) {
      Set<TriangularFacet> candidates = facetOctreeMap.get(index);
      for (TriangularFacet f : candidates) {
        if (f.intersects(source, ray)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public VectorIJK compute(UnwritableVectorIJK source, UnwritableVectorIJK ray, VectorIJK buffer) {
    NavigableSet<TriangularFacet> list = getIntersections(source, ray);
    if (list.size() > 0) {
      buffer.setTo(list.first().getCenter());
    }

    return buffer;
  }

}
