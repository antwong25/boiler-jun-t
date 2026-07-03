package org.example.boilerserver.service.impl;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.ReviewCreateDTO;
import org.example.boilerpojo.ReviewEntity;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.SellerRatingVO;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.constant.OrderConstant;
import org.example.constant.ReviewConstant;
import org.example.constant.TransactionConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    // ==================== Helper methods ====================

    private OrderEntity buildCompletedOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId("order001");
        order.setTransactionId("txn001");
        order.setOrderStatus(OrderConstant.STATUS_COMPLETED);
        order.setCreateTime(LocalDate.now());
        order.setUpdateTime(LocalDate.now());
        return order;
    }

    private TransactionEntity buildTransaction() {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId("txn001");
        transaction.setBuyerId("buyer001");
        transaction.setSellerId("seller001");
        transaction.setTransactionStatus(TransactionConstant.STATUS_COMPLETED);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_BOOKED);
        transaction.setLogisticsInfo("post001");
        return transaction;
    }

    private ReviewEntity buildReview() {
        ReviewEntity review = new ReviewEntity();
        review.setReviewId("review001");
        review.setBuyerId("buyer001");
        review.setPostId("post001");
        review.setOrderId("order001");
        review.setRating(5);
        review.setContent("Great seller!");
        review.setReviewTime(LocalDate.now());
        return review;
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
        seller.setCompletedTransactionCount(1);
        seller.setPositiveRatingRate(BigDecimal.ZERO);
        return seller;
    }

    private ReviewCreateDTO buildValidDto() {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId("order001");
        dto.setReviewerId("buyer001");
        dto.setRating(5);
        dto.setContent("Great seller!");
        return dto;
    }

    // ==================== submitReview tests ====================

    @Test
    void submitReview_success() {
        ReviewCreateDTO dto = buildValidDto();
        OrderEntity order = buildCompletedOrder();
        TransactionEntity transaction = buildTransaction();
        SellerEntity seller = buildSeller();

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(reviewMapper.getByOrderIdAndBuyerId("order001", "buyer001")).thenReturn(null);
        when(sellerMapper.getBySellerId("seller001")).thenReturn(seller);
        when(reviewMapper.countBySellerId("seller001")).thenReturn(1);
        when(reviewMapper.countPositiveBySellerId("seller001", ReviewConstant.POSITIVE_RATING_THRESHOLD)).thenReturn(1);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));

        ReviewVO result = reviewService.submitReview(dto);

        assertNotNull(result);
        assertEquals("buyer001", result.getReviewerId());
        assertEquals("post001", result.getPostId());
        assertEquals("order001", result.getOrderId());
        assertEquals(5, result.getRating());
        assertEquals("Great seller!", result.getContent());
        assertEquals("BuyerName", result.getReviewerName());

        verify(reviewMapper).insert(any(ReviewEntity.class));
        verify(sellerMapper).update(any(SellerEntity.class));
    }

    @Test
    void submitReview_orderNotFound_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        when(orderMapper.getByOrderId("order001")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("订单不存在", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_orderStatusNotCompleted_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        OrderEntity order = buildCompletedOrder();
        order.setOrderStatus(OrderConstant.STATUS_IN_PROGRESS);
        when(orderMapper.getByOrderId("order001")).thenReturn(order);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("仅已完成的订单可以评论", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_reviewerNotBuyer_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setReviewerId("otherUser");

        OrderEntity order = buildCompletedOrder();
        TransactionEntity transaction = buildTransaction();

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("仅订单买方可评价卖家", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_duplicateReview_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        OrderEntity order = buildCompletedOrder();
        TransactionEntity transaction = buildTransaction();
        ReviewEntity existing = buildReview();

        when(orderMapper.getByOrderId("order001")).thenReturn(order);
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(transaction);
        when(reviewMapper.getByOrderIdAndBuyerId("order001", "buyer001")).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("您已对此订单发表过评论", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_ratingTooLow_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setRating(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("评分必须在" + ReviewConstant.MIN_RATING + "到" + ReviewConstant.MAX_RATING + "之间", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_ratingTooHigh_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setRating(6);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("评分必须在" + ReviewConstant.MIN_RATING + "到" + ReviewConstant.MAX_RATING + "之间", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_emptyContent_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setContent("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("评论内容不能为空", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_nullOrderId_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setOrderId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("订单ID不能为空", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_nullReviewerId_throwsException() {
        ReviewCreateDTO dto = buildValidDto();
        dto.setReviewerId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(dto));
        assertEquals("评论者ID不能为空", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    // ==================== listByPost tests ====================

    @Test
    void listByPost_success() {
        ReviewEntity review = buildReview();
        when(reviewMapper.countByPostId("post001")).thenReturn(1);
        when(reviewMapper.listByPostId(eq("post001"), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of(review));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));

        PageResult<ReviewVO> result = reviewService.listByPost("post001", 1, 10, null, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("review001", result.getRecords().get(0).getReviewId());
        assertEquals("buyer001", result.getRecords().get(0).getReviewerId());
        assertEquals("BuyerName", result.getRecords().get(0).getReviewerName());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void listByPost_emptyPostId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.listByPost("", 1, 10, null, null));
        assertEquals("帖子ID不能为空", ex.getMessage());
    }

    // ==================== listByOrder tests ====================

    @Test
    void listByOrder_success() {
        ReviewEntity review = buildReview();
        when(reviewMapper.countByOrderId("order001")).thenReturn(1);
        when(reviewMapper.listByOrderId(eq("order001"), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of(review));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));

        PageResult<ReviewVO> result = reviewService.listByOrder("order001", 1, 10, null, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("review001", result.getRecords().get(0).getReviewId());
        assertEquals("order001", result.getRecords().get(0).getOrderId());
    }

    @Test
    void listByOrder_emptyOrderId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.listByOrder("", 1, 10, null, null));
        assertEquals("订单ID不能为空", ex.getMessage());
    }

    // ==================== listReceivedByUser tests ====================

    @Test
    void listReceivedByUser_success() {
        ReviewEntity review = buildReview();
        when(reviewMapper.countBySellerId("seller001")).thenReturn(1);
        when(reviewMapper.listBySellerId(eq("seller001"), eq(0), eq(10), isNull(), eq("desc")))
                .thenReturn(List.of(review));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "BuyerName"));

        PageResult<ReviewVO> result = reviewService.listReceivedByUser("seller001", 1, 10, null, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("review001", result.getRecords().get(0).getReviewId());
    }

    @Test
    void listReceivedByUser_emptyUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.listReceivedByUser("", 1, 10, null, null));
        assertEquals("用户ID不能为空", ex.getMessage());
    }

    // ==================== getSellerRating tests ====================

    @Test
    void getSellerRating_successWithReviews() {
        SellerEntity seller = buildSeller();
        when(sellerMapper.getBySellerId("seller001")).thenReturn(seller);
        when(reviewMapper.countBySellerId("seller001")).thenReturn(2);
        when(reviewMapper.avgRatingBySellerId("seller001")).thenReturn(4.5);
        when(reviewMapper.countPositiveBySellerId("seller001", ReviewConstant.POSITIVE_RATING_THRESHOLD)).thenReturn(2);

        SellerRatingVO result = reviewService.getSellerRating("seller001");

        assertNotNull(result);
        assertEquals("seller001", result.getSellerId());
        assertEquals(2, result.getTotalReviews());
        assertEquals(BigDecimal.valueOf(4.5).setScale(2, RoundingMode.HALF_UP), result.getAverageRating());
        assertEquals(new BigDecimal("100.00"), result.getPositiveRatingRate());
    }

    @Test
    void getSellerRating_sellerNotFound_throwsException() {
        when(sellerMapper.getBySellerId("sellerNotFound")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.getSellerRating("sellerNotFound"));
        assertEquals("卖家不存在", ex.getMessage());
    }

    @Test
    void getSellerRating_noReviews() {
        SellerEntity seller = buildSeller();
        when(sellerMapper.getBySellerId("seller001")).thenReturn(seller);
        when(reviewMapper.countBySellerId("seller001")).thenReturn(0);

        SellerRatingVO result = reviewService.getSellerRating("seller001");

        assertNotNull(result);
        assertEquals("seller001", result.getSellerId());
        assertEquals(0, result.getTotalReviews());
        assertEquals(BigDecimal.ZERO, result.getAverageRating());
        assertEquals(BigDecimal.ZERO, result.getPositiveRatingRate());
    }
}
