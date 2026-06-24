package org.example.boilerpojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话数据库映射实体，与 chatSession 表一一对应。
 * messageList 字段通过自定义 TypeHandler 实现 List<MessageItem> 与 TEXT JSON 的互转。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDO {

    /** 会话主键，UUID 生成 */
    private String sessionId;

    /** 卖家业务 ID */
    private String sellerId;

    /** 买家业务 ID */
    private String buyerId;

    /** 关联的帖子/商品 ID */
    private String postId;

    /** 会话创建时间 */
    private LocalDateTime createTime;

    /** 最近一条消息的发送时间，用于排序 */
    private LocalDateTime lastMessageTime;

    /** 是否已归档（软删除） */
    private Boolean isArchived;

    /** 聊天消息列表，数据库存为 JSON 数组，Java 端为 List<MessageItem> */
    private List<MessageItem> messageList;
}
