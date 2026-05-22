package auction.client.services;

import auction.client.utils.ServerTimeSync;
import auction.common.model.items.Item;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class AuctionTimerManager {
    private final Item item;

    // UI components (có thể null)
    private Label lblDays, lblHours, lblMins, lblSecs;
    private Label lblStatus;
    private HBox statusContainer;
    private Circle dot;

    private Timeline timeline;
    private FadeTransition liveFade;

    // Constructor đầy đủ cho màn hình Chi tiết
    public AuctionTimerManager(Item item, Label lblStatus, HBox statusContainer, Circle dot,
                               Label d, Label h, Label m, Label s) {
        this.item = item;
        this.lblStatus = lblStatus;
        this.statusContainer = statusContainer;
        this.dot = dot;
        this.lblDays = d; this.lblHours = h; this.lblMins = m; this.lblSecs = s;
    }

    // Constructor rút gọn cho cái Card (chỉ cần hiện status)
    public AuctionTimerManager(Item item, Label lblStatus, HBox statusContainer, Circle dot) {
        this.item = item;
        this.lblStatus = lblStatus;
        this.statusContainer = statusContainer;
        this.dot = dot;
    }

    public void start() {
        // Mỗi giây chạy hàm tick một lần
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null) timeline.stop();
        if (liveFade != null) liveFade.stop();
    }

    // Đây là hàm xử lý chính
    public void tick() {
        LocalDateTime now = ServerTimeSync.getNow();
        LocalDateTime start = item.getStartTime();
        LocalDateTime end = item.getEndTime();

        java.time.Duration diff;

        // Kiểm tra xem các thành phần UI có tồn tại không trước khi dùng
        if (statusContainer != null) {
            statusContainer.getStyleClass().removeAll("status-upcoming", "status-live", "status-finished");
        }

        if (now.isBefore(start)) {
            // --- UPCOMING ---
            updateStatusUI("UPCOMING", "status-upcoming", Color.BLACK, false);
            diff = java.time.Duration.between(now, start);
        } else if (now.isAfter(end)) {
            // --- FINISHED ---
            updateStatusUI("FINISHED", "status-finished", Color.GRAY, false);
            item.setStatus("CLOSED");
            displayTime(0);
            stop();
            return;
        } else {
            // --- LIVE NOW ---
            updateStatusUI("LIVE NOW", "status-live", Color.GREEN, true);
            if (!"OPEN".equals(item.getStatus())) item.setStatus("OPEN");
            diff = java.time.Duration.between(now, end);
        }

        displayTime(diff.getSeconds());
    }

    private void updateStatusUI(String text, String styleClass, Color timeColor, boolean isLive) {
        if (lblStatus != null) lblStatus.setText(text);
        if (statusContainer != null) statusContainer.getStyleClass().add(styleClass);
        setCountdownColor(timeColor);

        if (isLive && dot != null) {
            startLiveAnimation();
        } else {
            if (liveFade != null) liveFade.stop();
            if (dot != null) dot.setOpacity(1.0);
        }
    }

    private void displayTime(long seconds) {
        if (lblDays == null) return; // Nếu không có label thì không hiện

        if (seconds <= 0) {
            lblDays.setText("00"); lblHours.setText("00"); lblMins.setText("00"); lblSecs.setText("00");
            return;
        }

        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        lblDays.setText(String.format("%02d", d));
        lblHours.setText(String.format("%02d", h));
        lblMins.setText(String.format("%02d", m));
        lblSecs.setText(String.format("%02d", s));
    }

    private void setCountdownColor(Color color) {
        if (lblDays != null) {
            lblDays.setTextFill(color); lblHours.setTextFill(color);
            lblMins.setTextFill(color); lblSecs.setTextFill(color);
        }
    }

    private void startLiveAnimation() {
        if (dot == null) return;
        if (liveFade == null) {
            liveFade = new FadeTransition(Duration.seconds(0.8), dot);
            liveFade.setFromValue(1.0);
            liveFade.setToValue(0.2);
            liveFade.setCycleCount(Animation.INDEFINITE);
            liveFade.setAutoReverse(true);
        }
        liveFade.play();
    }
}