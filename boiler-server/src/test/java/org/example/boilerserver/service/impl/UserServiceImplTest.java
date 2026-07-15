package org.example.boilerserver.service.impl;

import org.example.boilerpojo.BuyerEntity;
import org.example.boilerpojo.CreditScoreRecalculateVO;
import org.example.boilerpojo.UserEntity;
import org.example.boilerpojo.UserVO;
import org.example.boilerserver.config.FileStorageProperties;
import org.example.boilerserver.mapper.BuyerMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.constant.TransactionConstant;
import org.example.constant.UserConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private BuyerMapper buyerMapper;
    @Mock
    private SellerMapper sellerMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private FileStorageProperties fileStorageProperties;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity buildBuyerUser(String userId, Integer creditScore, String verificationStatus) {
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setUsername("buyer-" + userId);
        user.setUserType(UserConstant.USER_TYPE_BUYER);
        user.setCreditScore(creditScore);
        user.setRegistrationDate(LocalDate.now());
        user.setVerificationStatus(verificationStatus);
        return user;
    }

    private BuyerEntity buildBuyer(String userId) {
        BuyerEntity buyer = new BuyerEntity();
        buyer.setBuyerId(userId);
        return buyer;
    }

    @Test
    void recalculateCreditScore_shouldUseNewUnifiedFormula() {
        UserEntity user = buildBuyerUser("buyer001", 60, UserConstant.VERIFICATION_STATUS_UNVERIFIED);
        when(userMapper.getByUserId("buyer001")).thenReturn(user);
        when(reviewMapper.countPositiveByRevieweeId("buyer001", 4)).thenReturn(2);
        when(reviewMapper.countNegativeByRevieweeId("buyer001", 2)).thenReturn(1);
        when(transactionMapper.countByUserIdAndStatus("buyer001", TransactionConstant.STATUS_COMPLETED)).thenReturn(4);
        when(buyerMapper.getByUserId("buyer001")).thenReturn(buildBuyer("buyer001"));

        UserVO result = userService.recalculateCreditScore("buyer001");

        assertNotNull(result);
        assertEquals(84, result.getCreditScore());
        verify(userMapper).update(any(UserEntity.class));
    }

    @Test
    void recalculateCreditScore_shouldCapComponentScoresAndSuspendConductScore() {
        UserEntity user = buildBuyerUser("buyer002", 65, UserConstant.VERIFICATION_STATUS_SUSPENDED);
        when(userMapper.getByUserId("buyer002")).thenReturn(user);
        when(reviewMapper.countPositiveByRevieweeId("buyer002", 4)).thenReturn(10);
        when(reviewMapper.countNegativeByRevieweeId("buyer002", 2)).thenReturn(0);
        when(transactionMapper.countByUserIdAndStatus("buyer002", TransactionConstant.STATUS_COMPLETED)).thenReturn(10);
        when(buyerMapper.getByUserId("buyer002")).thenReturn(buildBuyer("buyer002"));

        UserVO result = userService.recalculateCreditScore("buyer002");

        assertEquals(90, result.getCreditScore());
        verify(userMapper).update(any(UserEntity.class));
    }

    @Test
    void recalculateAllCreditScores_shouldUpdateOnlyChangedUsers() {
        UserEntity firstUser = buildBuyerUser("buyer001", 75, UserConstant.VERIFICATION_STATUS_UNVERIFIED);
        UserEntity secondUser = buildBuyerUser("buyer002", 60, UserConstant.VERIFICATION_STATUS_UNVERIFIED);
        when(userMapper.listAllUsers()).thenReturn(List.of(firstUser, secondUser));

        when(reviewMapper.countPositiveByRevieweeId("buyer001", 4)).thenReturn(0);
        when(reviewMapper.countNegativeByRevieweeId("buyer001", 2)).thenReturn(0);
        when(transactionMapper.countByUserIdAndStatus("buyer001", TransactionConstant.STATUS_COMPLETED)).thenReturn(0);
        when(buyerMapper.getByUserId("buyer001")).thenReturn(buildBuyer("buyer001"));

        when(reviewMapper.countPositiveByRevieweeId("buyer002", 4)).thenReturn(1);
        when(reviewMapper.countNegativeByRevieweeId("buyer002", 2)).thenReturn(0);
        when(transactionMapper.countByUserIdAndStatus("buyer002", TransactionConstant.STATUS_COMPLETED)).thenReturn(1);
        when(buyerMapper.getByUserId("buyer002")).thenReturn(buildBuyer("buyer002"));

        CreditScoreRecalculateVO result = userService.recalculateAllCreditScores();

        assertEquals(2, result.getTotalUserCount());
        assertEquals(1, result.getUpdatedUserCount());
        assertEquals(2, result.getUsers().size());
        assertEquals(75, result.getUsers().get(0).getCreditScore());
        assertEquals(80, result.getUsers().get(1).getCreditScore());
        verify(userMapper).update(secondUser);
        verify(userMapper, never()).update(firstUser);
    }
}
