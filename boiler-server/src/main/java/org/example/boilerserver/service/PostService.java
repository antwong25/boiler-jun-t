package org.example.boilerserver.service;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.AdminPostQueryDTO;
import org.example.boilerpojo.PostCreateDTO;
import org.example.boilerpojo.PostPageQueryDTO;
import org.example.boilerpojo.PostUpdateDTO;
import org.example.boilerpojo.PostVO;

import java.util.List;

public interface PostService {
    PostVO createPost(PostCreateDTO dto);

    PostVO getPostDetail(String postId);

    PostVO updatePost(PostUpdateDTO dto);

    void deletePost(String postId, String sellerId);

    void delistPost(String postId, String sellerId);

    void banPost(String postId, String adminUserId);

    PageResult<PostVO> listPublishedPosts(PostPageQueryDTO dto);

    PageResult<PostVO> filterPublishedPosts(PostPageQueryDTO dto);

    List<PostVO> listSellerPosts(String sellerId);

    PageResult<PostVO> adminListPosts(AdminPostQueryDTO dto);

    PostVO adminGetPostDetail(String postId);
}
