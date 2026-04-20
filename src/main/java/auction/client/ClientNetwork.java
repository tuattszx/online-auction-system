package auction.client;

import auction.common.message.Message;
import java.io.*;
import java.net.Socket;

public class ClientNetwork {
    private static final String SERVER_IP = "168.144.109.78";
    private static final int PORT = 8888;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect() throws IOException {
        socket = new Socket(SERVER_IP, PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public Message sendRequest(Message request) {
        try {
            out.writeObject(request);
            out.flush();
            return (Message) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}