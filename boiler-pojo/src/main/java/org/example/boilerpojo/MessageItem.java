package org.example.boilerpojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单条聊天消息结构体，存储在 chatSession 表的 messageList JSON 数组中。
 * 每条消息记录发送者身份、内容、时间及消息类型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageItem {

    /** 发送者业务 ID（买家或卖家） */
    private String senderId;

    /** 发送者类型：buyer（买家）/ seller（卖家） */
    private String senderType;

    /** 消息正文内容 */
    private String content;

    /** 消息发送时间 */
    private LocalDateTime sendTime;

    /** 消息类型：text（文本）/ image（图片） */
    private String msgType;
}
