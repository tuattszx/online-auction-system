
   package auction.client.controllers;

import auction.client.services.AuctionManager;
import auction.client.session.DataSession;
import auction.common.model.users.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

    public class AutoBidController {
        @FXML private Label lbbalance;
        private ItemviewController itemviewController;
        @FXML private TextField maxBidField;
        @FXML private TextField incrementField;
        @FXML private Label errorLabel;
        @FXML private Button startAutoBidButton;
        @FXML private Button cancelButton;

        private Popup parentPopup; // Giữ tham chiếu để tự ẩn chính nó khi xong
        private long currentPrice;
        private int itemId;

        // Hàm nhận dữ liệu từ ItemviewController truyền sang
        public void setInitData(Popup popup, long currentPrice,int idItem,ItemviewController main) {
            this.parentPopup = popup;
            this.currentPrice = currentPrice;
            this.itemviewController=main;
            this.itemId =idItem;

            // Thiết lập gợi ý nhập liệu ngay khi mở
            maxBidField.setPromptText("Phải lớn hơn " + (currentPrice));
            lbbalance.setText(DataSession.getInstance().getLoggedInUser().getBalance()+"");
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
            String incrementText = incrementField.getText().trim();

            if (maxBidText.isEmpty() || incrementText.isEmpty()) {
                errorLabel.setTextFill(Color.web("#E53E3E"));
                errorLabel.setText("⚠️ Vui lòng nhập đầy đủ thông tin!");
                errorLabel.setVisible(true);
                return;
            }

            try {
                long maxBid = Long.parseLong(maxBidText);
                long incre= Long.parseLong(incrementField.getText().trim());
                if (maxBid <= currentPrice) {
                    errorLabel.setTextFill(Color.web("#E53E3E"));
                    errorLabel.setText("⚠️ Giá tối đa phải lớn hơn giá hiện tại!");
                    errorLabel.setVisible(true);
                    return;
                }
                User loggedInUser = DataSession.getInstance().getLoggedInUser();
                if (loggedInUser == null) {
                    errorLabel.setText("⚠️ Bạn chưa đăng nhập!");
                    errorLabel.setVisible(true);
                    return;
                }

                maxBidField.setDisable(true);
                incrementField.setDisable(true);
                errorLabel.setTextFill(Color.BLUE);
                errorLabel.setText("Đang gửi yêu cầu lên hệ thống...");
                errorLabel.setVisible(true);

                AuctionManager.getInstance().setupAutoBidAsync(itemId, loggedInUser.getId(), maxBid, incre, loggedInUser.getUsername())
                        .thenAccept(response -> {
                            Platform.runLater(() -> {
                                maxBidField.setDisable(false);
                                incrementField.setDisable(false);

                                if (response != null && "SUCCESS".equals(response.getStatus())) {
                                    errorLabel.setTextFill(Color.web("#38A169"));
                                    errorLabel.setText("Done " + response.getData().toString());

                                    itemviewController.activateAutoBidStatus(maxBidText);

                                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                                    delay.setOnFinished(e -> {
                                        if (parentPopup != null) parentPopup.hide();
                                    });
                                    delay.play();
                                } else {
                                    errorLabel.setTextFill(Color.web("#E53E3E"));
                                    errorLabel.setText("Error " + (response != null ? response.getData().toString() : "Server từ chối!"));
                                }
                            });
                        }).exceptionally(ex -> {
                            Platform.runLater(() -> {
                                maxBidField.setDisable(false);
                                incrementField.setDisable(false);
                                errorLabel.setTextFill(Color.web("#E53E3E"));
                                errorLabel.setText("Lỗi kết nối mạng Server!");
                            });
                            return null;
                        });
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
