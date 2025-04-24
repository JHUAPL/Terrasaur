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
