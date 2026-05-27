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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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


}