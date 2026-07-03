package org.example.boilerpojo;

import lombok.Data;

@Data
public class PostSemanticSearchVO {
    private String postId;
    private Double score;
    private PostVO post;
}
