package terrasaur.utils.saaPlotLib.canvas.axis;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import picante.time.JulianDate;
import picante.time.TimeAdapter;
import picante.time.TimeSystem;
import picante.time.UTCEpoch;

public class UTCAxisX extends AxisX {

  private enum TIMESCALES {
    YEAR(365.25 * 86400), DAY(86400.), HOUR(3600.), MINUTE(60.), SECOND(1.), MILLISECOND(0.001);

    public final double duration;

    TIMESCALES(double duration) {
      this.duration = duration;
    }

  }

  /**
   * Write out tick marks in the format YYYY-DDDTHH:MM:SS.SSS
   */
  public final static Function<Double, String> ISOD = t -> TimeAdapter.getInstance().convertToUTC(t).toString();

  /**
   * Write out tick marks in the format YYYY-MM-DDTHH:MM:SS.SSS
   */
  public final static Function<Double, String> ISOC = t -> {
    UTCEpoch utc = TimeAdapter.getInstance().convertToUTC(t).createValueRoundedToMillisecs();
    int sec = (int) utc.getSec();
    int millis = (int) (1000 * (utc.getSec() - sec) + 0.5);
    return String.format("%4d-%02d-%02dT%02d:%02d:%02d.%03d", utc.getYear(), utc.getMonth(),
        utc.getDom(), utc.getHour(), utc.getMin(), sec, millis);
  };

  /**
   * 
   * @param min seconds past J2000
   * @param max seconds past J2000
   * @param title axis title
   * @param nTicks just a hint, may not be the actual number of ticks in the end. Use
   *        {@link UTCAxisX#setTickLabels(java.util.NavigableMap)} to explicitly set the ticks. But
   *        if you're doing that, why are you using this class?
   */
  public UTCAxisX(double min, double max, String title, int nTicks) {
    super(min, max, title);

    TimeAdapter ta = TimeAdapter.getInstance();
    TimeSystem<UTCEpoch> utcSystem = ta.getUTCTimeSys();

    TIMESCALES timeScale = TIMESCALES.YEAR;
    for (TIMESCALES t : TIMESCALES.values()) {
      if ((max - min) / t.duration > nTicks) {
        timeScale = t;
        break;
      }
    }

    UTCEpoch beginUTC = TimeAdapter.getInstance().convertToUTC(min).createValueRoundedToMillisecs();
    UTCEpoch endUTC = TimeAdapter.getInstance().convertToUTC(max).createValueRoundedToMillisecs();
    switch (timeScale) {
      case YEAR:
        beginUTC = new UTCEpoch(beginUTC.getYear(), beginUTC.getDoy(), 0, 0, 0);
        endUTC = utcSystem.add(new UTCEpoch(endUTC.getYear(), 0, 0, 0, 0), timeScale.duration);
        endUTC = new UTCEpoch(endUTC.getYear(), 0, 0, 0, 0);
        break;
      case DAY:
        beginUTC = new UTCEpoch(beginUTC.getYear(), beginUTC.getDoy(), 0, 0, 0);
        endUTC = utcSystem.add(new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), 0, 0, 0),
            timeScale.duration);
        endUTC = new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), 0, 0, 0);
        break;
      case HOUR:
        beginUTC = new UTCEpoch(beginUTC.getYear(), beginUTC.getDoy(), beginUTC.getHour(), 0, 0);
        endUTC =
            utcSystem.add(new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(), 0, 0),
                timeScale.duration);
        endUTC = new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(), 0, 0);
        break;
      case MINUTE:
        beginUTC = new UTCEpoch(beginUTC.getYear(), beginUTC.getDoy(), beginUTC.getHour(),
            beginUTC.getMin(), 0);
        endUTC = utcSystem.add(
            new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(), endUTC.getMin(), 0),
            timeScale.duration);
        endUTC =
            new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(), endUTC.getMin(), 0);
        break;
      case SECOND:
        beginUTC = new UTCEpoch(beginUTC.getYear(), beginUTC.getDoy(), beginUTC.getHour(),
            beginUTC.getMin(), Math.floor(beginUTC.getSec()));
        endUTC = utcSystem.add(new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(),
            endUTC.getMin(), Math.floor(endUTC.getSec())), timeScale.duration);
        endUTC = new UTCEpoch(endUTC.getYear(), endUTC.getDoy(), endUTC.getHour(), endUTC.getMin(),
            Math.floor(endUTC.getSec()));
        break;
      case MILLISECOND:
        beginUTC = beginUTC.createValueRoundedToMillisecs();
        endUTC = endUTC.createValueRoundedToMillisecs();
      default:
        break;
    }

    // put axis in units of the appropriate timescale
    JulianDate roundedBegin = JulianDate.fromUTCEpoch(beginUTC);
    JulianDate roundedEnd = JulianDate.fromUTCEpoch(endUTC);
    min = 0;
    max = 86400 * (roundedEnd.getDate() - roundedBegin.getDate()) / timeScale.duration;

    AxisRange range = new AxisRange(min, max);
    TickMarks tickMarks = new TickMarks(range, false);

    NavigableSet<Double> majorTicks = new TreeSet<>();
    for (Double tick : tickMarks.getMajorTicks()) {
      JulianDate jd = new JulianDate(roundedBegin.getDate() + tick * timeScale.duration / 86400);
      majorTicks.add(ta.convertToET(jd.toUTCEpoch()));
    }

    NavigableSet<Double> minorTicks = new TreeSet<>();
    for (Double tick : tickMarks.getMinorTicks()) {
      JulianDate jd = new JulianDate(roundedBegin.getDate() + tick * timeScale.duration / 86400);
      minorTicks.add(ta.convertToET(jd.toUTCEpoch()));
    }

    this.tickMarks = new TickMarks(new AxisRange(ta.convertToET(roundedBegin.toUTCEpoch()),
        ta.convertToET(roundedEnd.toUTCEpoch())), false);
    tickMarks.setMajorTicks(majorTicks);
    tickMarks.setMinorTicks(minorTicks);
    this.tickMarks = tickMarks;

    setTickLabelFunction(ISOC);

    /*-
    // put axis in units of the appropriate timescale
    double roundedBegin = ta.convertToET(beginUTC);
    double roundedEnd = ta.convertToET(endUTC);
    min = 0;
    max = (roundedEnd - roundedBegin) / timeScale.duration;
    
    AxisRange range = new AxisRange(min, max);
    TickMarks tickMarks = new TickMarks(range, false);
    
    NavigableSet<Double> majorTicks = new TreeSet<>();
    for (Double tick : tickMarks.getMajorTicks()) {
      majorTicks.add(roundedBegin + tick * timeScale.duration);
    }
    
    NavigableSet<Double> minorTicks = new TreeSet<>();
    for (Double tick : tickMarks.getMinorTicks()) {
      minorTicks.add(roundedBegin + tick * timeScale.duration);
    }
    
    this.tickMarks = new TickMarks(new AxisRange(roundedBegin, roundedEnd), false);
    tickMarks.setMajorTicks(majorTicks);
    tickMarks.setMinorTicks(minorTicks);
    
    this.tickMarks = tickMarks;
    */

  }

}
