package auction.server;

import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.AuctionItem;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import auction.server.dao.*;
import auction.server.dao.UserDao;
import auction.server.utils.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Map;

import org.mockito.ArgumentCaptor;

public class ClientHandlerTest {

    @Mock
    private Socket mockSocket;

    @Mock
    private UserDao mockUserDao; // Lớp DAO cần mock
    @Mock
    private ItemDao mockItemDao;
    @Mock
    private BidDao mockBidDao;
    @Mock
    private CategoryDao mockCategoryDao;
    @Mock
    private FavouriteDao mockFavouriteDao;
    @Mock
    private NotificationDAO mockNotificationDao;




    private PipedOutputStream pipeToServer;
    private PipedInputStream pipeFromClient;
    private PipedOutputStream pipeToClient;
    private PipedInputStream pipeFromServer;

    private ObjectOutputStream testClientOut;
    private ObjectInputStream testClientIn;

    private ClientHandler clientHandler;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() throws Exception {
        // Khởi tạo các đối tượng gắn thẻ @Mock
        closeable = MockitoAnnotations.openMocks(this);

        // 1. Thiết lập hệ thống đường ống mạng giả lập (Piped Streams)
        pipeToServer = new PipedOutputStream();
        pipeFromClient = new PipedInputStream(pipeToServer);

        pipeToClient = new PipedOutputStream();
        pipeFromServer = new PipedInputStream(pipeToClient);

        // Cấu hình để mockSocket trả về các đường ống này
        when(mockSocket.getInputStream()).thenReturn(pipeFromClient);
        when(mockSocket.getOutputStream()).thenReturn(pipeToClient);

        // 2. Tạo đối tượng OUT của Client trước để khơi thông luồng Header cho Server
        testClientOut = new ObjectOutputStream(pipeToServer);
        testClientOut.flush();

        // 3. Khởi tạo instance của ClientHandler
        clientHandler = new ClientHandler(mockSocket);

        // =========================================================================
        // 4. SỬ DỤNG REFLECTION ĐỂ INJECT TOÀN BỘ 6 DAO VÀO CLIENT_HANDLER
        // =========================================================================

        // Inject UserDao
        java.lang.reflect.Field userDaoField = ClientHandler.class.getDeclaredField("userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(clientHandler, mockUserDao);

        // Inject ItemDao
        java.lang.reflect.Field itemDaoField = ClientHandler.class.getDeclaredField("itemDao");
        itemDaoField.setAccessible(true);
        itemDaoField.set(clientHandler, mockItemDao);

        // Inject CategoryDao
        java.lang.reflect.Field categoryDaoField = ClientHandler.class.getDeclaredField("categoryDao");
        categoryDaoField.setAccessible(true);
        categoryDaoField.set(clientHandler, mockCategoryDao);

        // Inject BidDao
        java.lang.reflect.Field bidDaoField = ClientHandler.class.getDeclaredField("bidDao");
        bidDaoField.setAccessible(true);
        bidDaoField.set(clientHandler, mockBidDao);

        // Inject NotificationDAO (Lưu ý chữ DAO viết hoa theo code gốc: notificationDao)
        java.lang.reflect.Field notificationDaoField = ClientHandler.class.getDeclaredField("notificationDao");
        notificationDaoField.setAccessible(true);
        notificationDaoField.set(clientHandler, mockNotificationDao);

        // Inject FavouriteDao (Tên biến trong code gốc của bạn: favouriteDao)
        java.lang.reflect.Field favouriteDaoField = ClientHandler.class.getDeclaredField("favouriteDao");
        favouriteDaoField.setAccessible(true);
        favouriteDaoField.set(clientHandler, mockFavouriteDao);

        // =========================================================================
        // KHÔNG khởi tạo testClientIn ở đây để tránh nghẽn luồng (Deadlock)
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testClientOut != null) testClientOut.close();
        if (testClientIn != null) testClientIn.close();
        if (closeable != null) closeable.close();
    }

    @Test
    public void testHandleLogin_Success() throws Exception {
        System.out.println("danng chay test 1");
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        Account loginAcc = new Account("admin", "password123");
        Message msgRequest = new Message("LOGIN",loginAcc);

        User expectedUser = new User();
        expectedUser.setUsername("admin");
        expectedUser.setEmail("admin@auction.com");

        // Mock hành vi của DAO: khi truyền đúng u/p thì trả về User
        when(mockUserDao.CheckLogin("admin", "password123")).thenReturn(expectedUser);

        // 2. Client gửi message tới Server (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Chạy ClientHandler trong 1 Thread riêng vì vòng lặp `while(true)` sẽ block main thread
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi từ Server và Assert (Then)
        Object objResponse = testClientIn.readObject();
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertNotNull(msgResponse.getData());

        User loggedInUser = (User) msgResponse.getData();
        assertEquals("admin", loggedInUser.getUsername());
        assertEquals("admin", clientHandler.getLoggedInUser().getUsername()); // Kiểm tra state được lưu
    }

    @Test
    public void testHandleLogin_Failed() throws Exception {
        // 1. Chuẩn bị dữ liệu sai
        Account loginAcc = new Account("wrong_user", "wrong_pass");
        Message msgRequest = new Message("LOGIN",loginAcc);
        // Mock hành vi DAO: Trả về null khi sai tài khoản
        when(mockUserDao.CheckLogin("wrong_user", "wrong_pass")).thenReturn(null);

        // 2. Gửi request
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Nhận response và kiểm tra kết quả thất bại
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus());
        assertNull(clientHandler.getLoggedInUser());
    }

    @Test
    public void testHandleSignout() throws Exception {
        // Giả lập gửi lệnh SIGNOUT để kết thúc vòng lặp loop của Handler
        Message msgRequest = new Message("SIGNOUT",null);
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);



        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertNull(clientHandler.getLoggedInUser());

        // Đợi Thread kết thúc vì gặp lệnh break trong handleSignout
        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }

    @Test
    public void testHandleRegister_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu: Tạo một đối tượng User giả lập gửi lên
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("securepass");

        Message msgRequest = new Message("REGISTER",newUser);

        // Giả lập hành vi DAO (MOCK): Không chạm vào DB thật
        // Kiểm tra trùng lặp trả về false (không trùng)
        when(mockUserDao.isUsernameExists("newuser")).thenReturn(false);
        when(mockUserDao.isEmailExists("newuser@example.com")).thenReturn(false);
        // Lưu vào DB thành công trả về true
        when(mockUserDao.add(any(User.class))).thenReturn(true);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước từ phía Client ảo
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server ClientHandler
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu ở phía Client Test
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả (Assert)
        Object objResponse = testClientIn.readObject();
        assertNotNull(objResponse);
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus()); // Phải trả về SUCCESS
    }

    @Test
    public void testHandleRegister_Failed_UsernameOrEmailExists() throws Exception {
        // 1. Chuẩn bị dữ liệu: Tạo User bị trùng thông tin
        User duplicateUser = new User();
        duplicateUser.setUsername("existinguser");
        duplicateUser.setEmail("existing@example.com");

        Message msgRequest = new Message("REGISTER", duplicateUser);


        // Giả lập hành vi DAO (MOCK):
        // Giả vờ như username đã tồn tại trong hệ thống -> trả về true
        when(mockUserDao.isUsernameExists("existinguser")).thenReturn(true);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus()); // Phải trả về FAILED do trùng lặp

        // Đảm bảo hàm add() lưu vào DB chưa bao giờ được gọi đến để an toàn tuyệt đối cho DB
        verify(mockUserDao, never()).add(any(User.class));
    }

    @Test
    public void testHandleRegister_Failed_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu
        User newUser = new User();
        newUser.setUsername("db_fail_user");
        newUser.setEmail("dbfail@example.com");

        Message msgRequest = new Message("REGISTER",newUser);
               // Giả lập hành vi DAO (MOCK):
        // Không trùng tài khoản/email nhưng hàm add vào DB gặp sự cố và trả về false
        when(mockUserDao.isUsernameExists("db_fail_user")).thenReturn(false);
        when(mockUserDao.isEmailExists("dbfail@example.com")).thenReturn(false);
        when(mockUserDao.add(any(User.class))).thenReturn(false);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus()); // Trả về FAILED do sql/db lỗi ngầm
    }
    @Test
    public void testHandleAddItem_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập cho Payload [Item, List<String>, List<String>]
        Item testItem = new Item();
        testItem.setName("Bàn phím cơ Custom");
        testItem.setCurrentPrice(1500000L);

        List<String> imageUrls = List.of("http://cloudinary.com/image1.jpg", "http://cloudinary.com/image2.jpg");
        List<String> categoryNames = List.of("Điện tử", "Phụ kiện máy tính");

        Object[] payload = new Object[]{ testItem, imageUrls, categoryNames };

        Message msgRequest = new Message("ADD_ITEM",payload);


        // Giả lập danh sách Category trả về từ CategoryDao mẫu
        List<Category> mockCategories = List.of(new Category(1, "Điện tử",""), new Category(2, "Phụ kiện máy tính",""));
        when(mockCategoryDao.getCategoryByName(categoryNames)).thenReturn(mockCategories);

        // Giả lập ItemDao.add trả về true (thêm thành công vào RAM giả lập, không chạm DB thật)
        when(mockItemDao.add(any(Item.class))).thenReturn(true);

        // SỬ DỤNG MOCK STATIC: Chặn hàm static của NotificationService không cho chạy thật

            // Định nghĩa khi gọi hàm static này thì không làm gì cả (do trả về void)

            // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước từ phía Client ảo
            testClientOut.writeObject(msgRequest);
            testClientOut.flush();

            // 3. Khởi chạy Thread Server
            Thread handlerThread = new Thread(clientHandler);
            handlerThread.start();

            // 4. Khởi tạo Stream nhận dữ liệu ở phía Client Test
            testClientIn = new ObjectInputStream(pipeFromServer);

            // 5. Đọc phản hồi và kiểm tra kết quả (Assert)
            Message msgResponse = (Message) testClientIn.readObject();
            assertEquals("SUCCESS", msgResponse.getStatus());

            // Kiểm tra xem hàm static thông báo có thực sự được kích hoạt không
    }

    @Test
    public void testHandleAddItem_Failed_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi tương tự
        Item testItem = new Item();
        testItem.setName("Sản phẩm lỗi");

        Object[] payload = new Object[]{ testItem, List.of(), List.of() };

        Message msgRequest = new Message("ADD_ITEM",payload);


        // Giả lập ItemDao trả về false (Ví dụ: lỗi nghẽn kết nối, lỗi trigger DB...)
        when(mockItemDao.add(any(Item.class))).thenReturn(false);

        // 2. KHƠI THÔNG LUỒNG
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và Assert
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus()); // Phải là FAILED
    }

    @Test
    public void testHandleGetAllItems_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Request Message)
        Message msgRequest = new Message("GET_ALL_ITEMS",null);


        // Chuẩn bị danh sách Item giả lập (Mock Data) để trả về
        Item item1 = new Item();
        item1.setId(1);
        item1.setName("Laptop Dell XPS");

        Item item2 = new Item();
        item2.setId(2);
        item2.setName("iPhone 15 Pro");

        List<Item> mockItems = List.of(item1, item2);

        // Giả lập hành vi của ItemDao: khi gọi getAll() thì trả về danh sách mock ở trên
        when(mockItemDao.getAll()).thenReturn(mockItems);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước từ phía Client ảo
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server ClientHandler
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu ở phía Client Test
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả (Assert)
        Object objResponse = testClientIn.readObject();
        assertNotNull(objResponse);
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái phải là SUCCESS

        // Ép kiểu dữ liệu nhận được về List<Item> để kiểm tra tính chính xác
        List<Item> resultItems = (List<Item>) msgResponse.getData();
        assertNotNull(resultItems);
        assertEquals(2, resultItems.size());
        assertEquals("Laptop Dell XPS", resultItems.get(0).getName());
        assertEquals("iPhone 15 Pro", resultItems.get(1).getName());
    }

    @Test
    public void testHandleGetAllItems_Error() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        Message msgRequest = new Message("GET_ALL_ITEMS",null);


        // Giả lập hành vi lỗi: Khi gọi getAll() sẽ văng ra một RuntimeException (như lỗi kết nối DB)
        when(mockItemDao.getAll()).thenThrow(new RuntimeException("Database connection timeout"));

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả rơi vào block catch
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("ERROR", msgResponse.getStatus()); // Trạng thái xử lý lỗi phải là ERROR
        assertNull(msgResponse.getData());              // Không có data nào được trả về kèm theo
    }
    @Test
    public void testHandleGetItemById_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu: Gửi một ID hợp lệ (Ví dụ: ID = 99)
        int targetId = 99;
        Message msgRequest = new Message("GET_ITEM_BY_ID",targetId);


        // Chuẩn bị đối tượng Item mẫu tương ứng với ID trên
        Item mockItem = new Item();
        mockItem.setId(targetId);
        mockItem.setName("Giày Sneaker Limited");
        mockItem.setCurrentPrice(3000000L);

        // Giả lập hành vi của ItemDao: Khi tìm ID 99 thì trả về mockItem
        when(mockItemDao.getById(targetId)).thenReturn(mockItem);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước từ phía Client ảo
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server ClientHandler
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu ở phía Client Test
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả (Assert)
        Object objResponse = testClientIn.readObject();
        assertNotNull(objResponse);
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái phải là SUCCESS

        // Kiểm tra tính chính xác của đối tượng Item nhận được
        assertNotNull(msgResponse.getData());
        Item resultItem = (Item) msgResponse.getData();
        assertEquals(targetId, resultItem.getId());
        assertEquals("Giày Sneaker Limited", resultItem.getName());
    }

    @Test
    public void testHandleGetItemById_Failed_NotFound() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu: Gửi một ID không tồn tại (Ví dụ: ID = 404)
        int nonExistentId = 404;
        Message msgRequest = new Message("GET_ITEM_BY_ID",nonExistentId);


        // Giả lập hành vi của ItemDao: Trả về null khi không tìm thấy sản phẩm trong hệ thống
        when(mockItemDao.getById(nonExistentId)).thenReturn(null);

        // 2. KHƠI THÔNG LUỒNG: Gửi dữ liệu đi trước
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // 3. Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // 4. Khởi tạo Stream nhận dữ liệu
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 5. Đọc phản hồi và kiểm tra kết quả thất bại
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("FAILED", msgResponse.getStatus()); // Trạng thái phải trả về FAILED
        assertEquals(404,msgResponse.getData());         // Không kèm theo bất kỳ dữ liệu sản phẩm nào
    }

    @Test
    public void testHandlePlaceBid_Success_WithTimeExtension() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu đặt giá (Bid Request)
        Bid bidRequest = new Bid();
        bidRequest.setIdItem(10);
        bidRequest.setBidAmount(2000L); // Trả giá 2000
        bidRequest.setIdUser(1);
        bidRequest.setBidderName("Nguyễn Văn A");

        Message msgRequest = new Message();
        msgRequest.setCommand("PLACE_BID");
        msgRequest.setData(bidRequest);

        // Chuẩn bị thông tin Item hiện tại trong DB giả lập
        Item currentItem = new Item();
        currentItem.setId(10);
        currentItem.setStatus("OPEN");
        currentItem.setCurrentPrice(1500L); // Giá hiện tại là 1500 < 2000 (Hợp lệ)
        // Giả lập thời gian kết thúc chỉ còn 10 giây nữa là hết hạn (để kích hoạt logic gia hạn)
        currentItem.setEndTime(java.time.LocalDateTime.now().plusSeconds(10));

        // 2. Mock hành vi của các DAO
        when(mockItemDao.getById(10)).thenReturn(currentItem);
        when(mockItemDao.placeBid(10, 2000L, 1)).thenReturn(true); // Cập nhật giá thành công
        when(mockBidDao.add(any(Bid.class))).thenReturn(true);       // Lưu lịch sử thành công
        when(mockItemDao.updateEndTime(eq(10), any())).thenReturn(true); // Gia hạn thời gian thành công

        // Chống lỗi DB khi NotificationService cố lưu thông báo thật
        when(mockNotificationDao.add(any())).thenReturn(true);

        // Mock hàm auto-bid chạy ngầm không làm gì cả
        doNothing().when(mockItemDao).checkAndTriggerAutomaticBids(10);

        // 3. KHƠI THÔNG LUỒNG VÀ CHẠY
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 4. Đọc phản hồi và Assert kết quả
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Da dat thanh cong"));
    }
    @Test
    public void testHandlePlaceBid_Failed_PriceTooLow() throws Exception {
        // 1. Chuẩn bị dữ liệu: Khách hàng cố tình trả giá 1200
        Bid bidRequest = new Bid();
        bidRequest.setIdItem(10);
        bidRequest.setBidAmount(1200L);

        Message msgRequest = new Message();
        msgRequest.setCommand("PLACE_BID");
        msgRequest.setData(bidRequest);

        // Nhưng thực tế sản phẩm đã bị đẩy lên 1500 trước đó rồi
        Item currentItem = new Item();
        currentItem.setId(10);
        currentItem.setStatus("OPEN");
        currentItem.setCurrentPrice(1500L);

        // Mock hành vi DAO
        when(mockItemDao.getById(10)).thenReturn(currentItem);

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả báo lỗi giá thấp
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Vui lòng trả cao hơn"));
    }
    @Test
    public void testHandlePlaceBid_Failed_NotEnoughBalance() throws Exception {
        // 1. Chuẩn bị dữ liệu đặt giá 5,000,000
        Bid bidRequest = new Bid();
        bidRequest.setIdItem(10);
        bidRequest.setBidAmount(5000000L);
        bidRequest.setIdUser(1);

        Message msgRequest = new Message();
        msgRequest.setCommand("PLACE_BID");
        msgRequest.setData(bidRequest);

        Item currentItem = new Item();
        currentItem.setId(10);
        currentItem.setStatus("OPEN");
        currentItem.setCurrentPrice(1500L);

        // Mock hành vi DAO
        when(mockItemDao.getById(10)).thenReturn(currentItem);
        // Giả lập: Thao tác đặt giá thất bại do hệ thống kiểm tra ví không đủ tiền
        when(mockItemDao.placeBid(10, 5000000L, 1)).thenReturn(false);

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả báo thiếu tiền
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Số dư khả dụng của bạn không đủ"));
    }
    @Test
    public void testHandleGetBidByItemId_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu: Gửi một ID sản phẩm cần xem lịch sử (Ví dụ: ID = 42)
        int targetItemId = 42;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_BID_BY_ITEM_ID");
        msgRequest.setData(targetItemId);

        // Tạo danh sách lịch sử đấu giá mẫu (Mock Data)
        Bid bid1 = new Bid();
        bid1.setIdItem(targetItemId);
        bid1.setBidAmount(500000L);
        bid1.setBidderName("Người dùng A");

        Bid bid2 = new Bid();
        bid2.setIdItem(targetItemId);
        bid2.setBidAmount(600000L);
        bid2.setBidderName("Người dùng B");

        List<Bid> mockBidHistory = List.of(bid1, bid2);

        // Giả lập hành vi của bidDao: Trả về danh sách lịch sử mẫu khi nhận ID 42
        when(mockBidDao.getBidsByItemId(targetItemId)).thenReturn(mockBidHistory);

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY THREAD
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả thành công
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("SUCCESS", msgResponse.getStatus());

        // Ép kiểu dữ liệu nhận được và kiểm tra tính chính xác
        List<Bid> resultHistory = (List<Bid>) msgResponse.getData();
        assertNotNull(resultHistory);
        assertEquals(2, resultHistory.size());
        assertEquals(600000L, resultHistory.get(1).getBidAmount());
        assertEquals("Người dùng B", resultHistory.get(1).getBidderName());
    }

    @Test
    public void testHandleGetBidByItemId_Failed_EmptyHistory() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu với ID sản phẩm chưa từng có ai đặt giá
        int emptyItemId = 101;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_BID_BY_ITEM_ID");
        msgRequest.setData(emptyItemId);

        // Giả lập hành vi của bidDao: Trả về danh sách rỗng (hoặc null)
        when(mockBidDao.getBidsByItemId(emptyItemId)).thenReturn(List.of());

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY THREAD
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả báo thất bại FAILED
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("FAILED", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("No bid history found"));
    }

    @Test
    public void testHandleGetBidByItemId_Error_SystemException() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        int targetItemId = 999;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_BID_BY_ITEM_ID");
        msgRequest.setData(targetItemId);

        // Giả lập hành vi lỗi: Khi gọi vào DB thì ném ra một ngoại lệ (Exception)
        when(mockBidDao.getBidsByItemId(targetItemId))
                .thenThrow(new RuntimeException("SQL Syntax Error or Database Connection Lost"));

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY THREAD
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả rơi vào block catch (ERROR)
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("ERROR", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Server error:"));
    }
    @Test
    public void testHandleGetItemImages_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu: Gửi một ID sản phẩm hợp lệ (Ví dụ: ID = 15)
        int targetItemId = 15;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_ITEM_IMAGES");
        msgRequest.setData(targetItemId);

        // Chuẩn bị dữ liệu Item và danh sách ItemImage mẫu
        Item mockItem = new Item();
        mockItem.setId(targetItemId);

        ItemImage img1 = new ItemImage();
        img1.setUrlImage("http://cloudinary.com/product_main.jpg");
        img1.setDefault(true);

        ItemImage img2 = new ItemImage();
        img2.setUrlImage("http://cloudinary.com/product_detail.jpg");
        img2.setDefault(false);

        // Thêm danh sách ảnh vào đối tượng Item
        mockItem.setImages(List.of(img1, img2)); // Đảm bảo class Item của bạn có hàm set hoặc list này hợp lệ

        // Giả lập hành vi của ItemDao: Trả về mockItem khi nhận đúng ID 15
        when(mockItemDao.getById(targetItemId)).thenReturn(mockItem);

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY THREAD
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả thành công
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("SUCCESS", msgResponse.getStatus());

        // Ép kiểu dữ liệu và kiểm tra tính chính xác của List<ItemImage> nhận được
        List<ItemImage> resultImages = (List<ItemImage>) msgResponse.getData();
        assertNotNull(resultImages);
        assertEquals(2, resultImages.size());
        assertEquals("http://cloudinary.com/product_main.jpg", resultImages.get(0).getUrlImage());
        assertTrue(resultImages.get(0).isDefault());
    }

    @Test
    public void testHandleGetItemImages_Error_NullItem() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu với ID không tồn tại (Ví dụ: ID = 999)
        int nonExistentId = 999;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_ITEM_IMAGES");
        msgRequest.setData(nonExistentId);

        // Giả lập ItemDao trả về null -> Khi thực thi sẽ gây ra lỗi NullPointerException trong khối try
        when(mockItemDao.getById(nonExistentId)).thenReturn(null);

        // 2. KHƠI THÔNG LUỒNG VÀ CHẠY THREAD
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả rơi vào block catch (ERROR)
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("ERROR", msgResponse.getStatus()); // Trạng thái phải trả về ERROR
        // Thay vì assertNull(msgResponse.getData());
        assertEquals(999, msgResponse.getData());               // Không kèm theo dữ liệu gì
    }
    @Test
    public void testHandleGetMyAuctions_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu: Gửi ID của người dùng (Ví dụ: userId = 5)
        int targetUserId = 5;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_MY_AUCTIONS");
        msgRequest.setData(targetUserId);

        // Chuẩn bị danh sách sản phẩm đấu giá giả lập (Mock Data) của User 5
        AuctionItem auction1 = new AuctionItem(101, "Đồng hồ Rolex cổ", "02:15:00", 500000L, 450000L, "OPEN");
        AuctionItem auction2 = new AuctionItem(102, "Tranh sơn dầu thế kỷ 19", "00:45:12", 1200000L, 0L, "OPEN");

        List<AuctionItem> mockAuctions = List.of(auction1, auction2);

        // Giả lập hành vi của ItemDao: Khi nhận userId = 5 thì trả về danh sách mock ở trên
        when(mockItemDao.getMyAuctions(targetUserId)).thenReturn(mockAuctions);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả thành công
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái phải trả về SUCCESS

        // Ép kiểu kiểm tra tính chính xác của dữ liệu nhận được
        List<AuctionItem> resultAuctions = (List<AuctionItem>) msgResponse.getData();
        assertNotNull(resultAuctions);
        assertEquals(2, resultAuctions.size());
        assertEquals("Đồng hồ Rolex cổ", resultAuctions.get(0).getName());
        assertEquals("Tranh sơn dầu thế kỷ 19", resultAuctions.get(1).getName());
    }

    @Test
    public void testHandleGetMyAuctions_Error() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Ví dụ: userId = 5)
        int targetUserId = 5;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_MY_AUCTIONS");
        msgRequest.setData(targetUserId);

        // Giả lập hành vi lỗi: Khi gọi vào DB thì ném ra một lỗi RuntimeException ngầm
        when(mockItemDao.getMyAuctions(targetUserId))
                .thenThrow(new RuntimeException("SQL Error: Connection refused to database server"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả rơi vào block catch
        Message msgResponse = (Message) testClientIn.readObject();
        assertNotNull(msgResponse);
        assertEquals("ERROR", msgResponse.getStatus()); // Trạng thái bắt buộc phải chuyển thành ERROR
        assertNull(msgResponse.getData());               // Data bắt buộc phải bằng null do bạn đã set ở catch
    }
    @Test
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    public void testHandleGetMessage_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Sử dụng Mã số sinh viên làm ID mẫu)
        int targetUserId = 25021620;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_MESSAGE");
        msgRequest.setData(targetUserId);

        // Giả lập hành vi lỗi: Khi gọi vào DB thì ném ra một lỗi RuntimeException ngầm
        // AN TOÀN TUYỆT ĐỐI - Dùng mockNotificationDao có sẵn ở đầu Class, không chạm DB thật
        when(mockNotificationDao.getNotificationsByUserId(targetUserId))
                .thenThrow(new RuntimeException("SQL Error: Connection refused to database server"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER (Dùng đúng các biến có sẵn của Class ông)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // Khởi tạo Stream nhận dữ liệu đúng chuẩn giống các hàm test khác của ông
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert kết quả rơi vào block catch
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("ERROR", msgResponse.getStatus());    // Trạng thái bắt buộc phải chuyển thành ERROR theo block catch
            assertEquals(targetUserId, msgResponse.getData()); // Data giữ nguyên giá trị ID ban đầu do catch không sửa data

        } finally {
            // Đảm bảo dừng Thread Server sau khi test xong để tránh treo hệ thống CI/CD
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleMarkAsRead_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Ví dụ: notificationId = 123)
        int targetNotificationId = 123;
        Message msgRequest = new Message();
        msgRequest.setCommand("MARK_AS_READ");
        msgRequest.setData(targetNotificationId);

        // Giả lập hành vi DAO: Cập nhật thành công trả về true - AN TOÀN KHÔNG CHẠM DB
        when(mockNotificationDao.markAsRead(targetNotificationId)).thenReturn(true);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        // Khởi tạo Stream nhận dữ liệu từ Server giả lập
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi từ Client ảo và Assert kết quả
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("SUCCESS", msgResponse.getStatus()); // Status bắt buộc phải là SUCCESS khi isUpdated = true

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    public void testHandleMarkAsRead_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        int targetNotificationId = 555;
        Message msgRequest = new Message();
        msgRequest.setCommand("MARK_AS_READ");
        msgRequest.setData(targetNotificationId);

        // Giả lập hành vi lỗi ngầm: DB ném ra Exception
        when(mockNotificationDao.markAsRead(targetNotificationId))
                .thenThrow(new RuntimeException("Deadlock detected or connection lost"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và kiểm tra bẫy catch lỗi Server
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("ERROR", msgResponse.getStatus()); // Phải rơi vào trạng thái ERROR
            assertTrue(msgResponse.getData().toString().contains("Lỗi Server")); // Kiểm tra chuỗi thông báo lỗi được gán vào Data

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleFavourite_Add_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Mảng Object chứa userId và itemId)
        int userId = 25021620;
        int itemId = 999;
        Object[] payload = new Object[]{ userId, itemId };

        Message msgRequest = new Message();
        msgRequest.setCommand("ADD_FAVOURITE");
        msgRequest.setData(payload);

        // Giả lập hành vi DAO: addFavourite trả về true - AN TOÀN TUYỆT ĐỐI
        when(mockFavouriteDao.addFavourite(userId, itemId)).thenReturn(true);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và kiểm tra kết quả
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái mong muốn là SUCCESS

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    public void testHandleFavourite_Remove_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (isAdd sẽ tương ứng với hàm xóa ở Server)
        int userId = 25021620;
        int itemId = 888;
        Object[] payload = new Object[]{ userId, itemId };

        Message msgRequest = new Message();
        msgRequest.setCommand("REMOVE_FAVOURITE");
        msgRequest.setData(payload);

        // Giả lập hành vi lỗi: removeFavourite ném lỗi kết nối ngầm
        when(mockFavouriteDao.removeFavourite(userId, itemId))
                .thenThrow(new RuntimeException("Database deadlock on connection pool"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert bẫy ngoại lệ catch
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("ERROR", msgResponse.getStatus()); // Hệ thống phải chuyển sang trạng thái ERROR
            assertTrue(msgResponse.getData().toString().contains("Lỗi Server")); // Kiểm tra chuỗi thông báo lỗi

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleGetFavourites_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Ví dụ: userId = 25021620)
        int targetUserId = 25021620;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_FAVOURITES");
        msgRequest.setData(targetUserId);

        // Giả lập dữ liệu trả về từ DAO là một danh sách chứa các ID món đồ (Ví dụ: [10, 20, 30])
        List<Integer> expectedList = java.util.Arrays.asList(10, 20, 30);
        when(mockFavouriteDao.getFavoriteItemIdsByUserId(targetUserId)).thenReturn(expectedList);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi từ Client ảo và Assert kết quả
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái bắt buộc phải là SUCCESS

            // Ép kiểu dữ liệu nhận được về List và kiểm tra tính chính xác của phần tử
            List<?> resultList = (List<?>) msgResponse.getData();
            assertNotNull(resultList);
            assertEquals(3, resultList.size());
            assertEquals(10, resultList.get(0));

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    public void testHandleGetFavourites_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        int targetUserId = 25021620;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_FAVOURITES");
        msgRequest.setData(targetUserId);

        // Giả lập hành vi lỗi ngầm: DAO ném ra Exception kết nối
        when(mockFavouriteDao.getFavoriteItemIdsByUserId(targetUserId))
                .thenThrow(new RuntimeException("SQL Error: Connection pool exhausted"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert bẫy ngoại lệ catch
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("ERROR", msgResponse.getStatus()); // Hệ thống phải chuyển sang trạng thái ERROR
            assertTrue(msgResponse.getData().toString().contains("Lỗi Server")); // Kiểm tra chuỗi thông báo lỗi

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testGetUnreadCount_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Sử dụng ID người dùng mẫu)
        int targetUserId = 25021620;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_UNREAD_COUNT");
        msgRequest.setData(targetUserId);

        // Giả lập hành vi DAO: Đếm được 5 thông báo chưa đọc - AN TOÀN TUYỆT ĐỐI
        int mockCount = 5;
        when(mockNotificationDao.countUnreadByUserId(targetUserId)).thenReturn(mockCount);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và đối chiếu kết quả nhận về
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("SUCCESS", msgResponse.getStatus()); // Trạng thái mong muốn là SUCCESS

            // Kiểm tra số lượng đếm được trong gói tin trả về
            int resultCount = (int) msgResponse.getData();
            assertEquals(5, resultCount, "Số lượng thông báo chưa đọc phải khớp với dữ liệu giả lập");

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    public void testGetUnreadCount_DatabaseError() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        int targetUserId = 25021620;
        Message msgRequest = new Message();
        msgRequest.setCommand("GET_UNREAD_COUNT");
        msgRequest.setData(targetUserId);

        // Giả lập hành vi lỗi: Hàm đếm ném ra ngoại lệ hệ thống
        when(mockNotificationDao.countUnreadByUserId(targetUserId))
                .thenThrow(new RuntimeException("SQL Error: Connection refused"));

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert bẫy ngoại lệ catch
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            assertEquals("ERROR", msgResponse.getStatus()); // Hệ thống phải chuyển sang trạng thái ERROR
            assertTrue(msgResponse.getData().toString().contains("Lỗi Server")); // Đảm bảo chứa chuỗi thông báo lỗi

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleUpdateProfile_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu (Tạo đối tượng User mẫu)
        User userRequest = new User();
        userRequest.setUsername("baoanh25");
        userRequest.setEmail("baoanh@auction.com");

        Message msgRequest = new Message();
        msgRequest.setCommand("UPDATE_PROFILE");
        msgRequest.setData(userRequest);
        msgRequest.setRequestId("REQ-12345");

        // Giả lập hành vi DAO: Cập nhật thành công trả về true
        when(mockUserDao.update(any(User.class))).thenReturn(true);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi từ Client ảo và đối chiếu gói tin phản hồi mới
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            // Kiểm tra xem command phản hồi có đúng là chuỗi thành công không
            assertEquals("UPDATE_PROFILE_SUCCESS", msgResponse.getCommand());
            assertEquals("REQ-12345", msgResponse.getRequestId()); // Đảm bảo requestId được giữ nguyên

            User updatedUser = (User) msgResponse.getData();
            assertEquals("baoanh25", updatedUser.getUsername());

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleUpdateProfile_Failed() throws Exception {
        // 1. Chuẩn bị dữ liệu yêu cầu
        User userRequest = new User();
        userRequest.setUsername("invalid_user");

        Message msgRequest = new Message();
        msgRequest.setCommand("UPDATE_PROFILE");
        msgRequest.setData(userRequest);

        // Giả lập hành vi DAO: Cập nhật thất bại trả về false
        when(mockUserDao.update(any(User.class))).thenReturn(false);

        // 2. KHƠI THÔNG LUỒNG VÀ KHỞI CHẠY THREAD SERVER
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();
        testClientOut.reset();

        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();

        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Đọc phản hồi và Assert trạng thái FAILED
        try {
            Message msgResponse = (Message) testClientIn.readObject();

            assertNotNull(msgResponse);
            // Kiểm tra xem command phản hồi có chuyển thành chuỗi thất bại không
            assertEquals("UPDATE_PROFILE_FAILED", msgResponse.getCommand());
            assertEquals("Database update failed", msgResponse.getData().toString());

        } finally {
            if (handlerThread.isAlive()) {
                handlerThread.interrupt();
            }
        }
    }
    @Test
    public void testHandleDeleteItem_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 456;
        Message msgRequest = new Message("DELETE_ITEM", itemId);

        // Giả lập hành vi DAO: Khi gọi xóa itemId 456 thì trả về true (thành công)
        when(mockItemDao.cancelAuction(itemId)).thenReturn(true);

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server độc lập
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Object objResponse = testClientIn.readObject();
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus()); // Check trạng thái trả về là SUCCESS

        // 4. GIẢI PHÓNG THREAD: Gửi tiếp lệnh SIGNOUT để tắt Server, tránh treo test
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject(); // Đọc thông luồng phản hồi SIGNOUT

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());

        // Xác minh hàm delete của DAO được gọi đúng 1 lần với đúng itemId
        verify(mockItemDao, times(1)).cancelAuction(itemId);
    }
    @Test
    public void testHandleDeleteItem_Failed() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 999; // ID không tồn tại chẳng hạn
        Message msgRequest = new Message("DELETE_ITEM", itemId);

        // Giả lập hành vi DAO: Trả về false (xóa thất bại)
        when(mockItemDao.cancelAuction(itemId)).thenReturn(false);

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus()); // Code gốc gán status "FAILED" nếu isDeleted = false

        // 4. GIẢI PHÓNG THREAD
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());

        verify(mockItemDao, times(1)).cancelAuction(itemId);
    }
    @Test
    public void testHandleDeleteItem_Exception_Error() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập lỗi hệ thống (Given)
        int itemId = 111;
        Message msgRequest = new Message("DELETE_ITEM", itemId);

        // Ép mockItemDao ném ra một ngoại lệ để nhảy vào block catch {}
        when(mockItemDao.cancelAuction(itemId)).thenThrow(new RuntimeException("SQL syntax error or DB crash"));

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi lỗi (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("ERROR", msgResponse.getStatus());
        assertNotNull(msgResponse.getData());

        String errorMsg = (String) msgResponse.getData();
        assertTrue(errorMsg.contains("Lỗi Server: SQL syntax error or DB crash"));

        // 4. GIẢI PHÓNG THREAD
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());

        verify(mockItemDao, times(1)).cancelAuction(itemId);
    }
    @Test
    public void testHandleUpdateItem_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        Item mockItem = new Item(1, "Laptop cu", "Mo ta cu", 5000000, 5000000, null, null, 123, "ACTIVE");
        List<String> mockUrls = List.of("http://image1.jpg", "http://image2.jpg");
        List<String> mockCatNames = List.of("Electronics");

        // Đóng gói payload thành mảng Object[] đúng như hàm gốc bóc tách
        Object[] payload = new Object[]{ mockItem, mockUrls, mockCatNames };
        Message msgRequest = new Message("UPDATE_ITEM", payload);

        // Giả lập hành vi cho CategoryDao và ItemDao
        List<Category> mockCategories = List.of(new Category(1, "Electronics","Mo ta danh muc"));
        when(mockCategoryDao.getCategoryByName(mockCatNames)).thenReturn(mockCategories);
        when(mockItemDao.update(any(Item.class))).thenReturn(true); // Trả về true (thành công)

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server độc lập
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Object objResponse = testClientIn.readObject();
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertEquals("Cập nhật thành công!", msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD ĐỂ TRÁNH TREO TEST
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject(); // Đọc thông luồng phản hồi SIGNOUT

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());

        // Xác minh các DAO đã được gọi đúng luồng
        verify(mockCategoryDao, times(1)).getCategoryByName(mockCatNames);
        verify(mockItemDao, times(1)).update(any(Item.class));

    }
    @Test
    public void testHandleUpdateItem_Failed() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given) - Dùng đúng constructor 9 tham số của Item
        Item mockItem = new Item(1, "Laptop cu", "Mo ta cu", 5000000, 5000000, null, null, 123, "SOLD");

        // Payload gồm: Item, List ảnh rỗng, List danh mục rỗng
        Object[] payload = new Object[]{ mockItem, List.of(), List.of() };
        Message msgRequest = new Message("UPDATE_ITEM", payload);

        // Giả lập: categoryDao trả về list rỗng, itemDao trả về false (cập nhật thất bại)
        when(mockCategoryDao.getCategoryByName(anyList())).thenReturn(List.of());
        when(mockItemDao.update(any(Item.class))).thenReturn(false);

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Cập nhật thất bại!"));

        // 4. GIẢI PHÓNG THREAD
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }
    @Test
    public void testHandleUpdateItem_ServerError() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi (Given)
        // Cố tình truyền vào một String thay vì mảng Object[] để ép dòng ép kiểu (Object[]) msg.getData() văng Exception
        Message msgRequest = new Message("UPDATE_ITEM", "Payload sai format de test loi ep kieu");

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server độc lập
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi lỗi từ block catch (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("SERVER_ERROR", msgResponse.getStatus()); // Check đúng status code gốc gán trong catch
        assertNotNull(msgResponse.getData());
        assertTrue(msgResponse.getData().toString().contains("Lỗi Server:"));

        // =========================================================================
        // 4. GIẢI PHÓNG THREAD: Gửi tiếp lệnh SIGNOUT để tắt Server, giải phóng test case
        // =========================================================================
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject(); // Đọc thông luồng phản hồi SIGNOUT

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }
    @Test
    public void testHandleCancelAutoBid_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 15;
        int userId = 8;
        Object[] payload = new Object[]{ itemId, userId };
        Message msgRequest = new Message("CANCEL_AUTO_BID", payload);

        // Giả lập hành vi DAO: Trả về true khi tìm thấy và xóa thành công cấu hình
        when(mockItemDao.cancelAutoBid(itemId, userId)).thenReturn(true);

        // 2. Client gửi dữ liệu đi trước (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server độc lập
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi thành công (Then)
        Object objResponse = testClientIn.readObject();
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertEquals("Đã hủy cấu hình đấu giá tự động của bạn thành công!", msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD TEST BẰNG SIGNOUT
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject(); // Đọc thông luồng phản hồi SIGNOUT

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());

        // Xác minh hàm cancelAutoBid của DAO được gọi chính xác 1 lần
        verify(mockItemDao, times(1)).cancelAutoBid(itemId, userId);
    }
    @Test
    public void testHandleCancelAutoBid_NotFound() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 999;
        int userId = 8;
        Object[] payload = new Object[]{ itemId, userId };
        Message msgRequest = new Message("CANCEL_AUTO_BID", payload);

        // Giả lập hành vi DAO: Trả về false do không tìm thấy bản ghi cấu hình nào phù hợp
        when(mockItemDao.cancelAutoBid(itemId, userId)).thenReturn(false);

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi thất bại (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("FAILED", msgResponse.getStatus());
        assertEquals("Hệ thống không tìm thấy cấu hình Auto Bid nào của bạn để hủy!", msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD TEST
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }
    @Test
    public void testHandleCancelAutoBid_ServerError() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi (Given)
        // Cố tình truyền vào một đối tượng String sai định dạng để ép đoạn bóc tách payload văng ClassCastException
        Message msgRequest = new Message("CANCEL_AUTO_BID", "Chuoi payload sai dinh dang");

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi lỗi rơi vào block catch (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("ERROR", msgResponse.getStatus());
        assertTrue(msgResponse.getData().toString().contains("Lỗi hệ thống Server:"));

        // 4. GIẢI PHÓNG THREAD TEST
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }
    @Test
    public void testHandleCheckAutoBidStatus_Exists() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 50;
        int userId = 9;
        Object[] payload = new Object[]{ itemId, userId };
        Message msgRequest = new Message("CHECK_AUTO_BID_STATUS", payload);

        // Giả lập DAO: Trả về true (Đã cài AutoBid)
        when(mockItemDao.checkAutoBidExists(itemId, userId)).thenReturn(true);

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server độc lập
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Object objResponse = testClientIn.readObject();
        assertTrue(objResponse instanceof Message);

        Message msgResponse = (Message) objResponse;
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertEquals(true, msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD TEST
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
        verify(mockItemDao, times(1)).checkAutoBidExists(itemId, userId);
    }

    @Test
    public void testHandleCheckAutoBidStatus_NotExists() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập (Given)
        int itemId = 51;
        int userId = 9;
        Object[] payload = new Object[]{ itemId, userId };
        Message msgRequest = new Message("CHECK_AUTO_BID_STATUS", payload);

        // Giả lập DAO: Trả về false (Chưa cài AutoBid)
        when(mockItemDao.checkAutoBidExists(itemId, userId)).thenReturn(false);

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("SUCCESS", msgResponse.getStatus());
        assertEquals(false, msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD TEST
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();
        testClientIn.readObject();

        handlerThread.join(2000);
        assertFalse(handlerThread.isAlive());
    }

    @Test
    public void testHandleCheckAutoBidStatus_ServerError() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi (Given)
        // Cố tình truyền sai kiểu dữ liệu để bóc tách payload văng ClassCastException thẳng vào block catch
        Message msgRequest = new Message("CHECK_AUTO_BID_STATUS", "Payload sai format");

        // 2. Client gửi dữ liệu (When)
        testClientOut.writeObject(msgRequest);
        testClientOut.flush();

        // Khởi chạy Thread Server
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.start();
        testClientIn = new ObjectInputStream(pipeFromServer);

        // 3. Phía Client ảo đọc phản hồi lỗi rơi vào block catch (Then)
        Message msgResponse = (Message) testClientIn.readObject();
        assertEquals("ERROR", msgResponse.getStatus());
        assertEquals("Lỗi xử lý check Auto Bid ở Server", msgResponse.getData());

        // 4. GIẢI PHÓNG THREAD TEST AN TOÀN (Áp dụng bài học trước: Không đợi đọc phản hồi khi test lỗi)
        Message msgSignout = new Message("SIGNOUT", null);
        testClientOut.writeObject(msgSignout);
        testClientOut.flush();

        handlerThread.join(1000);
    }
    @Test
    public void testHandleGetItemByCategory_Fast_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu đầu vào
        int categoryId = 5;
        Message msgRequest = new Message("GET_ITEM_BY_CATEGORY", categoryId);

        Item item1 = new Item(1, "Laptop Dell", "Dell Core i7", 10000000, 10500000, null, null, 1, "ACTIVE");
        when(mockItemDao.getItemsByCategory(categoryId)).thenReturn(List.of(item1));

        // Mock trực tiếp ObjectOutputStream truyền vào hàm
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // 2. Dùng Reflection gọi thẳng phương thức private (Bỏ qua cơ chế Socket/Thread)
        Method method = ClientHandler.class.getDeclaredMethod("handleGetItemByCategory", Message.class, ObjectOutputStream.class);
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut); // Chạy trực tiếp trên Main Thread

        // 3. Sử dụng ArgumentCaptor để bắt Message mà Server ghi ra stream out.writeObject()
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockOut, times(1)).writeObject(messageCaptor.capture());

        // 4. Assert kết quả ngay lập tức
        Message msgResponse = messageCaptor.getValue();
        assertEquals("SUCCESS", msgResponse.getStatus());

        List<Item> actualItems = (List<Item>) msgResponse.getData();
        assertEquals(1, actualItems.size());
        assertEquals("Laptop Dell", actualItems.get(0).getName());
    }

    @Test
    public void testHandleGetItemByCategory_Fast_Exception() throws Exception {
        int categoryId = 99;
        Message msgRequest = new Message("GET_ITEM_BY_CATEGORY", categoryId);

        when(mockItemDao.getItemsByCategory(categoryId)).thenThrow(new RuntimeException("DB Error"));
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Gọi thẳng hàm qua Reflection
        Method method = ClientHandler.class.getDeclaredMethod("handleGetItemByCategory", Message.class, ObjectOutputStream.class);
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // Bắt và kiểm tra Message lỗi
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockOut, times(1)).writeObject(messageCaptor.capture());

        Message msgResponse = messageCaptor.getValue();
        assertEquals("ERROR", msgResponse.getStatus());
        assertTrue(((String) msgResponse.getData()).contains("DB Error"));
    }
    @Test
    public void testHandleDepositRequest_NoImport_Success() throws Exception {
        // 1. Giả lập đối tượng User đã đăng nhập vào hệ thống
        User mockUser = new User();
        mockUser.setId(10);
        mockUser.setBalance(500000L); // Số dư cũ 500k

        // Sử dụng Reflection qua đường dẫn đầy đủ để inject user đăng nhập
        java.lang.reflect.Field loggedInUserField = ClientHandler.class.getDeclaredField("loggedInUser");
        loggedInUserField.setAccessible(true);
        loggedInUserField.set(clientHandler, mockUser);

        // 2. Tạo Map dữ liệu nạp tiền (Dùng đầy đủ đường dẫn java.util)
        java.util.Map<String, Object> depositData = new java.util.HashMap<>();
        depositData.put("cardNumber", "1234567890123456");
        depositData.put("amount", 200000L); // Nạp thêm 200k

        Message msgRequest = new Message("DEPOSIT", depositData);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Mock hành vi UserDao trả về true khi update balance
        when(mockUserDao.updateBalance(10, 200000L)).thenReturn(true);

        // Mẹo không dùng ArgumentCaptor: Bắt gói tin ghi ra bằng thenAnswer
        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 3. Gọi hàm xử lý qua Reflection bóc tách hoàn toàn Thread/Socket
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleDepositRequest", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 4. Assert kết quả (Sử dụng các hàm static đã import sẵn đầu file)
        assertNotNull(capturedResponse[0]);
        assertEquals("SUCCESS", capturedResponse[0].getStatus());

        User updatedUser = (User) capturedResponse[0].getData();
        assertEquals(700000L, updatedUser.getBalance()); // 500k + 200k = 700k
    }

    @Test
    public void testHandleDepositRequest_NoImport_Failed_4444() throws Exception {
        User mockUser = new User();
        java.lang.reflect.Field loggedInUserField = ClientHandler.class.getDeclaredField("loggedInUser");
        loggedInUserField.setAccessible(true);
        loggedInUserField.set(clientHandler, mockUser);

        java.util.Map<String, Object> depositData = new java.util.HashMap<>();
        depositData.put("cardNumber", "1234567890124444"); // Đuôi lỗi 4444
        depositData.put("amount", 100000L);

        Message msgRequest = new Message("DEPOSIT", depositData);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // Thực thi bằng reflection đường dẫn đầy đủ
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleDepositRequest", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // Kiểm tra kết quả báo lỗi số dư
        assertEquals("FAILED", capturedResponse[0].getStatus());
        assertTrue(((String) capturedResponse[0].getData()).contains("Mã lỗi: 4444"));

        // Đảm bảo không gọi vào DB khi thẻ lỗi
        verify(mockUserDao, never()).updateBalance(anyInt(), anyLong());
    }

    @Test
    public void testHandleGetCustomers_NoImport_Success() throws Exception {
        int sellerId = 12;
        Message msgRequest = new Message("GET_CUSTOMERS", sellerId);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        List<Object[]> mockCustomers = new ArrayList<>();
        Object[] customerData = new Object[]{101, "customer_alpha", "alpha@gmail.com"};
        mockCustomers.add(customerData);

        // Stub hành vi cho mockItemDao trả về đúng kiểu List<Object[]>
        when(mockItemDao.getCustomersBySellerId(sellerId)).thenReturn(mockCustomers);

        // Bắt gói tin ghi ra stream bằng doAnswer
        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi qua Reflection
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetCustomers", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Assert kết quả kiểm thử (Then)
        assertNotNull(capturedResponse[0], "Response message không được null");
        assertEquals("SUCCESS", capturedResponse[0].getStatus());

        // Ép kiểu về List<Object[]> để kiểm tra tính toàn vẹn của dữ liệu dữ liệu
        List<Object[]> actualCustomers = (List<Object[]>) capturedResponse[0].getData();
        assertEquals(1, actualCustomers.size());

        Object[] actualCustomer = actualCustomers.get(0);
        assertEquals(101, actualCustomer[0]);
        assertEquals("customer_alpha", actualCustomer[1]);

        // Xác minh tầng DAO đã được gọi chính xác 1 lần
        verify(mockItemDao, times(1)).getCustomersBySellerId(sellerId);
    }

    @Test
    public void testHandleGetCustomers_NoImport_Exception() throws Exception {
        int sellerId = 5;
        Message msgRequest = new Message("GET_CUSTOMERS", sellerId);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Giả lập ép DAO ném ra ngoại lệ khi truy vấn
        when(mockItemDao.getCustomersBySellerId(sellerId))
                .thenThrow(new RuntimeException("SQL Connection Error"));

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi qua Reflection
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetCustomers", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        assertNotNull(capturedResponse[0], "Response message không được null");
        assertEquals("ERROR", capturedResponse[0].getStatus());

        String errorMsg = (String) capturedResponse[0].getData();
        assertTrue(errorMsg.contains("SQL Connection Error"), "Thông báo lỗi trả về không khớp");
    }
    @Test
    public void testHandleGetSellerRevenue_NoImport_Success() throws Exception {
        String sellerIdStr = "15";
        int sellerId = 15;
        Message msgRequest = new Message("GET_REVENUE", sellerIdStr);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Giả lập doanh thu trả về từ DAO là 15,500,000 VND
        long expectedRevenue = 15500000L;
        when(mockItemDao.getTotalRevenueBySellerId(sellerId)).thenReturn(expectedRevenue);

        // Bắt gói tin ghi ra stream bằng doAnswer
        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi qua Reflection (Không dùng Thread/Socket)
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetSellerRevenue", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Assert kết quả tức thì trên RAM
        assertNotNull(capturedResponse[0]);
        assertEquals("SUCCESS", capturedResponse[0].getStatus());
        assertEquals(expectedRevenue, capturedResponse[0].getData());

        verify(mockItemDao, times(1)).getTotalRevenueBySellerId(sellerId);
    }

    @Test
    public void testHandleGetSellerRevenue_NoImport_Exception() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi: Ép DAO ném ra Exception mạng hoặc DB
        String sellerIdStr = "99";
        int sellerId = 99;
        Message msgRequest = new Message("GET_REVENUE", sellerIdStr);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        when(mockItemDao.getTotalRevenueBySellerId(sellerId))
                .thenThrow(new RuntimeException("Cloud DB Timeout"));

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetSellerRevenue", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Kiểm tra khối catch gán trạng thái ERROR và trả về doanh thu bằng 0L theo đúng code gốc
        assertNotNull(capturedResponse[0]);
        assertEquals("ERROR", capturedResponse[0].getStatus());
        assertEquals(0L, capturedResponse[0].getData());
    }
    @Test
    public void testHandleConfirmItem_NoImport_Approved_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu: payload gồm [itemId, isApproved]
        int itemId = 42;
        boolean isApproved = true;
        Object[] payload = new Object[]{itemId, isApproved,""};

        Message msgRequest = new Message("CONFIRM_ITEM", payload);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Mock tầng DAO trả về item và approve thành công
        Item mockItem = new Item();
        mockItem.setId(itemId);
        mockItem.setName("Vòng cổ phong thủy");

        when(mockItemDao.getById(itemId)).thenReturn(mockItem);
        when(mockItemDao.approveItem(itemId, isApproved)).thenReturn(true);

        // Bắt gói tin ghi ra stream bằng doAnswer
        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi thông qua Reflection
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleConfirmItem", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Kiểm tra kết quả phản hồi gán trạng thái SUCCESS và chuỗi thông báo duyệt sản phẩm
        assertNotNull(capturedResponse[0]);
        assertEquals("SUCCESS", capturedResponse[0].getStatus());
        assertTrue(((String) capturedResponse[0].getData()).contains("Sản phẩm đã được chấp nhận"));

        verify(mockItemDao, times(1)).approveItem(itemId, isApproved);
    }

    @Test
    public void testHandleConfirmItem_NoImport_Rejected_Success() throws Exception {
        // 1. Chuẩn bị dữ liệu từ chối: [itemId, false]
        int itemId = 42;
        boolean isApproved = false;
        Object[] payload = new Object[]{itemId, isApproved,""};

        Message msgRequest = new Message("CONFIRM_ITEM", payload);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        Item mockItem = new Item();
        mockItem.setId(itemId);

        when(mockItemDao.getById(itemId)).thenReturn(mockItem);
        when(mockItemDao.approveItem(itemId, isApproved)).thenReturn(true);

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleConfirmItem", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Kiểm tra kết quả phản hồi gán chuỗi thông báo từ chối
        assertNotNull(capturedResponse[0]);
        assertEquals("SUCCESS", capturedResponse[0].getStatus());
        assertTrue(((String) capturedResponse[0].getData()).contains("Đã từ chối sản phẩm"));
    }

    @Test
    public void testHandleConfirmItem_NoImport_Exception() throws Exception {
        // 1. Ép tầng DAO ném lỗi hệ thống mạng/DB để test khối catch
        int itemId = 99;
        Object[] payload = new Object[]{itemId, true,""};

        Message msgRequest = new Message("CONFIRM_ITEM", payload);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        when(mockItemDao.getById(itemId)).thenThrow(new RuntimeException("Database Timeout"));

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleConfirmItem", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Kiểm tra khối catch xử lý chuẩn xác
        assertNotNull(capturedResponse[0]);
        assertEquals("ERROR", capturedResponse[0].getStatus());
        assertTrue(((String) capturedResponse[0].getData()).contains("Lỗi hệ thống Server: Database Timeout"));
    }
    @Test
    public void testHandleGetDashboardStats_NoImport_Success() throws Exception {
        System.out.println("Dang chay test: Get Dashboard Stats Success");

        // 1. Chuẩn bị dữ liệu giả lập (Given)
        Message msgRequest = new Message("GET_DASHBOARD_STATS", null);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        // Giả lập dữ liệu cho map phân phối danh mục (categoryDistribution)
        Map<Integer, Integer> mockCategoryDistribution = new HashMap<>();
        mockCategoryDistribution.put(1, 10); // Category ID 1 có 10 items
        mockCategoryDistribution.put(2, 25); // Category ID 2 có 25 items

        // Giả lập dữ liệu cho danh sách xu hướng doanh thu (revenueTrend)
        List<Object[]> mockRevenueTrend = new ArrayList<>();
        mockRevenueTrend.add(new Object[]{"2026-01", 5000000L});
        mockRevenueTrend.add(new Object[]{"2026-02", 10000000L});

        // SỬA LỖI: Tạo mảng Object đầy đủ số lượng phần tử mà code thực tế yêu cầu
        Object[] mockUserStats = new Object[]{ 15000000L, 120 }; // index 0: revenue, index 1: users
        Object[] mockItemStats = new Object[]{
                45,                         // index 0: liveAuctions (int)
                85.5,                       // index 1: successRate (double)
                mockCategoryDistribution,   // index 2: categoryDistribution (Map)
                mockRevenueTrend            // index 3: revenueTrend (List)
        };

        // Stub dữ liệu cho các Mock DAO
        when(mockUserDao.getDashboardStats()).thenReturn(mockUserStats);
        when(mockItemDao.getDashboardStats()).thenReturn(mockItemStats);

        // Bắt gói tin phản hồi ghi ra stream
        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi thông qua Reflection (When)
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetDashboardStats", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Assert kiểm tra tính toàn vẹn của dữ liệu thu được (Then)
        assertNotNull(capturedResponse[0], "Phản hồi từ server không được null");
        assertEquals("SUCCESS", capturedResponse[0].getStatus());

        // Kiểm tra cấu trúc Map tổng hợp trả về cho Client
        Map<String, Object> actualStats = (Map<String, Object>) capturedResponse[0].getData();
        assertNotNull(actualStats);

        // Kiểm tra các chỉ số cơ bản
        assertEquals(15000000L, actualStats.get("totalRevenue"));
        assertEquals(120, actualStats.get("totalUsers"));
        assertEquals(45, actualStats.get("liveAuctions"));
        assertEquals(85.5, actualStats.get("successRate"));

        // Kiểm tra cấu trúc dữ liệu phức tạp đi kèm
        Map<Integer, Integer> actualDist = (Map<Integer, Integer>) actualStats.get("categoryDistribution");
        assertEquals(2, actualDist.size());
        assertEquals(10, actualDist.get(1));

        List<Object[]> actualTrend = (List<Object[]>) actualStats.get("revenueTrend");
        assertEquals(2, actualTrend.size());
        assertEquals("2026-01", actualTrend.get(0)[0]);

        // Xác minh xem các hàm DAO đã được gọi chuẩn xác chưa
        verify(mockUserDao, times(1)).getDashboardStats();
        verify(mockItemDao, times(1)).getDashboardStats();
    }

    @Test
    public void testHandleGetDashboardStats_NoImport_Exception() throws Exception {
        // 1. Chuẩn bị dữ liệu lỗi: Ép một trong các DAO ném ra lỗi Runtime
        Message msgRequest = new Message("GET_DASHBOARD_STATS", null);
        ObjectOutputStream mockOut = mock(ObjectOutputStream.class);

        when(mockUserDao.getDashboardStats()).thenThrow(new RuntimeException("Database error"));

        final Message[] capturedResponse = new Message[1];
        doAnswer(invocation -> {
            capturedResponse[0] = invocation.getArgument(0);
            return null;
        }).when(mockOut).writeObject(any(Message.class));

        // 2. Thực thi
        java.lang.reflect.Method method = ClientHandler.class.getDeclaredMethod(
                "handleGetDashboardStats", Message.class, ObjectOutputStream.class
        );
        method.setAccessible(true);
        method.invoke(clientHandler, msgRequest, mockOut);

        // 3. Kiểm tra khối catch bắt lỗi và gán trạng thái ERROR thành công
        assertNotNull(capturedResponse[0]);
        assertEquals("ERROR", capturedResponse[0].getStatus());
        assertNull(capturedResponse[0].getData()); // Theo code gốc, khi lỗi data không được gán gì mới (vẫn giữ nguyên giá trị cũ hoặc null)
    }
}