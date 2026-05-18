package auction.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientManager {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public static void addClient(ClientHandler handler) {
        activeClients.add(handler);
        System.out.println("ClientManager: +1 client. Total: " + activeClients.size());
    }

    public static void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
        System.out.println("ClientManager: -1 client. Total: " + activeClients.size());
    }

    public static List<ClientHandler> getActiveClients() {
        return java.util.Collections.unmodifiableList(activeClients);
    }

    /**
     * Gửi dữ liệu tới TẤT CẢ mọi người đang online
     */
    public static void broadcast(Object message) {
        for (ClientHandler client : activeClients) {
            client.sendObject(message);
        }
    }
}
