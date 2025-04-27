package terrasaur.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrasaur.utils.AppVersion;

public class TerrasaurConfig {

  private static final Logger logger = LogManager.getLogger();

  private static TerrasaurConfig instance = null;

  private TerrasaurConfig() {}

  private ConfigBlock configBlock;

  public static ConfigBlock getConfig() {
    if (instance == null) {
      logger.error("Configuration has not been loaded!  Returning null.");
      return null;
    }
    return instance.configBlock;
  }

  public static ConfigBlock getTemplate() {
    if (instance == null) {
      instance = new TerrasaurConfig();
      ConfigBlockFactory factory = new ConfigBlockFactory();
      instance.configBlock = factory.getTemplate();
    }
    return instance.configBlock;
  }

  public static ConfigBlock load(Path filename) {
    if (!Files.exists(filename)) {
      System.err.println("Cannot load configuration file " + filename);
      Thread.dumpStack();
      System.exit(1);
    }
    if (instance == null) {
      instance = new TerrasaurConfig();

      try {
        PropertiesConfiguration config = new Configurations().properties(filename.toFile());
        instance.configBlock = new ConfigBlockFactory().fromConfig(config);
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
    }
    return instance.configBlock;
  }

  @Override
  public String toString() {
    StringWriter string = new StringWriter();
    try (PrintWriter pw = new PrintWriter(string)) {
      PropertiesConfiguration config = new ConfigBlockFactory().toConfig(instance.configBlock);
      PropertiesConfigurationLayout layout = config.getLayout();

      String now =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
              .withLocale(Locale.getDefault())
              .withZone(ZoneOffset.UTC)
              .format(Instant.now());
      layout.setHeaderComment(
          String.format(
              "Configuration file for %s\nCreated %s UTC", AppVersion.getVersionString(), now));

      config.write(pw);
    } catch (ConfigurationException | IOException e) {
      e.printStackTrace();
    }
    return string.toString();
  }

}
