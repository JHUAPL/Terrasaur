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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vtk.vtkNativeLibrary;

/**
 * Contains a method for loading the VTK native libraries. Any program which makes of VTK classes
 * must call loadVtkLibraries() beforehand.
 * 
 * @author kahneg1
 * @version 1.0
 *
 */
public class NativeLibraryLoader {

  private final static Logger logger = LogManager.getLogger(NativeLibraryLoader.class);

  /** load the non-GUI related VTK libraries */
  static public void loadVtkLibraries() {

    List<String> skipLibraries = new ArrayList<>();
    skipLibraries.add("vtkTestingRenderingJava");
    skipLibraries.add("vtkRendering");
    skipLibraries.add("vtkViews");
    skipLibraries.add("vtkInteraction");
    skipLibraries.add("vtkCharts");
    skipLibraries.add("vtkDomainsChemistry");
    skipLibraries.add("vtkIOParallel");
    skipLibraries.add("vtkIOExport");
    skipLibraries.add("vtkIOImport");
    skipLibraries.add("vtkIOMINC");
    skipLibraries.add("vtkFiltersHybrid");
    skipLibraries.add("vtkFiltersParallel");
    skipLibraries.add("vtkGeovis");

    for (vtkNativeLibrary lib : vtkNativeLibrary.values()) {
      try {
        boolean loadThis = true;
        for (String skipLibrary : skipLibraries) {
          if (lib.GetLibraryName().startsWith(skipLibrary)) {
            loadThis = false;
            break;
          }
        }

        if (loadThis)
          lib.LoadLibrary();

      } catch (UnsatisfiedLinkError e) {
        logger.warn(e.getLocalizedMessage());
      }
    }
  }

  /** load the JNISpice library */
  static public void loadSpiceLibraries() {
    System.loadLibrary("JNISpice");
  }
}
