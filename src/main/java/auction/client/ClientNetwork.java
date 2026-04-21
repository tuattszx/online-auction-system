package auction.client;

import auction.common.message.Message;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientNetwork {
    private static final String SERVER_IP = "168.144.109.78";
    private static final int PORT = 8888;

    public Message sendRequest(Message request) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SERVER_IP, PORT), 5000);
            socket.setSoTimeout(5000);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        } catch (Exception e) {
            // Trả về một Message đặc biệt để báo Server không hoạt động
            return new Message("SERVER_OFFLINE");
        }
    }
}