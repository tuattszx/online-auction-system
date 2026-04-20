package auction.server;

import auction.common.message.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private static final int PORT = 8888;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đấu giá đang chạy tại port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có người dùng kết nối: " + clientSocket.getInetAddress());

                // Tạo một luồng riêng để xử lý từng Client
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                Message inputMessage;
                while ((inputMessage = (Message) in.readObject()) != null) {
                    System.out.println("Nhận lệnh: " + inputMessage.getCommand());
                    // Xử lý logic tại đây dựa trên command
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Lỗi kết nối với client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
