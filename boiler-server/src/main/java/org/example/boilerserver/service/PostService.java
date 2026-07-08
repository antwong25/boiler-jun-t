package org.example.boilerserver.service;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.PostCreateDTO;
import org.example.boilerpojo.PostPageQueryDTO;
import org.example.boilerpojo.PostUpdateDTO;
import org.example.boilerpojo.PostVO;

public interface PostService {
    PostVO createPost(PostCreateDTO dto);

    PostVO getPostDetail(String postId);

    PostVO updatePost(PostUpdateDTO dto);

    void deletePost(String postId, String sellerId);

    PageResult<PostVO> listPublishedPosts(PostPageQueryDTO dto);

    PageResult<PostVO> filterPublishedPosts(PostPageQueryDTO dto);
}
