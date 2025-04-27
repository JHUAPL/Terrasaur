package terrasaur.utils.batch;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory class for returning concrete classes that implement BatchSubmit interface.
 * 
 * @author espirrc1
 *
 */
public class BatchSubmitFactory {
  
  private static Logger logger = LogManager.getLogger(BatchSubmitFactory.class);

  public static BatchSubmitI getBatchSubmit(List<String> commandList, BatchType batchType,
      GridType gridType) {

    // check whether or not gridType is valid for the run-time system.
    if (!gridType.checkType()) {
      logger.info("Changing batch submit to run in local mode!");
      gridType = GridType.LOCAL;
    }
    switch (gridType) {
      case SUNOPENGRID:
        logger.info("Will run " + gridType + " grid engine.");
        return new BatchSubmitOpenGrid(commandList, batchType);

      case LOCAL:
        logger.info("Will run in local sequential mode!");
        return new BatchSubmitLocal(commandList, BatchType.LOCAL_SEQUENTIAL);

      // default to returning class that runs in local mode.
      default:
        logger.info("Could not parse:" + gridType.toString() + ". Will run in local mode!");
        return new BatchSubmitLocal(commandList, batchType);
    }
  }

}
