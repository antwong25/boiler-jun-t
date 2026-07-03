package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单视图对象（含交易、帖子、买卖方信息）
 */
@Data
public class OrderVO {
    // 订单信息
    private String orderId;
    private String transactionId;
    private String orderStatus;
    private LocalDate createTime;
    private LocalDate updateTime;
    // 交易信息
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private String shopName;
    private BigDecimal transactionAmount;
    private String transactionStatus;
    private String bookingStatus;
    // 帖子信息
    private String postId;
    private String postTitle;
    private BigDecimal postPrice;
}
