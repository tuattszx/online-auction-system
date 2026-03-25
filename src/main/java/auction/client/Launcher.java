package auction.client;

import auction.client.controllers.MainAuctionController;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(MainAuctionController.class, args);
    }
}