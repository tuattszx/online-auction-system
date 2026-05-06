package auction.client.controllers;
import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.users.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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
        allIndicators = Arrays.asList(accountIndicator, addressesIndicator, paymentIndicator,VeriIndicator,emailIndicator);
        showIndicator(accountIndicator);
        hideAllPanes(paneAccount);
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
            hideAllPanes(paneAccount);
        } else if (id.equals("btnAddresses")) {
            addressesIndicator.setVisible(true);
            hideAllPanes(paneAddresses);
        } else if (id.equals("btnPayment")) {
            paymentIndicator.setVisible(true);
            hideAllPanes(panePayment);
        } else if (id.equals("btnEmail")) {
            emailIndicator.setVisible(true);
            hideAllPanes(paneEmails);
        } else if (id.equals("btnVeri")) {
            VeriIndicator.setVisible(true);
            hideAllPanes(paneVerification);
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
        ViewManager.switchScene(event, "seller_demo.fxml", "seller page");
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