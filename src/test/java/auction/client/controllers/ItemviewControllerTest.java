package auction.client.controllers;

import auction.client.session.DataSession;
import auction.common.model.items.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemviewControllerTest {
    private ItemviewController controller;

    @BeforeEach
    void setUp() {
        Item item=new Item();
        item.setImages(null);
        DataSession.getInstance().setSelectedItem(item);

        controller = new ItemviewController();
    }

    @Test
    @DisplayName("Kiểm tra toàn bộ các trường hợp lỗi và thành công của đặt giá")
    void testValidateBid() {
        // 1. Trường hợp để trống (Bạn đã viết)
        IllegalArgumentException error1 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("", 100, 1, 2, "USER"));
        assertEquals("Please enter the amount!", error1.getMessage());

        // 2. Trường hợp nhập chữ (Bạn đã viết)
        IllegalArgumentException error2 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("qwe", 100, 1, 2, "USER"));
        assertEquals("Please enter only the numbers.", error2.getMessage());

        // 5. Trường hợp đặt giá thấp hơn hoặc bằng giá hiện tại
        IllegalArgumentException error5 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("100", 100, 1, 2, "USER")); // bằng giá hiện tại
        assertEquals("The price to pay must be higher 100$", error5.getMessage());


    }

}