package terrasaur.utils.spice;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import picante.designpatterns.Blueprint;
import picante.mechanics.EphemerisID;
import picante.spice.SpiceEnvironmentBuilder;

public class SpkCodeBinder implements Blueprint<SpiceEnvironmentBuilder> {

  private final int spkIdCode;
  private final String spkName;
  private final EphemerisID id;

  public SpkCodeBinder(int spkIdCode, String spkName, EphemerisID id) {
    super();
    this.spkIdCode = spkIdCode;
    this.spkName = spkName;
    this.id = id;
  }

  private static final ByteSource sourceString(String name, int code) {
    StringBuilder linesBuilder = new StringBuilder();

    linesBuilder.append("\\begindata \n NAIF_BODY_NAME += ( '");
    linesBuilder.append(name);
    linesBuilder.append("' ) \n NAIF_BODY_CODE += ( ");
    linesBuilder.append(code);
    linesBuilder.append(" ) \n");

    ByteSource result =
        ByteSource.wrap(linesBuilder.toString().getBytes(Charsets.ISO_8859_1));
    return result;
  }

  @Override
  public SpiceEnvironmentBuilder configure(SpiceEnvironmentBuilder builder) {

    ByteSource sourceStr = sourceString(spkName, spkIdCode);
    try {
      builder.load(Long.valueOf(System.nanoTime()).toString(), sourceStr);
    } catch (Exception e) {
      Throwables.propagate(e);
    }

    builder.bindEphemerisID(spkName, id);

    return builder;
  }

}
