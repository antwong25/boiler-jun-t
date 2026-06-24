package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.ChatSessionDO;

import java.util.List;

/**
 * 聊天会话数据访问层接口，负责 chatSession 表的 CRUD 操作。
 */
@Mapper
public interface ChatSessionMapper {

    /**
     * 精准匹配唯一未归档会话：以 buyerId + sellerId + postId 三元组为唯一性约束，
     * 仅查询 is_archived = false 的记录。
     *
     * @param buyerId  买家业务 ID
     * @param sellerId 卖家业务 ID
     * @param postId   关联帖子 ID
     * @return 匹配到的唯一 ChatSessionDO，无匹配时返回 null
     */
    ChatSessionDO selectUniqueSession(@Param("buyerId") String buyerId,
                                      @Param("sellerId") String sellerId,
                                      @Param("postId") String postId);

    /**
     * 查询当前用户所有未归档会话（兼容买家/卖家双重身份），
     * 按 last_message_time 倒序排列，最新消息在前的会话排在最前。
     *
     * @param userId 当前用户业务 ID（可以是 buyerId 或 sellerId）
     * @return 该用户所有未归档会话列表，无会话时返回空列表
     */
    List<ChatSessionDO> selectUserUnArchiveSession(@Param("userId") String userId);

    /**
     * 按主键 sessionId 查询单条会话记录（不区分归档状态）。
     *
     * @param sessionId 会话主键
     * @return 匹配到的 ChatSessionDO，无匹配时返回 null
     */
    ChatSessionDO selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 插入一条新的聊天会话记录。
     *
     * @param session 待插入的会话实体
     * @return 受影响行数
     */
    int insert(ChatSessionDO session);

    /**
     * 更新指定会话的消息列表与最后消息时间。
     * messageList 字段通过 typeHandler 将 List<MessageItem> 序列化为 JSON 写入数据库。
     *
     * @param sessionId       目标会话主键
     * @param messageList     新的完整消息列表
     * @param lastMessageTime 新的最后消息时间
     * @return 受影响行数
     */
    int updateMessageList(@Param("sessionId") String sessionId,
                          @Param("messageList") List<org.example.boilerpojo.MessageItem> messageList,
                          @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);
}
