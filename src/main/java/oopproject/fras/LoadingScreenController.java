package oopproject.fras;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class LoadingScreenController implements Initializable {

    @FXML
    private ImageView logo;

    @FXML
    private ProgressBar LoadingBar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startLoading();
    }

    private void startLoading() {
        logoTranisition();
    }

    private void logoTranisition() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), logo);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.setCycleCount(1);
        fadeTransition.setAutoReverse(false);

        fadeTransition.play();
        fadeTransition.setOnFinished(actionEvent -> loadingBarTransition());
    }

    private void loadingBarTransition() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.7), LoadingBar);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.setCycleCount(1);
        fadeTransition.setAutoReverse(false);

        fadeTransition.play();
        fadeTransition.setOnFinished(actionEvent -> loadingBarProgress());
    }

    private void loadingBarProgress() {
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    final double progress = i / 100.0;
                    Thread.sleep(5);

                    LoadingBar.setProgress(progress);

                }
                Platform.runLater(() -> switchKeFaceRecog());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void switchKeFaceRecog() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), logo.getScene().getRoot());
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);

        fadeTransition.setOnFinished(actionEvent -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
                Parent root = loader.load();

                root.setOpacity(0);

                Stage stage = (Stage) logo.getScene().getWindow();
                Scene faceRecogScene = new Scene(root);
                faceRecogScene.getStylesheets().add("ui.css");
                stage.setScene(faceRecogScene);

                FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(0.5), root);
                fadeInTransition.setFromValue(0.0);
                fadeInTransition.setToValue(1.0);
                fadeInTransition.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        fadeTransition.play();
    }
}