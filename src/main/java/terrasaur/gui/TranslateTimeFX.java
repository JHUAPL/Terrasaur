package terrasaur.gui;

import java.util.Collection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import terrasaur.apps.TranslateTime;

public class TranslateTimeFX extends Application {

  // package private so it's visible from TranslateTimeController
  static TranslateTime tt;
  static Collection<Integer> sclkIDs;

  public static void setSCLKIDs(Collection<Integer> sclkIDs) {
    TranslateTimeFX.sclkIDs = sclkIDs;
  }

  public static void setTranslateTime(TranslateTime tt) {
    TranslateTimeFX.tt = tt;
  }

  public static void main(String[] args) {
    launch(args);
    Platform.exit();
  }

  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getClass().getResource("/terrasaur/gui/TranslateTime.fxml"));
    TranslateTimeController controller = new TranslateTimeController(stage);
    loader.setController(controller);

    Parent root = loader.load();
    Scene scene = new Scene(root);
    stage.setScene(scene);
    stage.show();
  }

}
