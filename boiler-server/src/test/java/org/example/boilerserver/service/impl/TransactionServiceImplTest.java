package org.example.boilerserver.service.impl;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.BuyerEntity;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.TransactionCreateDTO;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.TransactionQueryDTO;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.auth.AuthContext;
import org.example.boilerserver.auth.AuthUser;
import org.example.boilerserver.mapper.BuyerMapper;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.constant.OrderConstant;
import org.example.constant.PostConstant;
import org.example.constant.TransactionConstant;
import org.example.constant.UserConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SellerMapper sellerMapper;
    @Mock
    private BuyerMapper buyerMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ==================== Helper methods ====================

    private PostEntity buildPublishedPost() {
        PostEntity post = new PostEntity();
        post.setPostId("post001");
        post.setSellerId("seller001");
        post.setTitle("Test Boiler");
        post.setPrice(new BigDecimal("50000"));
        post.setStatus(PostConstant.STATUS_PUBLISHED);
        return post;
    }

    private BuyerEntity buildBuyer() {
        BuyerEntity buyer = new BuyerEntity();
        buyer.setBuyerId("buyer001");
        return buyer;
    }

    private TransactionEntity buildTransaction() {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId("txn001");
        transaction.setBuyerId("buyer001");
        transaction.setSellerId("seller001");
        transaction.setTransactionAmount(new BigDecimal("50000"));
        transaction.setTransactionTime(LocalDate.now());
        transaction.setTransactionStatus(TransactionConstant.STATUS_PENDING);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_BOOKED);
        transaction.setLogisticsInfo("post001");
        return transaction;
    }

    private UserEntity buildUser(String userId, String username) {
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setUsername(username);
        return user;
    }

    private SellerEntity buildSeller() {
        SellerEntity seller = new SellerEntity();
        seller.setSellerId("seller001");
        seller.setShopName("Test Shop");
        return seller;
    }

    private void loginAs(String userId, String userType) {
        AuthContext.setCurrentUser(new AuthUser(userId, userType));
    }

    // ==================== createTransaction tests ====================

    @Test
    void createTransaction_success() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");
        dto.setBuyerId("buyer001");

        PostEntity post = buildPublishedPost();
        BuyerEntity buyer = buildBuyer();
        TransactionEntity transaction = buildTransaction();

        when(postMapper.getByPostId("post001")).thenReturn(post);
        when(buyerMapper.getByBuyerId("buyer001")).thenReturn(buyer);
        when(transactionMapper.getByTransactionId(anyString())).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());

        TransactionVO result = transactionService.createTransaction(dto);

        assertNotNull(result);
        assertEquals("txn001", result.getTransactionId());
        assertEquals("buyer001", result.getBuyerId());
        assertEquals("seller001", result.getSellerId());
        assertEquals(new BigDecimal("50000"), result.getTransactionAmount());
        assertEquals(TransactionConstant.STATUS_PENDING, result.getTransactionStatus());
        assertEquals(TransactionConstant.BOOKING_STATUS_BOOKED, result.getBookingStatus());
        assertEquals("post001", result.getPostId());
        assertEquals("Test Boiler", result.getPostTitle());
        assertEquals("BuyerName", result.getBuyerName());
        assertEquals("SellerName", result.getSellerName());
        assertEquals("Test Shop", result.getShopName());

        verify(transactionMapper).insert(any(TransactionEntity.class));
        verify(postMapper).updateStatus("post001", PostConstant.STATUS_RESERVED);
    }

    @Test
    void createTransaction_postNotFound_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("postNotFound");
        dto.setBuyerId("buyer001");

        when(postMapper.getByPostId("postNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("帖子不存在", ex.getMessage());
        verify(transactionMapper, never()).insert(any());
        verify(postMapper, never()).updateStatus(anyString(), anyString());
    }

    @Test
    void createTransaction_postStatusNotPublished_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");
        dto.setBuyerId("buyer001");

        PostEntity post = buildPublishedPost();
        post.setStatus(PostConstant.STATUS_RESERVED);

        when(postMapper.getByPostId("post001")).thenReturn(post);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("帖子当前状态不支持预约", ex.getMessage());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void createTransaction_buyerNotFound_throwsException() {
        loginAs("buyerNotFound", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");
        dto.setBuyerId("buyerNotFound");

        when(postMapper.getByPostId("post001")).thenReturn(buildPublishedPost());
        when(buyerMapper.getByBuyerId("buyerNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("买家不存在", ex.getMessage());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void createTransaction_buyerIsSeller_throwsException() {
        loginAs("seller001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");
        dto.setBuyerId("seller001");

        when(postMapper.getByPostId("post001")).thenReturn(buildPublishedPost());
        when(buyerMapper.getByBuyerId("seller001")).thenReturn(buildBuyer());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("不能预约自己的帖子", ex.getMessage());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void createTransaction_nullPostId_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setBuyerId("buyer001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("帖子ID不能为空", ex.getMessage());
        verify(postMapper, never()).getByPostId(anyString());
    }

    @Test
    void createTransaction_emptyPostId_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("");
        dto.setBuyerId("buyer001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("帖子ID不能为空", ex.getMessage());
    }

    @Test
    void createTransaction_nullBuyerId_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("买家ID不能为空", ex.getMessage());
    }

    @Test
    void createTransaction_emptyBuyerId_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setPostId("post001");
        dto.setBuyerId("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(dto));
        assertEquals("买家ID不能为空", ex.getMessage());
    }

    @Test
    void createTransaction_nullDto_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(null));
        assertEquals("帖子ID不能为空", ex.getMessage());
    }

    // ==================== getTransaction tests ====================

    @Test
    void getTransaction_success() {
        TransactionEntity transaction = buildTransaction();
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPublishedPost());

        TransactionVO result = transactionService.getTransaction("txn001");

        assertNotNull(result);
        assertEquals("txn001", result.getTransactionId());
        assertEquals("buyer001", result.getBuyerId());
        assertEquals("seller001", result.getSellerId());
        assertEquals("BuyerName", result.getBuyerName());
        assertEquals("SellerName", result.getSellerName());
        assertEquals("Test Shop", result.getShopName());
        assertEquals("post001", result.getPostId());
        assertEquals("Test Boiler", result.getPostTitle());
    }

    @Test
    void getTransaction_notFound_throwsException() {
        when(transactionMapper.getByTransactionId("txnNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransaction("txnNotFound"));
        assertEquals("交易不存在", ex.getMessage());
    }

    @Test
    void getTransaction_emptyId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransaction(""));
        assertEquals("交易ID不能为空", ex.getMessage());
        verify(transactionMapper, never()).getByTransactionId(anyString());
    }

    // ==================== listMyTransactions tests ====================

    @Test
    void listMyTransactions_success() {
        TransactionQueryDTO dto = new TransactionQueryDTO();
        dto.setUserId("user001");
        dto.setPageNum(1);
        dto.setPageSize(10);

        TransactionEntity txn = buildTransaction();
        when(transactionMapper.countByUserIdAndStatus("user001", null)).thenReturn(1);
        when(transactionMapper.listByUserIdAndStatus(eq("user001"), isNull(), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of(txn));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPublishedPost());

        PageResult<TransactionVO> result = transactionService.listMyTransactions(dto);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("txn001", result.getRecords().get(0).getTransactionId());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void listMyTransactions_emptyResult() {
        TransactionQueryDTO dto = new TransactionQueryDTO();
        dto.setUserId("user001");

        when(transactionMapper.countByUserIdAndStatus("user001", null)).thenReturn(0);
        when(transactionMapper.listByUserIdAndStatus(eq("user001"), isNull(), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of());

        PageResult<TransactionVO> result = transactionService.listMyTransactions(dto);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listMyTransactions_nullUserId_throwsException() {
        TransactionQueryDTO dto = new TransactionQueryDTO();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.listMyTransactions(dto));
        assertEquals("用户ID不能为空", ex.getMessage());
    }

    @Test
    void listMyTransactions_nullDto_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.listMyTransactions(null));
        assertEquals("用户ID不能为空", ex.getMessage());
    }

    // ==================== cancelTransaction tests ====================

    @Test
    void cancelTransaction_success() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionEntity transaction = buildTransaction();
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPublishedPost());

        TransactionVO result = transactionService.cancelTransaction("txn001", "buyer001");

        assertNotNull(result);
        verify(transactionMapper).update(argThat(t ->
                TransactionConstant.STATUS_CANCELLED.equals(t.getTransactionStatus())
                        && TransactionConstant.BOOKING_STATUS_CANCELLED.equals(t.getBookingStatus())));
        verify(postMapper).updateStatus("post001", PostConstant.STATUS_PUBLISHED);
    }

    @Test
    void cancelTransaction_notFound_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        when(transactionMapper.getByTransactionId("txnNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.cancelTransaction("txnNotFound", "buyer001"));
        assertEquals("交易不存在", ex.getMessage());
        verify(transactionMapper, never()).update(any());
    }

    @Test
    void cancelTransaction_statusNotPending_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        TransactionEntity transaction = buildTransaction();
        transaction.setTransactionStatus(TransactionConstant.STATUS_COMPLETED);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.cancelTransaction("txn001", "buyer001"));
        assertEquals("仅待处理状态的交易可以取消", ex.getMessage());
        verify(transactionMapper, never()).update(any());
    }

    @Test
    void cancelTransaction_userNotBuyer_throwsException() {
        loginAs("otherUser", UserConstant.USER_TYPE_BUYER);
        TransactionEntity transaction = buildTransaction();
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.cancelTransaction("txn001", "otherUser"));
        assertEquals("仅买家可取消预约", ex.getMessage());
        verify(transactionMapper, never()).update(any());
    }

    @Test
    void cancelTransaction_emptyTransactionId_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.cancelTransaction("", "buyer001"));
        assertEquals("交易ID不能为空", ex.getMessage());
        verify(transactionMapper, never()).getByTransactionId(anyString());
    }

    // ==================== completeTransaction tests ====================

    @Test
    void completeTransaction_success() {
        loginAs("seller001", UserConstant.USER_TYPE_SELLER);
        TransactionEntity transaction = buildTransaction();
        PostEntity soldPost = buildPublishedPost();
        soldPost.setStatus(PostConstant.STATUS_SOLD);

        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(soldPost);
        when(orderMapper.getByTransactionId("txn001")).thenReturn(null);

        TransactionVO result = transactionService.completeTransaction("txn001", "seller001");

        assertNotNull(result);
        verify(transactionMapper).update(argThat(t ->
                TransactionConstant.STATUS_COMPLETED.equals(t.getTransactionStatus())));
        verify(orderMapper).insert(any(OrderEntity.class));
        verify(postMapper).updateStatus("post001", PostConstant.STATUS_SOLD);
    }

    @Test
    void completeTransaction_statusNotPending_throwsException() {
        loginAs("seller001", UserConstant.USER_TYPE_SELLER);
        TransactionEntity transaction = buildTransaction();
        transaction.setTransactionStatus(TransactionConstant.STATUS_COMPLETED);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.completeTransaction("txn001", "seller001"));
        assertEquals("仅已预订状态的交易可以成交", ex.getMessage());
        verify(transactionMapper, never()).update(any());
    }

    @Test
    void completeTransaction_userNotSeller_throwsException() {
        loginAs("buyer001", UserConstant.USER_TYPE_BUYER);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.completeTransaction("txn001", "buyer001"));
        assertEquals("仅卖家可确认成交", ex.getMessage());
        verify(transactionMapper, never()).update(any());
    }
}
