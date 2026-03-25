online-auction-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── auction/                <-- Package gốc của bạn
│   │   │       ├── common/             <-- CÁC LỚP DÙNG CHUNG (Models)
│   │   │       │   ├── User.java       (Chứa: username, password, role...)
│   │   │       │   ├── Item.java       (Chứa: tên món hàng, giá khởi điểm, time...)
│   │   │       │   └── Message.java    (Dùng để đóng gói dữ liệu gửi qua Socket)
│   │   │       │
│   │   │       ├── server/             <-- LOGIC PHÍA SERVER (Chạy độc lập)
│   │   │       │   ├── AuctionServer.java     (File có hàm main để bật Server)
│   │   │       │   ├── ClientHandler.java    (Quản lý từng người dùng kết nối vào)
│   │   │       │   └── DatabaseManager.java  (Kết nối MySQL để lưu User/Item)
│   │   │       │
│   │   │       └── client/             <-- LOGIC PHÍA NGƯỜI DÙNG (Giao diện)
│   │   │           ├── Launcher.java          (Chạy app từ đây)
│   │   │           ├── ClientNetwork.java     (Xử lý gửi/nhận data với Server)
│   │   │           └── controllers/           (Các file Controller bạn đã có)
│   │   │               ├── LoginController.java
│   │   │               ├── RegisterController.java
│   │   │               └── MainAuctionController.java
│   │   │
│   │   └── resources/                  <-- NƠI ĐỂ FILE GIAO DIỆN (.fxml)
│   │       └── auction/
│   │           ├── views/              (Chứa login-view.fxml, register-view.fxml)
│   │           ├── css/                (Chứa style.css)
│   │           └── images/             (Ảnh minh họa món hàng)
│   │
└── pom.xml                             (Khai báo JavaFX, Jackson/Gson, JDBC)