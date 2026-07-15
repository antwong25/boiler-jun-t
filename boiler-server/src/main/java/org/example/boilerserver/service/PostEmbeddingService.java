package org.example.boilerserver.service;

import org.example.boilerpojo.PostEmbeddingRecalculateVO;

public interface PostEmbeddingService {
    void vectorizePost(String postId);

    void deletePostVector(String postId);

    PostEmbeddingRecalculateVO vectorizeAllPosts();
}
