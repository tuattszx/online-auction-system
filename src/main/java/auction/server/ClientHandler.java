// File: src/main/java/auction/server/ClientHandler.java
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
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Object obj = in.readObject();
            if (obj instanceof Message msg) {
                if ("LOGIN".equals(msg.getCommand())) {
                    // Ép kiểu về Account (vì Client gửi Account object)
                    Account accReq = (Account) msg.getData();

                    // Kiểm tra Database qua UserDao
                    User user = UserDao.CheckLogin(accReq.getUsername(), accReq.getPassword());

                    if (user != null) {
                        msg.setStatus("SUCCESS");
                        msg.setData(user); // Trả về đối tượng User đầy đủ
                    } else {
                        msg.setStatus("FAILED");
                        msg.setData(null);
                    }

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