package terrasaur.config;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Include;
import jackfruit.annotations.Jackfruit;

@Jackfruit
public interface ConfigBlock {

  String introLines =
      """
    ###############################################################################
    # GENERAL PARAMETERS
    ###############################################################################
    """;

  @Comment(
      introLines
          + """
      Set the logging level.  Valid values in order of increasing detail:
            OFF
            FATAL
            ERROR
            WARN
            INFO
            DEBUG
            TRACE
            ALL
       See org.apache.logging.log4j.Level.""")
  @DefaultValue("INFO")
  String logLevel();

  @Comment(
      "Format for log messages.  See https://logging.apache.org/log4j/2.x/manual/layouts.html#PatternLayout for more details.")
  @DefaultValue("%highlight{%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%c{1}:%L] %msg%n%throwable}")
  String logFormat();

  @Comment(
      """
      Format for time strings.  Allowed values are:
      C     (e.g. 1986 APR 12 16:31:09.814)
      D     (e.g. 1986-102 // 16:31:12.814)
      J     (e.g. 2446533.18834276)
      ISOC  (e.g. 1986-04-12T16:31:12.814)
      ISOD  (e.g. 1986-102T16:31:12.814)""")
  @DefaultValue("ISOC")
  String timeFormat();

  @Include
  MissionBlock missionBlock();

  @Include
  SPCBlock spcBlock();
}
