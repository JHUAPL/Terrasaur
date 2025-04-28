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
import jackfruit.annotations.Jackfruit;

import java.util.List;

@Jackfruit(prefix = "mission")
public interface MissionBlock {

  String introLines =
      """
      ###############################################################################
      # MISSION PARAMETERS
      ###############################################################################
      """;

  @Comment(introLines + "Mission name (e.g. DART)")
  @DefaultValue("mission")
  String missionName();

  @Comment(
      """
      SPICE metakernel to read.  This may be specified more than once
      for multiple metakernels (e.g. /project/dart/data/SPICE/flight/mk/current.tm)""")
  @DefaultValue("metakernel.tm")
  List<String> metakernel();

  @Comment("Name of spacecraft frame (e.g. DART_SPACECRAFT)")
  @DefaultValue("SPACECRAFT_FRAME")
  String spacecraftFrame();

  @Comment("Instrument frame name (e.g. DART_DRACO)")
  @DefaultValue("INSTRUMENT_FRAME")
  String instrumentFrameName();
}
