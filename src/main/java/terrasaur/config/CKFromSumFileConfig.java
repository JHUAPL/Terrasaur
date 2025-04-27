package terrasaur.config;

import java.util.List;
import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;

@Jackfruit
public interface CKFromSumFileConfig {

  @Comment("""
      Body fixed frame for the target body.  If blank, use SPICE-defined
      body fixed frame.  This will be the reference frame unless the J2000
      parameter is set to true.""")
  @DefaultValue("IAU_DIMORPHOS")
  String bodyFrame();

  @Comment("Target body name.")
  @DefaultValue("DIMORPHOS")
  String bodyName();

  @Comment("""
      Extend CK past the last sumFile by this number of seconds.  Default
      is zero.  Attitude is assumed to be fixed to the value given by the
      last sumfile.""")
  @DefaultValue("0")
  double extend();

  @Comment("""
      SPC defines the camera X axis to be increasing to the right, Y to
      be increasing down, and Z to point into the page:

                      Z
                    /
                   /
                  o---> X
                  |
                Y |
                  v

      SPICE may use a different convention.  Use the flip parameters to
      map the camera axes to SPICE axes.  flipI = J sets camera axis I
      to SPICE axis J, where I is an axis name (X, Y, or Z), and J is a
      signed integer number for an axis. The values of J can be (-1, 1,
      -2, 2, -3, or 3), indicating the (-X, X, -Y, Y, -Z, Z) axis,
      respectively.  The user must take care to correctly enter the
      flipI values so that the resulting flipped axes form a
      well-defined, right-handed coordinate system.

      Examples:
         (flipX, flipY, flipZ) = ( 1, 2, 3) SPICE and camera frames coincide.
         (flipX, flipY, flipZ) = ( 2,-1, 3) SPICE frame is camera frame rotated 90 degrees about Z.
         (flipX, flipY, flipZ) = (-2, 1, 3) SPICE frame is camera frame rotated -90 degrees about Z.
         (flipX, flipY, flipZ) = ( 1,-2,-3) rotates the image 180 degrees about X.""")
  @DefaultValue("-1")
  int flipX();

  @Comment("Map the camera Y axis to a SPICE axis.  See flipX for details.")
  @DefaultValue("2")
  int flipY();

  @Comment("Map the camera Z axis to a SPICE axis.  See flipX for details.")
  @DefaultValue("-3")
  int flipZ();

  @Comment("""
      Supply this frame kernel to MSOPCK.  Only needed if the reference frame
      (set by bodyFrame or J2000) is not built into SPICE""")
  @DefaultValue("/project/dart/data/SPICE/flight/fk/didymos_system_001.tf")
  String fk();

  @Comment("Instrument frame name")
  @DefaultValue("DART_DRACO")
  String instrumentFrameName();

  @Comment("If set to true, use J2000 as the reference frame")
  @DefaultValue("true")
  boolean J2000();

  @Comment("Path to leapseconds kernel.")
  @DefaultValue("/project/dart/data/SPICE/flight/lsk/naif0012.tls")
  String lsk();

  @Comment("Path to spacecraft SCLK file.")
  @DefaultValue("/project/dart/data/SPICE/flight/sclk/dart_sclk_0204.tsc")
  String sclk();

  @Comment("Name of spacecraft frame.")
  @DefaultValue("DART_SPACECRAFT")
  String spacecraftFrame();

  @Comment("""
      SPICE metakernel to read.  This may be specified more than once
      for multiple metakernels.""")
  @DefaultValue("/project/dart/data/SPICE/flight/mk/current.tm")
  List<String> metakernel();


}
