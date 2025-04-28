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
package terrasaur.utils.saaPlotLib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlotUtilsTest {

    @Test
    public void testRoundFloor() {
        double number = 8803.364;
        assertEquals(10000, PlotUtils.getRoundCeiling(number, 0), 0.0);
        assertEquals(9000, PlotUtils.getRoundCeiling(number, 1), 0.0);
        assertEquals(8900, PlotUtils.getRoundCeiling(number, 2), 0.0);
        assertEquals(8810, PlotUtils.getRoundCeiling(number, 3), 0.0);
        assertEquals(8804, PlotUtils.getRoundCeiling(number, 4), 0.0);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 5) - 8803.4) < 0.05);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 6) - 8803.37) < 0.005);

        number = 0;
        assertEquals(0, PlotUtils.getRoundCeiling(number, 0), 0.0);

        number = -434.788113;
        assertEquals(0, PlotUtils.getRoundCeiling(number, 0), 0.0);
        assertEquals(-400, PlotUtils.getRoundCeiling(number, 1), 0.0);
        assertEquals(-430, PlotUtils.getRoundCeiling(number, 2), 0.0);
        assertEquals(-434, PlotUtils.getRoundCeiling(number, 3), 0.0);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 4) + 434.7) < 0.05);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 5) + 434.78) < 0.005);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 6) + 434.788) < 0.0005);
        assertTrue(Math.abs(PlotUtils.getRoundCeiling(number, 7) + 434.7881) < 0.00005);
    }

    @Test
    public void testRoundCeiling() {
        double number = 8803.364;
        assertEquals(0, PlotUtils.getRoundFloor(number, 0), 0.0);
        assertEquals(8000, PlotUtils.getRoundFloor(number, 1), 0.0);
        assertEquals(8800, PlotUtils.getRoundFloor(number, 2), 0.0);
        assertEquals(8800, PlotUtils.getRoundFloor(number, 3), 0.0);
        assertEquals(8803, PlotUtils.getRoundFloor(number, 4), 0.0);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 5) - 8803.3) < 0.05);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 6) - 8803.36) < 0.005);

        number = 0;
        assertEquals(0, PlotUtils.getRoundFloor(number, 0), 0.0);

        number = -434.788113;
        assertEquals(-1000, PlotUtils.getRoundFloor(number, 0), 0.0);
        assertEquals(-500, PlotUtils.getRoundFloor(number, 1), 0.0);
        assertEquals(-440, PlotUtils.getRoundFloor(number, 2), 0.0);
        assertEquals(-435, PlotUtils.getRoundFloor(number, 3), 0.0);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 4) + 434.8) < 0.05);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 5) + 434.79) < 0.005);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 6) + 434.789) < 0.0005);
        assertTrue(Math.abs(PlotUtils.getRoundFloor(number, 7) + 434.7882) < 0.00005);
    }

}
