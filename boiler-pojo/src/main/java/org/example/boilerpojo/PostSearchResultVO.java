package org.example.boilerpojo;

import lombok.Data;

@Data
public class PostSearchResultVO {
    private String postId;
    private Double score;
    private PostVO post;
}
