package auction.client.controllers;
import auction.client.ClientNetwork;
import auction.client.services.LanguageManager;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.users.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javafx.scene.layout.VBox;
import org.controlsfx.control.SearchableComboBox;

public class ProfileController  {
    @FXML private SearchableComboBox<String> countryPicker;
    @FXML private Label lbname;
    @FXML private Label lbusername;
    @FXML private Label lbemail;
    @FXML private Label lbname21;
    @FXML private Label lbphonenumber;
    @FXML private HBox btnAccount;
    @FXML private HBox btnAddresses;
    @FXML private HBox btnPayment;
    @FXML private HBox btnEmail;
    @FXML private HBox btnVeri;
    @FXML private HBox btnMyAuctions;
    @FXML private Region accountIndicator, addressesIndicator, paymentIndicator,emailIndicator,VeriIndicator;
    @FXML private VBox paneAccount;
    @FXML private VBox paneAddresses;
    @FXML private VBox panePayment;
    @FXML private VBox paneEmails;
    @FXML private VBox paneVerification;
    @FXML private HBox hboxsignout;
    @FXML private ComboBox<String> languagePicker; // Parameterized ComboBox
    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm
    @FXML
    private HeaderMenuController headerMenuController;
    private List<Region> allIndicators;
    ClientNetwork network = ClientNetwork.getInstance();
    private void hideAllPanes(VBox targetPane) {
        VBox[] allPanes = {paneAccount, paneAddresses, panePayment, paneEmails, paneVerification};
        for (VBox pane : allPanes) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }
        if (targetPane != null) {
            targetPane.setVisible(true);
            targetPane.setManaged(true);
        }
    }
    public void initialize() {
        // hiện gạch xanh
        allIndicators = Arrays.asList(accountIndicator, addressesIndicator, paymentIndicator,emailIndicator,VeriIndicator);
        updateSidebarUI(btnAccount, accountIndicator);
        hideAllPanes(paneAccount); //
        User user = DataSession.getInstance().getLoggedInUser();
        // chọn ngôn ngữ trong adress
        ObservableList<String> countries = FXCollections.observableArrayList(
                "Vietnam", "United States", "Japan", "United Kingdom", "France", "Germany"
        );
        countryPicker.setItems(countries);

        countryPicker.setValue("Vietnam");

        languagePicker.getItems().addAll("Tiếng Việt", "English"); // No unchecked call

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
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
    }
    private void updateSidebarUI(HBox selectedBtn, Region selectedIndicator) {
        // 1. Tạo danh sách chứa tất cả các HBox nút bấm của bạn
        List<HBox> allButtons = Arrays.asList(btnAccount, btnAddresses, btnPayment, btnEmail, btnVeri);

        // 2. Duyệt qua từng cặp Nút và Gạch để cập nhật Style đồng loạt bằng vòng lặp for
        for (int i = 0; i < allButtons.size(); i++) {
            HBox btn = allButtons.get(i);
            Region indicator = allIndicators.get(i);

            if (btn == null) continue;

            // Lấy chữ Label nằm bên trong HBox hiện tại để chỉnh màu chữ
            Label lbl = (Label) btn.getChildren().stream()
                    .filter(node -> node instanceof Label)
                    .findFirst().orElse(null);

            if (btn == selectedBtn) {
                // Nút ĐƯỢC BẤM: Nền sáng nhẹ, chữ trắng đậm nổi bật
                btn.setStyle("-fx-background-color:  #e8f0fe; -fx-background-radius: 8;");
                if (lbl != null) lbl.setStyle("-fx-text-fill:  #1a73e8; -fx-font-weight: bold;");
                if (indicator != null) indicator.setVisible(true);
            } else {
                // Các nút CÒN LẠI: Nền trong suốt, chữ xám mờ tinh tế nhường spotlight
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
                if (lbl != null) lbl.setStyle("-fx-text-fill:  #1a73e8; -fx-font-weight: normal;");
                if (indicator != null) indicator.setVisible(false);
            }
        }
    }

    @FXML
    private void handleSidebarClick(MouseEvent event) { //
        // 1. Kiểm tra xem HBox nào vừa được bấm dựa trên event
        HBox clickedBox = (HBox) event.getSource(); //
        String id = clickedBox.getId(); //

        // 2. Gọi hàm đồng bộ giao diện cho các HBox nút bấm ở Sidebar trái
        if (id.equals("btnAccount")) { //
            updateSidebarUI(btnAccount, accountIndicator);
            hideAllPanes(paneAccount); // Ẩn các Pane khác, hiện paneAccount bên phải
        } else if (id.equals("btnAddresses")) { //
            updateSidebarUI(btnAddresses, addressesIndicator);
            hideAllPanes(paneAddresses); // Ẩn các Pane khác, hiện paneAddresses bên phải
        } else if (id.equals("btnPayment")) { //
            updateSidebarUI(btnPayment, paymentIndicator);
            hideAllPanes(panePayment);
        } else if (id.equals("btnEmail")) {
            updateSidebarUI(btnEmail, emailIndicator);
            hideAllPanes(paneEmails); //
        } else if (id.equals("btnVeri")) {
            updateSidebarUI(btnVeri, VeriIndicator);
            hideAllPanes(paneVerification); //
        }
    }
    @FXML
    public void btnSaveSetting(ActionEvent event){
        ViewManager.showAlert(Alert.AlertType.INFORMATION,
            LanguageManager.getString("profile.label.notification"),
            LanguageManager.getString("profile.label.saveSuccess"));
        String selectedLanguage = languagePicker.getValue();
        if (selectedLanguage != null) {
            switch (selectedLanguage) {
                case "Tiếng Việt":
                    LanguageManager.setLocale("vi");
                    break;
                case "English":
                    LanguageManager.setLocale("en");
                    break;
                default:
                    break;
            }
        }


        ViewManager.clearCache();
        ViewManager.switchScene(event, "profile-view.fxml", LanguageManager.getString("profile.title"));
    }
    @FXML
    public void onSellerClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "seller-view.fxml", "seller page");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void onSignOutClick(MouseEvent event) {
        if (!ViewManager.confirmAlert("Thông báo", "Bạn có chắc chắn muốn đăng xuất không?")) return;
        Task<Message> logoutTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("SIGNOUT", null));
            }
        };

        logoutTask.setOnSucceeded(e -> {
            DataSession.getInstance().clear();
            ViewManager.clearCache();
            network.close(); // Đóng socket ở phía Client
            ViewManager.switchScene(event, "login-view.fxml", "Đăng nhập");
        });

        new Thread(logoutTask).start();
    }

}