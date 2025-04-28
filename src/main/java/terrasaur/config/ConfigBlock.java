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
