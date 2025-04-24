package terrasaur.utils.octree;

import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementation of a 3D heirarchical octree. The level 0 octree is set to the user supplied
 * bounding box. The level 1 octree consists of eight bounding boxes constructed by dividing each
 * axis of the level 0 octree in half. Each of these bounding boxes is further divided to create the
 * level 3 octree and so on.
 * <p>
 * Each bounding box in the octree structure has a unique index. Its level and position can be
 * determined by the index. The index ranges for the first ten octree levels are given below along
 * with the edge length of each bounding box in the level, starting with a box of unit length in
 * each dimension.
 * <p>
 * <table>
 * <tr>
 * <td>Level</td>
 * <td>min index</td>
 * <td>max index</td>
 * <td>Edge length</td>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>0</td>
 * <td>0</td>
 * <td>1.000000000</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1</td>
 * <td>8</td>
 * <td>0.500000000</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>9</td>
 * <td>72</td>
 * <td>0.250000000</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>73</td>
 * <td>584</td>
 * <td>0.125000000</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>585</td>
 * <td>4680</td>
 * <td>0.062500000</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>4681</td>
 * <td>37448</td>
 * <td>0.031250000</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>37449</td>
 * <td>299592</td>
 * <td>0.015625000</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>299593</td>
 * <td>2396744</td>
 * <td>0.007812500</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>2396745</td>
 * <td>19173960</td>
 * <td>0.003906250</td>
 * </tr>
 * <tr>
 * <td>9</td>
 * <td>19173961</td>
 * <td>153391688</td>
 * <td>0.001953125</td>
 * </tr>
 * </table>
 * 
 * This class supports up to 10 levels in the hierarchy. More boxes will overflow a 32 bit int.
 * 
 * @author nairah1
 *
 */
public class Octree {

  /**
   * need to use longs for index if a higher MAX_LEVEL is desired
   */
  public static final int MAX_LEVEL = 10;

  private BoundingBox boundingBox;
  private Map<Integer, Integer> startIndexMap;
  private NavigableMap<Integer, Integer> indexToLevelMap;
  private Map<Integer, Set<Integer>> containsMap = new HashMap<>();

  /**
   * Create an Octree from this BoundingBox
   * 
   * @param boundingBox
   */
  public Octree(BoundingBox boundingBox) {
    this.boundingBox = boundingBox;

    indexToLevelMap = new TreeMap<>();
    startIndexMap = new HashMap<>();
    startIndexMap.put(0, 0);
    indexToLevelMap.put(0, 0);
    for (int i = 1; i < MAX_LEVEL + 1; i++) {
      startIndexMap.put(i, getMinIndex(i));
      indexToLevelMap.put(getMaxIndex(i), i);
    }
  }

  private int getMinIndex(int level) {
    if (level < 1) {
      return 0;
    }
    return getMinIndex(level - 1) + (int) Math.pow(8, level - 1);
  }

  private int getMaxIndex(int level) {
    return getMinIndex(level) + (int) Math.pow(8, level) - 1;
  }

  /**
   * Return a set of indices contained by the bounding box defined by index
   * 
   * @param index
   * @return
   */
  public Set<Integer> contains(int index) {
    Set<Integer> indices = new HashSet<>();

    if (containsMap.containsKey(index)) {
      indices = containsMap.get(index);
    } else {
      int level = getLevel(index);
      int startIndex = startIndexMap.get(level);
      startIndexMap.put(level + 1, getMinIndex(level + 1));
      indexToLevelMap.put(getMaxIndex(level + 1), level + 1);

      int side = (int) Math.pow(2, level);
      int iindex = index - startIndex;

      int iz = iindex / side / side;
      int iy = (iindex - iz * side * side) / side;
      int ix = iindex - side * (iy + iz * side);

      side *= 2;
      startIndex = startIndexMap.get(level + 1);

      ix *= 2;
      iy *= 2;
      iz *= 2;

      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 2; j++) {
          for (int k = 0; k < 2; k++) {
            indices.add((ix + i) + side * (iy + j + side * (iz + k)) + startIndex);
          }
        }
      }
      containsMap.put(index, indices);
    }
    return indices;
  }

  /**
   * Given the index of a box, find its level in the Octree
   * 
   * @param index
   * @return
   */
  public int getLevel(int index) {
    return indexToLevelMap.get(indexToLevelMap.ceilingKey(index));
  }

  /**
   * Given a 3D point and level return the index of the smallest box containing the point
   * 
   * @param point
   * @param level
   * @return
   */
  public int getIndex(UnwritableVectorIJK point, int level) {
    UnwritableVectorIJK minPt = boundingBox.minPt();
    UnwritableVectorIJK maxPt = boundingBox.maxPt();

    double xScale = maxPt.getI() - minPt.getI();
    double yScale = maxPt.getJ() - minPt.getJ();
    double zScale = maxPt.getK() - minPt.getK();
    double x = (point.getI() - minPt.getI()) / xScale;
    double y = (point.getJ() - minPt.getJ()) / yScale;
    double z = (point.getK() - minPt.getK()) / zScale;

    int startIndex = startIndexMap.get(level);
    int side = (int) Math.pow(2, level);

    int ix = (int) (x * side);
    int iy = (int) (y * side);
    int iz = (int) (z * side);

    return ix + side * (iy + side * iz) + startIndex;
  }

  /**
   * Return the bounding box corresponding to index
   * 
   * @param index
   * @return
   */
  public BoundingBox getBoundingBox(int index) {
    int level = getLevel(index);
    long startIndex = startIndexMap.get(level);

    int side = (int) Math.pow(2, level);
    long iindex = index - startIndex;

    long iz = iindex / side / side;
    long iy = iindex / side - iz * side;
    long ix = iindex - side * (iy + iz * side);

    double minX = ((double) ix) / side;
    double maxX = (ix + 1.) / side;
    double minY = ((double) iy) / side;
    double maxY = (iy + 1.) / side;
    double minZ = ((double) iz) / side;
    double maxZ = (iz + 1.) / side;

    UnwritableVectorIJK minPt = boundingBox.minPt();
    UnwritableVectorIJK maxPt = boundingBox.maxPt();
    double xScale = maxPt.getI() - minPt.getI();
    minX = xScale * minX + minPt.getI();
    maxX = xScale * maxX + minPt.getI();
    double yScale = maxPt.getJ() - minPt.getJ();
    minY = yScale * minY + minPt.getJ();
    maxY = yScale * maxY + minPt.getJ();
    double zScale = maxPt.getK() - minPt.getK();
    minZ = zScale * minZ + minPt.getK();
    maxZ = zScale * maxZ + minPt.getK();

    return new BoundingBox(new UnwritableVectorIJK(minX, minY, minZ),
        new UnwritableVectorIJK(maxX, maxY, maxZ));
  }

  public static void main(String[] args) {
    int index = 0;
    BoundingBox bb = new BoundingBox(VectorIJK.ZERO, new UnwritableVectorIJK(1, 1, 1));
    Octree o = new Octree(bb);
    Set<Integer> indices = o.contains(index);
    BoundingBox orig = o.getBoundingBox(index);
    System.out.println(orig);
    for (int i : indices) {
      System.out.println(o.getBoundingBox(i));
    }

    UnwritableVectorIJK point = new UnwritableVectorIJK(0.1, 6.2, 0.77);
    for (int level = 0; level < Octree.MAX_LEVEL; level++) {
      index = o.getIndex(point, level);
      System.out.printf("%d %d %s\n", level, index, o.getBoundingBox(index));
    }
    /*-
    		for (int level = 0; level < Octree.MAX_LEVEL; level++) {
    			index = o.getIndex(point, level);
    			BoundingBox first = o.getBoundingBox(o.getMinIndex(level));
    			System.out.printf("<tr><td>%d</td><td>%9d</td><td>%9d</td><td>%.9f</td></tr>\n", level, o.getMinIndex(level), o.getMaxIndex(level),
    					first.getxRange().getLength());
    		}
    */
  }

}
