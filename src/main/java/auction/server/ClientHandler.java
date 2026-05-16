package auction.server;

import auction.common.message.BidUpdateNotification;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.AuctionItem;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private Socket socket;
    private final UserDao userDao = new UserDaoImpl();
    private final ItemDao itemDao = new ItemDaoImpl();
    private final CategoryDao categoryDao = new CategoryDaoImpl();
    private final BidDao bidDao = new BidDaoImpl();
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Không dùng try-with-resources cho Socket ở đây để tránh tự động đóng
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            ClientManager.addClient(this);
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
                            handleAddItem(msg, out);
                            break;
                        case "GET_ALL_ITEMS":
                            handleGetAllItems(msg, out);
                            break;
                        case "GET_ITEM_BY_ID":
                            handleGetItemById(msg, out);
                            break;
                        case "PLACE_BID":
                            handlePlaceBid(msg, out);
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
                        case "GET_MY_AUCTIONS":
                            handleGetMyAuctions(msg, out);
                            break;
                        case "SEND_BID_TO_USER":
                            handleSendMessageBid(msg, out);
                            break;
                        case "GET_MESSAGE":
                            handleGetMessage(msg, out);
                            break;
                        // Thêm các case khác như BID, VIEW_PRODUCT...
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Client ngắt kết nối đột ngột: " + e.getMessage());
        } finally {
            ClientManager.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public void sendObject(Object obj) {
        try {
            synchronized (out) {
                out.writeObject(obj);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("Lỗi gửi broadcast: " + e.getMessage());
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
            List<String> imageUrls = (List<String>) payload[1];
            String categoryName = (String) payload[2];

            // 2. Xử lý lưu các link ảnh vào đối tượng Item
            for (int i = 0; i < imageUrls.size(); i++) {
                // Link URL trực tiếp từ Cloudinary
                String cloudPath = imageUrls.get(i);

                // Tạo đối tượng ItemImage tương ứng
                ItemImage itemImg = new ItemImage();
                itemImg.setUrlImage(cloudPath); // Lưu link Cloudinary vào DB

                // Ảnh đầu tiên (vị trí index 0) làm ảnh mặc định
                itemImg.setDefault(i == 0);

                // Thêm vào list trong Item để DAO xử lý lưu DB một thể
                item.addImages(itemImg);
                // LOG 1: Kiểm tra payload
                System.out.println("DEBUG: Nhan duoc " + imageUrls.size() + " anh");
                System.out.println("DEBUG: Category name nhan duoc: " + categoryName);
            }

            // 3. Tìm Category object từ Database bằng tên
            Category category = categoryDao.getCategoryByName(categoryName);
            if (category != null) {
                item.addCategories(category);
            }
            System.out.println("Dang xu ly category: " + categoryName);

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
        out.reset();
    }

    private void handleGetAllItems(Message msg, ObjectOutputStream out) throws IOException {
        try {
            List<Item> items = itemDao.getAll();

            // XÓA BỎ TOÀN BỘ LOGIC XỬ LÝ IMAGE SERVICE TRONG NÀY
            // Không cần readImageBytes, không cần set imageData(null)
            // Vì bản thân đối tượng Item đã có List<ItemImage> chứa URL bên trong rồi

            msg.setStatus("SUCCESS");
            msg.setData(items);
        } catch (Exception e) {
            msg.setStatus("ERROR");
            e.printStackTrace();
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleGetItemById(Message msg, ObjectOutputStream out) throws IOException {
        int id = (int) msg.getData();
        Item item = itemDao.getById(id);
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
        } else {
            msg.setStatus("FAILED");
        }
        out.writeObject(msg);
        out.flush();
    }

    private void handlePlaceBid(Message msg, ObjectOutputStream out) throws IOException {
        String status = "FAILED";
        Object responseData = "Lỗi không xác định";
        BidUpdateNotification notification = null;
        try {
            Bid bidRequest = (Bid) msg.getData();

            Item currentItem = itemDao.getById(bidRequest.getIdItem());

            if (currentItem == null) {
                responseData = "Sản phẩm không tồn tại!";
            }
            /*else if (!"OPEN".equals(currentItem.getStatus())) {
                responseData= "Phiên đấu giá đang đóng, không thể đặt giá!";
            }*/
            else if (bidRequest.getBidAmount() <= currentItem.getCurrentPrice()) {
                responseData = "Giá đã bị đẩy lên € " + currentItem.getCurrentPrice() + ". Vui lòng trả cao hơn!";
            } else {
                boolean isUpdated = itemDao.placeBid(bidRequest.getIdItem(), bidRequest.getBidAmount(), bidRequest.getIdUser());
                if (isUpdated) {
                    // 4. Nếu cập nhật Item thành công, tiến hành lưu lịch sử vào bảng BIDS
                    boolean isHistorySaved = bidDao.add(bidRequest);

                    if (isHistorySaved) {
                        status = "SUCCESS";
                        responseData = "Da dat thanh cong: " + bidRequest.getBidAmount();

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime newEndTime = null;

                        if (java.time.Duration.between(now, currentItem.getEndTime()).getSeconds() < 30) {
                            newEndTime = currentItem.getEndTime().plusMinutes(2);
                            boolean updateTimeIsSuccess= itemDao.updateEndTime(currentItem.getId(), newEndTime);
                        }

                        notification = new BidUpdateNotification(
                                currentItem.getId(),
                                bidRequest.getBidAmount(),
                                bidRequest.getBidderName(),
                                now,
                                newEndTime
                        );
                    } else {
                        responseData = "Lỗi hệ thống khi lưu lịch sử đấu giá!";
                    }
                } else {
                    responseData = "Không thể đặt giá. Số dư khả dụng của bạn không đủ!";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = "ERROR";
            responseData = "Lỗi Server: " + e.getMessage();
        }
        finally {
            msg.setStatus(status);
            msg.setData(responseData);

            out.writeObject(msg);
            out.flush();
            out.reset();

            if (notification != null) {
                ClientManager.broadcast(notification);
            }
        }
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
                            return new Object[]{bid.getBidTime().format(formatter), bid.getBidAmount()};
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
            Item item = itemDao.getById(itemId);
            List<ItemImage> images = item.getImages();

            msg.setStatus("SUCCESS");
            msg.setData(images); // Gửi thẳng List đối tượng chứa URL về
        } catch (Exception e) {
            msg.setStatus("ERROR");
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleGetMyAuctions(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int userId = (int) msg.getData();

            List<AuctionItem> myAuctions = itemDao.getMyAuctions(userId);

            msg.setStatus("SUCCESS");
            msg.setData(myAuctions);

        } catch (Exception e) {
            System.err.println("Lỗi handleGetMyAuctions: " + e.getMessage());
            msg.setStatus("ERROR");
            msg.setData(null);
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleSendMessageBid(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // Dữ liệu nhận được từ ItemviewController: Object[] {selectedItem, currentUser}
            Object[] data = (Object[]) msg.getData();
            Item item = (Item) data[0];
            User user = (User) data[1];
            int idBid = user.getId();
            List<Bid> previousBids = bidDao.getBidsByItemId(item.getId());
            Set<Integer> targetUserIds = previousBids.stream()
                    .map(Bid::getIdUser)
                    .filter(userId -> userId != idBid)
                    .collect(Collectors.toSet());

            String notificationMessage = "Món hàng " + item.getName() + " đã được người dùng " + user.getUsername() + " đấu giá cao hơn với giá " + item.getCurrentPrice() ;
            for (int userId : targetUserIds) {
                bidDao.addNotification(userId, notificationMessage);
            }
            msg.setStatus("SUCCESS");
        } catch (Exception e) {
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
    private void handleGetMessage(Message msg, ObjectOutputStream out) throws  IOException{
        try{
            User user = (User) msg.getData();
            List<String> notification = bidDao.getNotification(user.getId());
            msg.setStatus("SUCCESS");
            msg.setData(notification);
        }catch (Exception e){
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
}