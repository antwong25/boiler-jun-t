package org.example.boilerpojo;

import lombok.Data;

/**
 * 创建交易请求DTO（买家预约帖子）
 */
@Data
public class TransactionCreateDTO {
    private String postId;
    private String buyerId;
}
