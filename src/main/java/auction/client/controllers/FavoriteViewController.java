package auction.client.controllers;

import auction.client.session.DataSession;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class FavoriteViewController extends MainViewController {
    @FXML
    private FlowPane favoriteContainer; // Container trên file fxml của bạn

    public void initialize() {
        renderFavorites();
        // Lắng nghe: Cứ mỗi khi danh sách thay đổi thì vẽ lại UI
        DataSession.getInstance().getFavoriteItems().addListener((ListChangeListener<Item>) c -> {
            renderFavorites();
        });
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
    }

    private void renderFavorites() {
        favoriteContainer.getChildren().clear();
        List<Item> favorites = DataSession.getInstance().getFavoriteItems();

        for (Item item : favorites) {
            // Tận dụng lại hàm tạo Card mà bạn đã viết ở trên
            VBox card = createItemCard(item);
            favoriteContainer.getChildren().add(card);
        }
    }
}
