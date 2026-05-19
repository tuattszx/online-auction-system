package auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastManager {

    // Phân loại 4 mẫu thông báo
    public enum ToastType {
        INFO, WARNING, ERROR, SUCCESS
    }

    public static void showToast(Stage stage, ToastType type, String message) {
        if (stage == null) return;

        Popup toast = new Popup();
        HBox toastRoot = new HBox();

        // Liên kết file CSS vào khung nạp động
        toastRoot.getStylesheets().add(ToastManager.class.getResource("/auction/css/toast.css").toExternalForm());
        toastRoot.getStyleClass().add("toast-container");

        // 1. Phân loại Icon và Class CSS dựa trên loại thông báo được chọn
        String iconSymbol = "";
        switch (type) {
            case INFO:
                iconSymbol = "ⓘ "; // Icon chữ i tròn
                toastRoot.getStyleClass().add("toast-info");
                break;
            case WARNING:
                iconSymbol = "⚠ "; // Icon hình tam giác cảnh báo
                toastRoot.getStyleClass().add("toast-warning");
                break;
            case ERROR:
                iconSymbol = "🚫 "; // Icon hình tròn gạch chéo
                toastRoot.getStyleClass().add("toast-error");
                break;
            case SUCCESS:
                iconSymbol = "✔ "; // Icon dấu tích xanh
                toastRoot.getStyleClass().add("toast-success");
                break;
        }

        // 2. Tạo phần nội dung văn bản (Gồm Icon + Nội dung)
        Label lblMessage = new Label(iconSymbol + message);
        lblMessage.getStyleClass().add("toast-text");

        // 3. Tạo nút đóng (Dấu x) nằm ở phía cuối bên phải
        Label lblClose = new Label("✖");
        lblClose.getStyleClass().add("toast-close-btn");
        lblClose.setOnMouseClicked(event -> toast.hide()); // Bấm dấu x tắt luôn

        // Dùng khoảng trống trung gian đẩy dấu x về sát mép phải màn hình
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toastRoot.getChildren().addAll(lblMessage, spacer, lblClose);
        toast.getContent().add(toastRoot);

        // 4. Định vị tọa độ (Góc dưới bên phải màn hình)
        double x = stage.getX() + stage.getWidth() - 380;
        double y = stage.getY() + stage.getHeight() - 80;
        toast.show(stage, x, y);

        // 5. Chuỗi hiệu ứng tự động ẩn hiện mượt mà
        toastRoot.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toastRoot);
        fadeIn.setToValue(1.0);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toastRoot);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, delay, fadeOut);
        sequence.setOnFinished(e -> toast.hide());
        sequence.play();
    }
}