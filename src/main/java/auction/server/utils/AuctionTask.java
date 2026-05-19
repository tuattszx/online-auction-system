package auction.server.utils;

import auction.server.dao.ItemDao;
import auction.server.dao.impl.ItemDaoImpl;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTask {
    private final ItemDao itemDao = new ItemDaoImpl();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ((ItemDaoImpl) itemDao).processExpiredItems();
                ((ItemDaoImpl) itemDao).processIncomingItems();
            } catch (Exception e) {
                System.err.println("[AuctionTask] Lỗi thực thi: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}