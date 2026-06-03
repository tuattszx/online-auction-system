# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

## 1. Mô tả bài toán và phạm vi hệ thống
**Online Auction System** là một ứng dụng Desktop hoạt động theo mô hình Client-Server, cung cấp nền tảng cho phép người dùng tham gia đấu giá các sản phẩm trực tuyến theo thời gian thực.
- **Phạm vi hệ thống:** Ứng dụng hỗ trợ các phiên đấu giá với tính năng cập nhật giá realtime, cơ chế đặt giá tự động (Auto-Bidding), quản lý danh mục sản phẩm, và thông báo đến người dùng. Hệ thống phân chia rõ ràng các vai trò: Quản trị viên (Admin), Người dùng (Seller / Bidder).

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
- **Ngôn ngữ lập trình:** Java
- **Giao diện người dùng:** JavaFX (Sử dụng FXML & CSS)
- **Quản lý dự án & Build tool:** Maven
- **Lưu trữ & Đa phương tiện:** Tích hợp Cloudinary (quản lý hình ảnh sản phẩm)
- **Kiến trúc:** Client - Server (Giao tiếp qua Socket/Network stream)
- **Môi trường yêu cầu:**
  - **JDK:** Phiên bản 17 trở lên.
  - **Hệ điều hành:** Đa nền tảng (Windows, macOS, Linux).
  - Có kết nối Internet để tải các thư viện Maven và đồng bộ dữ liệu hình ảnh (Cloudinary).

## 3. Cấu trúc thư mục và các module chính
Dự án được tổ chức theo cấu trúc tiêu chuẩn của Maven:

```text
src/main/
├── java/auction/
│   ├── client/       # Module phía Client (Giao diện người dùng)
│   │   ├── controllers/  # Các bộ điều khiển giao diện (Login, Main, Admin, AutoBid, v.v.)
│   │   ├── services/     # Xử lý logic phía client (AuctionManager, NotificationManager...)
│   │   └── utils/        # Công cụ hỗ trợ client (ImageService, ToastManager...)
│   ├── common/       # Module dùng chung giữa Client và Server
│   │   ├── message/      # Định dạng gói tin giao tiếp (BidUpdateNotification, Message...)
│   │   └── model/        # Các thực thể dữ liệu (User, Item, Bid, Category...)
│   └── server/       # Module phía Server (Xử lý trung tâm)
│       ├── dao/          # Tương tác với cơ sở dữ liệu (UserDao, ItemDao, BidDao...)
│       └── utils/        # Xử lý luồng dữ liệu, thông báo và quản lý Client đang kết nối
└── resources/auction/
    ├── css/          # Các tệp định dạng giao diện
    ├── img/          # Tài nguyên hình ảnh, logo
    ├── view/         # Các tệp giao diện FXML
    └── messages_*.properties # Hỗ trợ đa ngôn ngữ (Tiếng Anh, Tiếng Việt)
```

## 4. Hướng dẫn chạy chương trình
> **Lưu ý:** Máy chủ (Server) của hệ thống hiện đã được deploy và chạy tự động từ xa. Người dùng chỉ cần khởi chạy ứng dụng Client để kết nối và trải nghiệm hệ thống.

Dự án sử dụng Maven Wrapper (`mvnw`), do đó bạn không cần phải cài sẵn Maven trên máy tính, chỉ cần đảm bảo máy đã cài đặt **JDK 17+**.

### Bước 1: Biên dịch dự án
Mở terminal/command prompt tại thư mục gốc của dự án (nơi chứa file `pom.xml`) và chạy lệnh:

- **Trên Windows:**
```cmd
.\mvnw.cmd clean compile
```

- **Trên macOS / Linux:**
```bash
chmod +x mvnw
./mvnw clean compile
```

### Bước 2: Chạy Client (Khởi chạy ứng dụng)
Mở terminal và chạy lệnh sau (bạn có thể mở nhiều cửa sổ terminal cùng lúc để giả lập nhiều người dùng tương tác với nhau):

- **Trên Windows:**
```cmd
.\mvnw.cmd exec:java -D"exec.mainClass"="auction.client.Launcher"
```

- **Trên macOS / Linux:**
```bash
./mvnw exec:java -Dexec.mainClass="auction.client.Launcher"
```

## 5. Danh sách chức năng đã hoàn thành
- **Quản lý tài khoản:**
  - Đăng nhập, Đăng ký tài khoản mới, Quên mật khẩu.
  - Chỉnh sửa hồ sơ cá nhân (Profile).
  - Phân quyền Admin với các chức năng quản trị chuyên biệt.
- **Quản lý phiên đấu giá:**
  - Đăng tải sản phẩm mới.
  - Quản lý danh mục sản phẩm.
  - Tìm kiếm và lọc sản phẩm theo danh mục.
- **Tính năng đấu giá (Bidding):**
  - Xem thông tin chi tiết sản phẩm và lịch sử đặt giá.
  - Cập nhật giá thầu theo thời gian thực (Real-time).
  - **Tự động đặt giá (Auto-Bidding):** Tùy chỉnh cấu hình mức giá tối đa để hệ thống tự động cạnh tranh.
- **Tương tác người dùng:**
  - Nhận thông báo (Notification) ngay lập tức khi: có người trả giá cao hơn, phiên đấu giá kết thúc, đăng sản phẩm thành công, v.v.
- **Khác:**
  - Hỗ trợ đa ngôn ngữ (Tiếng Anh, Tiếng Việt)
  - Thêm phần yêu thích để theo dõi sản phẩm.

## 6. Link báo cáo và video
- **Video demo:**
  - https://drive.google.com/file/d/1EjPy2qiqYHpj6KI38e4qo5WZOZ0XiSaB/view
- **Báo cáo chi tiết:**
  - https://drive.google.com/drive/folders/1ISHC1Q30fGDaeuNsMRxvfkkCtVh6vDKD
