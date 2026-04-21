package auction.server;

import auction.common.message.Message;
import auction.common.model.users.Account;
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
        // Đảm bảo thứ tự khởi tạo Stream khớp với Client
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Object obj = in.readObject();
            if (obj instanceof Message msg) {
                if ("LOGIN".equals(msg.getCommand())) {
                    Account accountRequest = (Account) msg.getData();
                    System.out.println("Đang xử lý login: " + accountRequest.getUsername());

                    // Giả lập xử lý
                    msg.setStatus("SUCCESS");
                    out.writeObject(msg);
                    out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }
}