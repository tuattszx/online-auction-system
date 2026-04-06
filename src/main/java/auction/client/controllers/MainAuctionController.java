package auction.client.controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainAuctionController extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainAuctionController.class.getResource("/auction/view/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.getIcons().add(new javafx.scene.image.Image(
                MainAuctionController.class.getResourceAsStream("/auction/img/logo.png")
        ));
        stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}