package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 交易视图对象
 */
@Data
public class TransactionVO {
    private String transactionId;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private String shopName;
    private BigDecimal transactionAmount;
    private LocalDate transactionTime;
    private String transactionStatus;
    private String bookingStatus;
    private String logisticsInfo;
    private String postId;
    private String postTitle;
}
