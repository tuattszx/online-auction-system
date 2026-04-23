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
import java.util.Objects;

public class ViewManager {
    private static final Map<String, Parent> views = new HashMap<>();
    private static final String FXML_PATH_PREFIX = "/auction/view/";
    private static final String ICON_PATH = "/auction/img/logo.png";

    /**
     * Lấy view từ cache hoặc load từ FXML file
     * @param fxmlPath đường dẫn file FXML (ví dụ: "main-view.fxml")
     * @return Parent object của view
     * @throws IOException nếu không tìm thấy file
     */
    public static Parent getView(String fxmlPath) throws IOException {
        if (fxmlPath == null || fxmlPath.isEmpty()) {
            throw new IllegalArgumentException("FXML path không thể rỗng!");
        }

        if (!views.containsKey(fxmlPath)) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                        ViewManager.class.getResource(FXML_PATH_PREFIX + fxmlPath),
                        "Không tìm thấy file: " + fxmlPath
                    )
                );
                Parent root = loader.load();
                views.put(fxmlPath, root);
            } catch (NullPointerException e) {
                throw new IOException("File FXML không tồn tại: " + fxmlPath, e);
            }
        }
        return views.get(fxmlPath);
    }

    /**
     * Chuyển scene từ một node (button, label, v.v.)
     * @param event sự kiện từ user
     * @param fxmlPath đường dẫn file FXML
     * @param title tiêu đề cửa sổ
     */
    public static void switchScene(Event event, String fxmlPath, String title) {
        try {
            // Validation
            if (event == null) {
                throw new IllegalArgumentException("Event không thể null!");
            }
            if (fxmlPath == null || fxmlPath.isEmpty()) {
                throw new IllegalArgumentException("FXML path không thể rỗng!");
            }

            // Lấy source của event
            Object source = event.getSource();
            if (!(source instanceof Node)) {
                throw new ClassCastException("Event source phải là Node! Nhận: " + source.getClass().getName());
            }

            Node sourceNode = (Node) source;
            Scene scene = sourceNode.getScene();
            if (scene == null) {
                throw new RuntimeException("Node không được add vào Scene!");
            }

            Stage stage = (Stage) scene.getWindow();
            if (stage == null) {
                throw new RuntimeException("Scene không được add vào Stage!");
            }

            // Load view mới
            Parent root = getView(fxmlPath);
            if (root == null) {
                throw new RuntimeException("Không thể load view: " + fxmlPath);
            }

            // Set icon cho stage
            setStageIcon(stage);

            // Set title
            if (title != null && !title.isEmpty()) {
                stage.setTitle(title);
            }

            // Chuyển scene
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }

        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Input", e.getMessage());
            System.err.println("❌ Validation Error: " + e.getMessage());
        } catch (ClassCastException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Type", e.getMessage());
            System.err.println("❌ Type Error: " + e.getMessage());
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Runtime", e.getMessage());
            System.err.println("❌ Runtime Error: " + e.getMessage());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Tải File", "Không tìm thấy file: " + fxmlPath);
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Không Xác Định", e.getMessage());
            System.err.println("❌ Unexpected Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set icon cho stage (với try-with-resources để avoid leak)
     */
    private static void setStageIcon(Stage stage) {
        try (InputStream iconStream = ViewManager.class.getResourceAsStream(ICON_PATH)) {
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                stage.getIcons().clear();
                stage.getIcons().add(icon);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Không tìm thấy icon: " + ICON_PATH);
        }
    }

    /**
     * Xóa cache views
     */
    public static void clearCache() {
        views.clear();
        System.out.println("✅ View cache cleared");
    }

    /**
     * Hiển thị alert dialog
     */
    public static void showAlert(Alert.AlertType alertType, String title, String message) {
        if (title == null || title.isEmpty()) {
            title = alertType.toString();
        }

        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "Không có thông báo");
        alert.showAndWait();
    }
    public static boolean confirmAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).isPresent();
    }
}