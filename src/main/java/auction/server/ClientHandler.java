package auction.server;

import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import auction.server.dao.BidDao;
import auction.server.dao.CategoryDao;
import auction.server.dao.ItemDao;
import auction.server.dao.UserDao;
import auction.server.dao.impl.BidDaoImpl;
import auction.server.dao.impl.CategoryDaoImpl;
import auction.server.dao.impl.ItemDaoImpl;
import auction.server.dao.impl.UserDaoImpl;
import auction.server.utils.ImageService;

import java.io.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private final UserDao userDao=new UserDaoImpl();
    private final ItemDao itemDao=new ItemDaoImpl();
    private final CategoryDao categoryDao=new CategoryDaoImpl();
    private final BidDao bidDao=new BidDaoImpl();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Không dùng try-with-resources cho Socket ở đây để tránh tự động đóng
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) { // Vòng lặp giữ kết nối
                Object obj = in.readObject();
                if (obj instanceof Message msg) {
                    String command = msg.getCommand();
                    System.out.println("Server nhận lệnh: " + command);

                    if (command.equals("SIGNOUT")) {
                        handleSignout(msg, out);
                        break; // Thoát vòng lặp để đóng socket
                    }

                    switch (command) {
                        case "LOGIN":
                            handleLogin(msg, out);
                            break;
                        case "REGISTER":
                            handleRegister(msg, out);
                            break;
                        case "ADD_ITEM":
                            handleAddItem(msg,out);
                            break;
                        case "GET_ALL_ITEMS":
                            handleGetAllItems(msg, out);
                            break;
                        case "GET_ITEM_BY_ID":
                            handleGetItemById(msg,out);
                            break;
                        case "PLACE_BID":
                            handlePlaceBid(msg,out);
                            break;
                        case "GET_BID_BY_ITEM_ID":
                            handleGetBidByItemId(msg, out);
                            break;
                        case "GET_PRICE_CHART":
                            handleGetPriceChart(msg, out);
                            break;
                        case "GET_ITEM_IMAGES":
                            handleGetItemImages(msg, out);
                            break;
                        // Thêm các case khác như BID, VIEW_PRODUCT...
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Client ngắt kết nối đột ngột: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }

    // --- CÁC HÀM XỬ LÝ RIÊNG BIỆT ---

    private void handleLogin(Message msg, ObjectOutputStream out) throws IOException {
        // Lấy thông tin đăng nhập từ dữ liệu trong Message
        Account accReq = (Account) msg.getData();

        // Gọi UserDao (nằm trong package auction.server.dao của bạn)
        User user = userDao.CheckLogin(accReq.getUsername(), accReq.getPassword());

        if (user != null) {
            msg.setStatus("SUCCESS");
            msg.setData(user);
        } else {
            msg.setStatus("FAILED");
        }

        out.writeObject(msg);
        out.flush();
    }

    private void handleRegister(Message msg, ObjectOutputStream out) throws IOException {
        // Ép kiểu về User vì Client sẽ gửi đối tượng User sang
        User newUser = (User) msg.getData();

        // Kiểm tra xem username hoặc email đã tồn tại chưa
        if (userDao.isUsernameExists(newUser.getUsername()) || userDao.isEmailExists(newUser.getEmail())) {
            msg.setStatus("FAILED");
        } else {
            // Gọi hàm registerUser trong UserDao của bạn
            boolean isSuccess = userDao.add(newUser);
            if (isSuccess) {
                msg.setStatus("SUCCESS");
            } else {
                msg.setStatus("FAILED");
            }
        }

        out.writeObject(msg);
        out.flush();
    }
    private void handleSignout(Message msg, ObjectOutputStream out) throws IOException {
        msg.setStatus("SUCCESS");
        out.writeObject(msg);
        out.flush();
    }

    private void handleAddItem(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // 1. Giải nén gói dữ liệu từ Client
            Object[] payload = (Object[]) msg.getData();
            Item item = (Item) payload[0];
            List<byte[]> imagesBytes = (List<byte[]>) payload[1];
            List<String> fileNames = (List<String>) payload[2];
            String categoryName = (String) payload[3];

            // 2. Xử lý lưu các ảnh vật lý vào ổ cứng Server
            for (int i = 0; i < imagesBytes.size(); i++) {
                // Lưu ảnh và nhận về đường dẫn tương đối (vd: /items/abc.jpg)
                String dbPath = ImageService.saveImage(imagesBytes.get(i), fileNames.get(i));

                // Tạo đối tượng ItemImage tương ứng
                ItemImage itemImg = new ItemImage();
                itemImg.setUrlImage(dbPath);
                itemImg.setDefault(i == 0); // Ảnh đầu tiên làm ảnh mặc định

                item.addImages(itemImg); // Thêm vào list trong Item
            }

            // 3. Tìm Category object từ Database bằng tên
            Category category = categoryDao.getCategoryByName(categoryName);
            if (category != null) {
                item.addCategories(category);
            }

            // 4. Gọi ItemDao để lưu trọn bộ Item (bao gồm cả ảnh và category) vào DB
            // Hàm addItem của bạn đã có Transaction (Rollback) nên cực kỳ an toàn
            boolean isSuccess = itemDao.add(item);

            if (isSuccess) {
                msg.setStatus("SUCCESS");
                System.out.println("Đã thêm sản phẩm mới: " + item.getName());
            } else {
                msg.setStatus("FAILED");
            }

        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("SERVER_ERROR");
        }

        // 5. Trả phản hồi về cho Client
        out.writeObject(msg);
        out.flush();
    }

    private void handleGetAllItems(Message msg, ObjectOutputStream out) throws IOException {
        try{
            List<Item> items = itemDao.getAll(); // Gọi ItemDao lấy dữ liệu

            for (Item item : items) {
                if (item.getImages() != null && !item.getImages().isEmpty()) {
                    ItemImage firstImg = item.getImages().get(0);
                    firstImg.setImageData(ImageService.readImageBytes(firstImg.getUrlImage()));

                    for (int i = 1; i < item.getImages().size(); i++) {
                        item.getImages().get(i).setImageData(null);
                    }
                }
            }

            msg.setStatus("SUCCESS");
            msg.setData(items);
        }
        catch (Exception e){
            msg.setStatus("ERROR");
            e.printStackTrace();
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleGetItemById(Message msg,ObjectOutputStream out) throws IOException {
        int id = (int) msg.getData();
        Item item=itemDao.getById(id);
        if (item != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String oldStatus = item.getStatus();

            if (now.isBefore(item.getStartTime())) {
                item.setStatus("PENDING");
            } else if (now.isAfter(item.getEndTime())) {
                item.setStatus("CLOSED");
            } else {
                item.setStatus("OPEN");
            }

            if (!item.getStatus().equals(oldStatus)) {
                itemDao.updateStatus(item.getId(), item.getStatus());
            }

            msg.setStatus("SUCCESS");
            msg.setData(item);
        }
        else {
            msg.setStatus("FAILED");
        }
        out.writeObject(msg);
        out.flush();
    }

    private void handlePlaceBid(Message msg, ObjectOutputStream out) throws IOException{
        try{
            Bid bidRequest = (Bid) msg.getData();

            Item currentItem= itemDao.getById(bidRequest.getIdItem());

            if (currentItem==null){
                msg.setStatus("FAILED");
                msg.setData("Sản phẩm không tồn tại!");
            }
            /*else if (!"OPEN".equals(currentItem.getStatus())) {
                msg.setStatus("FAILED");
                msg.setData("Phiên đấu giá đang đóng, không thể đặt giá!");
                out.writeObject(msg);
                out.flush();
                return;
            }*/
            else if (bidRequest.getBidAmount() <= currentItem.getCurrentPrice()) {
                msg.setStatus("FAILED");
                msg.setData("Giá đã bị đẩy lên € " + currentItem.getCurrentPrice() + ". Vui lòng trả cao hơn!");
            }
            else{
                boolean isUpdated= itemDao.placeBid(bidRequest.getIdItem(),bidRequest.getBidAmount(),bidRequest.getIdUser());
                if (isUpdated) {
                    // 4. Nếu cập nhật Item thành công, tiến hành lưu lịch sử vào bảng BIDS
                    boolean isHistorySaved = bidDao.add(bidRequest);

                    if (isHistorySaved) {
                        msg.setStatus("SUCCESS");
                        msg.setData("Da dat thanh cong: " + bidRequest.getBidAmount());
                    } else {
                        msg.setStatus("FAILED");
                        msg.setData("Lỗi hệ thống khi lưu lịch sử đấu giá!");
                    }
                }
                else {
                    msg.setStatus("FAILED");
                    msg.setData("Không thể đặt giá. Có thể giá đã thay đổi!");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        }
        out.writeObject(msg);
        out.flush();
    }

    private void handleGetBidByItemId(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int itemId = (int) msg.getData(); // Extract item ID from the message
            List<Bid> bidHistory = bidDao.getBidsByItemId(itemId); // Fetch bid history

            if (bidHistory != null && !bidHistory.isEmpty()) {
                msg.setStatus("SUCCESS");
                msg.setData(bidHistory);
            } else {
                msg.setStatus("FAILED");
                msg.setData("No bid history found for the specified item.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Server error: " + e.getMessage());
        }

        out.writeObject(msg);
        out.flush();
    }

    private void handleGetPriceChart(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int itemId = (int) msg.getData(); // Extract item ID from the message
            List<Bid> bidHistory = bidDao.getBidsByItemId(itemId); // Fetch bid history

            if (bidHistory != null && !bidHistory.isEmpty()) {
                // Transform bid history into a format suitable for the chart
                List<Object[]> priceChartData = bidHistory.stream()
                        .sorted((b1, b2) -> b1.getBidTime().compareTo(b2.getBidTime())) // Sắp xếp tăng dần
                        .map(bid -> {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                            return new Object[]{ bid.getBidTime().format(formatter), bid.getBidAmount() };
                        })
                        .toList();

                msg.setStatus("SUCCESS");
                msg.setData(priceChartData);
            } else {
                msg.setStatus("FAILED");
                msg.setData("No price chart data found for the specified item.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Server error: " + e.getMessage());
        }

        out.writeObject(msg);
        out.flush();
    }

    private void handleGetItemImages(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int itemId = (int) msg.getData();
            // Lấy danh sách ảnh từ DB (chỉ cần URL/Path)
            Item item = itemDao.getById(itemId);
            List<ItemImage> images=item.getImages();

            for (ItemImage img : images) {
                // Đọc dữ liệu vật lý từ ổ cứng và nén (nếu chưa nén lúc upload)
                byte[] data = ImageService.readImageBytes(img.getUrlImage());
                img.setImageData(data);
            }

            msg.setStatus("SUCCESS");
            msg.setData(images);
        } catch (Exception e) {
            msg.setStatus("ERROR");
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }
}