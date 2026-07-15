package org.example.boilerpojo;

import lombok.Data;

@Data
public class PostSemanticSearchDTO {
    private String query;
    private Integer limit = 5;
    private Double minScore;
}
