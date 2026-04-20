package auction.server;

import auction.common.message.Message;
import auction.common.model.users.User;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg == null) break;

                // Xử lý logic dựa trên lệnh
                if ("LOGIN".equals(msg.getCommand())) {
                    User userRequest = (User) msg.getData();
                    // Tại đây bạn sẽ gọi DatabaseManager.getConnection() để kiểm tra DB
                    System.out.println("Đang xử lý đăng nhập cho: " + userRequest.getUsername());

                    // Giả sử đăng nhập thành công
                    msg.setStatus("SUCCESS");
                    out.writeObject(msg);
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối.");
        }
    }
}