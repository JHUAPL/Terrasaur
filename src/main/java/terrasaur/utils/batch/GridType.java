package terrasaur.utils.batch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Defines the type of grid engine that will be used to do the batch processing.
 * 
 * @author espirrc1
 *
 */
public enum GridType {
  
  SUNOPENGRID {

    // determine whether SUN open grid environment variable exists
    public boolean checkType() {
      if (System.getenv("SGE_ROOT") == null) {
        logger.error("Sun Open Grid engine environment variable $SGE_ROOT"
            + " not found. Cannot run in grid engine. Need to run in local mode.");
        return false;
      } else {
        return true;
      }
    }
  },

  LOCAL {
    // no distributed processing to be done.
    // all local processing.
    public boolean checkType() {
      return true;
    }

  };

  private static Logger logger = LogManager.getLogger(GridType.class);

  public abstract boolean checkType();

  /**
   * Parse the desired BatchGridType from the input string value. Will return with BatchGridType =
   * LOCAL if value could not be parsed.
   * 
   * @param value
   * @return
   */
  public static GridType parseType(String value) {

    return parseType(value, false);
  }

  /**
   * Parse the desired BatchGridType from the input string value. Will stop with a RuntimeException
   * if stopWithError is true.
   * 
   * @param value
   * @param stopWithError
   * @return
   */
  public static GridType parseType(String value, boolean stopWithError) {
    for (GridType gridType : values()) {
      if (gridType.toString().equals(value)) {
        return gridType;
      }
    }

    // fall to here if could not parse gridType from String.
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("Warning, could not parse open grid type from" + " string:" + value
        + ". Possible values are:\n");
    for (GridType gridType : values()) {
      sb.append(gridType.toString() + "\n");
    }
    String errMesg = sb.toString();

    if (stopWithError) {
      throw new RuntimeException(errMesg);
    } else {
      logger.warn("Warning, could not parse open grid type from" + " string:" + value);
      logger.warn("Will use " + LOCAL.toString());
      return LOCAL;
    }
  }

}
