package terrasaur.fits;

/**
 * Container class for storing the value and comment associated with a given fits keyword
 * 
 * @author espirrc1
 *
 */
public class FitsValCom {
  private String value;
  private String comment;

  public FitsValCom(String value, String comment) {
    this.value = value;
    this.comment = comment;
  }

  public String getV() {
    return value;
  }

  public String getC() {
    return comment;
  }

  public void setV(String newVal) {
    this.value = newVal;
  }

  public void setC(String newComment) {
    this.comment = newComment;
  }

  public void setVC(String newVal, String newComment) {
    setV(newVal);
    setC(newComment);
  }

}
