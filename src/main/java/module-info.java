module auction {
    // Các module cơ bản của JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base; // Cần thiết để nhận diện fx:collections

    requires java.sql;
    requires java.desktop;

    // Các thư viện bên thứ ba (Đảm bảo đã khai báo trong pom.xml)
    requires org.controlsfx.controls;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    // Cho phép JavaFX truy cập vào Controller và View
    opens auction.client.controllers to javafx.fxml;
    opens auction.view to javafx.fxml;

    // Nếu bạn có dùng các class Model trong FXML, hãy mở thêm package đó:
    // opens auction.common.model to javafx.fxml;

    // Xuất các package cần thiết
    exports auction.client;
    exports auction.client.controllers;
    exports auction.client.session;
    opens auction.client.session to javafx.fxml;
}