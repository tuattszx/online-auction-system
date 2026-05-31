package auction.common.model.items; // Sửa lại tên package cho đúng với cấu trúc dự án của bạn

import java.time.LocalDateTime;

public class Transaction {
    private int id;          // Mã giao dịch (Có thể dùng chuỗi số tự tăng hoặc UUID)
    private int userId;      // ID người gửi (Người thực hiện giao dịch)
    private String userName;
    private int receiverId;  // ID người
    private String receiverName;
    private int itemId;
    private String itemName;    // Tên mặt hàng giao dịch (Ví dụ: "Điện thoại iPhone 13")
    private long amount;        // Số tiền giao dịch (Kiểu long tương đương với Long trong TableColumn)
    private LocalDateTime time;        // Thời gian giao dịch (Ví dụ: "2026-06-01 12:00:00")

    // 1. Hàm khởi tạo không tham số (Constructor mặc định - Cần thiết cho việc ép kiểu/ Jackson bốc dữ liệu)
    public Transaction() {
    }

    // 2. Hàm khởi tạo đầy đủ tham số để nạp dữ liệu nhanh
    public Transaction(int id, int userId,String userName, int receiverId,String receiverName,int itemId, String itemName, long amount, LocalDateTime time) {
        this.id=id;
        this.userId=userId;
        this.userName=userName;
        this.receiverId=receiverId;
        this.receiverName=receiverName;
        this.itemId=itemId;
        this.itemName = itemName;
        this.amount = amount;
        this.time = time;
    }

    // 3. Các hàm Getter và Setter (Bắt buộc phải có đúng tên để PropertyValueFactory ánh xạ lên bảng)
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {return userName;}
    public void setUserName(String userName){this.userName=userName;}

    public int getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {return  receiverName;}
    public void setReceiverName(String receiverName){this.receiverName=receiverName;}

    public int getItemId() {return itemId;}
    public void setItemId(int itemId){this.itemId=itemId;}

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

    public LocalDateTime getTime() {
        return time;
    }
    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}