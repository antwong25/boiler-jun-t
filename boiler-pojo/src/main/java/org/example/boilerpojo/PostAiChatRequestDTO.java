package org.example.boilerpojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostAiChatRequestDTO {
    private String currentUserInput;
    private List<AiChatMessageDTO> recentMessages = new ArrayList<>();
    private Integer limit = 5;
    private Double minScore;
}
