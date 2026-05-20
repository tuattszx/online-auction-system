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
    private final List<String> category;
    private final boolean isEditMode;

    public UploadItemTask(Item item, List<File> files, List<String> category) {
        this.item = item;
        this.files = files;
        this.category = category;
        this.isEditMode=false;
        this.updateTitle(item.getName()); // Tên item hiển thị trên thanh loading
    }

    public UploadItemTask(Item item, List<File> files, List<String> category, boolean isEditMode) {
        this.item = item;
        this.files = files;
        this.category = category;
        this.isEditMode=isEditMode;
        this.updateTitle(item.getName());
    }

    @Override
    protected Message call() throws Exception {
        List<String> imageUrls = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            int total = files.size();
            for (int i = 0; i < total; i++) {
                if (isCancelled()) break;

                updateMessage("Đang tải ảnh " + (i + 1) + "/" + total);
                String url = ImageService.uploadToCloud(files.get(i));
                if (url != null) imageUrls.add(url);

                updateProgress(i + 1, total); // Cập nhật thanh ProgressBar
            }
        }
        updateMessage("Đang lưu lên Server...");
        Object[] payload = new Object[]{item, imageUrls, category};
        String command = isEditMode ? "UPDATE_ITEM" : "ADD_ITEM";
        return ClientNetwork.getInstance().sendRequest(new Message(command, payload));
    }
}
