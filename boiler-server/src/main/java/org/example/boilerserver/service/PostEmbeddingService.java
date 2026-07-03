package org.example.boilerserver.service;

public interface PostEmbeddingService {
    void vectorizePost(String postId);

    void deletePostVector(String postId);
}
