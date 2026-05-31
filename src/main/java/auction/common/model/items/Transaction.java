package auction.common.model.items; // Sửa lại tên package cho đúng với cấu trúc dự án của bạn

public class Transaction {
    private String id;          // Mã giao dịch (Có thể dùng chuỗi số tự tăng hoặc UUID)
    private String userId;      // ID người gửi (Người thực hiện giao dịch)
    private String receiverId;  // ID người nhận
    private String itemName;    // Tên mặt hàng giao dịch (Ví dụ: "Điện thoại iPhone 13")
    private long amount;        // Số tiền giao dịch (Kiểu long tương đương với Long trong TableColumn)
    private String time;        // Thời gian giao dịch (Ví dụ: "2026-06-01 12:00:00")

    // 1. Hàm khởi tạo không tham số (Constructor mặc định - Cần thiết cho việc ép kiểu/ Jackson bốc dữ liệu)
    public Transaction() {
    }

    // 2. Hàm khởi tạo đầy đủ tham số để nạp dữ liệu nhanh
    public Transaction(String id, String userId, String receiverId, String itemName, long amount, String time) {
        this.id = id;
        this.userId = userId;
        this.receiverId = receiverId;
        this.itemName = itemName;
        this.amount = amount;
        this.time = time;
    }

    // 3. Các hàm Getter và Setter (Bắt buộc phải có đúng tên để PropertyValueFactory ánh xạ lên bảng)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}