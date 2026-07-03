package org.example.boilerpojo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 订单实体类，对应 order 表
 */
@Data
public class OrderEntity {
    private String orderId;
    private String transactionId;
    private String orderStatus;
    private LocalDate createTime;
    private LocalDate updateTime;
}
