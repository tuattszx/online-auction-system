package auction.server;

import auction.common.message.BidUpdateNotification;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.AuctionItem;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.common.model.items.Transaction;
import auction.common.model.notifications.Notification;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import auction.server.dao.*;
import auction.server.dao.impl.*;
import auction.server.utils.NotificationService;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private final UserDao userDao = new UserDaoImpl();
    private final ItemDao itemDao = new ItemDaoImpl();
    private final CategoryDao categoryDao = new CategoryDaoImpl();
    private final BidDao bidDao = new BidDaoImpl();
    private final NotificationDAO notificationDao=new NotificationDaoImpl();
    private final FavouriteDao favouriteDao= new FavouriteDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private ObjectOutputStream out;
    private User loggedInUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    public User getLoggedInUser() {
        return loggedInUser;
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
                        this.loggedInUser=null;
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
                        case "GET_USER_BY_ID":
                            handleGetUserById(msg, out);
                            break;
                        case "GET_ALL_USERS":
                            handleGetAllUsers(msg, out);
                            break;
                        case "WARN_USER":
                            handleWarningUser(msg, out);
                            break;
                        case "DELETE_USER":
                            handleDeleteUser(msg, out);
                            break;
                        case "UNBAN_USER":
                            handleUnbanUser(msg,out);
                            break;
                        case "GET_DASHBOARD_STATS":
                            handleGetDashboardStats(msg,out);
                            break;
                        case "ADD_ITEM":
                            System.out.println("-> Server đã nhận được lệnh AAA_PROFILE!");
                            handleAddItem(msg, out);
                            break;
                        case "GET_ALL_ITEMS":
                            handleGetAllItems(msg, out);
                            break;
                        case "GET_ALL_TRANSACTIONS":
                            handleGetAllTransaction(msg,out);
                            break;
                        case "GET_UNAPPROVED_ITEMS":
                            handleGetUnapproveItems(msg,out);
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
                        case "GET_MESSAGE":
                            handleGetMessage(msg, out);
                            break;
                        case "MARK_AS_READ":
                            handleMaskAsRead(msg, out);
                            break;
                        case "ADD_FAVOURITE":
                            handleFavourite(msg, out, true);
                            break;
                        case "REMOVE_FAVOURITE":
                            handleFavourite(msg, out, false);
                            break;
                        case "GET_FAVOURITES":
                            handleGetFavourites(msg, out);
                            break;
                        case "GET_UNREAD_COUNT":
                            getUnreadCount(msg, out);
                            break;
                        case "UPDATE_PROFILE":
                            handleUpdateProfile(msg,out);
                            break;
                        case "GET_SELLER_PRODUCTS":
                            handleGetItemBySeller(msg,out);
                            break;
                        case "DELETE_ITEM":
                            handleDeleteItem(msg,out);
                            break;
                        case "UPDATE_ITEM":
                            handleUpdateItem(msg,out);
                            break;
                        case "SET_UP_AUTO_BID":
                            handleSetupAutoBid(msg,out);
                            break;
                        case "CANCEL_AUTO_BID":
                            handleCancelAutoBid(msg,out);
                            break;
                        case "CHECK_AUTO_BID_STATUS":
                            handleCheckAutoBidStatus(msg, out);
                            break;
                        case "GET_BY_CATEGORY":
                            handleGetItemByCategory(msg, out);
                            break;
                        case "DEPOSIT_REQUEST":
                            handleDepositRequest(msg, out);
                            break;
                        case "GET_CUSTOMERS":
                            handleGetCustomers(msg, out);
                            break;
                        case "GET_SELLER_REVENUE":
                            handleGetSellerRevenue(msg, out);
                            break;
                        case "CONFIRM_ITEM":
                            handleConfirmItem(msg, out);
                            break;
                        case "FORGOT_PASSWORD":
                            handleForgotPassword(msg, out);
                            break;
                        case "UPDATE_PASSWORD":
                            handleUpdatePassword(msg,out);
                            break;
                        case "CANCEL_AUCTION":
                            handleCancelAuction(msg,out);
                            break;
                        case "CLEAR_ALL_NOTIF":
                            handleClearAllNotifByUserId(msg,out);
                            break;
                        case "READ_ALL_NOTIF":
                            handleReadAllNotifByUserId(msg,out);
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
            this.loggedInUser=user;
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

    private void handleGetUserById(Message msg, ObjectOutputStream out) throws IOException {
        int userId = (int) msg.getData();
        User user = userDao.getById(userId);
        if (user != null) {
            msg.setStatus("SUCCESS");
            msg.setData(user);
        } else {
            msg.setStatus("FAILED");
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleGetAllUsers(Message msg, ObjectOutputStream out) throws IOException {
        List<User> users = userDao.getAll();
        if (users != null) {
            msg.setStatus("SUCCESS");
            msg.setData(users);
        } else {
            msg.setStatus("FAILED");
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleWarningUser(Message msg, ObjectOutputStream out) throws IOException {
        try {
            Object[] payload= (Object[]) msg.getData();
            int userId= (int) payload[0];
            String reason= (String) payload[1];

            NotificationService.sendAdminWarningNotification(userId, reason,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            msg.setStatus("SUCCESS");
        } catch (Exception e) {
            msg.setStatus("ERROR");
            e.printStackTrace();
        }
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleDeleteUser(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int idUser = (int) msg.getData();
            boolean isSuccess= userDao.delete(idUser);

            if (isSuccess) {
                msg.setStatus("SUCCESS");
            } else {
                msg.setStatus("FAILED");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleUnbanUser(Message msg, ObjectOutputStream out) throws IOException{
        try {
            int idUser = (int) msg.getData();
            boolean isSuccess= userDao.unbanUser(idUser);

            if (isSuccess) {
                msg.setStatus("SUCCESS");
            } else {
                msg.setStatus("FAILED");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
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
            List<String> categoryName = (List<String>) payload[2];

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
            if (categoryName!=null) {
                List<Category> category = categoryDao.getCategoryByName(categoryName);
                item.setCategories(category);
                System.out.println("Dang xu ly category: " + categoryName);
            }
            // 4. Gọi ItemDao để lưu trọn bộ Item (bao gồm cả ảnh và category) vào DB
            // Hàm addItem của bạn đã có Transaction (Rollback) nên cực kỳ an toàn
            boolean isSuccess = itemDao.add(item);

            if (isSuccess) {
                msg.setStatus("SUCCESS");
                System.out.println("Đã thêm sản phẩm mới: " + item.getName());
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                NotificationService.handleSendMessageAddItem(item, now);

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

    private void handleGetUnapproveItems(Message msg, ObjectOutputStream out) throws IOException{
        try {
            List<Item> items= itemDao.getUnapprovedItems();

            msg.setStatus("SUCCESS");
            msg.setData(items);
        } catch (Exception e){
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
        int targetItemId=-1;
        long finalPriceTrigger=0;
        try {
            Bid bidRequest = (Bid) msg.getData();
            targetItemId = bidRequest.getIdItem();
            finalPriceTrigger = bidRequest.getBidAmount();

            Item currentItem = itemDao.getById(bidRequest.getIdItem());

            if (currentItem == null) {
                responseData = "Sản phẩm không tồn tại!";
            }
            else if (!"OPEN".equals(currentItem.getStatus())) {
                responseData= "Phiên đấu giá đang đóng, không thể đặt giá!";
            }
            else if (bidRequest.getBidAmount() <= currentItem.getCurrentPrice()) {
                responseData = "Giá đã bị đẩy lên $ " + currentItem.getCurrentPrice() + ". Vui lòng trả cao hơn!";
            } else {
                boolean isUpdated = itemDao.placeBid(bidRequest.getIdItem(), bidRequest.getBidAmount(), bidRequest.getIdUser());
                if (isUpdated) {
                    // 4. Nếu cập nhật Item thành công, tiến hành lưu lịch sử vào bảng BIDS
                    boolean isHistorySaved = bidDao.add(bidRequest);

                    if (isHistorySaved) {
                        status = "SUCCESS";
                        responseData = "Da dat thanh cong: " + bidRequest.getBidAmount();

                        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
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

                        NotificationService.handleSendMessageBid(currentItem, bidRequest, now);
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

            if ("SUCCESS".equals(status) && targetItemId != -1) {
                final int itemIdFinal = targetItemId;
                final long priceFinal = finalPriceTrigger;

                Thread autoBidThread = new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        itemDao.checkAndTriggerAutomaticBids(itemIdFinal);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                autoBidThread.setDaemon(true);
                autoBidThread.start();
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
        out.reset();
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

    private void handleGetMessage(Message msg, ObjectOutputStream out) throws  IOException{
        try{
            Integer id = (Integer) msg.getData();
            List<Notification> listNotifications = notificationDao.getNotificationsByUserId(id);
            msg.setStatus("SUCCESS");
            msg.setData(listNotifications);
        }catch (Exception e){
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleMaskAsRead(Message msg, ObjectOutputStream out) throws IOException {
        try {
            Integer notificationId = (Integer) msg.getData();
            boolean isUpdated = notificationDao.markAsRead(notificationId);
            if (isUpdated) {
                msg.setStatus("SUCCESS");
            } else {
                msg.setStatus("FAILED");
                msg.setData("Không tìm thấy thông báo hoặc đã được đánh dấu là đã đọc.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleFavourite(Message msg,ObjectOutputStream out, boolean isAdd) throws IOException{
        try{
            Object[] payload = (Object[]) msg.getData();
            int userId = (int) payload[0];
            int itemId = (int) payload[1];

            boolean isAdded = isAdd ? favouriteDao.addFavourite(userId, itemId) : favouriteDao.removeFavourite(userId, itemId);
            msg.setStatus(isAdded ? "SUCCESS" : "FAILED");
        }
        catch (Exception e){
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleGetFavourites(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int userId = (int) msg.getData();
            List<Integer> favouriteItems = favouriteDao.getFavoriteItemIdsByUserId(userId);
            msg.setStatus("SUCCESS");
            msg.setData(favouriteItems);
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void getUnreadCount(Message msg, ObjectOutputStream out) throws IOException {
        try{
            int userId = (int) msg.getData();
            int unreadCount = notificationDao.countUnreadByUserId(userId);
            msg.setStatus("SUCCESS");
            msg.setData(unreadCount);
        }catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleUpdateProfile(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // 1. Lấy đối tượng User từ thuộc tính 'data' (Sử dụng getData() thay vì getObject())
                User userToUpdate = (User) msg.getData();

                // 3. Thực thi lưu thông tin đã sửa xuống Database
                boolean success = userDao.update(userToUpdate);

                // 4. Tạo gói tin phản hồi sử dụng Constructor có sẵn của bạn: Message(String command, Object data)
                Message responseMsg;
                if (success) {
                    // Gửi chuỗi "UPDATE_PROFILE_SUCCESS" qua command
                    responseMsg = new Message("UPDATE_PROFILE_SUCCESS", userToUpdate);
                    System.out.println("User " + userToUpdate.getUsername() + " updated successfully!");
                } else {
                    // Gửi chuỗi "UPDATE_PROFILE_FAILED" kèm thông báo lỗi hoặc null
                    responseMsg = new Message("UPDATE_PROFILE_FAILED", "Database update failed");
                    System.out.println("Failed to update user " + userToUpdate.getUsername());
                }
                responseMsg.setRequestId(msg.getRequestId());

                // 5. Gửi trả gói tin phản hồi về client thông qua ObjectOutputStream (out) của bạn
                // (Bạn nhớ thay thế 'out' bằng tên biến luồng xuất Socket tương ứng trong ServerThread/ClientHandler của mình)
                if (out != null) {
                    out.writeObject(responseMsg);
                    out.flush();
                }

            } catch (Exception e) {
                System.err.println("Error while handling update profile: " + e.getMessage());
                e.printStackTrace();
            }
    }

    private void handleGetItemBySeller(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int sellerId = (int) msg.getData();
            List<Item> itemsBySeller = itemDao.getItemsBySeller(sellerId);
            msg.setStatus("SUCCESS");
            msg.setData(itemsBySeller);
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleDeleteItem(Message msg, ObjectOutputStream out) throws IOException{
        try{
            int idItem=(int) msg.getData();
            boolean isDeleted = itemDao.cancelAuction(idItem);
            msg.setStatus(isDeleted ? "SUCCESS" : "FAILED");
        }catch (Exception e){
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleUpdateItem(Message msg, ObjectOutputStream out) throws IOException{
        try{
            Object[] payload = (Object[]) msg.getData();
            Item item = (Item) payload[0];
            List<String> imageUrls = (List<String>) payload[1];
            List<String> categoryName = (List<String>) payload[2];

            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (int i = 0; i < imageUrls.size(); i++) {
                    ItemImage itemImg = new ItemImage();
                    itemImg.setUrlImage(imageUrls.get(i));
                    itemImg.setDefault(i == 0);
                    item.addImages(itemImg);
                }
            }

            if (categoryName != null) {
                List<Category> category = categoryDao.getCategoryByName(categoryName);
                item.setCategories(category);
            }

            boolean isSuccess = itemDao.update(item);

            if (isSuccess) {
                msg.setStatus("SUCCESS");
                msg.setData("Cập nhật thành công!");

                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                NotificationService.handleSendMessageUpdateItem(item, now);
            } else {
                msg.setStatus("FAILED");
                msg.setData("Cập nhật thất bại! Vui lòng kiểm tra lại trạng thái sản phẩm.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("SERVER_ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        }

        // 6. Trả kết quả phản hồi về cho Client
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    private void handleSetupAutoBid(Message msg, ObjectOutputStream out) throws IOException {
        String status = "FAILED";
        Object responseData = "Lỗi thiết lập hệ thống tự động";
        try {
            Object[] payload = (Object[]) msg.getData();
            int itemId = (int) payload[0];
            int userId = (int) payload[1];
            long maxBid = (long) payload[2];
            long increment = (long) payload[3];
            String username = (String) payload[4];

            Item currentItem = itemDao.getById(itemId);
            if (currentItem == null) {
                responseData = "Sản phẩm không tồn tại để thiết lập tự động!";
            } else if (!"OPEN".equals(currentItem.getStatus())) {
                responseData = "Phiên đấu giá không trong trạng thái mở, không thể cài đặt tự động!";
            } else if (maxBid <= currentItem.getCurrentPrice()) {
                responseData = "Mức giá trần (Max Bid) phải lớn hơn giá hiện tại của sản phẩm!";
            } else {
                boolean isSuccess = itemDao.setupAutoBid(itemId, userId, maxBid, increment, username);

                if (isSuccess) {
                    status = "SUCCESS";
                    responseData = "Đã kích hoạt cấu hình đấu giá tự động thành công!";

                    msg.setStatus(status);
                    msg.setData(responseData);
                    out.writeObject(msg);
                    out.flush();
                    out.reset();

                    status = "ALREADY_SENT";
                    final int itemIdFinal = itemId;
                    final long currentPriceFinal = currentItem.getCurrentPrice();

                    Thread autoBidTriggerThread = new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            itemDao.checkAndTriggerAutomaticBids(itemIdFinal);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    autoBidTriggerThread.setDaemon(true);
                    autoBidTriggerThread.start();
                } else {
                    responseData = "Không thể lưu cấu hình. Vui lòng kiểm tra lại số dư tài khoản!";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = "ERROR";
            responseData = "Lỗi Server: " + e.getMessage();
        } finally {
            if (!"ALREADY_SENT".equals(status)) {
                msg.setStatus(status);
                msg.setData(responseData);
                out.writeObject(msg);
                out.flush();
                out.reset();
            }
        }
    }

    private void handleCancelAutoBid(Message msg, ObjectOutputStream out) throws IOException {
        String status = "FAILED";
        Object responseData = "Hủy cấu hình tự động thất bại.";
        try {
            Object[] payload = (Object[]) msg.getData();
            int itemId = (int) payload[0];
            int userId = (int) payload[1];

            boolean isSuccess = itemDao.cancelAutoBid(itemId, userId);

            if (isSuccess) {
                status = "SUCCESS";
                responseData = "Đã hủy cấu hình đấu giá tự động của bạn thành công!";
            } else {
                responseData = "Hệ thống không tìm thấy cấu hình Auto Bid nào của bạn để hủy!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = "ERROR";
            responseData = "Lỗi hệ thống Server: " + e.getMessage();
        } finally {
            msg.setStatus(status);
            msg.setData(responseData);

            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleCheckAutoBidStatus(Message msg, ObjectOutputStream out) throws IOException {
        String status = "FAILED";
        Object responseData = false; // Mặc định là chưa cài đặt
        try {
            Object[] payload = (Object[]) msg.getData();
            int itemId = (int) payload[0];
            int userId = (int) payload[1];

            boolean exists = itemDao.checkAutoBidExists(itemId, userId);

            status = "SUCCESS";
            responseData = exists;
        } catch (Exception e) {
            e.printStackTrace();
            status = "ERROR";
            responseData = "Lỗi xử lý check Auto Bid ở Server";
        } finally {
            msg.setStatus(status);
            msg.setData(responseData);

            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleGetItemByCategory(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int categoryId = (int) msg.getData();
            List<Item> itemsByCategory = itemDao.getItemsByCategory(categoryId);
            msg.setStatus("SUCCESS");
            msg.setData(itemsByCategory);
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
    private void handleDepositRequest(Message msg, ObjectOutputStream out) throws IOException {
        // Ép kiểu về Map vì Client đóng gói dữ liệu thẻ và số tiền vào Map
        Map<String, Object> depositData = (Map<String, Object>) msg.getData();
        String cardNumber = (String) depositData.get("cardNumber");
        long amount = (long) depositData.get("amount");

        // Giả định bạn có cách lấy thông tin User đang kết nối từ phiên (Session/Context)
        // Hoặc nếu Client gửi kèm thông tin userId trong gói tin, bạn có thể lấy ra.
        // Ở đây tôi lấy ví dụ là lấy User hiện tại từ session hệ thống của bạn
        User currentUser = this.getLoggedInUser();

        // Kiểm tra giả lập kịch bản lỗi hệ thống thẻ (Sandbox cho bài tập lớn)
        if (cardNumber.endsWith("4444")) {
            msg.setStatus("FAILED");
            // Bạn có thể gửi kèm chuỗi thông báo lỗi vào data để Client hiển thị
            msg.setData("Tài khoản thẻ không đủ số dư (Mã lỗi: 4444).");
        } else if (cardNumber.endsWith("9999")) {
            msg.setStatus("FAILED");
            msg.setData("Thẻ đã bị khóa hoặc hết hạn sử dụng (Mã lỗi: 9999).");
        } else if (cardNumber.length() < 12) {
            msg.setStatus("FAILED");
            msg.setData("Số thẻ không hợp lệ. Vui lòng kiểm tra lại!");
        } else {
            // Gọi hàm updateBalance trong UserDao của bạn để cộng tiền vào Database
            boolean isSuccess = userDao.updateBalance(currentUser.getId(), amount);

            if (isSuccess) {
                // Cập nhật lại số dư mới vào đối tượng User trong bộ nhớ Server
                currentUser.setBalance(currentUser.getBalance() + amount);

                msg.setStatus("SUCCESS");
                // Gửi lại đối tượng User đã cập nhật số dư về để Client đồng bộ UI
                msg.setData(currentUser);
            } else {
                msg.setStatus("FAILED");
                msg.setData("Lỗi hệ thống khi cập nhật số dư vào Cơ sở dữ liệu.");
            }
        }
        out.reset();
        out.writeObject(msg);
        out.flush();
    }
    private void handleGetCustomers(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // 1. Bóc tách dữ liệu gửi lên từ Client (id của Seller)
            // Vì Client gửi dạng String.valueOf(sellerId), ta parse về kiểu int
            int sellerId = (int) msg.getData();

            // 2. Gọi xuống tầng DAO thực thi câu lệnh SQL kết nối bảng ITEMS để lấy danh sách User
            List<Object[]> customers = itemDao.getCustomersBySellerId(sellerId);

            // 3. Đóng gói dữ liệu kết quả trả về khi thành công
            msg.setStatus("SUCCESS");
            msg.setData(customers); // Gán danh sách List<User> vào data của gói tin

        } catch (Exception e) {
            e.printStackTrace();
            // Đóng gói trạng thái lỗi và thông báo chi tiết trả về Client
            msg.setStatus("ERROR");
            msg.setData("Lỗi Server: " + e.getMessage());

        } finally {
            // 4. Đẩy gói tin phản hồi ngược về Client qua Socket Stream
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
    private void handleGetSellerRevenue(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // 1. Bóc tách ID người bán gửi lên từ Client
            int sellerId = Integer.parseInt((String) msg.getData());

            // 2. Gọi xuống ItemDaoImpl để lấy tổng số tiền thực tế từ Cloud DB
            long revenue = itemDao.getTotalRevenueBySellerId(sellerId);

            // 3. Đóng gói phản hồi thành công
            msg.setStatus("SUCCESS");
            msg.setData(revenue); // Truyền giá trị kiểu số Long về cho Client

        } catch (Exception e) {
            System.err.println("Lỗi xử lý gói tin tính doanh thu:");
            e.printStackTrace();

            // Đóng gói phản hồi thất bại
            msg.setStatus("ERROR");
            msg.setData(0L); // Trả về số tiền bằng 0 nếu gặp lỗi mạng/DB
        } finally {
            // 4. Đẩy ngược gói tin phản hồi về ứng dụng Client qua Socket Stream
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
    private void handleConfirmItem(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // 1. Lấy ID sản phẩm gửi từ Client lên
            Object [] payload=(Object[]) msg.getData();
            int itemId = (int) payload[0];
            boolean isApproved=(boolean) payload[1];
            String reason= (String) payload[2];

            Item item= itemDao.getById(itemId);
            // 2. Chạy câu lệnh SQL chuyển trạng thái sang 'OPEN'
            boolean isSuccess = itemDao.approveItem(itemId, isApproved);

            // 3. Đóng gói phản hồi dựa vào kết quả thực thi
            if (isSuccess) {
                msg.setStatus("SUCCESS");
                msg.setData(isApproved ? " Sản phẩm đã được chấp nhận" : "Đã từ chối sản phẩm");
                if (isApproved){ NotificationService.handleSendMessageApproveItem(item,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));}
                else NotificationService.handleSendMessageRejectItem(item,reason,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            } else {
                msg.setStatus("ERROR");
                msg.setData("Không tìm thấy sản phẩm hoặc sản phẩm không thể cập nhật.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi hệ thống Server: " + e.getMessage());
        } finally {
            // Phản hồi lại gói tin về Client qua luồng Stream Socket
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleGetDashboardStats(Message msg, ObjectOutputStream out) throws IOException {
        try {
            // Khởi tạo một Map để gom nhóm 4 dữ liệu thống kê
            Map<String, Object> stats = new HashMap<>();
            Object[] fromUser= userDao.getDashboardStats();
            Object[] fromItem= itemDao.getDashboardStats();
            long totalRevenue = (long) fromUser[0];
            int liveAuctions = (int) fromItem[0];
            int totalUsers =(int) fromUser[1];
            double successRate = (double) fromItem[1];
            Map<String,Integer> categoryDistribution= (Map<String, Integer>) fromItem[2];
            List<Object[]> revenueTrend= (List<Object[]>) fromItem[3];

            stats.put("totalRevenue", totalRevenue);
            stats.put("liveAuctions", liveAuctions);
            stats.put("totalUsers", totalUsers);
            stats.put("successRate", successRate);
            stats.put("categoryDistribution",categoryDistribution);
            stats.put("revenueTrend", revenueTrend);

            msg.setStatus("SUCCESS");
            msg.setData(stats);
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
    private void handleForgotPassword(Message msg, ObjectOutputStream out) throws IOException {
        String emailReq = (String) msg.getData();

        // 1. Kiểm tra xem email có trong DB không
        if (userDao.isEmailExists(emailReq)) {
            // CÓ TỒN TẠI -> Tiến hành cấp mã OTP cố định
            String fixedOtpCode = "123456";

            msg.setStatus("SUCCESS");
            msg.setData(fixedOtpCode);
            System.out.println("[SERVER] Email hợp lệ. Đã cấp OTP: " + fixedOtpCode);
        } else {
            // KHÔNG TỒN TẠI -> Trả về lỗi FAILED cho Client
            msg.setStatus("FAILED");
            msg.setData("Email không tồn tại trên hệ thống!");
            System.out.println("[SERVER] Từ chối cấp OTP vì Email không tồn tại: " + emailReq);
        }

        out.writeObject(msg);
        out.flush();
    }

    private void handleUpdatePassword(Message msg, ObjectOutputStream out) throws IOException {
        // Bóc tách mảng Object dữ liệu gửi từ Client
        Object[] data = (Object[]) msg.getData();
        String email = (String) data[0];
        String newHashedPassword = (String) data[1];

        System.out.println("[SERVER] Đang xử lý đổi mật khẩu cho email: " + email);

        // 1. Tìm thông tin User thông qua Email để bốc ra ID
        User user = userDao.findUserByEmail(email);

        boolean success = false;
        if (user != null) {
            // 2. Gọi CHÍNH XÁC hàm updatePassword bằng ID của bạn
            success = userDao.updatePassword(user.getId(), newHashedPassword);
        }

        // 3. Phản hồi kết quả về cho Client
        if (success) {
            msg.setStatus("SUCCESS");
            msg.setData("Cập nhật mật khẩu thành công.");
        } else {
            msg.setStatus("FAILED");
            msg.setData("Tài khoản không tồn tại hoặc lỗi kết nối cơ sở dữ liệu!");
        }

        out.writeObject(msg);
        out.flush();
    }

    private void handleGetAllTransaction(Message msg, ObjectOutputStream out) throws IOException{
        try {
            List<Transaction> transactions = transactionDao.getAll();

            if (transactions != null) {
                msg.setStatus("SUCCESS");
                msg.setData(transactions);
            } else {
                msg.setStatus("FAILED");
            }
        } catch (Exception e){
            msg.setStatus("ERROR");
            e.printStackTrace();
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleCancelAuction(Message msg, ObjectOutputStream out) throws IOException {
        try {
            Object[] payload = (Object[]) msg.getData();
            int itemId = (int) payload[0];
            String reason = (String) payload[1];
            boolean isCancelled = itemDao.cancelAuction(itemId);

            if (isCancelled) {
                msg.setStatus("SUCCESS");
                msg.setData("Đấu giá đã được hủy thành công.");
                NotificationService.handleSendMessageRejectItem(itemDao.getById(itemId), reason, LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            } else {
                msg.setStatus("FAILED");
                msg.setData("Không tìm thấy sản phẩm hoặc sản phẩm không thể hủy.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi hệ thống Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleClearAllNotifByUserId(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int userId = (int) msg.getData();
            boolean isCleared = notificationDao.deleteAllByUserId(userId);

            if (isCleared) {
                msg.setStatus("SUCCESS");
                msg.setData("Đã xóa tất cả dữ liệu thành công.");
            } else {
                msg.setStatus("FAILED");
                msg.setData("Không thể xóa dữ liệu. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi hệ thống Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }

    private void handleReadAllNotifByUserId(Message msg, ObjectOutputStream out) throws IOException {
        try {
            int userId = (int) msg.getData();
            boolean isUpdated = notificationDao.readAllByUserId(userId);

            if (isUpdated) {
                msg.setStatus("SUCCESS");
                msg.setData("Đã đánh dấu tất cả thông báo là đã đọc.");
            } else {
                msg.setStatus("FAILED");
                msg.setData("Không thể cập nhật thông báo. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setStatus("ERROR");
            msg.setData("Lỗi hệ thống Server: " + e.getMessage());
        } finally {
            out.writeObject(msg);
            out.flush();
            out.reset();
        }
    }
}