package auction.server;

import auction.common.message.Message;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import auction.server.dao.UserDao;
import java.io.*;
import java.net.Socket;

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

}