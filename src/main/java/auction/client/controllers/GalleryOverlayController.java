package auction.client.controllers;

import auction.common.model.items.ItemImage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryOverlayController {

    @FXML private StackPane galleryOverlayRoot;
    @FXML private ScrollPane imageScrollPane;
    @FXML private StackPane imageContainer;
    @FXML private ImageView expandedImageView;
    @FXML private VBox rightThumbnailContainer;
    @FXML private ScrollPane thumbnailScrollPane;

    private List<ItemImage> imageList = new ArrayList<>();
    private int currentIndex = 0;

    // Các thuộc tính phục vụ tính năng Zoom và Pan (Kéo thả ảnh)
    private double scaleFactor = 1.0;
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double translateAnchorX;
    private double translateAnchorY;

    @FXML
    public void initialize() {
        setupZoomAndPan();
    }

    public void setData(List<ItemImage> images, int initialIndex) {
        this.imageList = new ArrayList<>(images);
        this.currentIndex = initialIndex;

        displayImageAtIndex(currentIndex);
        displayThumbnails(images,rightThumbnailContainer);
    }

    void displayImageAtIndex(int index) {
        if (imageList == null || imageList.isEmpty()) return;
        if (index < 0 || index >= imageList.size()) return;

        currentIndex = index;
        String url = imageList.get(currentIndex).getUrlImage();

        if (url != null && !url.isEmpty()) {
            Image img = new Image(url, true);
            expandedImageView.setImage(img);

            // Reset lại độ thu phóng và vị trí ảnh về mặc định ban đầu khi chuyển ảnh
            resetZoom();

            // Đồng bộ trạng thái mờ/sáng cho danh sách ảnh nhỏ bên phải
            updateThumbnailHighlight();
        }
    }

     void displayThumbnails(List<ItemImage> images, VBox thumbnailContainer) {
        if (images == null || images.isEmpty()) return;

        Platform.runLater(() -> {
            // Xóa sạch các ảnh cũ đang hiển thị trong container
            thumbnailContainer.getChildren().clear();

            // Thay đổi vòng lặp dùng index (int i) để truyền vị trí chính xác của ảnh
            for (int i = 0; i < images.size(); i++) {
                ItemImage imgModel = images.get(i);

                // 1. Lấy URL từ Cloudinary
                String url = imgModel.getUrlImage();
                if (url == null || url.isEmpty()) continue;

                // 2. Tạo Image trực tiếp từ URL (tham số true giúp load ngầm không lag UI)
                Image img = new Image(url, true);
                ImageView thumb = new ImageView(img);

                // Giữ nguyên toàn bộ cấu hình kích thước và style cũ của bạn
                thumb.setFitHeight(80.0);
                thumb.setFitWidth(80.0);
                thumb.setPickOnBounds(true);
                thumb.setPreserveRatio(true);
                thumb.setCursor(Cursor.HAND);
                thumb.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

                // Mặc định ban đầu cho mờ nhẹ, hàm displayImageAtIndex sẽ lo việc làm sáng ảnh được chọn
                thumb.setOpacity(0.4);

                // Đóng gói vị trí index cố định của ảnh nhỏ này
                final int targetIndex = i;

                // Bấm ảnh nhỏ thì gọi hàm đồng bộ để thay đổi cả ảnh to và index hệ thống
                thumb.setOnMouseClicked(event -> {
                    displayImageAtIndex(targetIndex);
                });

                // Thêm vào Container (VBox/HBox) của bạn
                thumbnailContainer.getChildren().add(thumb);
            }

            // Mặc định kích hoạt hiển thị tấm ảnh đầu tiên (index = 0) ngay khi nạp xong dữ liệu
            if (!thumbnailContainer.getChildren().isEmpty()) {
                displayImageAtIndex(0);
            }
        });
    }

    private void updateThumbnailHighlight() {
        for (int i = 0; i < rightThumbnailContainer.getChildren().size(); i++) {
            Node node = rightThumbnailContainer.getChildren().get(i);
            if (node instanceof ImageView) {
                ImageView thumb = (ImageView) node;
                if (i == currentIndex) {
                    thumb.setOpacity(1.0);
                    thumb.setEffect(new DropShadow(12, Color.web("#0052FF")));
                } else {
                    thumb.setOpacity(0.5);
                    thumb.setEffect(null);
                }
            }
        }
    }

    // Thiết lập tính năng cuộn chuột để phóng to/thu nhỏ và kéo thả di chuyển ảnh
    private void setupZoomAndPan() {
        // 1. Xử lý Zoom bằng con lăn chuột (Scroll)
        imageScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
            event.consume(); // Chặn tính năng cuộn trang mặc định của ScrollPane

            double zoomIntensity = 0.1;
            if (event.getDeltaY() > 0) {
                scaleFactor += zoomIntensity;
            } else {
                scaleFactor -= zoomIntensity;
            }

            // Giới hạn tỷ lệ zoom từ 0.5x đến 4.0x
            scaleFactor = Math.max(0.5, Math.min(4.0, scaleFactor));

            expandedImageView.setScaleX(scaleFactor);
            expandedImageView.setScaleY(scaleFactor);
        });

        // 2. Kích hoạt sự kiện nhấn chuột để bắt đầu Kéo/Thả ảnh (Pan)
        expandedImageView.setOnMousePressed(event -> {
            if (scaleFactor > 1.0) { // Chỉ cho phép kéo di chuyển khi ảnh đang được phóng to
                expandedImageView.setCursor(Cursor.CLOSED_HAND);
                mouseAnchorX = event.getSceneX();
                mouseAnchorY = event.getSceneY();
                translateAnchorX = expandedImageView.getTranslateX();
                translateAnchorY = expandedImageView.getTranslateY();
            }
        });

        expandedImageView.setOnMouseDragged(event -> {
            if (scaleFactor > 1.0) {
                expandedImageView.setTranslateX(translateAnchorX + (event.getSceneX() - mouseAnchorX));
                expandedImageView.setTranslateY(translateAnchorY + (event.getSceneY() - mouseAnchorY));
            }
        });

        expandedImageView.setOnMouseReleased(event -> {
            expandedImageView.setCursor(Cursor.DEFAULT);
        });
    }

    private void resetZoom() {
        scaleFactor = 1.0;
        expandedImageView.setScaleX(1.0);
        expandedImageView.setScaleY(1.0);
        expandedImageView.setTranslateX(0);
        expandedImageView.setTranslateY(0);
    }

    @FXML
    void handlePrevImage(ActionEvent event) {
        int newIndex = currentIndex - 1;
        if (newIndex < 0) newIndex = imageList.size() - 1;
        displayImageAtIndex(newIndex);
    }

    @FXML
    void handleNextImage(ActionEvent event) {
        int newIndex = currentIndex + 1;
        if (newIndex >= imageList.size()) newIndex = 0;
        displayImageAtIndex(newIndex);
    }

}