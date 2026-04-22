package auction.client;

import auction.common.message.Message;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientNetwork {
    private static final String SERVER_IP = "168.144.109.78";
    private static final int PORT = 8888;

    private static ClientNetwork instance;
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static ClientNetwork getInstance(){
        if (instance == null) instance = new ClientNetwork();
        return instance;
    }
    // Hàm kết nối lần đầu (gọi khi bắt đầu App hoặc trước khi Login)
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_IP, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
    }

    public Message sendRequest(Message request) {
        try {
            connect(); // Đảm bảo luôn có kết nối

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new Message("SERVER_OFFLINE");
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) { }
    }
}