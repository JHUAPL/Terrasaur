package terrasaur.utils.lidar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * This class parses the JSON output of lidar-optimize
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class LidarTransformation {

  private final static Logger logger = LogManager.getLogger(LidarTransformation.class);

  private List<Double> translation;
  private List<Double> rotation;
  private List<Double> centerOfRotation;
  private int startId;
  private int stopId;
  private double minErrorBefore;
  private double maxErrorBefore;
  private double stdBefore;
  private double minErrorAfter;
  private double maxErrorAfter;
  private double rmsAfter;
  private double meanErrorAfter;
  private double stdAfter;

  private Vector3D translationObject;
  private Rotation rotationObject;
  private Vector3D centerOfRotationObject;

  /**
   * Create a transform with the translation set to {@link Vector3D#ZERO}, rotation to
   * {@link Rotation#IDENTITY}, and center of rotation to {@link Vector3D#ZERO}.
   * 
   * @return
   */
  public static LidarTransformation defaultTransform() {
    LidarTransformation t = new LidarTransformation();
    t.translationObject = Vector3D.ZERO;
    t.rotationObject = Rotation.IDENTITY;
    t.centerOfRotationObject = Vector3D.ZERO;

    return t;
  }

  private static LidarTransformation fromJSON(Reader reader) {
    Gson gson = new Gson();
    JsonReader jsonReader = new JsonReader(reader);
    LidarTransformation object =
        gson.fromJson(jsonReader, new TypeToken<LidarTransformation>() {}.getType());
    return object;
  }

  /**
   * Load a LidarTransformation object from a JSON file.
   * 
   * @param file
   * @return
   */
  public static LidarTransformation fromJSON(File file) {
    FileReader reader = null;
    try {
      reader = new FileReader(file);
      return fromJSON(reader);
    } catch (FileNotFoundException e) {
      logger.warn(e.getLocalizedMessage());
    }
    return null;
  }

  /**
   * Load a LidarTransformation object from a JSON string.
   * 
   * @param string
   * @return
   */
  public static LidarTransformation fromJSON(String string) {
    StringReader reader = new StringReader(string);
    return fromJSON(reader);
  }

  public Vector3D getCenterOfRotation() {
    if (centerOfRotationObject == null) {
      centerOfRotationObject =
          new Vector3D(centerOfRotation.get(0), centerOfRotation.get(1), centerOfRotation.get(2));
    }
    return centerOfRotationObject;
  }

  public Rotation getRotation() {
    if (rotationObject == null) {
      rotationObject =
          new Rotation(rotation.get(0), rotation.get(1), rotation.get(2), rotation.get(3), true);
    }
    return rotationObject;
  }

  public Vector3D getTranslation() {
    if (translationObject == null) {
      translationObject = new Vector3D(translation.get(0), translation.get(1), translation.get(2));
    }
    return translationObject;
  }

  /**
   * Transform an input point. Steps:
   * <ol>
   * <li>Subtract {@link #getCenterOfRotation()}</li>
   * <li>Apply {@link #getRotation()}</li>
   * <li>Add {@link #getCenterOfRotation()}</li>
   * <li>Add {@link #getTranslation()}</li>
   * </ol>
   * 
   * @param point
   * @return transformed point
   */
  public Vector3D transformPoint(Vector3D point) {
    Vector3D transformed = point.subtract(getCenterOfRotation());
    transformed = getRotation().applyTo(transformed);
    transformed = transformed.add(getCenterOfRotation());
    transformed = transformed.add(getTranslation());

    return transformed;
  }

}


