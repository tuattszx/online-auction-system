module vnu.uet.onlineauctionsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens vnu.uet.onlineauctionsystem to javafx.fxml;
    exports vnu.uet.onlineauctionsystem;
}