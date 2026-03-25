package auction.client.controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainAuctionController extends Application {
    public static void switchScene(Stage stage, String fxml, String title){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(MainAuctionController.class.getResource("/auction/view/" + fxml));
            Scene scene = new Scene(fxmlLoader.load(), 720, 540);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    @Override
    public void start(Stage stage) throws IOException {
        // Trỏ đúng vào file FXML của bạn trong folder auction
        FXMLLoader fxmlLoader = new FXMLLoader(MainAuctionController.class.getResource("/auction/view/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 720, 540);

        stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
        stage.setScene(scene);
        stage.show(); // Hiển thị màn hình
    }

    public static void main(String[] args) {
        launch();
    }
}