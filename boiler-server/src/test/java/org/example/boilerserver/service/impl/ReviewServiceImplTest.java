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
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private ReviewCreateDTO buildDto(String reviewerId, Integer rating, String content) {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId("order001");
        dto.setReviewerId(reviewerId);
        dto.setRating(rating);
        dto.setContent(content);
        return dto;
    }

    private ReviewEntity buildStoredReview(String reviewerId, String revieweeId, int rating) {
        ReviewEntity review = new ReviewEntity();
        review.setReviewId("review001");
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setPostId("post001");
        review.setOrderId("order001");
        review.setRating(rating);
        review.setContent("已评价");
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
        seller.setPositiveRatingRate(BigDecimal.ZERO);
        return seller;
    }

    @Test
    void submitReview_buyerToSeller_success() {
        ReviewCreateDTO dto = buildDto("buyer001", ReviewConstant.MAX_RATING, "合作顺利");

        when(orderMapper.getByOrderId("order001")).thenReturn(buildCompletedOrder());
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(buildTransaction());
        when(reviewMapper.getByOrderIdAndReviewerId("order001", "buyer001")).thenReturn(null);
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(reviewMapper.countByRevieweeId("seller001")).thenReturn(1);
        when(reviewMapper.countPositiveByRevieweeId("seller001", ReviewConstant.POSITIVE_RATING_THRESHOLD)).thenReturn(1);
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "买家A"));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "卖家B"));

        ReviewVO result = reviewService.submitReview(dto);

        assertNotNull(result);
        assertEquals("buyer001", result.getReviewerId());
        assertEquals("seller001", result.getRevieweeId());
        assertEquals("好评", result.getRatingLabel());
        verify(reviewMapper).insert(any(ReviewEntity.class));
        verify(sellerMapper).update(any(SellerEntity.class));
    }

    @Test
    void submitReview_sellerToBuyer_success() {
        ReviewCreateDTO dto = buildDto("seller001", ReviewConstant.MIN_RATING, "");

        when(orderMapper.getByOrderId("order001")).thenReturn(buildCompletedOrder());
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(buildTransaction());
        when(reviewMapper.getByOrderIdAndReviewerId("order001", "seller001")).thenReturn(null);
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "卖家B"));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "买家A"));

        ReviewVO result = reviewService.submitReview(dto);

        assertNotNull(result);
        assertEquals("seller001", result.getReviewerId());
        assertEquals("buyer001", result.getRevieweeId());
        assertEquals("差评", result.getRatingLabel());
        verify(reviewMapper).insert(any(ReviewEntity.class));
        verify(sellerMapper, never()).update(any(SellerEntity.class));
    }

    @Test
    void submitReview_orderNotCompleted_throwsException() {
        OrderEntity order = buildCompletedOrder();
        order.setOrderStatus(OrderConstant.STATUS_IN_PROGRESS);
        when(orderMapper.getByOrderId("order001")).thenReturn(order);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(buildDto("buyer001", ReviewConstant.MAX_RATING, "ok")));
        assertEquals("仅已完成的订单可以评论", ex.getMessage());
    }

    @Test
    void submitReview_outsiderReviewer_throwsException() {
        when(orderMapper.getByOrderId("order001")).thenReturn(buildCompletedOrder());
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(buildTransaction());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(buildDto("other001", ReviewConstant.MAX_RATING, "ok")));
        assertEquals("仅订单买卖双方可以互评", ex.getMessage());
    }

    @Test
    void submitReview_duplicateReview_throwsException() {
        when(orderMapper.getByOrderId("order001")).thenReturn(buildCompletedOrder());
        when(transactionMapper.getByTransactionId("txn001")).thenReturn(buildTransaction());
        when(reviewMapper.getByOrderIdAndReviewerId("order001", "buyer001"))
                .thenReturn(buildStoredReview("buyer001", "seller001", ReviewConstant.MAX_RATING));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(buildDto("buyer001", ReviewConstant.MAX_RATING, "ok")));
        assertEquals("您已对此订单发表过评论", ex.getMessage());
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_invalidBinaryRating_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(buildDto("buyer001", 3, "ok")));
        assertEquals("评价结果仅支持好评或差评", ex.getMessage());
    }

    @Test
    void listReceivedByUser_returnsGenericReviews() {
        ReviewEntity review = buildStoredReview("seller001", "buyer001", ReviewConstant.MIN_RATING);
        when(reviewMapper.countByRevieweeId("buyer001")).thenReturn(1);
        when(reviewMapper.listByRevieweeId("buyer001", 0, 10, "reviewTime", "desc")).thenReturn(List.of(review));
        when(userMapper.getByUserId("seller001")).thenReturn(buildUser("seller001", "卖家B"));
        when(userMapper.getByUserId("buyer001")).thenReturn(buildUser("buyer001", "买家A"));

        PageResult<ReviewVO> page = reviewService.listReceivedByUser("buyer001", 1, 10, "reviewTime", "desc");

        assertEquals(1, page.getTotal());
        assertEquals("seller001", page.getRecords().get(0).getReviewerId());
        assertEquals("buyer001", page.getRecords().get(0).getRevieweeId());
    }

    @Test
    void getSellerRating_usesGenericReviewStats() {
        when(sellerMapper.getBySellerId("seller001")).thenReturn(buildSeller());
        when(reviewMapper.countByRevieweeId("seller001")).thenReturn(2);
        when(reviewMapper.countPositiveByRevieweeId("seller001", ReviewConstant.POSITIVE_RATING_THRESHOLD)).thenReturn(1);
        when(reviewMapper.avgRatingByRevieweeId("seller001")).thenReturn(3.0);

        SellerRatingVO result = reviewService.getSellerRating("seller001");

        assertEquals(2, result.getTotalReviews());
        assertEquals(new BigDecimal("3.00"), result.getAverageRating());
        assertEquals(new BigDecimal("50.00"), result.getPositiveRatingRate());
    }
}
