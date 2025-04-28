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
package terrasaur.utils.tessellation;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;
import picante.math.vectorspace.VectorIJK;

/**
 * See
 * 
 * https://www.researchgate.net/publication/230745267_Spiral_Tessellation_on_the_Sphere
 * 
 * <p>
 * based on Bob Demajistre's code for SIMPLEX
 * 
 * 
 * @author nairah1
 *
 */
public class AbrateTessellation implements SphericalTessellation {

  private static Logger logger = LogManager.getLogger(AbrateTessellation.class);

  /* employs naming conventions from the paper where possible */
  private long n;
  private long m;

  /**
   * number of non-polar tiles in the tessellation (which is total number of tiles - 2). Use
   * {@link #getNumTiles()} to get the actual number of tiles in the tessellation.
   * 
   * @return
   */
  public long getM() {
    return m;
  }

  /**
   * Number of spiral turns in the tessellation
   * 
   * @return
   */
  public long getN() {
    return n;
  }

  /**
   * Create a set of equal area tiles approximately uniformly distributed about the unit sphere. The
   * number of tiles will not necessarily equal to npts. Use {@link #getNumTiles()} to get the
   * actual number of tiles in the tessellation.
   * 
   * @param nTiles Number of tiles to use
   */
  public AbrateTessellation(long nTiles) {
    double tileArea = 4 * FastMath.PI / nTiles;
    // eq 6
    n = FastMath.round(FastMath.PI / FastMath.sqrt(tileArea));
    // eq 7
    m = FastMath.round(
        4. * FastMath.PI * FastMath.sin(FastMath.sqrt(tileArea)) / FastMath.pow(tileArea, 1.5));
  }

  /**
   * Create a set of equal area tiles approximately uniformly distributed about the unit sphere.
   * 
   * @param n number of spiral turns
   * @param m number of non-polar tiles (which is total # of tiles - 2)
   */
  public AbrateTessellation(long n, long m) {
    this.n = n;
    this.m = m;
  }

  /**
   * Create a set of tiles with the same areas and positions as the supplied tessellation.
   * 
   * @param other
   */
  public AbrateTessellation(AbrateTessellation other) {
    this(other.n, other.m);
  }

  /**
   * Return a map where the key is the tile index and the value being a set of tile indices from the
   * other tessellation with centers contained within the key tile.
   * 
   * @param other should be a tessellation with smaller tiles, but doesn't have to be.
   * @return
   */
  public Map<Long, Set<Long>> mapTiles(AbrateTessellation other) {
    Map<Long, Set<Long>> mapTiles = new HashMap<>();

    for (long otherTile = 0; otherTile < other.getNumTiles(); otherTile++) {
      long thisTile = getTileIndex(other.getTileCenter(otherTile));
      Set<Long> tiles = mapTiles.get(thisTile);
      if (tiles == null) {
        tiles = new HashSet<>();
        mapTiles.put(thisTile, tiles);
      }
      tiles.add(otherTile);
    }

    return mapTiles;
  }

  /**
   * returns a vector to the position along the spiral, based on equation 1
   * 
   * @param t allowed range is -PI/2 to PI/2
   * @return
   */
  private UnwritableVectorIJK gamma(double t) {
    if (t < -FastMath.PI / 2) {
      return new UnwritableVectorIJK(0, 0, 1);
    }
    if (t > FastMath.PI / 2) {
      return new UnwritableVectorIJK(0, 0, -1);
    }

    double x = FastMath.cos(t) * FastMath.cos(n * FastMath.PI + 2. * n * t);
    double y = FastMath.cos(t) * FastMath.sin(n * FastMath.PI + 2. * n * t);
    double z = -FastMath.sin(t);
    UnwritableVectorIJK result = new UnwritableVectorIJK(x, y, z);
    return result;
  }

  /**
   * returns a vector to the position along the spiral, based on equation 1
   * 
   * @param t allowed range is -PI/2 to PI/2
   * @return
   */
  private LatitudinalVector gammaLV(double t) {
    return CoordConverters.convertToLatitudinal(gamma(t));
  }

  /**
   * returns the position along the spiral for a given tile (eqn 4)
   * 
   * @param itile
   * @return
   */
  private double tfun(double itile) {
    double result =
        FastMath.acos(FastMath.cos(FastMath.PI / (2. * n)) * (1. - 2. * (itile - 1.) / m))
            - (n + 1.) * FastMath.PI / (2. * n);
    return result;
  }

  /**
   * Total number of tiles covering the surface. This is {@link #getM()} + 2 (polar tiles).
   * 
   * @return
   */
  @Override
  public long getNumTiles() {
    return m + 2;
  }

  /**
   * Returns the XYZ position of the desired vertex for a given tile in the tessellation. If this is
   * the north or south pole the zero vector is returned. This value is also returned if the vnum is
   * less than 0 or greater than 3
   * 
   * @param tile
   * @param vnum vertex number of the quadrilateral:
   *        <table>
   *        <tr>
   *        <td>0</td>
   *        <td>upper left (the northwest corner)</td>
   *        </tr>
   *        <tr>
   *        <td>1</td>
   *        <td>upper right</td>
   *        </tr>
   *        <tr>
   *        <td>2</td>
   *        <td>lower right (southeast corner)</td>
   *        </tr>
   *        <tr>
   *        <td>3</td>
   *        <td>lower left</td>
   *        </tr>
   *        </table>
   * @return returns the XYZ position of the desired vertex for a given tile in the tessellation
   */
  public UnwritableVectorIJK tileVertexIJK(long tile, int vnum) {

    if (tile == 0) {
      return new UnwritableVectorIJK(0, 0, 1);
    }
    if (tile == m + 1) {
      return new UnwritableVectorIJK(0, 0, -1);
    }

    UnwritableVectorIJK result = new UnwritableVectorIJK(0, 0, 0);

    if (vnum < 0 || vnum > 3) {
      return result;
    }

    long mytile = ((vnum == 1) || (vnum == 2)) ? tile + 1 : tile;
    double tadd = 0.;
    if ((vnum == 2) || (vnum == 3)) {
      tadd = FastMath.PI / n;
    }

    result = gamma(tfun(mytile) + tadd);

    return result;
  }

  /**
   * Returns the longitude, latitude pair (radians) for a given tile in the tessellation. If this is
   * the north or south pole the zero vector is returned. This value is also returned if the vnum is
   * less than 0 or greater than 3.
   * 
   * @param tile
   * @param vnum vertex number of the quadrilateral:
   *        <table>
   *        <tr>
   *        <td>0</td>
   *        <td>upper left (the northwest corner)</td>
   *        </tr>
   *        <tr>
   *        <td>1</td>
   *        <td>upper right</td>
   *        </tr>
   *        <tr>
   *        <td>2</td>
   *        <td>lower right (southeast corner)</td>
   *        </tr>
   *        <tr>
   *        <td>3</td>
   *        <td>lower left</td>
   *        </tr>
   *        </table>
   * @return returns the longitude, latitude pair (radians) for a given tile in the tessellation
   */
  public LatitudinalVector tileVertex(long tile, int vnum) {
    return CoordConverters.convertToLatitudinal(tileVertexIJK(tile, vnum));
  }

  @Override
  public long getTileIndex(LatitudinalVector lv) {
    long k = (long) ((n * FastMath.PI - lv.getLongitude() - 2. * n * lv.getLatitude())
        / (2. * FastMath.PI) + 1e-12);
    double tt = lv.getLongitude() / (2. * n) + (FastMath.PI / n) * k - FastMath.PI / 2.;
    double id = (m
        * (FastMath.cos(FastMath.PI / (2. * n))
            - FastMath.cos(tt + (n + 1.) * FastMath.PI / (2. * n)))
        / (2. * FastMath.cos(FastMath.PI / (2. * n)))) + 1.;
    long result = (long) id;

    return result;
  }

  @Override
  public LatitudinalVector getTileCenter(long i) {
    return CoordConverters.convertToLatitudinal(getTileCenterIJK(i));
  }

  /**
   * Get the tile center as an {@link UnwritableVectorIJK}.
   * 
   * @param i
   * @return
   */
  public UnwritableVectorIJK getTileCenterIJK(long i) {
    VectorIJK buffer = new VectorIJK();
    if (i == 0) {
      buffer.setTo(0, 0, 1);
    } else if (i == m + 1) {
      buffer.setTo(0, 0, -1);
    } else {
      for (int vnum = 0; vnum < 4; vnum++) {
        buffer.setTo(VectorIJK.add(buffer, tileVertexIJK(i, vnum)));
      }
      buffer.unitize();
    }
    return buffer;
  }

  /**
   * return tile to the left of this tile i.e., adjoining side 3-0
   * 
   * @param tile
   * @return
   */
  public long leftTile(long tile) {
    long result = tile - 1;
    return result < 0 ? 0 : result;
  }

  /**
   * return tile to the right of this tile i.e., adjoining side 1-2
   * 
   * @param tile
   * @return
   */
  public long rightTile(long tile) {
    long result = tile + 1;
    return result > m + 1 ? m + 1 : result;
  }

  /**
   * return a sorted set of tiles above or northward of a given tile, i.e., adjoining side 0-1
   * 
   * @param tile
   * @return
   */
  public NavigableSet<Long> aboveTiles(long tile) {
    NavigableSet<Long> result = new TreeSet<Long>();

    double ti = tfun(tile);
    double ti1 = tfun(tile + 1);
    double pin = FastMath.PI / n;
    double pi2 = FastMath.PI / 2.;

    if (ti1 < (-pi2 + pin)) {
      return result; // nothing north of this
    }
    LatitudinalVector p2 = gammaLV(ti1 - pin);
    long d1 = 0;
    /* the small thing is to make handle precision problems */
    double small = pin * 0.05; // this is 0.05 of a tile extent in latitude

    long d2 = getTileIndex(new LatitudinalVector(1, p2.getLatitude() - small, p2.getLongitude()));
    if (FastMath.abs(d2 - tile) <= 1) {
      d2 = getTileIndex(new LatitudinalVector(1, p2.getLatitude() + small, p2.getLongitude()));
    }
    if (ti > (-pi2 + pin)) {
      LatitudinalVector p1 = gammaLV(ti - pin);
      d1 = getTileIndex(new LatitudinalVector(1, p1.getLatitude() - small, p1.getLongitude()));
      if (FastMath.abs(d1 - tile) <= 1) {
        d1 = getTileIndex(new LatitudinalVector(1, p1.getLatitude() + small, p1.getLongitude()));
      }
    }

    for (long ic = d1; ic <= d2; ic++) {
      result.add(ic);
    }
    return result;
  }

  /**
   * return a sorted set of tiles below or southward of a given tile, i.e., adjoining side 2-3
   * 
   * @param tile
   * @return
   */
  public NavigableSet<Long> belowTiles(long tile) {
    NavigableSet<Long> result = new TreeSet<Long>();

    double ti = tfun(tile);
    double ti1 = tfun(tile + 1);
    double pin = FastMath.PI / n;
    double pi2 = FastMath.PI / 2.;

    if (tile > m) {
      return result; // null
    }
    if (ti > (pi2 - 2. * pin)) {
      result.add(m + 1);
      return result;
    }
    LatitudinalVector p3 = gammaLV(ti1 + pin);
    long d4 = m + 1;
    /* the small thing is to make handle precision problems */
    double small = pin * 0.05; // this is 0.05 of a tile extent in latitude

    long d3 = getTileIndex(p3);
    if (FastMath.abs(d3 - tile) <= 1) {
      d3 = getTileIndex(new LatitudinalVector(1, p3.getLatitude() - small, p3.getLongitude()));
    }

    if (ti1 < (pi2 - 2. * pin)) {
      LatitudinalVector p4 = gammaLV(ti + pin);
      d4 = getTileIndex(p4);
      if (FastMath.abs(d4 - tile) <= 1) {
        d4 = getTileIndex(new LatitudinalVector(1, p4.getLatitude() - small, p4.getLongitude()));
      }

    } else {
      result.add(m + 1);
      return result;
    }

    for (long ic = d4; ic <= d3; ic++) {
      result.add(ic);
    }

    return result;
  }

  /**
   * add all matching tiles along the spiral to the left and right of tileInRegion
   * 
   * @param matches existing set of matching tiles
   * @param tileInRegion starting tile to test
   * @param predicate function to test tile center
   * @return
   */
  private void addLeftAndRightTiles(Set<Long> matches, Long tileInRegion,
      Predicate<UnwritableVectorIJK> predicate) {

    if (!matches.contains(tileInRegion) && predicate.test(getTileCenterIJK(tileInRegion))) {
      matches.add(tileInRegion);
    }

    long tileIndex = rightTile(tileInRegion);
    while (true) {
      if (!matches.contains(tileIndex)) {
        if (!predicate.test(getTileCenterIJK(tileIndex))) {
          break;
        }
        matches.add(tileIndex);
      }
      tileIndex = rightTile(tileIndex);
      // south pole
      if (tileIndex == m + 1) {
        matches.add(tileIndex);
        break;
      }
    }

    tileIndex = leftTile(tileInRegion);
    while (true) {
      if (!matches.contains(tileIndex)) {
        if (!predicate.test(getTileCenterIJK(tileIndex))) {
          break;
        }
        matches.add(tileIndex);
      }
      tileIndex = leftTile(tileIndex);
      // north pole
      if (tileIndex == 0) {
        matches.add(tileIndex);
        break;
      }
    }

  }

  /**
   * Check every tile in the tessellation against the predicate.
   * 
   * @param predicate
   * @return
   */
  public Set<Long> matchingTilesIJK(Predicate<UnwritableVectorIJK> predicate) {
    Set<Long> matches = new HashSet<>();

    for (long tile = 0; tile < getNumTiles(); tile++) {
      if (predicate.test(getTileCenterIJK(tile))) {
        matches.add(tile);
      }
    }
    return matches;
  }

  /**
   * Get the {@link Set} of contiguous tiles which satisfy the predicate.
   * 
   * @param tileInRegion starting tile to test
   * @param predicate function to test tile center
   * @return set of tiles that satisfy the supplied predicate in the contiguous region including
   *         tileInRegion
   */
  public Set<Long> matchingTiles(Long tileInRegion, Predicate<UnwritableVectorIJK> predicate) {
    Set<Long> matches = new HashSet<>();

    Set<Long> tilesInOriginalLine = new HashSet<>();
    addLeftAndRightTiles(tilesInOriginalLine, tileInRegion, predicate);

    Set<Long> tilesInLine = tilesInOriginalLine;
    matches.addAll(tilesInLine);

    // Add all tiles above this line
    while (true) {
      if (tilesInLine.size() == 0) {
        break;
      }

      Set<Long> aboveTiles = new HashSet<>();
      for (Long tile : tilesInLine) {
        aboveTiles.addAll(aboveTiles(tile));
      }

      tilesInLine = new HashSet<>();
      for (Long tile : aboveTiles) {
        addLeftAndRightTiles(tilesInLine, tile, predicate);
      }

      // if this is false, then no new tiles were added to matches
      if (!matches.addAll(tilesInLine)) {
        break;
      }
    }

    tilesInLine = tilesInOriginalLine;
    // Add all tiles below this line
    while (true) {
      if (tilesInLine.size() == 0) {
        break;
      }

      Set<Long> belowTiles = new HashSet<>();
      for (Long tile : tilesInLine) {
        belowTiles.addAll(belowTiles(tile));
      }

      tilesInLine = new HashSet<>();
      for (Long tile : belowTiles) {
        addLeftAndRightTiles(tilesInLine, tile, predicate);
      }

      // if this is false, then no new tiles were added to matches
      if (!matches.addAll(tilesInLine)) {
        break;
      }
    }

    return matches;
  }

  /**
   * Return the set of tiles whose centers are contained within outline.
   * 
   * @param outline
   * @return
   */
  public Set<Long> tilesWithin(List<LatitudinalVector> outline) {

    if (outline.size() == 0) {
      return new HashSet<>();
    }

    VectorIJK centerIJK = new VectorIJK();
    for (LatitudinalVector lv : outline) {
      centerIJK.setTo(VectorIJK.add(centerIJK, CoordConverters.convert(lv)));
    }
    centerIJK.scale(1. / outline.size());

    LatitudinalVector center = CoordConverters.convertToLatitudinal(centerIJK);
    Long centerTile = getTileIndex(center);

    StereographicProjection proj = new StereographicProjection(center);
    LatitudinalVector first = outline.get(0);
    Point2D xy0 = proj.forward(first);
    Path2D.Double path = new Path2D.Double();
    path.moveTo(xy0.getX(), xy0.getY());
    for (int i = 1; i < outline.size(); i++) {
      Point2D xy = proj.forward(outline.get(i));
      path.lineTo(xy.getX(), xy.getY());
    }
    path.closePath();

    Predicate<UnwritableVectorIJK> withinPolygon = new Predicate<UnwritableVectorIJK>() {
      @Override
      public boolean test(UnwritableVectorIJK t) {
        LatitudinalVector lv = CoordConverters.convertToLatitudinal(t);
        Point2D xy = proj.forward(lv);
        return path.contains(xy);
      }
    };

    return matchingTiles(centerTile, withinPolygon);
  }

  public static void main(String[] args) {

    AbrateTessellation abt = new AbrateTessellation(100, 12156);
    long index = 5604;

    logger.printf(Level.INFO, "numTiles %d n %d m %d", abt.getNumTiles(), abt.n, abt.m);
    logger.info("Left " + abt.leftTile(index));
    logger.info("Right " + abt.rightTile(index));
    StringBuffer sb = new StringBuffer();
    for (long tile : abt.aboveTiles(index)) {
      sb.append(String.format("%d ", tile));
    }
    logger.info("Above " + sb.toString());

    sb = new StringBuffer();
    for (long tile : abt.belowTiles(index)) {
      sb.append(String.format("%d ", tile));
    }
    logger.info("Below " + sb.toString());

  }

}
