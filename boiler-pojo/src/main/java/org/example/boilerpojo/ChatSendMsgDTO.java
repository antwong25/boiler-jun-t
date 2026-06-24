package org.example.boilerpojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送聊天消息入参。
 * 前端传入消息内容后，由服务端补全 sendTime 并持久化。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendMsgDTO {

    /** 目标会话 ID */
    private String sessionId;

    /** 发送者业务 ID */
    private String senderId;

    /** 发送者类型：buyer / seller */
    private String senderType;

    /** 消息正文 */
    private String content;

    /** 消息类型：text / image */
    private String msgType;
}
