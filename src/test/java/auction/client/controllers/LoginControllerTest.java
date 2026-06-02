package auction.client.controllers;

import auction.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class LoginControllerTest {
    private LoginController loginController;

    @BeforeEach
    void setUp() {
        loginController = new LoginController();
    }

    @Test
    @DisplayName("Kiểm tra dữ liệu trống - Phải trả về false")
    void testValidateInput_Empty() {
        assertFalse(loginController.validateInput("", ""));
        assertFalse(loginController.validateInput("user", ""));
        assertFalse(loginController.validateInput(null, "123"));
    }

    @Test
    @DisplayName("Kiểm tra dữ liệu hợp lệ - Phải trả về true")
    void testValidateInput_Valid() {
        assertTrue(loginController.validateInput("admin", "password123"));
    }


    @Test
    @DisplayName("Xử lý khi Server trả về null (Offline)")
    void testProcessResponse_Offline() {
        String status = loginController.handleLoginResponse(null);
        assertEquals("ERROR", status);
    }
}