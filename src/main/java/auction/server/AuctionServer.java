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
}
