module onlineauctionsystem {
    requires javafx.controls;
    requires javafx.fxml;

    opens auction to javafx.fxml; // Rất quan trọng để load được Controller
    exports auction;
}