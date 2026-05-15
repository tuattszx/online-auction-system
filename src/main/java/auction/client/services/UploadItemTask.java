package auction.client.services;

import auction.client.ClientNetwork;
import auction.client.utils.ImageService;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UploadItemTask extends Task<Message> {
    private final Item item;
    private final List<File> files;
    private final String category;

    public UploadItemTask(Item item, List<File> files, String category) {
        this.item = item;
        this.files = files;
        this.category = category;
        this.updateTitle(item.getName()); // Tên item hiển thị trên thanh loading
    }

    @Override
    protected Message call() throws Exception {
        List<String> imageUrls = new ArrayList<>();
        int total = files.size();

        for (int i = 0; i < total; i++) {
            if (isCancelled()) break;

            updateMessage("Đang tải ảnh " + (i + 1) + "/" + total);
            String url = ImageService.uploadToCloud(files.get(i));
            if (url != null) imageUrls.add(url);

            updateProgress(i + 1, total); // Cập nhật thanh ProgressBar
        }

        updateMessage("Đang lưu lên Server...");
        Object[] payload = new Object[]{item, imageUrls, category};
        return ClientNetwork.getInstance().sendRequest(new Message("ADD_ITEM", payload));
    }
}
