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
package terrasaur.apps;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.text.WordUtils;
import terrasaur.config.ConfigBlock;
import terrasaur.config.ConfigBlockFactory;
import terrasaur.config.TerrasaurConfig;
import terrasaur.templates.TerrasaurTool;
import terrasaur.utils.AppVersion;

public class DumpConfig implements TerrasaurTool {

  @Override
  public String shortDescription() {
    return "Write a sample configuration file to use with Terrasaur, using defaults for DART.";
  }

  @Override
  public String fullDescription(Options options) {
    return WordUtils.wrap(
        "This program writes out sample configuration files to be used with Terrasaur.  "
            + "It takes a single argument, which is the name of the directory that will contain "
            + "the configuration files to be written.",
        80);
  }

  public static void main(String[] args) {
    // if no arguments, print the usage and exit
    if (args.length == 0) {
      System.out.println(new DumpConfig().fullDescription(null));
      System.exit(0);
    }

    // if -shortDescription is specified, print short description and exit.
    for (String arg : args) {
      if (arg.equals("-shortDescription")) {
        System.out.println(new DumpConfig().shortDescription());
        System.exit(0);
      }
    }

    File path = Paths.get(args[0]).toFile();

    ConfigBlock configBlock = TerrasaurConfig.getTemplate();
    try (PrintWriter pw = new PrintWriter(path)) {
      PropertiesConfiguration config = new ConfigBlockFactory().toConfig(configBlock);
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
      throw new RuntimeException(e);
    }

    System.out.println("Wrote config file to " + path.getAbsolutePath());
  }
}
