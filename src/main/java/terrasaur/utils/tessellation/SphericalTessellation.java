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
