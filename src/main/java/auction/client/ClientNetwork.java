package auction.client;

import auction.client.services.AuctionSubscriptionManager;
import auction.common.message.BidUpdateNotification;
import auction.common.message.Message;
import auction.common.model.notifications.Notification;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClientNetwork {
    private static final String SERVER_IP = "168.144.109.78";
    private static final int PORT = 8888;

    private static ClientNetwork instance;
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    private final Map<String, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>();

    private ClientNetwork() { }

    public static synchronized ClientNetwork getInstance(){
        if (instance == null) instance = new ClientNetwork();
        return instance;
    }
    // Hàm kết nối lần đầu (gọi khi bắt đầu App hoặc trước khi Login)
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_IP, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            startListening();
        }
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    Object obj = in.readObject();

                    if (obj instanceof BidUpdateNotification notification) {
                        // TRƯỜNG HỢP 1: Server tự đẩy về (Broadcast)
                        // ta báo cho UI cập nhật
                        AuctionSubscriptionManager.getInstance().notifyUpdate(notification);
                    }
                    else if (obj instanceof Notification notification) {
                        // TRƯỜNG HỢP MỚI: Bắt thông báo chấm đỏ/hộp thư real-time từ Server gửi riêng
                        auction.client.services.NotificationSubscriptionManager.getInstance().notifyNewNotification(notification);
                    }
                    else if (obj instanceof Message response) {
                        // TRƯỜNG HỢP 2: Phản hồi cho một lệnh ta đã gửi (Login, GetItem...)
                        String reqId = response.getRequestId();
                        CompletableFuture<Message> future = pendingRequests.remove(reqId);
                        if (future != null) {
                            future.complete(response);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Mất kết nối Server trong khi lắng nghe: " + e.getMessage());
                close();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public CompletableFuture<Message> sendRequestAsync(Message request){
        try {
            connect(); // Đảm bảo luôn có kết nối

            String requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);

            CompletableFuture<Message> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);

            out.writeObject(request);
            out.flush();
            out.reset();

            return future;
        } catch (Exception e) {
            CompletableFuture<Message> failed = new CompletableFuture<>();
            failed.complete(new Message("SERVER_OFFLINE"));
            return failed;
        }
    }

    public Message sendRequest(Message request) {
        try {
            return sendRequestAsync(request).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Lỗi hoặc Timeout: " + e.getMessage());
            return new Message("TIMEOUT_OR_ERROR");
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) { }
    }
}