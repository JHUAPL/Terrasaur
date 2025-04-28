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
