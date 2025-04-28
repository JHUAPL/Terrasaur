/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package terrasaur.gui;

import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.NavigableSet;
import java.util.ResourceBundle;
import java.util.TreeSet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import terrasaur.apps.TranslateTime;
import terrasaur.utils.AppVersion;
import spice.basic.SpiceException;

public class TranslateTimeController implements Initializable {

  private TranslateTime tt;

  public TranslateTimeController(Stage stage) {
    this.tt = TranslateTimeFX.tt;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.title.setText("Translate Time");
    this.version.setText(AppVersion.getVersionString());

    // populate SCLK menu
    NavigableSet<Integer> sclkIDs = new TreeSet<>(TranslateTimeFX.sclkIDs);
    this.sclkChoice.getItems().addAll(sclkIDs);
    this.sclkChoice.getSelectionModel().selectedIndexProperty()
        .addListener(new ChangeListener<Number>() {

          @Override
          public void changed(ObservableValue<? extends Number> observable, Number oldValue,
              Number newValue) {
            tt.setSCLKKernel(sclkChoice.getItems().get((Integer) newValue));

          }

        });
    this.sclkChoice.getSelectionModel().select(0);

    try {
      String zTime = ZonedDateTime.now(ZoneOffset.UTC).toString().strip();
      // strip off the final "Z"
      this.tt.setUTC(zTime.substring(0, zTime.length() - 1));
      updateTime();
    } catch (SpiceException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private Label title;

  @FXML
  private Label version;

  @FXML
  private TextField julianString;

  @FXML
  private void setJulian() throws NumberFormatException, SpiceException {
    if (julianString.getText().trim().length() > 0)
      tt.setJulianDate(Double.parseDouble(julianString.getText()));
    updateTime();
  }

  @FXML
  private ChoiceBox<Integer> sclkChoice;

  @FXML
  private TextField sclkString;

  @FXML
  private void setSCLK() throws SpiceException {
    if (sclkString.getText().trim().length() > 0)
      tt.setSCLK(sclkString.getText());
    updateTime();
  }

  @FXML
  private TextField tdbString;

  @FXML
  private void setTDB() throws SpiceException {
    if (tdbString.getText().trim().length() > 0)
      tt.setTDB(Double.parseDouble(tdbString.getText()));
    updateTime();
  }

  @FXML
  private TextField tdbCalendarString;

  @FXML
  private void setTDBCalendar() throws SpiceException {
    if (tdbCalendarString.getText().trim().length() > 0)
      tt.setTDBCalendarString(tdbCalendarString.getText());
    updateTime();
  }

  @FXML
  private TextField utcString;

  @FXML
  private Label utcLabel;

  @FXML
  private void setUTC() throws SpiceException {
    if (utcString.getText().trim().length() > 0)
      tt.setUTC(utcString.getText());
    updateTime();
  }

  private void updateTime() throws SpiceException {
    julianString.setText(tt.toJulian());
    sclkString.setText(tt.toSCLK().toString());
    tdbString.setText(String.format("%.6f", tt.toTDB().getTDBSeconds()));
    tdbCalendarString.setText(tt.toTDB().toString("YYYY-MM-DDTHR:MN:SC.### ::TDB"));
    utcString.setText(tt.toTDB().toUTCString("ISOC", 3));
    utcLabel.setText(String.format("UTC (DOY %s)", tt.toTDB().toString("DOY")));
  }

}
