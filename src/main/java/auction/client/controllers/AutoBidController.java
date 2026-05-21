
   package auction.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

    public class AutoBidController {
        private ItemviewController itemviewController;
        @FXML private TextField maxBidField;
        @FXML private TextField incrementField;
        @FXML private Label errorLabel;

        private Popup parentPopup; // Giữ tham chiếu để tự ẩn chính nó khi xong
        private double currentPrice;


        // Hàm nhận dữ liệu từ ItemviewController truyền sang
        public void setInitData(Popup popup, double currentPrice,ItemviewController main) {
            this.parentPopup = popup;
            this.currentPrice = currentPrice;
            this.itemviewController=main;

            // Thiết lập gợi ý nhập liệu ngay khi mở
            maxBidField.setPromptText("Phải lớn hơn " + (currentPrice + 10) + " €");
            errorLabel.setVisible(false);
        }

        @FXML
        void handleCancel(ActionEvent event) {
            if (parentPopup != null) {
                parentPopup.hide();
            }
        }

        @FXML
        void handleStartAutoBid(ActionEvent event) {
            errorLabel.setVisible(false);
            String maxBidText = maxBidField.getText().trim();

            try {
                double maxBid = Double.parseDouble(maxBidText);
                double incre=Double.parseDouble(incrementField.getText().trim());
                if (maxBid <= currentPrice) {
                    errorLabel.setTextFill(Color.web("#E53E3E"));
                    errorLabel.setText("⚠️ Giá tối đa phải lớn hơn giá hiện tại!");
                    errorLabel.setVisible(true);
                    return;
                }
                if(incre <5){
                    errorLabel.setTextFill(Color.web("#E53E3E"));
                    errorLabel.setText("⚠️ Bước giá phải lớn hơn 4!");
                    errorLabel.setVisible(true);
                    return;
                }
                if (maxBidText.isEmpty()) {
                    errorLabel.setTextFill(Color.web("#E53E3E"));
                    errorLabel.setText("⚠️ Vui lòng nhập giá tối đa!");
                    errorLabel.setVisible(true);
                    return;
                }
                if(incrementField.getText().trim().isEmpty()){
                    errorLabel.setTextFill(Color.web("#E53E3E"));
                    errorLabel.setText("⚠️ Vui lòng nhập bước giá!");
                    errorLabel.setVisible(true);
                    return;
                }
            } catch (NumberFormatException e) {
                errorLabel.setTextFill(Color.web("#E53E3E"));
                errorLabel.setText("⚠️ Vui lòng nhập số hợp lệ!");
                errorLabel.setVisible(true);
                return;
            }

            // --- THÀNH CÔNG ---
            errorLabel.setTextFill(Color.web("#38A169"));
            errorLabel.setText("✅ Kích hoạt Auto Bid thành công!");
            errorLabel.setVisible(true);
            itemviewController.activateAutoBidStatus(maxBidText);

            // Đợi 1 giây rồi ẩn popup
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            delay.setOnFinished(e -> {
                if (parentPopup != null) parentPopup.hide();
            });
            delay.play();
        }
    }
