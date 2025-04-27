package terrasaur.utils.gravity;

public class GravityResult implements Comparable<GravityResult> {

  int index;
  double[] xyz;
  double area;
  double potential;
  double[] acc;
  double elevation;

  public int getIndex() {
    return index;
  }

  public double[] getXYZ() {
    double[] tmp = new double[3];
    System.arraycopy(xyz, 0, tmp, 0, 3);
    return tmp;
  }

  public double getArea() {
    return area;
  }

  void setArea(double area) {
    this.area = area;
  }

  public double getPotential() {
    return potential;
  }

  public double[] getAcc() {
    double[] tmp = new double[3];
    System.arraycopy(acc, 0, tmp, 0, 3);
    return tmp;
  }

  public double getElevation() {
    return elevation;
  }

  void setElevation(double elevation) {
    this.elevation = elevation;
  }

  GravityResult(int index, double[] xyz, double potential, double[] acc) {
    this.index = index;
    this.xyz = new double[3];
    System.arraycopy(xyz, 0, this.xyz, 0, 3);
    this.potential = potential;
    this.acc = new double[3];
    System.arraycopy(acc, 0, this.acc, 0, 3);
    elevation = 0;
    area = 1;
  }

  @Override
  public int compareTo(GravityResult o) {
    return Integer.compare(index, o.index);
  }

  public String toCSV() {
    return String.format("%d, %14.8e, %14.8e, %14.8e, %14.8e, %14.8e, %14.8e, %14.8e, %14.8e",
        index, xyz[0], xyz[1], xyz[2], area, potential, acc[0], acc[1], acc[2]);
  }

  public static GravityResult fromCSV(String csv) {
    String[] parts = csv.split(",");
    int index = Integer.parseInt(parts[0]);
    double[] xyz = new double[3];
    for (int i = 0; i < 3; i++)
      xyz[i] = Double.parseDouble(parts[i + 1]);
    double area = Double.parseDouble(parts[4]);
    double potential = Double.parseDouble(parts[5]);
    double[] acc = new double[3];
    for (int i = 0; i < 3; i++)
      acc[i] = Double.parseDouble(parts[i + 6]);

    GravityResult gr = new GravityResult(index, xyz, potential, acc);
    gr.setArea(area);
    return gr;
  }

}
