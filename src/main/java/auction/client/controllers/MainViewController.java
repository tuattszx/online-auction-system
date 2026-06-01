package auction.client.controllers;

import auction.client.services.AuctionTimerManager;
import auction.client.services.Cleanable;
import auction.client.services.LanguageManager;
import auction.client.session.DataSession;
import auction.common.model.items.ItemImage;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class MainViewController implements Cleanable {
    // public Label lbbalance;
    @FXML
    private TextField txtsearch;
    @FXML
    private ComboBox<String> sortPrice, sortTime;
    @FXML
    private Label lbusername;
    @FXML
    private FlowPane flitems;
    @FXML
    private ScrollPane scrollCategories; // Phải trùng với fx:id="scrollCategories" trong FXML
    @FXML
    protected HeaderMenuController headerMenuController;
    @FXML
    private VBox vboxThisweek;

    private static final String ACTIVE_CATEGORY_STYLE = "-fx-border-color: #0052ff; -fx-border-width: 0 0 3 0;";
    // SỬA LỖI: Khai báo biến network
    private ClientNetwork network = ClientNetwork.getInstance();
    private final List<AuctionTimerManager> activeCardTimers = new java.util.ArrayList<>();
    private List<Item> allAssets = new ArrayList<>();
    private List<Item> filteredAssets = new ArrayList<>();

    @FXML
    public void initialize() {
        DataSession.getInstance().setMainViewController(this);

        // Cần ép kiểu String cho ComboBox để tránh lỗi type-safety
        sortPrice.getItems().addAll(LanguageManager.getString("mainview.label.a_z"), LanguageManager.getString("mainview.label.z_a"));

        // Gắn sự kiện lắng nghe khi người dùng chọn sắp xếp theo giá
        sortPrice.setOnAction(event -> handleSortPrice());

        // GỌI HÀM: Để tải dữ liệu ngay khi mở trang
        loadItems();
    }

    private void handleSortPrice() {
        String selected = sortPrice.getValue();
        if (selected == null || filteredAssets == null || filteredAssets.isEmpty()) return;

        if (selected.equals(LanguageManager.getString("mainview.label.a_z"))) {
            filteredAssets.sort((o1, o2) -> Double.compare(o1.getCurrentPrice(), o2.getCurrentPrice()));
        } else if (selected.equals(LanguageManager.getString("mainview.label.z_a"))) {
            filteredAssets.sort((o1, o2) -> Double.compare(o2.getCurrentPrice(), o1.getCurrentPrice()));
        }

        // Sau khi sort xong, render lại danh sách đã sắp xếp
        renderItems(filteredAssets);
    }

    public void handleSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // Nếu ô tìm kiếm trống -> Kho tạm = Toàn bộ kho gốc
            filteredAssets = new ArrayList<>(allAssets);
        } else {
            String lowerKey = keyword.toLowerCase().trim();

            // Lọc bằng Stream và nạp vào kho tạm
            filteredAssets = allAssets.stream()
                    .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(lowerKey))
                    .toList();
        }

        // Áp dụng lại bộ lọc sắp xếp nếu người dùng đang chọn sortPrice
        handleSortPrice();

        // Nếu không có sắp xếp, render thẳng kết quả lọc
        if (sortPrice.getValue() == null) {
            renderItems(filteredAssets);
        }
    }

    private void loadItems() {
        cleanupTimers();
        flitems.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            flitems.getChildren().add(createSkeletonCard());
        }

        Task<List<Item>> loadTask = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                // Gửi request thông qua kết nối Socket
                Message response = network.sendRequest(new Message("GET_ALL_ITEMS", null));

                if (response == null || !"SUCCESS".equals(response.getStatus())) {
                    throw new RuntimeException("Server không phản hồi hoặc có lỗi xảy ra");
                }
                return (List<Item>) response.getData();
            }
        };

        // 2. Khi Task chạy thành công (đã lấy được danh sách Item)
        loadTask.setOnSucceeded(event -> {
            List<Item> result = loadTask.getValue();
            flitems.getChildren().clear();
            System.out.println("Đã tải xong " + (result != null ? result.size() : 0) + " sản phẩm.");
            if (result != null) {
                this.allAssets = new ArrayList<>(result);       // Nạp đầy kho gốc
                this.filteredAssets = new ArrayList<>(result);  // Nạp đầy kho tạm ban đầu
            }

            // Kiểm tra xem có đang chọn sort không, nếu có thì sort luôn data mới tải
            if (sortPrice.getValue() != null) {
                handleSortPrice();
            } else {
                renderItems(result);
            }

            if (DataSession.getInstance().getLoggedInUser() != null && result != null) {
                loadUserFavoritesInBackground(result);
            }
        });

        // 3. Khi Task thất bại (Lỗi mạng, Server sập, hoặc lỗi Serialization)
        loadTask.setOnFailed(event -> {
            Throwable exception = loadTask.getException();
            exception.printStackTrace();
            System.err.println("Lỗi khi tải dữ liệu: " + exception.getMessage());
        });

        // 4. Thực thi Task trên một Thread riêng
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadUserFavoritesInBackground(List<Item> allItems) {
        Task<Void> favTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int userId = DataSession.getInstance().getLoggedInUser().getId();
                Message favResponse = network.sendRequest(new Message("GET_FAVOURITES", userId));

                if (favResponse != null && "SUCCESS".equals(favResponse.getStatus())) {
                    List<Integer> favoriteIds = (List<Integer>) favResponse.getData();

                    Platform.runLater(() -> {
                        DataSession.getInstance().getFavoriteItems().clear();

                        for (Item item : allItems) {
                            if (favoriteIds.contains(item.getId())) {
                                DataSession.getInstance().addFavorite(item);
                            }
                        }
                    });
                }
                return null;
            }
        };

        Thread favThread = new Thread(favTask);
        favThread.setDaemon(true);
        favThread.start();
    }

    private VBox createSkeletonCard() {
        VBox card = new VBox();
        card.setPrefSize(200, 280);
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(10));

        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);"
        );

        Color skeletonColor = Color.web("#eeeeee");

        Rectangle imgSkeleton = new Rectangle(180, 140);
        imgSkeleton.setFill(skeletonColor);
        imgSkeleton.setArcWidth(10);
        imgSkeleton.setArcHeight(10);

        Rectangle nameSkeleton = new Rectangle(150, 16);
        nameSkeleton.setFill(skeletonColor);
        nameSkeleton.setArcWidth(5);
        nameSkeleton.setArcHeight(5);

        Rectangle priceSkeleton = new Rectangle(100, 18);
        priceSkeleton.setFill(skeletonColor);
        priceSkeleton.setArcWidth(5);
        priceSkeleton.setArcHeight(5);

        Rectangle btnSkeleton = new Rectangle(120, 30);
        btnSkeleton.setFill(skeletonColor);
        btnSkeleton.setArcWidth(20);
        btnSkeleton.setArcHeight(20);

        card.getChildren().addAll(imgSkeleton, nameSkeleton, priceSkeleton, btnSkeleton);

        FadeTransition ft = new FadeTransition(Duration.millis(1000), card);
        ft.setFromValue(1.0);
        ft.setToValue(0.4);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        return card;
    }

    private void renderItems(List<Item> items) {
        Platform.runLater(() -> {
            flitems.getChildren().clear();
            if (items != null) {
                for (Item item : items) {
                    flitems.getChildren().add(createItemCard(item));
                }
            }
        });
    }

    public VBox createItemCard(Item item) {
        VBox card = new VBox();
        card.setPrefSize(200, 280);
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4); " +
                        "-fx-cursor: hand;"
        );

        ImageView imgView = new ImageView();
        imgView.setFitHeight(140);
        imgView.setFitWidth(180);
        imgView.setPreserveRatio(false);

        boolean imageLoaded = false;
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            ItemImage defaultImg = item.getImages().stream()
                    .filter(ItemImage::isDefault)
                    .findFirst()
                    .orElse(item.getImages().get(0));

            String imageUrl = defaultImg.getUrlImage();
            String optimizedUrl = imageUrl.replace("/upload/", "/upload/w_200,c_fill,f_auto,q_auto/");

            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    Image img = new Image(optimizedUrl, 140, 180, true, true, true);
                    imgView.setImage(img);
                    imageLoaded = true;
                } catch (Exception e) {
                    System.err.println("Lỗi khi tải ảnh từ URL: " + e.getMessage());
                }
            }
        }
        if (!imageLoaded) {
            try {
                imgView.setImage(new Image(getClass().getResourceAsStream("/auction/img/images.jpg")));
            } catch (Exception e) {
                System.err.println("Không tìm thấy file ảnh mặc định trong resources");
            }
        }

        HBox nameAndHeartBox = new HBox();
        nameAndHeartBox.setAlignment(Pos.CENTER_LEFT);
        nameAndHeartBox.setSpacing(10);
        nameAndHeartBox.setPadding(new Insets(0, 5, 0, 5));

        Label nameLabel = new Label(item.getName());
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        nameLabel.setMaxWidth(140);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        nameLabel.setWrapText(false);
        nameLabel.setEllipsisString("...");

        Label heartIcon = new Label("❤");
        if (DataSession.getInstance().getFavoriteItems().contains(item)) {
            heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
        } else {
            heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
        }
        DataSession.getInstance().getFavoriteItems().addListener((ListChangeListener<Item>) change -> {
            if (DataSession.getInstance().getFavoriteItems().contains(item)) {
                heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
            } else {
                heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
            }
        });

        heartIcon.setOnMouseClicked(e -> {
            int currentUserId = DataSession.getInstance().getLoggedInUser().getId();
            Object[] payload = new Object[]{ currentUserId, item.getId() };
            if (DataSession.getInstance().getFavoriteItems().contains(item)) {
                DataSession.getInstance().removeFavorite(item);
                heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
                ClientNetwork.getInstance().sendRequestAsync(new Message("REMOVE_FAVOURITE", payload));
            } else {
                DataSession.getInstance().addFavorite(item);
                heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
                ClientNetwork.getInstance().sendRequestAsync(new Message("ADD_FAVOURITE", payload));
            }
            e.consume();
        });

        nameAndHeartBox.getChildren().addAll(nameLabel, heartIcon);

        Label priceLabel = new Label(String.format("%,d $", item.getCurrentPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0052ff;");

        Button bidBtn = new Button("Đấu giá");
        bidBtn.setPrefWidth(120);
        bidBtn.setStyle(
                "-fx-background-color: #0052ff; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 5 15 5 15;"
        );

        bidBtn.setOnMouseEntered(e -> bidBtn.setStyle(bidBtn.getStyle() + "-fx-background-color: #003db3;"));
        bidBtn.setOnMouseExited(e -> bidBtn.setStyle(bidBtn.getStyle() + "-fx-background-color: #0052ff;"));
        bidBtn.setOnAction(event -> {
            DataSession.getInstance().setSelectedItem(item);
            ViewManager.switchScene(event, "item-view.fxml", "Chi tiết");
        });

        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.setSpacing(6);
        statusContainer.setMaxHeight(30);
        statusContainer.setPrefWidth(120);
        statusContainer.getStylesheets().add(getClass().getResource("/auction/css/labelstatus.css").toExternalForm());

        Circle dot = new Circle(4);
        dot.getStyleClass().add("badge-dot");

        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("badge-text");

        statusContainer.getStyleClass().add("badge-container");

        AuctionTimerManager cardTimer = new AuctionTimerManager(
                item, lblStatus, statusContainer, dot);
        cardTimer.tick();

        statusContainer.getChildren().addAll(dot, lblStatus);

        card.getChildren().addAll(imgView, statusContainer, nameAndHeartBox, priceLabel, bidBtn);

        card.setOnMouseClicked(event -> {
            DataSession.getInstance().setSelectedItem(item);
            ViewManager.switchScene(event, "item-view.fxml", "Chi tiết");
        });

        return card;
    }

    @FXML private HBox categoryBar;

    @FXML
    private void handleCategoryClick(MouseEvent event) {
        VBox clickedCategory = (VBox) event.getSource();
        String catIdStr = (String) clickedCategory.getUserData();

        if (catIdStr == null || catIdStr.isEmpty()) {
            return;
        }
        for (Node node : categoryBar.getChildren()) {
            if (node instanceof VBox) {
                node.setStyle("");
            }
        }
        clickedCategory.setStyle(ACTIVE_CATEGORY_STYLE);

        int categoryId = Integer.parseInt(catIdStr);

        cleanupTimers();
        flitems.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            flitems.getChildren().add(createSkeletonCard());
        }

        network.sendRequestAsync(new Message("GET_BY_CATEGORY", categoryId))
                .thenAccept(response -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        List<Item> filteredItems = (List<Item>) response.getData();

                        Platform.runLater(() -> {
                            flitems.getChildren().clear();
                            if (filteredItems != null) {
                                this.allAssets = new ArrayList<>(filteredItems);
                                this.filteredAssets = new ArrayList<>(filteredItems);
                            } else {
                                this.allAssets.clear();
                                this.filteredAssets.clear();
                            }

                            if (filteredItems == null || filteredItems.isEmpty()) {
                                Label lbEmpty = new Label("Hiện tại chưa có sản phẩm nào thuộc danh mục này.");
                                lbEmpty.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-padding: 20;");
                                flitems.getChildren().add(lbEmpty);
                            } else {
                                // Nếu đang chọn sortPrice thì sort danh mục luôn
                                if (sortPrice.getValue() != null) {
                                    handleSortPrice();
                                } else {
                                    renderItems(this.filteredAssets);
                                }

                                if (DataSession.getInstance().getLoggedInUser() != null) {
                                    loadUserFavoritesInBackground(filteredItems);
                                }
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            flitems.getChildren().clear();
                            System.err.println("Server báo lỗi khi lấy dữ liệu danh mục!");
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        flitems.getChildren().clear();
                        System.err.println("Lỗi kết nối mạng: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleThisWeekClick(MouseEvent event) {
        loadItems();
        for (Node node : categoryBar.getChildren()) {
            if (node instanceof VBox) {
                node.setStyle("");
            }
        }
        vboxThisweek.setStyle(ACTIVE_CATEGORY_STYLE);

    }

    @FXML
    public void onItemClick(MouseEvent event) {
        ViewManager.switchScene(event, "item-view.fxml", "Chi tiết sản phẩm");
    }

    @FXML
    public void scrollRight() {
        if (scrollCategories != null) {
            double currentValue = scrollCategories.getHvalue();
            double newValue = currentValue + 0.2;

            if (newValue > 1.0) newValue = 1.0;

            scrollCategories.setHvalue(newValue);
            System.out.println("Đã bấm nút cuộn phải. Vị trí hiện tại: " + newValue);
        } else {
            System.out.println("Lỗi: scrollCategories đang bị null!");
        }
    }

    private void cleanupTimers() {
        for (AuctionTimerManager timer : activeCardTimers) {
            if (timer != null) {
                timer.stop();
            }
        }
        activeCardTimers.clear();
    }

    @Override
    public void cleanup() {
        cleanupTimers();
    }
}