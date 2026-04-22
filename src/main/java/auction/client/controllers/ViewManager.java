package auction.client.controllers;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    private static Map<String, Parent> views = new HashMap<>();

    public static Parent getView(String fxmlPath) throws IOException {
        if (!views.containsKey(fxmlPath)) {
            FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource("/auction/view/" + fxmlPath));
            views.put(fxmlPath, loader.load());
        }
        return views.get(fxmlPath);
    }

    public static void switchScene(Event event, String fxmlPath, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = ViewManager.getView(fxmlPath);
            InputStream iconStream = ViewManager.class.getResourceAsStream("/auction/img/logo.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }

            stage.setTitle(title);

            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void clearCache() {
        views.clear();
    }

    public static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}