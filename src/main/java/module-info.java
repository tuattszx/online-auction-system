module auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    // Cấp quyền cho JavaFX đọc các file Controller trong package mới
    opens auction.client.controllers to javafx.fxml;
    opens auction.view to javafx.fxml;

    // Xuất các package để hệ thống có thể chạy hàm main
    exports auction.client;
    exports auction.client.controllers;
//    exports auction.server;
//    exports auction.common;
}