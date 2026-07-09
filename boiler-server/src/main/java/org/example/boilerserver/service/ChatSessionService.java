package org.example.boilerserver.service;

import org.example.boilerpojo.ChatSendMsgDTO;
import org.example.boilerpojo.ChatSessionCreateDTO;
import org.example.boilerpojo.ChatSessionVO;

import java.util.List;

/**
 * 聊天会话业务逻辑接口，定义会话创建、消息收发、会话列表查询的操作契约。
 */
public interface ChatSessionService {

    /**
     * 获取或创建聊天会话。
     * 以 buyerId + sellerId + postId 三元组为唯一条件匹配：
     * 若会话已存在，直接返回已有会话的 VO；若不存在，用 UUID 生成 sessionId，
     * 初始化空消息列表并入库，然后返回新会话 VO。
     *
     * @param dto 包含 buyerId, sellerId, postId 的入参
     * @return 已有或新建的 ChatSessionVO
     */
    ChatSessionVO getOrCreateSession(ChatSessionCreateDTO dto);

    /**
     * 向指定会话发送一条消息。
     * 根据 sessionId 查询会话，若不存在则抛出业务异常。
     * 组装 MessageItem 追加到 messageList 尾部，更新 lastMessageTime 为当前时间，整体更新入库。
     *
     * @param dto 包含 sessionId, senderId, senderType, content, msgType 的入参
     * @return true 表示消息发送成功
     */
    boolean sendMessage(ChatSendMsgDTO dto);

    /**
     * 查询当前登录用户所有未归档会话，按 lastMessageTime 倒序排列。
     *
     * @param userId 当前用户业务 ID（兼容买家/卖家身份）
     * @return 该用户所有未归档会话的 VO 列表，无会话时返回空列表
     */
    List<ChatSessionVO> listUserSession(String userId);
}
