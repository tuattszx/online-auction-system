package auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 8888;
    // Tạo một Thread Pool tối đa 50 luồng
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đấu giá đang chạy tại port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có kết nối mới: " + clientSocket.getInetAddress());

                // Thay vì new Thread().start(), hãy giao cho Thread Pool xử lý
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Đóng Thread Pool khi tắt Server
            threadPool.shutdown();
        }
    }
}