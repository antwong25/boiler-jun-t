package org.example.boilerpojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostAiChatResponseVO {
    private String action;
    private String assistantReply;
    private String vectorQuery;
    private Integer resultCount;
    private PostFilterSearchDTO structuredFilter;
    private List<PostSearchResultVO> results = new ArrayList<>();
}
