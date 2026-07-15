package org.example.boilerpojo;

import lombok.Data;

import java.util.List;

@Data
public class PostEmbeddingRecalculateVO {
    private Integer totalPostCount;
    private Integer generatedCount;
    private Integer failedCount;
    private List<PostEmbeddingResultVO> results;

    @Data
    public static class PostEmbeddingResultVO {
        private String postId;
        private String title;
        private Boolean success;
        private String errorMessage;
    }
}
