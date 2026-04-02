package auction.client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SwitchScene {
    public static int height = 480;
    public static int width = 640;
    public static void switchScene(Stage stage, String fxml, String title){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(MainAuctionController.class.getResource("/auction/view/" + fxml));
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
