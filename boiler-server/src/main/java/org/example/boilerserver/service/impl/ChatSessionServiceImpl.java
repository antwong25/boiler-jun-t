package org.example.boilerserver.service.impl;

import org.example.boilerpojo.ChatSendMsgDTO;
import org.example.boilerpojo.ChatSessionCreateDTO;
import org.example.boilerpojo.ChatSessionDO;
import org.example.boilerpojo.ChatSessionVO;
import org.example.boilerpojo.MessageItem;
import org.example.boilerserver.mapper.ChatSessionMapper;
import org.example.boilerserver.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天会话业务逻辑实现类。
 * 所有写操作均添加 @Transactional 事务保护，确保数据一致性。
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    /**
     * 构造器注入 ChatSessionMapper，便于单元测试与扩展。
     */
    public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }

    /**
     * 获取或创建聊天会话。
     * 以 buyerId + sellerId + postId 三元组为唯一条件匹配未归档会话：
     * - 命中：直接转换为 VO 返回
     * - 未命中：UUID 生成 sessionId，初始化空消息列表并入库，返回新会话 VO
     */
    @Override
    @Transactional
    public ChatSessionVO getOrCreateSession(ChatSessionCreateDTO dto) {
        // 1. 精准匹配已有未归档会话
        ChatSessionDO existingSession = chatSessionMapper.selectUniqueSession(
                dto.getBuyerId(), dto.getSellerId(), dto.getPostId());

        if (existingSession != null) {
            return convertToVO(existingSession);
        }

        // 2. 不存在则创建新会话
        LocalDateTime now = LocalDateTime.now();
        ChatSessionDO newSession = ChatSessionDO.builder()
                .sessionId(UUID.randomUUID().toString())
                .buyerId(dto.getBuyerId())
                .sellerId(dto.getSellerId())
                .postId(dto.getPostId())
                .createTime(now)
                .lastMessageTime(now)
                .isArchived(false)
                .messageList(Collections.emptyList())
                .build();

        chatSessionMapper.insert(newSession);
        return convertToVO(newSession);
    }

    /**
     * 向指定会话发送一条消息。
     * 校验会话存在性，组装 MessageItem 追加到消息列表尾部，更新最后消息时间并持久化。
     */
    @Override
    @Transactional
    public boolean sendMessage(ChatSendMsgDTO dto) {
        // 1. 校验会话存在性
        ChatSessionDO session = chatSessionMapper.selectBySessionId(dto.getSessionId());
        if (session == null) {
            throw new RuntimeException("聊天会话不存在，sessionId: " + dto.getSessionId());
        }

        // 2. 组装新消息
        MessageItem newMessage = MessageItem.builder()
                .senderId(dto.getSenderId())
                .senderType(dto.getSenderType())
                .content(dto.getContent())
                .sendTime(LocalDateTime.now())
                .msgType(dto.getMsgType())
                .build();

        // 3. 追加到现有消息列表（防御性处理 null 列表）
        List<MessageItem> messageList = session.getMessageList();
        if (messageList == null) {
            messageList = new ArrayList<>();
        }
        messageList.add(newMessage);

        // 4. 更新入库
        chatSessionMapper.updateMessageList(dto.getSessionId(), messageList, newMessage.getSendTime());
        return true;
    }

    /**
     * 查询当前登录用户所有未归档会话，按 lastMessageTime 倒序排列。
     */
    @Override
    public List<ChatSessionVO> listUserSession(String userId) {
        List<ChatSessionDO> sessionList = chatSessionMapper.selectUserUnArchiveSession(userId);
        if (sessionList == null || sessionList.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 ChatSessionDO 转换为 ChatSessionVO，逐字段显式映射，避免隐式拷贝的不可控风险。
     */
    private ChatSessionVO convertToVO(ChatSessionDO sessionDO) {
        return ChatSessionVO.builder()
                .sessionId(sessionDO.getSessionId())
                .sellerId(sessionDO.getSellerId())
                .buyerId(sessionDO.getBuyerId())
                .postId(sessionDO.getPostId())
                .createTime(sessionDO.getCreateTime())
                .lastMessageTime(sessionDO.getLastMessageTime())
                .isArchived(sessionDO.getIsArchived())
                .messageList(sessionDO.getMessageList())
                .build();
    }
}
