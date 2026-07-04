package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.PostAiChatRequestDTO;
import org.example.boilerpojo.PostAiChatResponseVO;
import org.example.boilerpojo.PostCreateDTO;
import org.example.boilerpojo.PostSemanticSearchDTO;
import org.example.boilerpojo.PostSemanticSearchVO;
import org.example.boilerpojo.PostUpdateDTO;
import org.example.boilerpojo.PostVO;
import org.example.boilerserver.service.PostAiChatService;
import org.example.boilerserver.service.PostSemanticSearchService;
import org.example.boilerserver.service.PostService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final PostSemanticSearchService postSemanticSearchService;
    private final PostAiChatService postAiChatService;

    public PostController(
            PostService postService,
            PostSemanticSearchService postSemanticSearchService,
            PostAiChatService postAiChatService
    ) {
        this.postService = postService;
        this.postSemanticSearchService = postSemanticSearchService;
        this.postAiChatService = postAiChatService;
    }

    // 卖家发布帖子时同步提交锅炉详情
    @PostMapping
    public Result<PostVO> createPost(@RequestBody PostCreateDTO dto) {
        return Result.success(postService.createPost(dto));
    }

    // 查看帖子详情时会累计浏览量
    @GetMapping("/{postId}")
    public Result<PostVO> getPostDetail(@PathVariable String postId) {
        return Result.success(postService.getPostDetail(postId));
    }

    // 编辑帖子后重新进入待审核状态
    @PutMapping
    public Result<PostVO> updatePost(@RequestBody PostUpdateDTO dto) {
        return Result.success(postService.updatePost(dto));
    }

    // 仅发帖卖家本人可以删除帖子
    @DeleteMapping("/{postId}")
    public Result<String> deletePost(@PathVariable String postId, @RequestParam String sellerId) {
        postService.deletePost(postId, sellerId);
        return Result.success("删除成功");
    }

    @PostMapping("/semantic-search")
    public Result<List<PostSemanticSearchVO>> semanticSearch(@RequestBody PostSemanticSearchDTO dto) {
        return Result.success(postSemanticSearchService.search(dto));
    }

    @PostMapping("/ai-chat")
    public Result<PostAiChatResponseVO> aiChat(@RequestBody PostAiChatRequestDTO dto) {
        return Result.success(postAiChatService.chat(dto));
    }
}
