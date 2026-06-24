package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 交易实体类，对应 transaction 表
 */
@Data
public class TransactionEntity {
    private String transactionId;
    private String buyerId;
    private String sellerId;
    private BigDecimal transactionAmount;
    private LocalDate transactionTime;
    private String transactionStatus;
    private String bookingStatus;
    private String logisticsInfo;
}
