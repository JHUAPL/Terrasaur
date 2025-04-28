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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Before;
import org.junit.Test;

public class TestBatchSubmit {

  @Before
  public void setup() {
    // set logger configuration

    // see https://www.baeldung.com/log4j2-programmatic-config and
    // https://logging.apache.org/log4j/2.x/manual/customconfig.html
    ConfigurationBuilder<BuiltConfiguration> builder =
        ConfigurationBuilderFactory.newConfigurationBuilder();

    AppenderComponentBuilder console = builder.newAppender("Stdout", "CONSOLE");

    LayoutComponentBuilder layout = builder.newLayout("PatternLayout");
    layout.addAttribute("pattern", "%d %-5level [%C{1} %M:%L] %msg%n%throwable");
    console.add(layout);

    builder.add(console);

    RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
    rootLogger.add(builder.newAppenderRef("Stdout"));
    builder.add(rootLogger);

    Configurator.initialize(builder.build());
  }

  @Test
  public void testLocalSequential() {
    BatchType batchType = BatchType.LOCAL_SEQUENTIAL;
    GridType gridType = GridType.LOCAL;

    List<String> commandList = new ArrayList<>();

    commandList.add("uname -a");

    BatchSubmitI batchSubmit = BatchSubmitFactory.getBatchSubmit(commandList, batchType, gridType);

    try {
      batchSubmit.runBatchSubmitinDir(".");
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }



}
