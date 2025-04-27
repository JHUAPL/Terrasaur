package terrasaur.utils.saaPlotLib.data;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

public class HistogramDataSet extends DiscreteDataSet {

  private final List<Double> binBoundaryList;
  private final NavigableSet<Double> binBoundarySet;
  private List<Double> yValues;

  public int getTotal() {
    return yValues.size();
  }

  public List<Double> getYValues() {
    return new ArrayList<>(yValues);
  }

  public HistogramDataSet(String name, List<Double> binBoundaryList) {
    super(name);
    this.binBoundaryList = binBoundaryList;
    binBoundarySet = new TreeSet<>(binBoundaryList);

    // initialize all bins to 0
    for (int i = 1; i < binBoundaryList.size(); i++) {
      double binCenter = (binBoundaryList.get(i - 1) + binBoundaryList.get(i)) / 2;
      data.add(binCenter, 0.);
    }

    yValues = new ArrayList<>();
  }

  public void add(double y) {
    Double lowerBound = binBoundarySet.floor(y);
    Double upperBound = binBoundarySet.higher(y);
    if (lowerBound == null) lowerBound = binBoundarySet.first();
    if (upperBound == null) upperBound = binBoundarySet.last();

    double binCenter = (lowerBound + upperBound) / 2;

    int index = data.getX().indexOf(binCenter);
    if (index != -1) {
      // this value is contained within one of the bins
      Point4D p = data.get(index);
      data.set(p.getIndex(), p.getX(), p.getY() + 1);
    }

    yValues.add(y);
  }

  @Override
  public void add(double x, double y) {
    add(y);
  }

  /**
   * @param name name of new dataset
   * @return dataset where the values all sum to 1
   */
  public HistogramDataSet getNormalized(String name) {
    HistogramDataSet ds = new HistogramDataSet(name, binBoundaryList);
    PointList normalized = new PointList();
    for (Point4D p : data) {
      normalized.add(p.getX(), p.getY() / getTotal());
    }
    ds.data = normalized;
    ds.yValues = new ArrayList<>(yValues);

    return ds;
  }

  public List<Double> getBinBoundaryList() {
    return binBoundaryList;
  }

  public NavigableSet<Double> getBinBoundarySet() {
    return binBoundarySet;
  }
}
