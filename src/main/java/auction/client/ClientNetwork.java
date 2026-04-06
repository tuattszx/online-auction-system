package auction.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientNetwork {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 1234); // IP và Port của Server
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
    }

    public static Object sendRequest(Object request) throws Exception {
        connect();
        out.writeObject(request);
        out.flush();
        return in.readObject(); 
    }

    public static void close() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    }
}
