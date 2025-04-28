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
