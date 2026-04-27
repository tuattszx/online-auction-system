package auction.server;

import auction.common.message.Message;
import auction.common.model.categories.Category;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import auction.server.dao.CategoryDao;
import auction.server.dao.ItemDao;
import auction.server.dao.UserDao;
import auction.server.utils.ImageService;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;

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
        User user = UserDao.CheckLogin(accReq.getUsername(), accReq.getPassword());

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
        if (UserDao.isUsernameExists(newUser.getUsername()) || UserDao.isEmailExists(newUser.getEmail())) {
            msg.setStatus("FAILED");
        } else {
            // Gọi hàm registerUser trong UserDao của bạn
            boolean isSuccess = UserDao.registerUser(newUser);
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
            Category category = CategoryDao.getCategoryByName(categoryName);
            if (category != null) {
                item.addCategories(category);
            }

            // 4. Gọi ItemDao để lưu trọn bộ Item (bao gồm cả ảnh và category) vào DB
            // Hàm addItem của bạn đã có Transaction (Rollback) nên cực kỳ an toàn
            boolean isSuccess = ItemDao.addItem(item);

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
            List<Item> items = ItemDao.getAllItems(); // Gọi ItemDao lấy dữ liệu

            for (Item item : items) {
                if (item.getImages() != null && !item.getImages().isEmpty()) {
                    for (ItemImage img : item.getImages()) {
                        // Sử dụng hàm readImageBytes đã viết trong ImageService
                        byte[] data = ImageService.readImageBytes(img.getUrlImage());
                        img.setImageData(data); // Gán mảng byte vào model
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
}