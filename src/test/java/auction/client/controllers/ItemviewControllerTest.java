package auction.client.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemviewControllerTest {
    private ItemviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ItemviewController();
    }

    @Test
    @DisplayName("Kiểm tra toàn bộ các trường hợp lỗi và thành công của đặt giá")
    void testValidateBid() {
        // 1. Trường hợp để trống (Bạn đã viết)
        IllegalArgumentException error1 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("", 100, 1, 2, "USER"));
        assertEquals("Vui lòng nhập số tiền!", error1.getMessage());

        // 2. Trường hợp nhập chữ (Bạn đã viết)
        IllegalArgumentException error2 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("qwe", 100, 1, 2, "USER"));
        assertEquals("Vui lòng chỉ nhập số!", error2.getMessage());

        // 3. Trường hợp người bán tự đấu giá đồ của mình
        IllegalArgumentException error3 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("200", 100, 5, 5, "USER")); // userId = sellerId = 5
        assertEquals("Admin hoặc người bán không thể đấu giá!", error3.getMessage());

        // 4. Trường hợp Admin cố tình đấu giá (thường Admin chỉ quản lý)
        IllegalArgumentException error4 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("200", 100, 1, 2, "ADMIN"));
        assertEquals("Admin hoặc người bán không thể đấu giá!", error4.getMessage());

        // 5. Trường hợp đặt giá thấp hơn hoặc bằng giá hiện tại
        IllegalArgumentException error5 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("100", 100, 1, 2, "USER")); // bằng giá hiện tại
        assertEquals("Giá trả phải lớn hơn 100$", error5.getMessage());

        IllegalArgumentException error6 = assertThrows(IllegalArgumentException.class, () ->
                controller.validateBid("50", 100, 1, 2, "USER")); // thấp hơn giá hiện tại
        assertEquals("Giá trả phải lớn hơn 100$", error6.getMessage());

        // 6. Trường hợp đặt giá hợp lệ (Không văng ra lỗi gì cả)
        assertDoesNotThrow(() ->
                        controller.validateBid("150", 100, 1, 2, "USER"),
                "Giá 150 cao hơn 100 và đúng role thì không được báo lỗi");
    }

}