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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ICQUtils {
    private static final Logger logger = LogManager.getLogger();

    public static double[] xyf2u(int q, double x, double y, int face, double[] ax) {
        double pi = Math.acos(-1.0);
        double[] dt = new double[3];
        double[] v = new double[3];
        int[][][] u = defu();

        dt[0] = Math.tan((2 * x / q - 1) * pi / 4);
        dt[1] = Math.tan((2 * y / q - 1) * pi / 4);
        dt[2] = 1 / Math.sqrt(1 + dt[0] * dt[0] + dt[1] * dt[1]);
        dt[0] *= dt[2];
        dt[1] *= dt[2];

        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 3; j++) {
                int sign = u[j][k][face];
                if (sign != 0) {
                    v[k] = dt[j] * sign * Math.sqrt(ax[k]);
                }
            }
        }

        return new Vector3D(v).normalize().toArray();
    }

    static int[][][] defu() {
        int[][][] u = new int[3][3][6];

        u[0][0][0] = 1;
        u[1][1][0] = -1;
        u[2][2][0] = 1;

        u[0][0][1] = 1;
        u[1][2][1] = -1;
        u[2][1][1] = -1;

        u[0][1][2] = -1;
        u[1][2][2] = -1;
        u[2][0][2] = -1;

        u[0][0][3] = -1;
        u[1][2][3] = -1;
        u[2][1][3] = 1;

        u[0][1][4] = 1;
        u[1][2][4] = -1;
        u[2][0][4] = 1;

        u[0][0][5] = 1;
        u[1][1][5] = 1;
        u[2][2][5] = -1;

        return u;
    }

    public static void writeICQ(int q, double[][][][] vec, String filename){

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println(q);
            for (int f = 0; f < 6; f++) {
                for (int j = 0; j <= q; j++) {
                    for (int i = 0; i <= q; i++) {
                        for (int k = 0; k < 3; k++) {
                            out.printf("%12.5f", vec[k][i][j][f]);
                        }
                        out.println();
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }

    }


}
