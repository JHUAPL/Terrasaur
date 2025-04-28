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

public class AppVersion {
    public final static String lastCommit = "25.04.27";
    // an M at the end of gitRevision means this was built from a "dirty" git repository
    public final static String gitRevision = "cb0f7f8";
    public final static String applicationName = "Terrasaur";
    public final static String dateString = "2025-Apr-28 15:06:13 UTC";

	private AppVersion() {}

    /**
     * Terrasaur version 25.04.27-cb0f7f8 built 2025-Apr-28 15:06:13 UTC
     */
    public static String getFullString() {
      return String.format("%s version %s-%s built %s", applicationName, lastCommit, gitRevision, dateString);
    }

    /**
     * Terrasaur version 25.04.27-cb0f7f8
     */
    public static String getVersionString() {
      return String.format("%s version %s-%s", applicationName, lastCommit, gitRevision);
    }
}

