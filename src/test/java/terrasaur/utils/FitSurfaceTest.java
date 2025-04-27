package terrasaur.utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import net.jafama.FastMath;
import org.junit.Ignore;
import org.junit.Test;
import terrasaur.utils.saaPlotLib.canvas.AreaPlot;
import terrasaur.utils.saaPlotLib.canvas.PlotCanvas;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp;
import terrasaur.utils.saaPlotLib.colorMaps.ColorRamp.TYPE;
import terrasaur.utils.saaPlotLib.colorMaps.ImmutableColorBar;
import terrasaur.utils.saaPlotLib.config.ImmutablePlotConfig;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.util.StringFunctions;

public class FitSurfaceTest {

  /**
   * plot a multivariate function compared with its fit
   */
  @Test
  @Ignore
  public void test01()   {
    int degree = 5; // degree 6 is a worse fit
    int numPts = (degree + 1) * (degree + 1);
    numPts = 61;
    double maxX = numPts / 10.;

    double[][] x = new double[numPts][numPts];
    double[][] y = new double[numPts][numPts];
    double[][] f = new double[numPts][numPts];

    for (int i = 0; i < numPts; i++) {
      for (int j = 0; j < numPts; j++) {
        x[i][j] = i * maxX / (numPts - 1.);
        y[j][i] = x[i][j];
      }
    }

    MultivariateFunction func = new MultivariateFunction() {
      @Override
      public double value(double[] point) {
        return -FastMath.sin(2 * point[0]) + FastMath.cos(point[1] / 2);
      }
    };

    List<Vector3D> points = new ArrayList<>();
    for (int i = 0; i < numPts; i++) {
      for (int j = 0; j < numPts; j++) {
        double[] point = {x[i][j], y[i][j]};
        f[i][j] = func.value(point);
        points.add(new Vector3D(x[i][j], y[i][j], f[i][j]));
      }
    }

    FitSurface app = new FitSurface(points, degree);

    MultivariateFunction fit = new MultivariateFunction() {
      @Override
      public double value(double[] point) {
        return app.value(point[0], point[1]);
      }
    };


    List<Vector3D> surfaceFit = new ArrayList<>();
    for (int i = 0; i < numPts; i++) {
      for (int j = 0; j < numPts; j++) {
        double[] point = {x[i][j], y[i][j]};
        f[i][j] = fit.value(point);
        surfaceFit.add(new Vector3D(x[i][j], y[i][j], f[i][j]));
      }
    }

    MultivariateFunction diff = new MultivariateFunction() {
      @Override
      public double value(double[] point) {
        return func.value(point) - fit.value(point);
      }
    };
    
    PlotConfig config = ImmutablePlotConfig.builder().width(800).height(600).title("Func").build();

    AxisX xLowerAxis = new AxisX(0, maxX, "X (degrees)", StringFunctions.toDegrees("%.0f"));
    AxisY yLeftAxis = new AxisY(0, maxX, "Y (degrees)", StringFunctions.toDegrees("%.0f"));

    ColorRamp ramp = ColorRamp.create(TYPE.PARULA, -1, 1);

    AreaPlot canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();
    canvas.plot(func, ramp, xLowerAxis, yLeftAxis);
    // Vertical, along right side
    canvas.drawColorBar(ImmutableColorBar.builder()
        .rect(new Rectangle(canvas.getPageWidth() - 60, config.topMargin(), 10, config.height()))
        .ramp(ramp).numTicks(9).tickFunction(StringFunctions.fixedFormat("%.2f")).build());

    PlotCanvas.showJFrame(canvas.getImage());

    config = ImmutablePlotConfig.copyOf(config).withTitle("Fit");
    canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();
    canvas.plot(fit, ramp, xLowerAxis, yLeftAxis);
    // Vertical, along right side
    canvas.drawColorBar(ImmutableColorBar.builder()
        .rect(new Rectangle(canvas.getPageWidth() - 60, config.topMargin(), 10, config.height()))
        .ramp(ramp).numTicks(9).tickFunction(StringFunctions.fixedFormat("%.2f")).build());

    PlotCanvas.showJFrame(canvas.getImage());

    config = ImmutablePlotConfig.copyOf(config).withTitle("Func-Fit");
    ramp = ColorRamp.create(TYPE.PARULA, -1, 1);
    ramp.addLimitColors();
    canvas = new AreaPlot(config);
    canvas.setAxes(xLowerAxis, yLeftAxis);
    canvas.drawAxes();
    canvas.plot(diff, ramp, xLowerAxis, yLeftAxis);
    // Vertical, along right side
    canvas.drawColorBar(ImmutableColorBar.builder()
        .rect(new Rectangle(canvas.getPageWidth() - 60, config.topMargin(), 10, config.height()))
        .ramp(ramp).numTicks(9).tickFunction(StringFunctions.fixedFormat("%.2f")).build());

    PlotCanvas.showJFrame(canvas.getImage());

    PolyDataUtil.writeVTKPoints("points.vtk", points);
    PolyDataUtil.writeVTKPoints("surface" + degree + ".vtk", surfaceFit);
  }

}
