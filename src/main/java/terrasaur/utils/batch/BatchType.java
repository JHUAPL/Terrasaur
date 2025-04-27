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
