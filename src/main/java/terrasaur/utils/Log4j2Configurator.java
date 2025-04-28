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
package terrasaur.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * A simple configuration class.
 * 
 * Default settings:
 * <ul>
 * <li>Pattern is "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%c{1}:%L] %msg%n%throwable"<br>
 * (e.g. 2021-11-09 19:32:37.119 INFO [LoggingTest:25] Level INFO)</li>
 * <li>Log level is {@link Level#INFO}</li>
 * </ul>
 * 
 * @author nairah1
 *
 */
public class Log4j2Configurator {

  private PatternLayout layout;
  private Map<String, FileAppender> fileAppenders;

  private static Log4j2Configurator instance = null;

  /**
   * Get an instance of this singleton class.
   * 
   * @return
   */
  synchronized public static Log4j2Configurator getInstance() {
    if (instance == null) {
      instance = new Log4j2Configurator();
    }
    return instance;
  }

  private Log4j2Configurator() {
    final LoggerContext loggerContext = LoggerContext.getContext(false);
    final Configuration config = loggerContext.getConfiguration();
    layout = PatternLayout.newBuilder().withPattern(DefaultConfiguration.DEFAULT_PATTERN)
        .withConfiguration(config).build();
    fileAppenders = new HashMap<>();
    setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%c{1}:%L] %msg%n%throwable");
    setLevel(Level.INFO);
  }

  /**
   * 
   * @return a map of logger names to {@link LoggerConfig}
   */
  private Map<String, LoggerConfig> getLoggerMap() {
    final LoggerContext loggerContext = LoggerContext.getContext(false);
    final Configuration config = loggerContext.getConfiguration();

    Map<String, LoggerConfig> loggerMap = new HashMap<>(config.getLoggers());
    loggerMap.put(LogManager.getRootLogger().getName(),
        config.getLoggerConfig(LogManager.getRootLogger().getName()));
    return Collections.unmodifiableMap(loggerMap);
  }

  /**
   * Append log to named file, or create it if it doesn't exist.
   * 
   * @param filename
   */
  public void addFile(String filename) {
    final LoggerContext loggerContext = LoggerContext.getContext(false);
    Map<String, LoggerConfig> loggerMap = getLoggerMap();

    FileAppender appender = FileAppender.newBuilder().setName(filename).withFileName(filename)
        .setLayout(layout).build();
    appender.start();

    for (String loggerName : loggerMap.keySet()) {
      LoggerConfig loggerConfig = loggerMap.get(loggerName);
      loggerConfig.addAppender(appender, null, null);
    }
    loggerContext.updateLoggers();
    fileAppenders.put(filename, appender);
  }

  /**
   * Stop logging to named file.
   * 
   * @param filename
   */
  public void removeFile(String filename) {
    if (fileAppenders.containsKey(filename)) {
      final LoggerContext loggerContext = LoggerContext.getContext(false);
      Map<String, LoggerConfig> loggerMap = getLoggerMap();

      FileAppender appender = fileAppenders.get(filename);
      for (String loggerName : loggerMap.keySet()) {
        LoggerConfig loggerConfig = loggerMap.get(loggerName);
        loggerConfig.removeAppender(appender.getName());
      }
      loggerContext.updateLoggers();
      fileAppenders.remove(filename);
    }
  }

  /**
   * Set the layout pattern for all {@link ConsoleAppender} and {@link FileAppender} objects.
   * 
   * @param pattern
   */
  public void setPattern(String pattern) {
    final LoggerContext loggerContext = LoggerContext.getContext(false);
    final Configuration config = loggerContext.getConfiguration();

    layout = PatternLayout.newBuilder().withConfiguration(config).withPattern(pattern).build();

    Map<String, LoggerConfig> loggerMap = getLoggerMap();
    for (String loggerName : loggerMap.keySet()) {
      LoggerConfig loggerConfig = loggerMap.get(loggerName);
      Map<String, Appender> appenderMap = loggerConfig.getAppenders();
      for (String appenderName : appenderMap.keySet()) {
        Appender newAppender = null;
        Appender oldAppender = appenderMap.get(appenderName);

        // there should be a better way to do this - a toBuilder() method on the appender would be
        // really useful
        if (oldAppender instanceof ConsoleAppender) {
          newAppender = ConsoleAppender.newBuilder().setName(appenderName).setConfiguration(config)
              .setLayout(layout).build();
        } else if (oldAppender instanceof FileAppender) {
          newAppender = FileAppender.newBuilder().setName(appenderName).setConfiguration(config)
              .withFileName(((FileAppender) oldAppender).getFileName()).setLayout(layout).build();
        }
        if (newAppender != null) {
          newAppender.start();
          loggerConfig.removeAppender(appenderName);
          loggerConfig.addAppender(newAppender, null, null);
        }
      }
    }
    loggerContext.updateLoggers();
  }

  /**
   * Sets the levels of <code>parentLogger</code> and all 'child' loggers to the given
   * <code>level</code>. This is simply a call to
   * 
   * <pre>
   * Configurator.setAllLevels(parentLogger, level)
   * </pre>
   * 
   * @param parentLogger
   * @param level
   */
  public void setLevel(String parentLogger, Level level) {
    Configurator.setAllLevels(parentLogger, level);
  }

  /**
   * Set all logger levels. This is simply a call to
   * 
   * <pre>
   * setLevel(LogManager.getRootLogger().getName(), level)
   * </pre>
   * 
   * @param level
   */
  public void setLevel(Level level) {
    setLevel(LogManager.getRootLogger().getName(), level);
  }

}
