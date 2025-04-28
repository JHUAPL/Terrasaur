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

import picante.math.coords.CoordConverters;
import picante.math.coords.LatitudinalVector;
import picante.math.vectorspace.UnwritableVectorIJK;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public interface SphericalTessellation {

  /**
   * Number of tiles in the tessellation.
   * 
   * @return
   */
  public long getNumTiles();

  /**
   * Return the index of the tile containing the unit vector ijk.
   *
   * @param ijk
   * @return
   */
  public default long getTileIndex(UnwritableVectorIJK ijk) {
    return getTileIndex(CoordConverters.convertToLatitudinal(ijk));
  }

  /**
   * Return the index of the tile containing the unit vector lv.
   * 
   * @param lv
   * @return
   */
  public long getTileIndex(LatitudinalVector lv);

  /**
   * Return the unit latitudinal vector pointing to the tile center with index i.
   * 
   * @param i
   * @return
   */
  public LatitudinalVector getTileCenter(long i);

  /**
   * Return the set of tiles that satisfy the supplied predicate.
   * 
   * @param predicate
   * @return
   */
  public default Set<Long> matchingTiles(Predicate<LatitudinalVector> predicate) {
    Set<Long> matches = new HashSet<>();
    for (long i = 0; i < getNumTiles(); i++) {
      if (predicate.test(getTileCenter(i))) {
        matches.add(i);
      }
    }
    return matches;
  }

}
