package oopproject.fras;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Parent root = FXMLLoader.load(getClass().getResource("LoadingScreen.fxml"));

        Scene scene = new Scene(root, 1280, 720);

        Image logo = new Image(getClass().getResourceAsStream("img/logo.png"));
        stage.getIcons().add(logo);
        stage.setTitle("Attendance System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}