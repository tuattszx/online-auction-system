package auction.client.controllers;

import auction.client.session.DataSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class HeaderMenuController {

    @FXML private TextField txtsearch;
    @FXML private Label lbsell;
    @FXML private Label lbbalance;
    @FXML private AnchorPane bellContainer;
    @FXML private VBox notifPane;
    @FXML private VBox vboxNotifItems;

    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm

    // Hàm dùng để ẩn thanh tìm kiếm và thu hồi lại diện tích trống
    public void hideSearchBar() {
        if (searchBar != null) {
            searchBar.setVisible(false);
            searchBar.setManaged(false); // Dòng này cực kỳ quan trọng: nó giúp các thành phần khác tự động tràn vào lấp chỗ trống, không để lại một khoảng trắng vô duyên.
        }
    }
    // Viết thêm hàm này để các Controller khác gọi tới
    public void setBalance(String amount) {
        lbbalance.setText(amount);
    }

    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }

    @FXML
    public void OnMouseCart(MouseEvent event){
        ViewManager.switchScene(event,"cart_view.fxml", "rỏ hàng");

    }

    @FXML
    public void onSellerClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "seller_demo.fxml", "seller page");
    }

    @FXML
    public void GoToFavoriteView(MouseEvent event){
        ViewManager.switchScene(event,"favourite_view.fxml", " yêu thích");

    }

    @FXML
    void handleShowNotifications(MouseEvent event) {
        // Ẩn/hiển thị cái pop-up notifPane nhỏ ở chuông
        notifPane.setVisible(!notifPane.isVisible());
    }

    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (DataSession.getInstance().getLoggedInUser() == null) return;

        String view = DataSession.getInstance().getLoggedInUser().getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }

    @FXML
    public void onBellClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "notification_view.fxml", "thông báo");
    }

    @FXML
    private void onSearchAction() {
        // Implement sau
    }
}