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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.codec.language.bm.Lang;
import org.controlsfx.control.SearchableComboBox;
import org.mindrot.jbcrypt.BCrypt;

public class ProfileController  {
    @FXML private SearchableComboBox<String> countryPicker;
    @FXML private Label lbname,lbAlert;
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
    @FXML private TextField txtNameInput, txtEmailInput, txtPhoneInput, txtFirstNameInput, txtLastNameInput,txtDeliveryAddressInput, txtShippingPhoneInput, txtCardName, txtCardNumber, txtAmount;
    @FXML private PasswordField txtPasswordInput;
    @FXML private ComboBox<String> currencyPicker; // Khai báo thêm để quản lý Currency
    @FXML
    private HeaderMenuController headerMenuController;
    private List<Region> allIndicators;
    ClientNetwork network = ClientNetwork.getInstance();

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

        languagePicker.getItems().addAll("Tiếng Việt", "English"); // No unchecked call

        if (user!= null){
            lbname.setText(user.getDisplayName());
            lbusername.setText(user.getUsername());
            lbemail.setText(user.getEmail());
            lbAlert.setVisible(false);
            languagePicker.setValue(user.getLanguage());
            if (txtFirstNameInput != null) txtFirstNameInput.setText(user.getFirstName());
            if (txtLastNameInput != null) txtLastNameInput.setText(user.getLastName());
            if (txtShippingPhoneInput != null) txtShippingPhoneInput.setText(user.getShippingPhone());
            if (txtDeliveryAddressInput != null) txtDeliveryAddressInput.setText(user.getAddress());
            if (txtCardName != null) {
                txtCardName.setText(user.getCardHolderName());
            }
            if (txtCardNumber != null) {
                txtCardNumber.setText(user.getCardNumber());
            }


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
    private void btnSavePayment(ActionEvent event) {
        User user = DataSession.getInstance().getLoggedInUser();
        if (user == null) return;
        String finalCardName = txtCardName.getText() !=null ? txtCardName.getText().trim() : "";
        String finalCardNumber = txtCardNumber.getText() !=null ? txtCardNumber.getText().trim():"";
        String amountStr = txtAmount.getText() !=null ? txtAmount.getText().trim():"0.0";
        if (finalCardNumber.isEmpty() || finalCardName.isEmpty() || amountStr.isEmpty()) {
            lbAlert.setTextFill(Color.web("#E53E3E"));
            lbAlert.setText("Vui lòng nhập đầy đủ thông tin thẻ và số tiền cần nạp!");
            lbAlert.setVisible(true);
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountStr);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            lbAlert.setTextFill(Color.web("#E53E3E"));
            lbAlert.setText( "Số tiền nạp phải là số nguyên dương hợp lệ!");
            lbAlert.setVisible(true);
            return;
        }
        Map<String, Object> depositData = new HashMap<>();
        depositData.put("cardName", finalCardName);
        depositData.put("cardNumber", finalCardNumber);
        depositData.put("amount", amount);
        try {
            Message request = new Message("DEPOSIT_REQUEST", depositData);
            // Gửi request và đợi phản hồi đồng bộ từ Server
            Message response = ClientNetwork.getInstance().sendRequest(request);

            if (response != null && "SUCCESS".equals(response.getStatus())) {
                // Ép kiểu dữ liệu User mới nhận về từ Server để cập nhật lại Session
                User updatedUser = (User) response.getData();
                DataSession.getInstance().setLoggedInUser(updatedUser);

                ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Nạp tiền thành công! Số dư hiện tại của bạn: $" + updatedUser.getBalance());

                // Xóa sạch ô nhập tiền sau khi nạp thành công
                txtAmount.clear();
            } else {
                // Hiển thị nội dung lỗi phản hồi cụ thể từ Server (Hết tiền, sai thẻ...)
                System.out.println("--- KIỂU DỮ LIỆU THỰC TẾ: " + response.getData().getClass().getName());
                String errorMsg = (response != null) ? (String) response.getData() : "Server không phản hồi.";
                ViewManager.showAlert(Alert.AlertType.ERROR, "Giao dịch thất bại", errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
        }

    }
    @FXML
    private void btnSaveSetting(ActionEvent event) {
        User user = DataSession.getInstance().getLoggedInUser();
        if (user == null) return;
        String finalCardName = txtCardName.getText() !=null ? txtCardName.getText().trim() : "";
        String finalCardNumber = txtCardNumber.getText() !=null ? txtCardNumber.getText().trim():"";
        String amountStr = txtAmount.getText() !=null ? txtAmount.getText().trim():"0.0";

        // 1. Kiểm tra nếu đang mở ô sửa thì lấy giá trị mới từ ô sửa, ngược lại giữ nguyên giá trị cũ của nhãn Label
        String finalName = editNameBox.isVisible() ? txtNameInput.getText().trim() : lbname.getText();
        String finalEmail = editEmailBox.isVisible() ? txtEmailInput.getText().trim() : lbemail.getText();
        String finalPhone = editPhoneBox.isVisible() ? txtPhoneInput.getText().trim() : lbphonenumber.getText();
        String finalPassword = user.getPassword();

        if (editPasswordBox.isVisible()) {
            String inputPassword = txtPasswordInput.getText();

            // Nếu người dùng có gõ gì đó vào ô mật khẩu
            if (inputPassword != null && !inputPassword.isEmpty()) {

                try {
                    // SỬA TẠI ĐÂY: Dùng BCrypt.checkpw để kiểm tra xem mật khẩu nhập vào có KHÁC mật khẩu cũ không.
                    // Nếu checkpw trả về true nghĩa là nhập trùng mật khẩu cũ -> Ta không cần hiện popup bắt xác thực lại nữa.
                    boolean isSameAsOld = org.mindrot.jbcrypt.BCrypt.checkpw(inputPassword, user.getPassword());

                    if (!isSameAsOld) {
                        // Tạo một mảng lưu kết quả từ popup (dùng mảng vì biến trong lambda phải là hiệu dụng final)
                        final String[] verifiedPassword = {null};

                        // Gọi hàm mở Popup kiểm tra mật khẩu cũ và xác nhận mật khẩu mới
                        showChangePasswordConfirmPopup(user.getPassword(), inputPassword, verifiedPassword);

                        // Nếu người dùng hủy popup hoặc nhập sai mật khẩu cũ -> Dừng hàm Save luôn, không lưu gì cả
                        if (verifiedPassword[0] == null) {
                            return;
                        }

                        // Nếu xác thực thành công qua popup, lấy mật khẩu mới (đã được mã hóa BCrypt bên trong popup) chuẩn bị gửi lên server
                        user.setPassword(verifiedPassword[0]);
                    String a=user.getPassword();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Phòng trường hợp chuỗi trong user.getPassword() chưa kịp hash hoặc sai định dạng BCrypt
                    System.out.println("Lỗi kiểm tra định dạng BCrypt mật khẩu cũ.");
                }
            }
        }
        // 2. Dữ liệu từ tab Address (Lấy từ các Input mới)
        String finalFirstName = txtFirstNameInput.getText()!=null ? txtFirstNameInput.getText().trim() : "";
        String finalLastName = txtLastNameInput.getText() !=null ? txtLastNameInput.getText().trim():"";
        String finalCountry = countryPicker.getValue() != null ? countryPicker.getValue() : "Vietnam";
        String finalDeliveryAddress = txtDeliveryAddressInput.getText() !=null ? txtDeliveryAddressInput.getText() : "";
        String finalShippingPhone = txtShippingPhoneInput.getText() !=null ? txtShippingPhoneInput.getText() : "";
        String finalLanguage= languagePicker.getValue() != null ? languagePicker.getValue() : "Vietnam";
        // 2. Set dữ liệu vào model User
        user.setDisplayName(finalName);
        user.setEmail(finalEmail);
        user.setPhoneNumber(finalPhone);
        user.setFirstName(finalFirstName);
        user.setLastName(finalLastName);
        user.setCountry(finalCountry);
        user.setAddress(finalDeliveryAddress);
        user.setShippingPhone(finalShippingPhone);
        user.setCardHolderName(finalCardName);
        user.setCardNumber(finalCardNumber);
        user.setLanguage(finalLanguage);


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

                if (txtFirstNameInput != null) txtFirstNameInput.setText(user.getFirstName());
                if (txtLastNameInput != null) txtLastNameInput.setText(user.getLastName());
                if (txtShippingPhoneInput != null) txtShippingPhoneInput.setText(user.getShippingPhone());
                if (txtDeliveryAddressInput != null) txtDeliveryAddressInput.setText(user.getAddress());
                if (txtCardName != null) {
                    txtCardName.setText(user.getCardHolderName());
                }
                if (txtCardNumber != null) {
                    txtCardNumber.setText(user.getCardNumber());
                }

                // Đồng bộ cho ComboBox Country
                if (countryPicker != null && user.getCountry() != null) {
                    countryPicker.setValue(user.getCountry());
                }

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
                ViewManager.clearCache();
                ViewManager.switchScene(event, "profile-view.fxml", LanguageManager.getString("profile.title"));

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
    private void showChangePasswordConfirmPopup(String hashedOldPassword, String newPasswordInput, String[] resultContainer) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setPrefWidth(380);
        root.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(15, 23, 42, 0.1), 15, 0, 0, 4);"
        );

        // Header
        VBox header = new VBox(4);
        Label titleLabel = new Label("Xác thực đổi mật khẩu");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#0f172a"));
        Label subTitleLabel = new Label("Vui lòng nhập mật khẩu hiện tại để xác nhận thay đổi.");
        subTitleLabel.setFont(Font.font("System", 12));
        subTitleLabel.setTextFill(Color.web("#64748b"));
        header.getChildren().addAll(titleLabel, subTitleLabel);

        // Input ô nhập mật khẩu hiện tại
        VBox inputFieldBox = new VBox(6);
        Label lblOld = new Label("Mật khẩu hiện tại của bạn");
        lblOld.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        lblOld.setTextFill(Color.web("#475569"));

        PasswordField txtConfirmOld = new PasswordField();
        txtConfirmOld.setPromptText("Nhập mật khẩu hiện tại");
        txtConfirmOld.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;"
        );
        inputFieldBox.getChildren().addAll(lblOld, txtConfirmOld);

        // Label báo lỗi
        Label lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("System", 12));
        lblError.setVisible(false);

        // Footer Buttons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setPrefSize(70, 32);
        btnCancel.setOnAction(e -> popupStage.close());

        Button btnConfirm = new Button("Xác nhận");
        btnConfirm.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnConfirm.setPrefSize(90, 32);

        btnConfirm.setOnAction(e -> {
            String enteredOldPassword = txtConfirmOld.getText();

            if (enteredOldPassword == null || enteredOldPassword.isEmpty()) {
                lblError.setText("Vui lòng không để trống!");
                lblError.setVisible(true);
                return;
            }

            try {
                // THAY ĐỔI QUAN TRỌNG: Sử dụng BCrypt.checkpw để so sánh chuỗi thô nhập vào và chuỗi đã hash trong DB
                // Tham số 1: Mật khẩu thô (user gõ vào ô text)
                // Tham số 2: Mật khẩu đã mã hóa (lấy từ user.getPassword() truyền vào)
                if (BCrypt.checkpw(enteredOldPassword, hashedOldPassword)) {

                    // Khớp thành công -> Tiến hành mã hóa luôn mật khẩu mới trước khi gửi lên Server (Rất khuyến khích làm ở Client)
                    // Nếu bạn muốn Server tự mã hóa thì chỉ cần gán: resultContainer[0] = newPasswordInput;
                    String hashedNewPassword = BCrypt.hashpw(newPasswordInput, BCrypt.gensalt());
                    resultContainer[0] = hashedNewPassword;

                    popupStage.close();
                } else {
                    lblError.setText("Mật khẩu hiện tại không chính xác!");
                    lblError.setVisible(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                lblError.setText("Lỗi hệ thống khi kiểm tra mật khẩu!");
                lblError.setVisible(true);
            }
        });

        footer.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(header, inputFieldBox, lblError, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);
        popupStage.showAndWait();
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
        if (!ViewManager.confirmAlert(LanguageManager.getString("alert.signout.title"), LanguageManager.getString("alert.signout.content"))) return;
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