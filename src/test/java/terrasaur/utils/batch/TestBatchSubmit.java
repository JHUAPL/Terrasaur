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
