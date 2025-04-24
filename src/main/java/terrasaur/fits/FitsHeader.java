package terrasaur.fits;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import terrasaur.utils.StringUtil;

/**
 * @deprecated this class has been replaced by the FitsHdr class and the FitsHeaderFactory
 *             implementation of the FitsHdr class. USE THIS CLASS AT YOUR PERIL. THE KEYWORDS HAVE
 *             NOT BEEN UPDATED SINCE THIS CLASS WAS DEPRECATED.
 * @author espirrc1
 *
 */
@Deprecated
public class FitsHeader {

  public final FitsValCom bitPix;
  public final FitsValCom nAxis1;
  public final FitsValCom nAxis2;
  public final FitsValCom nAxis3;

  // fits keywords common to all ALTWG products
  public final FitsValCom hdrVers;
  public final FitsValCom mission;
  public final FitsValCom hostName;
  public final FitsValCom target;
  public final FitsValCom origin;
  public final FitsValCom spocid;
  public final FitsValCom sdparea;
  public final FitsValCom sdpdesc;
  public final FitsValCom missionPhase;
  public final FitsValCom dataSource;
  public final FitsValCom dataSourceV;
  public final FitsValCom dataSourceS;
  public final FitsValCom dataSourceFile;
  public final FitsValCom datasrcd;
  public final FitsValCom productName;
  public final FitsValCom dateprd;
  public final FitsValCom productType;
  public final FitsValCom productVer;
  public final FitsValCom author;

  // for ancillary fits products
  // map Name(description): tilt, gravity, slope, etc
  // map Type: global or local
  public final FitsValCom objFile;
  public final FitsValCom mapName;
  public final FitsValCom mapType;

  // center lon, lat. common to both local and global anci fits
  public final FitsValCom clon;
  public final FitsValCom clat;

  // global anci fits geometry
  public final FitsValCom minlon;
  public final FitsValCom maxlon;
  public final FitsValCom minlat;
  public final FitsValCom maxlat;

  // local anci fits geometry
  public final FitsValCom ulcLon;
  public final FitsValCom ulcLat;
  public final FitsValCom llcLon;
  public final FitsValCom llcLat;
  public final FitsValCom lrcLon;
  public final FitsValCom lrcLat;
  public final FitsValCom urcLon;
  public final FitsValCom urcLat;
  public final FitsValCom cntr_v_x;
  public final FitsValCom cntr_v_y;
  public final FitsValCom cntr_v_z;
  public final FitsValCom ux_x;
  public final FitsValCom ux_y;
  public final FitsValCom ux_z;
  public final FitsValCom uy_x;
  public final FitsValCom uy_y;
  public final FitsValCom uy_z;
  public final FitsValCom uz_x;
  public final FitsValCom uz_y;
  public final FitsValCom uz_z;
  public final FitsValCom gsd;
  public final FitsValCom gsdi;

  // common to both local and global anci fits
  public final FitsValCom sigma;
  public final FitsValCom sigDef;
  public final FitsValCom dqual1;
  public final FitsValCom dqual2;
  public final FitsValCom dsigDef;
  public final FitsValCom density;
  public final FitsValCom rotRate;
  public final FitsValCom refPot;
  public final FitsValCom tiltRad;
  public final FitsValCom tiltMaj;
  public final FitsValCom tiltMin;
  public final FitsValCom tiltPa;
  public final FitsValCom mapVer;

  public final EnumMap<HeaderTag, FitsValCom> tag2valcom;

  private FitsHeader(FitsHeaderBuilder b) {
    this.bitPix = b.bitPix;
    this.nAxis1 = b.nAxis1;
    this.nAxis2 = b.nAxis2;
    this.nAxis3 = b.nAxis3;
    this.hdrVers = b.hdrVers;
    this.mission = b.mission;
    this.hostName = b.hostName;
    this.target = b.target;
    this.origin = b.origin;
    this.spocid = b.spocid;
    this.sdparea = b.sdparea;
    this.sdpdesc = b.sdpdesc;
    this.missionPhase = b.missionPhase;
    this.dataSource = b.dataSource;
    this.dataSourceFile = b.dataSourceFile;
    this.dataSourceV = b.dataSourceV;
    this.datasrcd = b.datasrcd;
    this.dataSourceS = b.dataSourceS;
    this.productName = b.productName;
    this.dateprd = b.dateprd;
    this.productType = b.productType;
    this.productVer = b.productVer;
    this.objFile = b.objFile;
    this.author = b.author;
    this.mapName = b.mapName;
    this.mapType = b.mapType;
    this.clon = b.clon;
    this.clat = b.clat;
    this.minlon = b.minlon;
    this.maxlon = b.maxlon;
    this.minlat = b.minlat;
    this.maxlat = b.maxlat;
    this.ulcLon = b.ulcLon;
    this.ulcLat = b.ulcLat;
    this.llcLon = b.llcLon;
    this.llcLat = b.llcLat;
    this.lrcLon = b.lrcLon;
    this.lrcLat = b.lrcLat;
    this.urcLon = b.urcLon;
    this.urcLat = b.urcLat;
    this.cntr_v_x = b.cntr_v_x;
    this.cntr_v_y = b.cntr_v_y;
    this.cntr_v_z = b.cntr_v_z;
    this.ux_x = b.ux_x;
    this.ux_y = b.ux_y;
    this.ux_z = b.ux_z;
    this.uy_x = b.uy_x;
    this.uy_y = b.uy_y;
    this.uy_z = b.uy_z;
    this.uz_x = b.uz_x;
    this.uz_y = b.uz_y;
    this.uz_z = b.uz_z;
    this.gsd = b.gsd;
    this.gsdi = b.gsdi;
    this.sigma = b.sigma;
    this.sigDef = b.sigDef;
    this.dqual1 = b.dqual1;
    this.dqual2 = b.dqual2;
    this.dsigDef = b.dsigDef;
    this.mapVer = b.mapVer;
    this.density = b.density;
    this.rotRate = b.rotRate;
    this.refPot = b.refPot;
    this.tiltRad = b.tiltRad;

    // hardcode semi-major and semi-minor axis = radius
    // since we only deal with circles for now
    this.tiltMaj = b.tiltRad;
    this.tiltMin = b.tiltRad;
    this.tiltPa = b.tiltPa;
    this.tag2valcom = b.tag2valcom;
  }

  public static class FitsHeaderBuilder {

    // initialize the FITS keywords. Some of them may not change during the mission, but
    // the option to change them is given via the public methods.
    private FitsValCom bitPix = new FitsValCom("32", null);
    private FitsValCom nAxis1 = new FitsValCom("1024", null);
    private FitsValCom nAxis2 = new FitsValCom("1024", null);
    private FitsValCom nAxis3 = new FitsValCom("numberPlanesNotSet", null);

    private FitsValCom hdrVers =
        new FitsValCom(HeaderTag.HDRVERS.value(), HeaderTag.HDRVERS.comment());
    private FitsValCom mission =
        new FitsValCom(HeaderTag.MISSION.value(), HeaderTag.MISSION.comment());
    private FitsValCom hostName = new FitsValCom(HeaderTag.HOSTNAME.value(), null);
    private FitsValCom target = new FitsValCom(HeaderTag.TARGET.value(), null);
    private FitsValCom origin =
        new FitsValCom(HeaderTag.ORIGIN.value(), HeaderTag.ORIGIN.comment());
    private FitsValCom spocid =
        new FitsValCom(HeaderTag.SPOC_ID.value(), HeaderTag.SPOC_ID.comment());
    private FitsValCom sdparea =
        new FitsValCom(HeaderTag.SDPAREA.value(), HeaderTag.SDPAREA.comment());
    private FitsValCom sdpdesc =
        new FitsValCom(HeaderTag.SDPDESC.value(), HeaderTag.SDPDESC.comment());
    private FitsValCom missionPhase =
        new FitsValCom(HeaderTag.MPHASE.value(), HeaderTag.MPHASE.comment());
    private FitsValCom dataSource =
        new FitsValCom(HeaderTag.DATASRC.value(), HeaderTag.DATASRC.comment());
    private FitsValCom dataSourceFile =
        new FitsValCom(HeaderTag.DATASRCF.value(), HeaderTag.DATASRCF.comment());
    private FitsValCom dataSourceS =
        new FitsValCom(HeaderTag.DATASRCS.value(), HeaderTag.DATASRCS.comment());
    private FitsValCom dataSourceV =
        new FitsValCom(HeaderTag.DATASRCV.value(), HeaderTag.DATASRCV.comment());
    private FitsValCom software =
        new FitsValCom(HeaderTag.SOFTWARE.value(), HeaderTag.SOFTWARE.comment());
    private FitsValCom softver =
        new FitsValCom(HeaderTag.SOFT_VER.value(), HeaderTag.SOFT_VER.comment());
    private FitsValCom datasrcd =
        new FitsValCom(HeaderTag.DATASRCD.value(), HeaderTag.DATASRCD.comment());
    private FitsValCom productName =
        new FitsValCom(HeaderTag.PRODNAME.value(), HeaderTag.PRODNAME.comment());
    private FitsValCom dateprd =
        new FitsValCom(HeaderTag.DATEPRD.value(), HeaderTag.DATEPRD.comment());
    private FitsValCom productType = new FitsValCom("productTypeNotSet", null);
    private FitsValCom productVer =
        new FitsValCom(HeaderTag.PRODVERS.value(), HeaderTag.PRODVERS.comment());
    private FitsValCom mapVer =
        new FitsValCom(HeaderTag.MAP_VER.value(), HeaderTag.MAP_VER.comment());

    private FitsValCom objFile =
        new FitsValCom(HeaderTag.OBJ_FILE.value(), HeaderTag.OBJ_FILE.comment());
    private FitsValCom author =
        new FitsValCom(HeaderTag.CREATOR.value(), HeaderTag.CREATOR.comment());

    private FitsValCom mapName =
        new FitsValCom(HeaderTag.MAP_NAME.value(), HeaderTag.MAP_NAME.comment());
    private FitsValCom mapType =
        new FitsValCom(HeaderTag.MAP_TYPE.value(), HeaderTag.MAP_TYPE.comment());
    private FitsValCom clon = new FitsValCom("-999", HeaderTag.CLON.comment());
    private FitsValCom clat = new FitsValCom("-999", HeaderTag.CLAT.comment());
    private FitsValCom minlon = new FitsValCom("-999", HeaderTag.MINLON.comment());
    private FitsValCom maxlon = new FitsValCom("-999", HeaderTag.MAXLON.comment());
    private FitsValCom minlat = new FitsValCom("-999", HeaderTag.MINLAT.comment());
    private FitsValCom maxlat = new FitsValCom("-999", HeaderTag.MAXLAT.comment());
    private FitsValCom pxperdeg = new FitsValCom("-999", HeaderTag.PXPERDEG.comment());
    private FitsValCom ulcLon = new FitsValCom("-999", HeaderTag.ULCLNG.comment());
    private FitsValCom ulcLat = new FitsValCom("-999", HeaderTag.ULCLAT.comment());
    private FitsValCom llcLon = new FitsValCom("-999", HeaderTag.LLCLNG.comment());
    private FitsValCom llcLat = new FitsValCom("-999", HeaderTag.LLCLAT.comment());
    private FitsValCom lrcLon = new FitsValCom("-999", HeaderTag.LRCLNG.comment());
    private FitsValCom lrcLat = new FitsValCom("-999", HeaderTag.LRCLAT.comment());
    private FitsValCom urcLon = new FitsValCom("-999", HeaderTag.URCLNG.comment());
    private FitsValCom urcLat = new FitsValCom("-999", HeaderTag.URCLAT.comment());
    private FitsValCom cntr_v_x = new FitsValCom("-999", HeaderTag.CNTR_V_X.comment());
    private FitsValCom cntr_v_y = new FitsValCom("-999", HeaderTag.CNTR_V_Y.comment());
    private FitsValCom cntr_v_z = new FitsValCom("-999", HeaderTag.CNTR_V_Z.comment());
    private FitsValCom ux_x = new FitsValCom("-999", HeaderTag.UX_X.comment());
    private FitsValCom ux_y = new FitsValCom("-999", HeaderTag.UX_Y.comment());
    private FitsValCom ux_z = new FitsValCom("-999", HeaderTag.UX_Z.comment());
    private FitsValCom uy_x = new FitsValCom("-999", HeaderTag.UY_X.comment());
    private FitsValCom uy_y = new FitsValCom("-999", HeaderTag.UY_Y.comment());
    private FitsValCom uy_z = new FitsValCom("-999", HeaderTag.UY_Z.comment());
    private FitsValCom uz_x = new FitsValCom("-999", HeaderTag.UZ_X.comment());
    private FitsValCom uz_y = new FitsValCom("-999", HeaderTag.UZ_Y.comment());
    private FitsValCom uz_z = new FitsValCom("-999", HeaderTag.UZ_Z.comment());
    private FitsValCom gsd = new FitsValCom("-999", HeaderTag.GSD.comment());
    private FitsValCom gsdi = new FitsValCom("-999", HeaderTag.GSDI.comment());
    private FitsValCom sigma = new FitsValCom("-999", "N/A");
    private FitsValCom sigDef = new FitsValCom(HeaderTag.SIGMA.value(), HeaderTag.SIGMA.comment());
    private FitsValCom dqual1 = new FitsValCom("-999", HeaderTag.DQUAL_1.comment());
    private FitsValCom dqual2 = new FitsValCom("-999", HeaderTag.DQUAL_2.comment());
    private FitsValCom dsigDef =
        new FitsValCom(HeaderTag.DSIG_DEF.value(), HeaderTag.DSIG_DEF.comment());
    private FitsValCom density = new FitsValCom("-999", HeaderTag.DENSITY.comment());
    private FitsValCom rotRate = new FitsValCom("-999", HeaderTag.ROT_RATE.comment());
    private FitsValCom refPot = new FitsValCom("-999", HeaderTag.REF_POT.comment());
    private FitsValCom tiltRad = new FitsValCom("-999", HeaderTag.TILT_RAD.comment());
    private FitsValCom tiltMaj = new FitsValCom("-999", HeaderTag.TILT_MAJ.comment());
    private FitsValCom tiltMin = new FitsValCom("-999", HeaderTag.TILT_MIN.comment());
    private FitsValCom tiltPa = new FitsValCom("0", HeaderTag.TILT_PA.comment());

    private EnumMap<HeaderTag, FitsValCom> tag2valcom =
        new EnumMap<HeaderTag, FitsValCom>(HeaderTag.class);

    public FitsHeaderBuilder() {

      /*
       * initialize the map between header tags and the fits val com variables. This allows us to
       * use enumeration to select which of the fitsvalcom variables we want to update, eliminating
       * the need for specific 'set' statements for each variable.
       */
      tag2valcom.put(HeaderTag.HDRVERS, hdrVers);
      tag2valcom.put(HeaderTag.MISSION, mission);
      tag2valcom.put(HeaderTag.HOSTNAME, hostName);
      tag2valcom.put(HeaderTag.TARGET, target);
      tag2valcom.put(HeaderTag.ORIGIN, origin);
      tag2valcom.put(HeaderTag.SPOC_ID, spocid);
      tag2valcom.put(HeaderTag.SDPAREA, sdparea);
      tag2valcom.put(HeaderTag.SDPDESC, sdpdesc);
      tag2valcom.put(HeaderTag.MPHASE, missionPhase);
      tag2valcom.put(HeaderTag.DATASRC, dataSource);
      tag2valcom.put(HeaderTag.DATASRCV, dataSourceV);
      tag2valcom.put(HeaderTag.DATASRCF, dataSourceFile);
      tag2valcom.put(HeaderTag.DATASRCS, dataSourceS);
      // removed from ALTWG keywords per Map Format SIS draft v2
      tag2valcom.put(HeaderTag.DATASRCD, datasrcd);
      tag2valcom.put(HeaderTag.SOFTWARE, software);
      tag2valcom.put(HeaderTag.SOFT_VER, softver);
      tag2valcom.put(HeaderTag.PRODNAME, productName);
      tag2valcom.put(HeaderTag.DATEPRD, dateprd);
      tag2valcom.put(HeaderTag.PRODVERS, productVer);
      tag2valcom.put(HeaderTag.MAP_VER, mapVer);
      tag2valcom.put(HeaderTag.CREATOR, author);
      tag2valcom.put(HeaderTag.OBJ_FILE, objFile);
      tag2valcom.put(HeaderTag.CLON, clon);
      tag2valcom.put(HeaderTag.CLAT, clat);
      tag2valcom.put(HeaderTag.MINLON, minlon);
      tag2valcom.put(HeaderTag.MAXLON, maxlon);
      tag2valcom.put(HeaderTag.MINLAT, minlat);
      tag2valcom.put(HeaderTag.MAXLAT, maxlat);
      tag2valcom.put(HeaderTag.PXPERDEG, pxperdeg);
      tag2valcom.put(HeaderTag.LLCLNG, llcLon);
      tag2valcom.put(HeaderTag.LLCLAT, llcLat);
      tag2valcom.put(HeaderTag.LRCLNG, lrcLon);
      tag2valcom.put(HeaderTag.LRCLAT, lrcLat);
      tag2valcom.put(HeaderTag.URCLNG, urcLon);
      tag2valcom.put(HeaderTag.URCLAT, urcLat);
      tag2valcom.put(HeaderTag.ULCLNG, ulcLon);
      tag2valcom.put(HeaderTag.ULCLAT, ulcLat);
      tag2valcom.put(HeaderTag.CNTR_V_X, cntr_v_x);
      tag2valcom.put(HeaderTag.CNTR_V_Y, cntr_v_y);
      tag2valcom.put(HeaderTag.CNTR_V_Z, cntr_v_z);
      tag2valcom.put(HeaderTag.UX_X, ux_x);
      tag2valcom.put(HeaderTag.UX_Y, ux_y);
      tag2valcom.put(HeaderTag.UX_Z, ux_z);
      tag2valcom.put(HeaderTag.UY_X, uy_x);
      tag2valcom.put(HeaderTag.UY_Y, uy_y);
      tag2valcom.put(HeaderTag.UY_Z, uy_z);
      tag2valcom.put(HeaderTag.UZ_X, ux_x);
      tag2valcom.put(HeaderTag.UZ_Y, ux_y);
      tag2valcom.put(HeaderTag.UZ_Z, ux_z);
      tag2valcom.put(HeaderTag.GSD, gsd);
      tag2valcom.put(HeaderTag.GSDI, gsdi);
      tag2valcom.put(HeaderTag.SIGMA, sigma);
      tag2valcom.put(HeaderTag.SIG_DEF, sigDef);
      tag2valcom.put(HeaderTag.DQUAL_1, dqual1);
      tag2valcom.put(HeaderTag.DQUAL_2, dqual2);
      tag2valcom.put(HeaderTag.DSIG_DEF, dsigDef);
      tag2valcom.put(HeaderTag.DENSITY, density);
      tag2valcom.put(HeaderTag.ROT_RATE, rotRate);
      tag2valcom.put(HeaderTag.REF_POT, refPot);
      tag2valcom.put(HeaderTag.TILT_RAD, tiltRad);
      tag2valcom.put(HeaderTag.TILT_MAJ, tiltMaj);
      tag2valcom.put(HeaderTag.TILT_MIN, tiltMin);
      tag2valcom.put(HeaderTag.TILT_PA, tiltPa);
      tag2valcom.put(HeaderTag.MAP_NAME, mapName);
      tag2valcom.put(HeaderTag.MAP_TYPE, mapType);
      tag2valcom.put(HeaderTag.MAP_VER, mapVer);

    }

    public FitsHeaderBuilder setTarget(String val, String comment) {
      this.target.setV(val);
      this.target.setC(comment);
      return this;
    }

    public FitsHeaderBuilder setBitPix(String val, String comment) {
      this.bitPix.setV(val);
      this.bitPix.setC(comment);
      return this;
    }

    public FitsHeaderBuilder setNAx1(String val, String comment) {
      this.nAxis1.setV(val);
      this.nAxis1.setC(comment);
      return this;
    }

    public FitsHeaderBuilder setNAx2(String val, String comment) {
      this.nAxis2.setV(val);
      this.nAxis2.setC(comment);
      return this;
    }

    public FitsHeaderBuilder setNAx3(String val, String comment) {
      this.nAxis3.setV(val);
      this.nAxis3.setC(comment);
      return this;
    }

    public FitsHeaderBuilder setVCbyHeaderTag(HeaderTag hdrTag, String value, String comment) {

      if (tag2valcom.containsKey(hdrTag)) {
        tag2valcom.get(hdrTag).setVC(value, comment);
      }
      return this;
    }

    public FitsHeaderBuilder setVbyHeaderTag(HeaderTag hdrTag, String value) {
      if (tag2valcom.containsKey(hdrTag)) {
        tag2valcom.get(hdrTag).setV(value);
      }
      return this;
    }

    public FitsHeaderBuilder setCbyHeaderTag(HeaderTag hdrTag, String comment) {
      if (tag2valcom.containsKey(hdrTag)) {
        tag2valcom.get(hdrTag).setC(comment);
      }
      return this;
    }

    /**
     * Set values in the ancillary header builder class by parsing the headerCard. Parse appropriate
     * values as determined by the value of the headercard key.
     * 
     * @param headerCard
     * @return
     */
    public FitsHeaderBuilder setbyHeaderCard(HeaderCard headerCard) {
      HeaderTag hdrTag = HeaderTag.END;
      try {
        hdrTag = HeaderTag.valueOf(headerCard.getKey());
        setVCbyHeaderTag(hdrTag, headerCard.getValue(), headerCard.getComment());
      } catch (IllegalArgumentException e) {
        if ((headerCard.getKey().contains("COMMENT")) || (headerCard.getKey().contains("PLANE"))) {
        } else {
          System.out.println(headerCard.getKey() + " not a HeaderTag");
        }
      } catch (NullPointerException ne) {
        System.out.println("null pointer exception for:" + headerCard.getKey());
      }

      return this;

    }

    public FitsHeader build() {

      return new FitsHeader(this);
    }

  }

  /**
   * Loops through map of fits header cards and tries to parse values relevant to the ALTWG Fits
   * file.
   * 
   * @param map
   */
  public static FitsHeaderBuilder copyFitsHeader(Map<String, HeaderCard> map) {

    FitsHeaderBuilder hdrBuilder = new FitsHeaderBuilder();

    // loop through each of the HeaderCards in the map and see if any will help build the altwg
    // header
    for (Map.Entry<String, HeaderCard> entry : map.entrySet()) {
      HeaderCard thisCard = entry.getValue();
      hdrBuilder.setbyHeaderCard(thisCard);
    }
    return hdrBuilder;
  }

  /**
   * Copy the fits header from fits file and use it to populate and return FitsHeaderBuilder.
   * 
   * @param fitsFile
   * @return
   */
  public static FitsHeaderBuilder copyFitsHeader(File fitsFile) {

    FitsHeaderBuilder hdrBuilder = new FitsHeaderBuilder();
    try {
      Map<String, HeaderCard> map = FitsUtil.getFitsHeaderAsMap(fitsFile.getCanonicalPath());
      hdrBuilder = copyFitsHeader(map);
      return hdrBuilder;
    } catch (FitsException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      String errmesg = "ERROR in FitsHeader.copyFitsHeader()! " + " Unable to parse fits file:"
          + fitsFile.toString() + " for fits header!";
      System.err.println(errmesg);
      System.exit(1);

    }
    return hdrBuilder;
  }

  /**
   * Parse a fits configuration file and update a FitsHeaderBuilder. The builder is used to generate
   * a fits header.
   * 
   * @param configFile
   * @param hdrBuilder - can either be an existing builder or null. If null then will create and
   *        return a new builder.
   * @return
   * @throws IOException
   */
  public static FitsHeaderBuilder configHdrBuilder(String configFile, FitsHeaderBuilder hdrBuilder)
      throws IOException {

    File checkF = new File(configFile);
    if (!checkF.exists()) {
      // System.out.println("ERROR:FITS header configuration file:" + configFile + " does not
      // exist!");
      String errMesg = "ERROR:FITS header configuration file:" + configFile + " does not exist!";
      throw new RuntimeException(errMesg);
    }

    if (hdrBuilder == null) {
      System.out.println("builder passed to FitsHeader.configHdrBuilder() is null. Generating"
          + " new FitsHeaderBuilder");
      hdrBuilder = new FitsHeaderBuilder();
    }
    List<String> content = FileUtils.readLines(new File(configFile), Charset.defaultCharset());
    boolean separatorFound = false;
    for (String line : content) {

      String[] keyval = line.split("#");
      if (keyval.length > 1) {

        separatorFound = true;
        // check if there is a match w/ HeaderTags
        HeaderTag thisTag = HeaderTag.tagFromString(keyval[0]);

        if (thisTag != HeaderTag.NOMATCH) {
          // pass to fits header builder and see if it matches on a fits keyword

          if (keyval.length == 2) {
            // assume user only wants to overwrite the value portion. Leave the comments alone.
            System.out.println("setting " + thisTag.toString() + " to " + keyval[1]);
            hdrBuilder.setVbyHeaderTag(thisTag, keyval[1]);
          } else if (keyval.length == 3) {
            if (keyval[2].contains("null")) {
              // user explicitly wants to override any comment in this header with null
              hdrBuilder.setVCbyHeaderTag(thisTag, keyval[1], null);
            } else {
              System.out.println("setting " + thisTag.toString() + " to " + keyval[1]
                  + ", comment to " + keyval[2]);
              hdrBuilder.setVCbyHeaderTag(thisTag, keyval[1], keyval[2]);
            }
          } else {
            System.out.println(
                "Warning: the following line in the config file line has more than 2 colons:");
            System.out.println(line);
            System.out.println("Cannot parse. skipping this line");
          }

        }
      }
    }
    if (!separatorFound) {
      System.out.println("WARNING! The fits config file:" + configFile
          + " does not appear to be a valid config file! There is no # separator!");
    }
    return hdrBuilder;
  }

  /**
   * Given a fits keyword return the object containing the keyword value and comment.
   * 
   * @param tag - fits keyword as enumeration.
   * @return HeaderCard
   * @throws HeaderCardException
   */
  public HeaderCard getHeaderCard(HeaderTag tag) throws HeaderCardException {

    // format for double values
    String fmtS = "%18.13f";
    if (tag2valcom.containsKey(tag)) {
      FitsValCom valcom = tag2valcom.get(tag);

      /*
       * FitsValCom stores the values as String because extracting the value from the source fits
       * file returns it as a string. Need to convert geometry values to double and store it in the
       * HeaderCard as a double. Then when written to the fits file it will not have quotes around
       * the value.
       */

      /*
       * type of value to store in headercard. =0 string (single quotes around value) =1 formatted
       * double =2 free-form double
       */
      int returnType = 0;

      switch (tag) {

        case PXPERDEG:
        case CLON:
        case CLAT:
        case MINLON:
        case MAXLON:
        case MINLAT:
        case MAXLAT:
        case URCLNG:
        case URCLAT:
        case LRCLNG:
        case LRCLAT:
        case LLCLNG:
        case LLCLAT:
        case ULCLNG:
        case ULCLAT:
        case GSDI:
          returnType = 1;
          break;

        case CNTR_V_X:
        case CNTR_V_Y:
        case CNTR_V_Z:
        case UX_X:
        case UX_Y:
        case UX_Z:
        case UY_X:
        case UY_Y:
        case UY_Z:
        case UZ_X:
        case UZ_Y:
        case UZ_Z:
        case DENSITY:
        case ROT_RATE:
        case REF_POT:
        case TILT_RAD:
        case TILT_MAJ:
        case TILT_MIN:
        case TILT_PA:
        case GSD:
        case SIGMA:
        case DQUAL_1:
        case DQUAL_2:
          returnType = 2;
          break;

        default:
          returnType = 0;
          break;
      }

      switch (returnType) {
        case 0:
          return new HeaderCard(tag.toString(), valcom.getV(), valcom.getC());

        case 1:
          return new HeaderCard(tag.toString(), StringUtil.str2fmtD(fmtS, valcom.getV()),
              valcom.getC());

        case 2:
          return new HeaderCard(tag.toString(), StringUtil.parseSafeD(valcom.getV()),
              valcom.getC());

      }

    }

    String errMesg = "ERROR!, cannot find fits keyword:" + tag.toString();
    throw new RuntimeException(errMesg);
  }

  /**
   * Initialize fits header builder for using keyword/values from a fits config file. Can be used to
   * initialize headerBuilder for any fits file.
   * 
   * @param configFile
   * @return
   */
  public static FitsHeaderBuilder initHdrBuilder(String configFile) {
    FitsHeaderBuilder hdrBuilder = new FitsHeaderBuilder();

    if (configFile != null) {
      // try to load config file if it exists and modify fits header builder with it.
      try {
        configHdrBuilder(configFile, hdrBuilder);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("Error trying to read config file:" + configFile);
      }
    }
    return hdrBuilder;
  }

}
