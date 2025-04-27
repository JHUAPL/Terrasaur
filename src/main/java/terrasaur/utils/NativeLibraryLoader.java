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
