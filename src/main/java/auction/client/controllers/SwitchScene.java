package auction.client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SwitchScene {
    public static int height = 480;
    public static int width = 640;
    public static void switchScene(Stage stage, String fxml, String title){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(SwitchScene.class.getResource("/auction/view/" + fxml));
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            stage.getIcons().add(new javafx.scene.image.Image(
                    SwitchScene.class.getResourceAsStream("/auction/img/logo.png")
            ));
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
