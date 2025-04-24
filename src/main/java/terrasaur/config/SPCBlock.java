package terrasaur.config;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;

@Jackfruit(prefix = "spc")
public interface SPCBlock {

      String introLines =
            """
      ###############################################################################
      # SPC PARAMETERS
      ###############################################################################
      """;

    @Comment(introLines + """
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

}
