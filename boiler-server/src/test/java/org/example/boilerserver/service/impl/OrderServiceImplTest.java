package org.example.boilerserver.service.impl;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.OrderCreateDTO;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.OrderQueryDTO;
import org.example.boilerpojo.OrderVO;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.constant.OrderConstant;
import org.example.constant.PostConstant;
import org.example.constant.TransactionConstant;
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
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ==================== Helper methods ====================

    private OrderEntity buildOrder(String status) {
        OrderEntity order = new OrderEntity();
        order.setOrderId("order001");
        order.setTransactionId("txn001");
        order.setOrderStatus(status);
        order.setCreateTime(LocalDate.now());
        order.setUpdateTime(LocalDate.now());
        return order;
    }

    private TransactionEntity buildTransaction(String status) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId("txn001");
        transaction.setBuyerId("buyer001");
        transaction.setSellerId("seller001");
        transaction.setTransactionAmount(new BigDecimal("50000"));
        transaction.setTransactionTime(LocalDate.now());
        transaction.setTransactionStatus(status);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_BOOKED);
        transaction.setLogisticsInfo("post001");
        return transaction;
    }

    private PostEntity buildPost() {
        PostEntity post = new PostEntity();
        post.setPostId("post001");
        post.setSellerId("seller001");
        post.setTitle("Test Boiler");
        post.setPrice(new BigDecimal("50000"));
        post.setStatus(PostConstant.STATUS_PUBLISHED);
        return post;
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
        seller.setCompletedTransactionCount(0);
        return seller;
    }

    // ==================== createOrder tests ====================

    @Test
    void createOrder_success() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTransactionId("txn001");
        dto.setOperatorId("buyer001");

        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_PENDING);
        OrderEntity order = buildOrder(OrderConstant.STATUS_PENDING_CONFIRM);

        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(orderMapper.getByOrderId(anyString())).thenReturn(order);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        OrderVO result = orderService.createOrder(dto);

        assertNotNull(result);
        assertEquals("order001", result.getOrderId());
        assertEquals("txn001", result.getTransactionId());
        assertEquals(OrderConstant.STATUS_PENDING_CONFIRM, result.getOrderStatus());
        assertEquals("buyer001", result.getBuyerId());
        assertEquals("seller001", result.getSellerId());
        assertEquals("BuyerName", result.getBuyerName());
        assertEquals("SellerName", result.getSellerName());
        assertEquals("Test Shop", result.getShopName());
        assertEquals("post001", result.getPostId());
        assertEquals("Test Boiler", result.getPostTitle());

        verify(orderMapper).insert(any(OrderEntity.class));
    }

    @Test
    void createOrder_transactionNotFound_throwsException() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTransactionId("txnNotFound");
        dto.setOperatorId("buyer001");

        when(transactionMapper.getByTransactionId("txnNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto));
        assertEquals("交易不存在", ex.getMessage());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createOrder_transactionStatusNotPending_throwsException() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTransactionId("txn001");
        dto.setOperatorId("buyer001");

        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_COMPLETED);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto));
        assertEquals("仅待处理状态的交易可以创建订单", ex.getMessage());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createOrder_operatorNotBuyerOrSeller_throwsException() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTransactionId("txn001");
        dto.setOperatorId("otherUser");

        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_PENDING);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto));
        assertEquals("仅买家可创建订单", ex.getMessage());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createOrder_nullTransactionId_throwsException() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setOperatorId("buyer001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto));
        assertEquals("交易ID不能为空", ex.getMessage());
    }

    @Test
    void createOrder_nullOperatorId_throwsException() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTransactionId("txn001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto));
        assertEquals("操作者ID不能为空", ex.getMessage());
    }

    // ==================== confirmOrder tests ====================

    @Test
    void confirmOrder_success() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_PENDING_CONFIRM);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_PENDING);

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        OrderVO result = orderService.confirmOrder("order001", "seller001");

        assertNotNull(result);
        verify(orderMapper).update(argThat(o ->
                OrderConstant.STATUS_IN_PROGRESS.equals(o.getOrderStatus())));
        verify(transactionMapper).update(argThat(t ->
                TransactionConstant.STATUS_IN_PROGRESS.equals(t.getTransactionStatus())));
    }

    @Test
    void confirmOrder_orderNotFound_throwsException() {
        when(orderMapper.getByOrderId("orderNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.confirmOrder("orderNotFound", "seller001"));
        assertEquals("订单不存在", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    @Test
    void confirmOrder_statusNotPendingConfirm_throwsException() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_IN_PROGRESS);
        when(orderMapper.getByOrderId("order001")).thenReturn(order);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.confirmOrder("order001", "seller001"));
        assertEquals("仅待确认状态的订单可以确认", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    @Test
    void confirmOrder_operatorNotSeller_throwsException() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_PENDING_CONFIRM);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_PENDING);

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.confirmOrder("order001", "otherUser"));
        assertEquals("仅卖家可确认订单", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    // ==================== completeOrder tests ====================

    @Test
    void completeOrder_success() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_IN_PROGRESS);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_IN_PROGRESS);
        SellerEntity seller = buildSeller();

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(sellerMapper.getBySellerId("seller001")).thenReturn(seller);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        OrderVO result = orderService.completeOrder("order001", "buyer001");

        assertNotNull(result);
        verify(orderMapper).update(argThat(o ->
                OrderConstant.STATUS_COMPLETED.equals(o.getOrderStatus())));
        verify(transactionMapper).update(argThat(t ->
                TransactionConstant.STATUS_COMPLETED.equals(t.getTransactionStatus())));
        verify(postMapper).updateStatus("post001", PostConstant.STATUS_SOLD);
        verify(sellerMapper).update(argThat(s ->
                s.getCompletedTransactionCount() != null && s.getCompletedTransactionCount() == 1));
    }

    @Test
    void completeOrder_statusNotInProgress_throwsException() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_PENDING_CONFIRM);
        when(orderMapper.getByOrderId("order001")).thenReturn(order);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.completeOrder("order001", "buyer001"));
        assertEquals("仅进行中状态的订单可以完成", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    @Test
    void completeOrder_operatorNotBuyerOrSeller_throwsException() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_IN_PROGRESS);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_IN_PROGRESS);

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.completeOrder("order001", "otherUser"));
        assertEquals("无权操作此订单", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    // ==================== cancelOrder tests ====================

    @Test
    void cancelOrder_success() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_PENDING_CONFIRM);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_PENDING);

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        OrderVO result = orderService.cancelOrder("order001", "buyer001");

        assertNotNull(result);
        verify(orderMapper).update(argThat(o ->
                OrderConstant.STATUS_CANCELLED.equals(o.getOrderStatus())));
        verify(transactionMapper).update(argThat(t ->
                TransactionConstant.STATUS_CANCELLED.equals(t.getTransactionStatus())
                        && TransactionConstant.BOOKING_STATUS_CANCELLED.equals(t.getBookingStatus())));
        verify(postMapper).updateStatus("post001", PostConstant.STATUS_PUBLISHED);
    }

    @Test
    void cancelOrder_statusCompleted_throwsException() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_COMPLETED);
        when(orderMapper.getByOrderId("order001")).thenReturn(order);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder("order001", "buyer001"));
        assertEquals("仅待确认或进行中状态的订单可以取消", ex.getMessage());
        verify(orderMapper, never()).update(any());
    }

    // ==================== getOrderDetail tests ====================

    @Test
    void getOrderDetail_success() {
        OrderEntity order = buildOrder(OrderConstant.STATUS_IN_PROGRESS);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_IN_PROGRESS);

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        OrderVO result = orderService.getOrderDetail("order001");

        assertNotNull(result);
        assertEquals("order001", result.getOrderId());
        assertEquals("txn001", result.getTransactionId());
        assertEquals(OrderConstant.STATUS_IN_PROGRESS, result.getOrderStatus());
        assertEquals("buyer001", result.getBuyerId());
        assertEquals("seller001", result.getSellerId());
        assertEquals("BuyerName", result.getBuyerName());
        assertEquals("SellerName", result.getSellerName());
        assertEquals("Test Shop", result.getShopName());
        assertEquals("post001", result.getPostId());
        assertEquals("Test Boiler", result.getPostTitle());
    }

    @Test
    void getOrderDetail_notFound_throwsException() {
        when(orderMapper.getByOrderId("orderNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderDetail("orderNotFound"));
        assertEquals("订单不存在", ex.getMessage());
    }

    // ==================== listMyOrders tests ====================

    @Test
    void listMyOrders_success() {
        OrderQueryDTO dto = new OrderQueryDTO();
        dto.setUserId("user001");
        dto.setPageNum(1);
        dto.setPageSize(10);

        OrderEntity order = buildOrder(OrderConstant.STATUS_IN_PROGRESS);
        TransactionEntity transaction = buildTransaction(TransactionConstant.STATUS_IN_PROGRESS);

        when(orderMapper.countByUserIdAndStatus("user001", null)).thenReturn(1);
        when(orderMapper.listByUserIdAndStatus(eq("user001"), isNull(), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of(order));
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "SellerName"));
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(postMapper.getByPostId("post001")).thenReturn(buildPost());

        PageResult<OrderVO> result = orderService.listMyOrders(dto);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("order001", result.getRecords().get(0).getOrderId());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void listMyOrders_emptyResult() {
        OrderQueryDTO dto = new OrderQueryDTO();
        dto.setUserId("user001");

        when(orderMapper.countByUserIdAndStatus("user001", null)).thenReturn(0);
        when(orderMapper.listByUserIdAndStatus(eq("user001"), isNull(), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of());

        PageResult<OrderVO> result = orderService.listMyOrders(dto);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listMyOrders_nullUserId_throwsException() {
        OrderQueryDTO dto = new OrderQueryDTO();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.listMyOrders(dto));
        assertEquals("用户ID不能为空", ex.getMessage());
    }
}
