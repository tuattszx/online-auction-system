package auction.client.controllers;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class MainAuctionController extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        SwitchScene.switchScene(stage, "login-view.fxml", "Đăng nhập");
    }

    public static void main(String[] args) {
        launch();
    }
}