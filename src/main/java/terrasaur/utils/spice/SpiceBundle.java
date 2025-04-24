package terrasaur.utils.spice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picante.mechanics.*;
import picante.mechanics.providers.aberrated.AberratedEphemerisProvider;
import picante.mechanics.providers.lockable.LockableEphemerisProvider;
import picante.mechanics.providers.reference.ReferenceEphemerisProvider;
import picante.spice.MetakernelReader;
import picante.spice.SpiceEnvironment;
import picante.spice.SpiceEnvironmentBuilder;
import picante.spice.adapters.SpiceEphemerisID;
import picante.spice.adapters.SpiceFrameID;
import picante.spice.kernel.KernelInstantiationException;
import picante.spice.kernelpool.UnwritableKernelPool;
import picante.spice.kernelpool.KernelPool;
import picante.spice.provided.EphemerisNames;
import picante.spice.provided.FrameNames;
import picante.time.TimeConversion;

/**
 * Container class with a {@link SpiceEnvironment}, {@link AberratedEphemerisProvider}, and
 * {@link UnwritableKernelPool}.
 * 
 * @author nairah1
 *
 */
public class SpiceBundle {

  private final static Logger logger = LogManager.getLogger(SpiceBundle.class);

  private SpiceEnvironment spiceEnv;
  private AberratedEphemerisProvider abProvider;
  private UnwritableKernelPool kernelPool;
  private TimeConversion timeConversion;

  private List<File> loadedKernels;
  private Map<String, EphemerisID> objectNameBindings;
  private Map<Integer, EphemerisID> objectIDBindings;
  private Map<String, FrameID> frameNameBindings;
  private Map<Integer, FrameID> frameIDBindings;
  private Map<EphemerisID, FrameID> bodyFixedFrames;

  public static final class Builder {
    private List<File> kernels;
    private KernelPool kernelPool;
    private List<PositionVectorFunction> additionalEphemerisSources;
    private List<FrameTransformFunction> additionalFrameSources;
    private Map<FrameID, EphemerisID> additionalFrameCenters;
    private boolean lockable;
    private SpiceEnvironmentBuilder builder;
    private Map<String, EphemerisID> objectNameBindings;
    private Map<Integer, EphemerisID> objectIDBindings;
    private Map<String, FrameID> frameNameBindings;
    private Map<Integer, FrameID> frameIDBindings;

    /**
     * Add these kernels to the kernel list
     * 
     * @param kernels
     * @return
     */
    public Builder addKernelList(List<File> kernels) {
      this.kernels.addAll(kernels);
      return this;
    }

    /**
     * Add this kernel pool to the existing kernel pool
     * 
     * @param kernelPool
     * @return
     */
    public Builder addKernelPool(KernelPool kernelPool) {
      this.kernelPool.load(kernelPool);
      return this;
    }

    /**
     * Add the kernels and kernel pool variables in this list of metakernels to the existing kernel
     * list and kernel pool
     * 
     * @param metakernels
     * @return
     */
    public Builder addMetakernels(List<String> metakernels) {
      List<File> kernelPaths = new ArrayList<>();
      KernelPool kernelPool = new KernelPool();

      for (String mk : metakernels) {
        MetakernelReader mkReader = new MetakernelReader(mk);
        if (mkReader.isGood()) {
          kernelPaths.addAll(mkReader.getKernelsToLoad());
          kernelPool.load(mkReader.getKernelPool());
          if (mkReader.hasWarnings()) {
            for (String s : mkReader.getWarnLog())
              logger.warn(s.trim());
          }
        } else {
          for (String s : mkReader.getErrLog())
            logger.warn(s.trim());
          logger.warn("Did not load {}", mk);
        }
      }

      addKernelList(kernelPaths);
      addKernelPool(kernelPool);
      return this;
    }

    /**
     * Add these ephemerisSources to the {@link EphemerisAndFrameProvider} after the SPICE kernels
     * have been loaded.
     * 
     * @param funcs
     * @return
     */
    public Builder addEphemerisSources(List<PositionVectorFunction> funcs) {
      additionalEphemerisSources.addAll(funcs);
      return this;
    }

    /**
     * Add these frameSources to the {@link EphemerisAndFrameProvider} after the SPICE kernels have
     * been loaded.
     * 
     * @param funcs
     * @return
     */
    public Builder addFrameSources(List<FrameTransformFunction> funcs) {
      additionalFrameSources.addAll(funcs);
      return this;
    }

    /**
     * Add these FrameID -> EphemerisID mappings.
     * 
     * @param map
     * @return
     */
    public Builder addFrameCenters(Map<FrameID, EphemerisID> map) {
      additionalFrameCenters.putAll(map);
      return this;
    }

    /**
     * If true, use a {@link LockableEphemerisProvider} rather than the default
     * {@link ReferenceEphemerisProvider}.
     * 
     * @param lockable
     * @return
     */
    public Builder setLockable(boolean lockable) {
      this.lockable = lockable;
      return this;
    }

    public Builder setSpiceEnvironmentBuilder(SpiceEnvironmentBuilder builder) {
      this.builder = builder;
      return this;
    }

    public Builder() {
      this.kernels = new ArrayList<>();
      this.kernelPool = new KernelPool();
      this.additionalEphemerisSources = new ArrayList<>();
      this.additionalFrameSources = new ArrayList<>();
      this.additionalFrameCenters = new LinkedHashMap<>();
      lockable = false;
      builder = new SpiceEnvironmentBuilder();
      objectNameBindings = new HashMap<>();
      objectIDBindings = new HashMap<>();
      frameNameBindings = new HashMap<>();
      frameIDBindings = new HashMap<>();
    }

    public SpiceBundle build() {
      try {
        for (File kernel : kernels) {
          try {
            builder.load(kernel.getCanonicalPath(), kernel);
          } catch (KernelInstantiationException e) {
            logger.warn(e.getLocalizedMessage());
            logger.warn(String.format("Using forgiving loader for unsupported kernel format: %s.",
                kernel.getPath()));
            builder.forgivingLoad(kernel.getCanonicalPath(), kernel);
          }
        }
      } catch (KernelInstantiationException | IOException e) {
        logger.warn(e.getLocalizedMessage(), e);
      }

      // now bind name/code pairs to ephemeris IDs
      KernelPool mkPool = new KernelPool(kernelPool);
      SpiceEnvironment env = builder.build();
      UnwritableKernelPool envPool = env.getPool();
      KernelPool fullPool = new KernelPool();
      fullPool.load(envPool);
      fullPool.load(mkPool);

      // map of SPICE codes to ephemeris objects
      Map<Integer, EphemerisID> boundIds = new HashMap<>();

      // add built in objects
      EphemerisNames builtInIds = new EphemerisNames();
      for (Integer key : builtInIds.getStandardBindings().keySet()) {
        EphemerisID value = builtInIds.getStandardBindings().get(key);
        boundIds.put(key, value);
        objectNameBindings.put(value.getName().toUpperCase(), value);
        objectIDBindings.put(key, value);
      }

      // add built in frames
      FrameNames builtInFrames = new FrameNames();
      for (Integer key : builtInFrames.getStandardBindings().keySet()) {
        FrameID value = builtInFrames.getStandardBindings().get(key);
        frameNameBindings.put(value.getName().toUpperCase(), value);
        frameIDBindings.put(key, value);
      }

      for (PositionVectorFunction f : env.getEphemerisSources()) {
        // initialize with known objects in SPK files
        EphemerisID id = f.getTargetID();
        if (id instanceof SpiceEphemerisID) {
          SpiceEphemerisID spiceID = (SpiceEphemerisID) id;
          boundIds.put(spiceID.getIDCode(), spiceID);
        }
      }

      if (envPool.hasKeyword("NAIF_BODY_NAME") && envPool.hasKeyword("NAIF_BODY_CODE")) {
        List<String> names = envPool.getStrings("NAIF_BODY_NAME");
        List<Integer> codes = envPool.getIntegers("NAIF_BODY_CODE");

        if (names.size() != codes.size())
          logger.warn(String.format(
              "NAIF_BODY_CODE has %d entries while NAIF_BODY_NAME has %d entries.  Will not bind any of these ids.",
              codes.size(), names.size()));
        else {
          for (int i = 0; i < codes.size(); i++) {
            int code = codes.get(i);
            String name = names.get(i);
            SpiceEphemerisID spiceID = new SpiceEphemerisID(code, name);
            objectNameBindings.put(name.toUpperCase(), spiceID);
            objectIDBindings.put(code, spiceID);
            // builder.bindEphemerisID(name, spiceID);
            new SpkCodeBinder(code, name, spiceID).configure(builder);
            logger.debug("From text kernel: binding name {} to code {}", name, code);
            boundIds.put(code, spiceID);
          }
        }
      }

      // bind SPICE ids defined in metakernels
      if (mkPool.hasKeyword("NAIF_BODY_NAME") && mkPool.hasKeyword("NAIF_BODY_CODE")) {
        List<String> names = mkPool.getStrings("NAIF_BODY_NAME");
        List<Integer> codes = mkPool.getIntegers("NAIF_BODY_CODE");

        if (names.size() != codes.size())
          logger.warn(String.format(
              "NAIF_BODY_CODE has %d entries while NAIF_BODY_NAME has %d entries.  Will not bind any of these ids.",
              codes.size(), names.size()));
        else {
          for (int i = 0; i < codes.size(); i++) {
            int code = codes.get(i);
            String name = names.get(i);
            SpiceEphemerisID spiceID = new SpiceEphemerisID(code, name);
            objectNameBindings.put(name.toUpperCase(), spiceID);
            objectIDBindings.put(code, spiceID);
            // builder.bindEphemerisID(name, spiceID);
            new SpkCodeBinder(code, name, spiceID).configure(builder);
            logger.debug("From metakernel: binding name {} to code {}", name, code);
            boundIds.put(code, spiceID);
          }
        }
      }

      // now populate frame lookup maps
      Set<String> keywords = fullPool.getKeywords();
      for (String keyword : keywords) {
        if (keyword.startsWith("FRAME_") && keyword.endsWith("_NAME")) {
          String[] parts = keyword.split("_");
          int id = Integer.parseInt(parts[1]);

          String spiceName = fullPool.getStrings(keyword).get(0);
          String frameKeyword = String.format("FRAME_%s", spiceName);
          if (!fullPool.hasKeyword(frameKeyword)) {
            logger.warn("Kernel pool does not contain keyword " + frameKeyword);
            continue;
          }
          Integer spiceFrameCode = fullPool.getDoubles(frameKeyword).get(0).intValue();

          if (spiceFrameCode != id) {
            logger.warn(
                String.format("Expected keyword %s to be %d, found %d instead.  Skipping this one.",
                    frameKeyword, id, spiceFrameCode));
            continue;
          }

          FrameID frameID = new SpiceFrameID(spiceFrameCode);
          frameNameBindings.put(spiceName, frameID);
          frameIDBindings.put(spiceFrameCode, frameID);
          builder.bindFrameID(spiceName, frameID);

          // The FRAME_XXX_CENTER can have a numeric or string value
          String centerKey = String.format("FRAME_%d_CENTER", spiceFrameCode);

          EphemerisID spiceID = null;
          String centerCodeString = "";
          if (fullPool.isStringValued(centerKey)) {
            String centerCode = fullPool.getStrings(centerKey).get(0);
            spiceID = objectNameBindings.get(centerCode);
            centerCodeString = centerCode;
          } else {
            int centerCode = 0;
            if (fullPool.isDoubleValued(centerKey)) {
              centerCode = fullPool.getDoubles(centerKey).get(0).intValue();
            } else if (fullPool.isIntegerValued(centerKey)) {
              centerCode = fullPool.getIntegers(centerKey).get(0);
            }
            spiceID = boundIds.get(centerCode);
            centerCodeString = String.format("%d", centerCode);
          }

          if (spiceID == null)
            logger.warn(String.format("Unknown ephemeris object specified by FRAME_%d_CENTER (%s).",
                spiceFrameCode, centerCodeString));
          else {
            additionalFrameCenters.put(frameID, spiceID);
            logger.debug("Binding SPICE frame {} to frame code {}", frameID.getName(),
                spiceFrameCode);
          }
        }
      }

      // populate body fixed map
      keywords = fullPool.getKeywords();
      Map<EphemerisID, FrameID> bodyFixedFrames = new HashMap<>();
      for (EphemerisID body : objectNameBindings.values()) {
        FrameID iauFrame =
            frameNameBindings.get(String.format("IAU_%s", body.getName().toUpperCase()));
        if (iauFrame != null)
          bodyFixedFrames.put(body, iauFrame);
      }
      for (String keyword : keywords) {
        if (keyword.startsWith("OBJECT_") && keyword.endsWith("_FRAME")) {
          String[] parts = keyword.split("_");
          EphemerisID thisBody = null;
          try {
            Integer idCode = Integer.parseInt(parts[1]);
            thisBody = objectIDBindings.get(idCode);
          } catch (NumberFormatException e) {
            thisBody = objectNameBindings.get(parts[1].toUpperCase());
          }
          if (thisBody != null) {
            FrameID thisFrame = null;
            if (fullPool.isIntegerValued(keyword))
              thisFrame = frameIDBindings.get(fullPool.getIntegers(keyword).get(0));
            else
              thisFrame = frameNameBindings.get(fullPool.getStrings(keyword).get(0).toUpperCase());
            if (thisFrame != null)
              bodyFixedFrames.put(thisBody, thisFrame);
          }
        }
      }

      env = builder.build();
      List<PositionVectorFunction> envEphSources = new ArrayList<>(env.getEphemerisSources());
      envEphSources.addAll(additionalEphemerisSources);
      List<FrameTransformFunction> envFrameSources = new ArrayList<>(env.getFrameSources());
      envFrameSources.addAll(additionalFrameSources);

      EphemerisAndFrameProvider provider =
          lockable ? new LockableEphemerisProvider(envEphSources, envFrameSources)
              : new ReferenceEphemerisProvider(envEphSources, envFrameSources);

      SpiceBundle bundle = new SpiceBundle();
      bundle.spiceEnv = env;
      Map<FrameID, EphemerisID> frameCenters = new LinkedHashMap<>(env.getFrameCenterMap());
      frameCenters.putAll(additionalFrameCenters);

      bundle.abProvider = AberratedEphemerisProvider.createSingleIteration(provider, frameCenters);
      KernelPool kp = new KernelPool();
      kp.load(env.getPool());
      kp.load(kernelPool);
      bundle.kernelPool = new UnwritableKernelPool(kp);
      bundle.loadedKernels = Collections.unmodifiableList(kernels);
      bundle.objectNameBindings = Collections.unmodifiableMap(objectNameBindings);
      bundle.objectIDBindings = Collections.unmodifiableMap(objectIDBindings);
      bundle.frameNameBindings = Collections.unmodifiableMap(frameNameBindings);
      bundle.frameIDBindings = Collections.unmodifiableMap(frameIDBindings);
      bundle.bodyFixedFrames = Collections.unmodifiableMap(bodyFixedFrames);

      // initialize TimeSystems with this kernel pool
      bundle.timeConversion = new TimeConversion(env.getLSK());

      return bundle;
    }
  }

  private SpiceBundle() {}

  public SpiceEnvironment getSpiceEnv() {
    return spiceEnv;
  }

  public AberratedEphemerisProvider getAbProvider() {
    return abProvider;
  }

  /**
   * Returns the kernel pool. This includes key/value pairs supplied in metakernels to the builder,
   * while {@link SpiceEnvironment#getPool()} does not.
   * 
   * @return
   */
  public UnwritableKernelPool getKernelPool() {
    return kernelPool;
  }

  /**
   * Return a TimeConversion object that can be used to convert between the UTC, TDB, TDT, TAI, and
   * GPS time systems.
   * 
   * @return
   */
  public TimeConversion getTimeConversion() {
    return timeConversion;
  }

  /**
   * Return the last file in the list of loaded kernels matching supplied expression.
   * 
   * @param regex Regular expression, used in a {@link Pattern} to match filenames in list of loaded
   *        kernels.
   * 
   * @return file matching regex, or null if not found
   */
  public File findKernel(String regex) {
    Pattern p = Pattern.compile(regex);
    return getKernels().stream().filter(f -> p.matcher(f.getPath()).matches())
        .reduce((first, second) -> second).orElse(null);
  }

  /**
   * Return a list of kernels in the order they were loaded.
   * 
   * @return
   */
  public List<File> getKernels() {
    return loadedKernels;
  }

  /**
   * Split path on {@link File#separator}, return a list of strings each shorter than wrap that when
   * concatenated return the original path. Intended for use when writing a metakernel; split paths
   * longer than 80 characters into two strings.
   * 
   * @param f
   * @param wrap
   * @return
   */
  private List<String> splitPath(File f, int wrap) {

    List<String> list = new ArrayList<>();
    String[] parts = f.getAbsolutePath().split(File.separator);

    StringBuffer sb = new StringBuffer(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      if (sb.toString().length() + parts[i].length() > wrap) {
        sb.append(File.separator);
        list.add(sb.toString());
        sb = new StringBuffer(parts[i]);
      } else {
        sb.append(String.format("%s%s", File.separator, parts[i]));
      }
    }
    list.add(sb.toString());

    return list;
  }

  /**
   * Write a metakernel containing all kernels loaded in this bundle
   * 
   * @param file
   */
  public void writeSingleMetaKernel(File file, String comments) {
    try (PrintWriter pw = new PrintWriter(file)) {
      pw.println("KPL/MK");
      pw.println(comments);
      pw.println("\\begindata\n");
      pw.println("KERNELS_TO_LOAD = (");
      for (File f : loadedKernels) {

        List<String> parts = splitPath(f, 78);
        if (parts.size() == 1)
          pw.printf("'%s'\n", f.getAbsolutePath());
        else {
          for (int i = 0; i < parts.size() - 1; i++) {
            String part = parts.get(i);
            pw.printf("'%s+'\n", part);
          }
          pw.printf("'%s'\n", parts.get(parts.size() - 1));
        }
      }
      pw.println(")\n");

      // print out other _TO_LOAD variables.
      for (String keyword : kernelPool.getKeywords()) {
        if (keyword.endsWith("_TO_LOAD")) {
          if (keyword.length() > 32)
            logger.warn("Kernel variable {} has length {} (SPICE max is 32 characters)", keyword,
                keyword.length());
          pw.printf("%s = (\n", keyword);
          for (String value : kernelPool.getStrings(keyword)) {
            File f = new File(value);
            List<String> parts = splitPath(f, 78);
            if (parts.size() == 1)
              pw.printf("'%s'\n", f.getAbsolutePath());
            else {
              for (int i = 0; i < parts.size() - 1; i++) {
                String part = parts.get(i);
                pw.printf("'%s+'\n", part);
              }
              pw.printf("'%s'\n", parts.get(parts.size() - 1));
            }
          }
          pw.println(")\n");
        }
      }
      pw.println("\\begintext");
    } catch (FileNotFoundException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }

  /**
   * @param idCode
   * @return the {@link EphemerisID} from the pool of known objects with the given id or null if
   *         there is no such object
   */
  public EphemerisID getObject(int idCode) {
    EphemerisID object = objectIDBindings.get(idCode);
    if (object == null) {
      try {
        object = getAbProvider().getKnownObjects(new HashSet<>()).stream().filter(
            id -> id instanceof SpiceEphemerisID && ((SpiceEphemerisID) id).getIDCode() == idCode)
            .findFirst().get();
      } catch (NoSuchElementException e) {
        logger.warn("No object with id {} has been defined", idCode);
      }
    }
    return object;
  }

  /**
   * @param name
   * @return the {@link EphemerisID} from the pool of known objects with the given name or null if
   *         there is no such object
   */
  public EphemerisID getObject(String name) {
    EphemerisID object = objectNameBindings.get(name.toUpperCase());
    if (object == null) {
      try {
        object = getAbProvider().getKnownObjects(new HashSet<>()).stream()
            .filter(id -> name.equalsIgnoreCase(id.getName())).findFirst().get();
      } catch (NoSuchElementException e) {
        try {
          object = getObject(Integer.parseInt(name));
        } catch (NumberFormatException e1) {
        }
        if (object == null)
          logger.warn("No object {} has been defined", name);
      }
    }

    return object;
  }

  /**
   * @param name
   * @return the {@link FrameID} from the pool of known frames with the given name
   */
  public FrameID getFrame(String name) {
    FrameID frame = frameNameBindings.get(name);
    if (frame == null) {
      try {
        frame = getAbProvider().getKnownFrames(new HashSet<>()).stream()
            .filter(id -> name.equalsIgnoreCase(id.getName())).findFirst().get();
      } catch (NoSuchElementException e) {
        try {
          frame = getFrame(Integer.parseInt(name));
        } catch (NumberFormatException e1) {
        }
        if (frame == null)
          logger.warn("No frame {} has been defined", name);
      }
    }
    return frame;
  }

  /**
   * Return the body fixed frame associated with this body. Normally this is the IAU frame. A kernel
   * may define a body fixed frame using the OBJECT_*_FRAME keyword. From the "frames" required
   * reading:
   * 
   * <pre>
   *    OBJECT_&lt;name or spk_id&gt;_FRAME =  '&lt;frame name&gt;'
   * or
   *    OBJECT_&lt;name or spk_id&gt;_FRAME =  &lt;frame ID code&gt;
   * </pre>
   * 
   * @param body
   * @return
   */
  public FrameID getBodyFixedFrame(EphemerisID body) {
    return bodyFixedFrames.get(body);
  }

  /**
   * 
   * @param idCode
   * @return the {@link FrameID} from the pool of known frames with the given id
   */
  public FrameID getFrame(int idCode) {
    FrameID frame = frameIDBindings.get(idCode);
    if (frame == null) {
      try {
        frame = getAbProvider().getFrameProvider().getKnownFrames(new HashSet<>()).stream()
            .filter(id -> id instanceof SpiceFrameID && ((SpiceFrameID) id).getIDCode() == idCode)
            .collect(Collectors.toList()).get(0);
      } catch (NoSuchElementException e) {
        logger.warn("No frame with id has been defined", idCode);
      }
    }
    return frame;
  }

  /**
   * Add objects known to the provider as well as objects bound by metakernels to the supplied
   * buffer.
   * 
   * @param buffer the buffer to receive the additional ephemeris ID codes
   * 
   * @return a reference to buffer for convenience.
   */
  public Set<EphemerisID> getKnownObjects(Set<EphemerisID> buffer) {
    getAbProvider().getKnownObjects(buffer);
    buffer.addAll(objectIDBindings.values());
    return buffer;
  }

  /**
   * Add frames known to the provider as well as the frames bound by the metakernel to the supplied
   * buffer.
   * 
   * @param buffer the buffer to receive the additional frame ID codes
   * @return a reference to buffer for convenience
   */
  public Set<FrameID> getKnownFrames(Set<FrameID> buffer) {
    getAbProvider().getFrameProvider().getKnownFrames(buffer);
    buffer.addAll(frameIDBindings.values());
    return buffer;
  }
}
