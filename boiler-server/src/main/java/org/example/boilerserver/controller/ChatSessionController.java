package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.ChatSendMsgDTO;
import org.example.boilerpojo.ChatSessionCreateDTO;
import org.example.boilerpojo.ChatSessionVO;
import org.example.boilerserver.service.ChatSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天模块 REST 控制器，统一请求前缀 /api/chat。
 * 提供会话创建、消息发送、会话列表查询三个核心接口。
 * 买家用户虽无发帖权限，但可正常使用本模块全部聊天接口。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 构造器注入 ChatSessionService。
     */
    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 创建或获取聊天会话。
     * 以 buyerId + sellerId + postId 三元组为唯一键，已存在则返回已有会话，否则新建。
     *
     * @param dto 包含 buyerId, sellerId, postId 的入参
     * @return Result 包装的 ChatSessionVO
     */
    @PostMapping("/session/create")
    public Result<ChatSessionVO> createSession(@RequestBody ChatSessionCreateDTO dto) {
        ChatSessionVO sessionVO = chatSessionService.getOrCreateSession(dto);
        return Result.success(sessionVO);
    }

    /**
     * 向指定会话发送一条聊天消息。
     * 服务端自动补全 sendTime，将消息追加到会话消息列表尾部。
     *
     * @param dto 包含 sessionId, senderId, senderType, content, msgType 的入参
     * @return Result 包装的 Boolean，true 表示消息发送成功
     */
    @PostMapping("/message/send")
    public Result<Boolean> sendMessage(@RequestBody ChatSendMsgDTO dto) {
        boolean result = chatSessionService.sendMessage(dto);
        return Result.success(result);
    }

    /**
     * 查询当前用户所有未归档会话列表，按最近消息时间倒序排列。
     * 同时兼容买家与卖家身份：查询条件为 buyer_id = userId OR seller_id = userId。
     *
     * @param userId 当前登录用户业务 ID
     * @return Result 包装的 List<ChatSessionVO>
     */
    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> listUserSession(@RequestParam("userId") String userId) {
        List<ChatSessionVO> sessionList = chatSessionService.listUserSession(userId);
        return Result.success(sessionList);
    }
}
