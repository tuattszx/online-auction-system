package auction.client.controllers;
import auction.common.model.users.User;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.util.Arrays;
import java.util.List;

public class ProfileController  {
    @FXML
    public Label lbname;
    @FXML
    public Label lbusername;
    @FXML
    public Label lbemail;
    @FXML
    public Label lbname21;
    @FXML
    public Label lbphonenumber;
    @FXML
    private HBox btnAccount;

    @FXML
    private HBox btnAddresses;

    @FXML
    private HBox btnPayment;
    @FXML
    private HBox btnEmail;
    @FXML
    private HBox btnVeri;
    @FXML
    private Region accountIndicator, addressesIndicator, paymentIndicator,emailIndicator,VeriIndicator;
    private List<Region> allIndicators;

    public void initialize() {
        // Gom tất cả gạch xanh vào một List
        allIndicators = Arrays.asList(accountIndicator, addressesIndicator, paymentIndicator,VeriIndicator,emailIndicator);
        // Mặc định hiện cái đầu tiên (Account)
        showIndicator(accountIndicator);

        User user = UserSession.loggedInUser;

        if (user!= null){
            lbname.setText(user.getDisplayName());
            lbusername.setText(user.getUsername());
            lbemail.setText(user.getEmail());

            char[] repeat = new char[user.getPassword().length()];
            java.util.Arrays.fill(repeat, '*');
            String result = new String(repeat);
            lbname21.setText(result);

            lbphonenumber.setText(user.getPhoneNumber());

        }
    }
    private void showIndicator(Region activeIndicator) {
        // Ẩn tất cả gạch xanh đi
        for (Region r : allIndicators) {
            if (r != null) r.setVisible(false);
        }
        // Chỉ hiện cái gạch của mục vừa bấm
        activeIndicator.setVisible(true);
    }
    @FXML
    private void handleSidebarClick(MouseEvent event) {
        // 1. Tắt hết tất cả gạch xanh đang hiện
        accountIndicator.setVisible(false);
        addressesIndicator.setVisible(false);
        paymentIndicator.setVisible(false);
        emailIndicator.setVisible(false);
        VeriIndicator.setVisible(false);

        // 2. Kiểm tra xem HBox nào vừa được bấm dựa trên fx:id
        HBox clickedBox = (HBox) event.getSource();
        String id = clickedBox.getId();

        // 3. Bật gạch xanh tương ứng lên
        if (id.equals("btnAccount")) {
            accountIndicator.setVisible(true);
        } else if (id.equals("btnAddresses")) {
            addressesIndicator.setVisible(true);
        } else if (id.equals("btnPayment")) {
            paymentIndicator.setVisible(true);
        }else if (id.equals("btnEmail")) {
            emailIndicator.setVisible(true);}
        else if (id.equals("btnVeri")) {
            VeriIndicator.setVisible(true);}
    }
    @FXML
    public void onBackToMainClick(javafx.event.ActionEvent event) {
        ViewManager.switchScene(event, "main-view.fxml", "Hệ thống Đấu giá - Trang chủ");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void onLogoutClick(javafx.event.ActionEvent event) {
        UserSession.loggedInUser = null;
        ViewManager.clearCache();
        ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

}
