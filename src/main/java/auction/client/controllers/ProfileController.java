package auction.client.controllers;
import auction.client.ClientNetwork;
import auction.client.services.LanguageManager;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.users.User;
import auction.server.DatabaseManager;
import auction.server.dao.UserDao;
import auction.server.dao.impl.UserDaoImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
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
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox viewNameBox, viewEmailBox, viewPasswordBox, viewPhoneBox;
    @FXML private HBox editNameBox, editEmailBox, editPasswordBox, editPhoneBox;
    @FXML private TextField txtNameInput, txtEmailInput, txtPhoneInput;
    @FXML private PasswordField txtPasswordInput;
    @FXML private ComboBox<String> currencyPicker; // Khai báo thêm để quản lý Currency
    @FXML
    private HeaderMenuController headerMenuController;
    private List<Region> allIndicators;
    ClientNetwork network = ClientNetwork.getInstance();

    public void initialize() {
        // hiện gạch xanh
        if (mainScrollPane != null) {
            mainScrollPane.addEventFilter(javafx.scene.input.ScrollEvent.ANY, event -> {
                event.consume(); // "Nuốt" sự kiện cuộn, khóa cứng khung nhìn
            });
        }
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
    // --- HÀM ẨN/HIỆN GIAO DIỆN KHI ẤN CHANGE/CANCEL ---

    @FXML void showEditName() { txtNameInput.setText(lbname.getText()); viewNameBox.setVisible(false); editNameBox.setVisible(true); }
    @FXML void hideEditName() { viewNameBox.setVisible(true); editNameBox.setVisible(false); }

    @FXML void showEditEmail() { txtEmailInput.setText(lbemail.getText()); viewEmailBox.setVisible(false); editEmailBox.setVisible(true); }
    @FXML void hideEditEmail() { viewEmailBox.setVisible(true); editEmailBox.setVisible(false); }

    @FXML void showEditPassword() { txtPasswordInput.setText(""); viewPasswordBox.setVisible(false); editPasswordBox.setVisible(true); }
    @FXML void hideEditPassword() { viewPasswordBox.setVisible(true); editPasswordBox.setVisible(false); }

    @FXML void showEditPhone() { txtPhoneInput.setText(lbphonenumber.getText()); viewPhoneBox.setVisible(false); editPhoneBox.setVisible(true); }
    @FXML void hideEditPhone() { viewPhoneBox.setVisible(true); editPhoneBox.setVisible(false); }

    // --- HÀM LUÔN ĐỒNG BỘ VÀ LƯU KHI ẤN NÚT SAVE CHÍNH ---
    @FXML
    private void btnSaveSetting(ActionEvent event) {
        User user = DataSession.getInstance().getLoggedInUser();
        if (user == null) return;

        // 1. Kiểm tra nếu đang mở ô sửa thì lấy giá trị mới từ ô sửa, ngược lại giữ nguyên giá trị cũ của nhãn Label
        String finalName = editNameBox.isVisible() ? txtNameInput.getText().trim() : lbname.getText();
        String finalEmail = editEmailBox.isVisible() ? txtEmailInput.getText().trim() : lbemail.getText();
        String finalPhone = editPhoneBox.isVisible() ? txtPhoneInput.getText().trim() : lbphonenumber.getText();
        String finalPassword = editPasswordBox.isVisible() ? txtPasswordInput.getText() : user.getPassword();

        // 2. Set dữ liệu vào model User
        user.setDisplayName(finalName);
        user.setEmail(finalEmail);
        user.setPhoneNumber(finalPhone);
        if (editPasswordBox.isVisible() && !finalPassword.isEmpty()) {
            user.setPassword(finalPassword);
        }


        // 5. Gọi hàm update database của bạn
        // 5. GỬI REQUEST VÀ ĐỢI KẾT QUẢ ĐỒNG BỘ TỪ SERVER QUA MẠNG
        try {
            // Đóng gói đối tượng user hiện tại vào Message
            Message updateRequest = new Message("UPDATE_PROFILE", user);

            // Gọi hàm sendRequest(), nó sẽ tự chặn luồng đợi Server ghi DB xong và phản hồi về
            Message responseFromServer = ClientNetwork.getInstance().sendRequest(updateRequest);

            // Kiểm tra kết quả phản hồi từ Server trả về
            if (responseFromServer != null && "UPDATE_PROFILE_SUCCESS".equals(responseFromServer.getCommand())) {

                // Lấy đối tượng User đã được Server cập nhật (nếu Server có đính kèm lại vào data)
                User updatedUser = (User) responseFromServer.getData();
                if (updatedUser != null) {
                    // CẬP NHẬT LẠI TRONG SESSION GỐC - Giúp tắt đi bật lại không bị mất dữ liệu
                    DataSession.getInstance().setLoggedInUser(updatedUser);
                } else {
                    // Nếu Server không trả về Object mới, lưu luôn object hiện tại vào Session
                    DataSession.getInstance().setLoggedInUser(user);
                }

                // 6. CẬP NHẬT LÊN CÁC LABEL GIAO DIỆN TĨNH
                lbname.setText(user.getDisplayName());
                lbemail.setText(user.getEmail());
                lbphonenumber.setText(user.getPhoneNumber());

                // Định dạng lại hiển thị mật khẩu bằng dấu hoa thị (*)
                if (user.getPassword() != null) {
                    char[] repeat = new char[user.getPassword().length()];
                    java.util.Arrays.fill(repeat, '*');
                    lbname21.setText(new String(repeat));
                }

                // Xử lý Ngôn ngữ Local (nếu có)
                if (languagePicker != null && languagePicker.getValue() != null) {
                    String selectedLanguage = languagePicker.getValue();
                    if ("Tiếng Việt".equals(selectedLanguage)) LanguageManager.setLocale("vi");
                    else if ("English".equals(selectedLanguage)) LanguageManager.setLocale("en");
                }

                // 7. ẨN CÁC Ô NHẬP, QUAY LẠI GIAO DIỆN CHỈ XEM BAN ĐẦU
                hideEditName();
                hideEditEmail();
                hideEditPassword();
                hideEditPhone();

                // 8. THÔNG BÁO THÀNH CÔNG RỰC RỠ
                ViewManager.showAlert(
                        Alert.AlertType.INFORMATION,
                        LanguageManager.getString("profile.label.notification"),
                        LanguageManager.getString("profile.label.saveSuccess")
                );

            } else {
                // Trường hợp Server phản hồi lệnh UPDATE_PROFILE_FAILED hoặc bị TIMEOUT
                ViewManager.showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Server báo lỗi: Không thể lưu thông tin vào cơ sở dữ liệu."
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            ViewManager.showAlert(
                    Alert.AlertType.ERROR,
                    "Network Error",
                    "Có lỗi xảy ra trong quá trình truyền tải: " + e.getMessage()
            );
        }
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