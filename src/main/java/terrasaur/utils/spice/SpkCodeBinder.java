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
