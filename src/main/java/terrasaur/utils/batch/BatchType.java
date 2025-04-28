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
 * Defines type of processing to be done on a batch of commands. Usually a form of local or
 * clustered network processing.
 * 
 * @author espirrc1
 *
 */
public enum BatchType {

  GRID_ENGINE, GRID_ENGINE_2, GRID_ENGINE_3, GRID_ENGINE_4, GRID_ENGINE_6, GRID_ENGINE_8, GRID_ENGINE_10, GRID_ENGINE_16, GRID_ENGINE_17, GRID_ENGINE_23, GRID_ENGINE_32, GRID_ENGINE_33, GRID_ENGINE_35, GRID_ENGINE_45, GNU_PARALLEL, LOCAL_SEQUENTIAL;

  private static Logger logger = LogManager.getLogger(BatchType.class);

  /**
   * Return the BatchType appropriate to the desired number of slots per job.
   * 
   * @param numSlotsPerJob
   * @return
   */
  public static BatchType getSlotsType(int numSlotsPerJob) {

    if (numSlotsPerJob == 2) {
      return GRID_ENGINE_2;
    } else if (numSlotsPerJob == 3) {
      return GRID_ENGINE_3;
    } else if (numSlotsPerJob == 4) {
      return GRID_ENGINE_4;
    } else if (numSlotsPerJob == 6) {
      return GRID_ENGINE_6;
    } else if (numSlotsPerJob == 8) {
      return GRID_ENGINE_8;
    } else if (numSlotsPerJob == 10) {
      return GRID_ENGINE_10;
    } else if (numSlotsPerJob == 16) {
      return GRID_ENGINE_16;
    } else if (numSlotsPerJob == 17) {
      return GRID_ENGINE_17;
    } else if (numSlotsPerJob == 23) {
      return GRID_ENGINE_23;
    } else if (numSlotsPerJob == 32) {
      return GRID_ENGINE_32;
    } else if (numSlotsPerJob == 33) {
      return GRID_ENGINE_33;
    } else if (numSlotsPerJob == 35) {
      return GRID_ENGINE_35;
    } else if (numSlotsPerJob == 45) {
      return GRID_ENGINE_45;
    } else {
      String errMesg = "ERROR! Can only support 2, 3, 4, 6, 8, 10, 16, 17, or 32 slots per job.";
      throw new RuntimeException(errMesg);
    }
  }

  /**
   * Return the number of slots per job as indicated by the BatchType. Only applicable for
   * GRID_ENGINE_# enums. The rest will return 999.
   * 
   * @param gridBatchType
   * @return
   */
  public static int slotPerJob(BatchType gridBatchType) {
    switch (gridBatchType) {
      case GRID_ENGINE_2:
        return 2;

      case GRID_ENGINE_3:
        return 3;

      case GRID_ENGINE_4:
        return 4;

      case GRID_ENGINE_6:
        return 6;

      case GRID_ENGINE_8:
        return 8;

      case GRID_ENGINE_10:
        return 10;

      case GRID_ENGINE_16:
        return 16;

      case GRID_ENGINE_17:
        return 17;

      case GRID_ENGINE_23:
        return 23;

      case GRID_ENGINE_32:
        return 32;

      case GRID_ENGINE_33:
        return 33;

      case GRID_ENGINE_35:
        return 35;

      case GRID_ENGINE_45:
        return 45;

      default:
        logger.warn(
            "WARNING: slotsPerJob not applicable to BatchType:" + gridBatchType.toString());
        logger.warn("returning 999");
        return 999;
    }

  }

}
