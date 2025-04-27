package terrasaur.fits;

public enum UnitDir {

  UX {
    public int getAxis() {
      return 1;
    }
  },

  UY {

    public int getAxis() {
      return 2;
    }
  },

  UZ {

    public int getAxis() {
      return 3;
    }
  };

  public abstract int getAxis();
}
