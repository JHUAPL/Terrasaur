package terrasaur.altwg.pipeline;

/**
 * Enum to store the different types of naming conventions.
 * 
 * @author espirrc1
 *
 */
public enum NameConvention {

  ALTPRODUCT, ALTNFTMLN, DARTPRODUCT, NOMATCH, NONEUSED;

  public static NameConvention parseNameConvention(String name) {
    for (NameConvention nameConvention : values()) {
      if (nameConvention.toString().toLowerCase().equals(name.toLowerCase())) {
        System.out.println("parsed naming convention:" + nameConvention.toString());
        return nameConvention;
      }
    }
    NameConvention nameConvention = NameConvention.NOMATCH;
    System.out
        .println("NameConvention.parseNameConvention()" + " could not parse naming convention:"
            + name + ". Returning:" + nameConvention.toString());
    return nameConvention;
  }
}
