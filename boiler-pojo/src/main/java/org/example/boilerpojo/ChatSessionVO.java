package org.example.boilerpojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回前端的聊天会话完整视图对象，字段与 ChatSessionDO 保持一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    /** 会话主键 */
    private String sessionId;

    /** 卖家业务 ID */
    private String sellerId;

    /** 买家业务 ID */
    private String buyerId;

    /** 关联的帖子/商品 ID */
    private String postId;

    /** 会话创建时间 */
    private LocalDateTime createTime;

    /** 最近一条消息的发送时间 */
    private LocalDateTime lastMessageTime;

    /** 是否已归档 */
    private Boolean isArchived;

    /** 聊天消息列表 */
    private List<MessageItem> messageList;
}
