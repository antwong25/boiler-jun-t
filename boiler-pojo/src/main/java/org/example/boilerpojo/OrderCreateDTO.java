package org.example.boilerpojo;

import lombok.Data;

/**
 * 创建订单请求DTO
 */
@Data
public class OrderCreateDTO {
    private String transactionId;
    private String operatorId;
}
