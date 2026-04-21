package auction.client;

import auction.common.message.Message;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientNetwork {
    private static final String SERVER_IP = "168.144.109.78";
    private static final int PORT = 8888;

    public Message sendRequest(Message request) {
        // Sử dụng try-with-resources để tự động đóng socket và stream
        try (Socket socket = new Socket()) {
            // Thiết lập timeout 5 giây để tránh treo UI nếu server sập
            socket.connect(new InetSocketAddress(SERVER_IP, PORT), 5000);
            socket.setSoTimeout(5000);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Gửi yêu cầu
            out.writeObject(request);
            out.flush();

            // Nhận phản hồi
            return (Message) in.readObject();

        } catch (Exception e) {
            System.err.println("Lỗi kết nối Server: " + e.getMessage());
            return new Message("ERROR", null);
        }
    }
}