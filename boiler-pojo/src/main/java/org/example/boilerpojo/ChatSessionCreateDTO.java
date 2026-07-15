package org.example.boilerpojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建聊天会话入参。
 * 以 buyerId + sellerId + postId 三元组作为会话唯一性约束。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionCreateDTO {

    /** 买家业务 ID */
    private String buyerId;

    /** 卖家业务 ID */
    private String sellerId;

    /** 关联的帖子/商品 ID */
    private String postId;
}
