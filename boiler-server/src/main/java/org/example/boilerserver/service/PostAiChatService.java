package org.example.boilerserver.service;

import org.example.boilerpojo.PostAiChatRequestDTO;
import org.example.boilerpojo.PostAiChatResponseVO;

public interface PostAiChatService {
    PostAiChatResponseVO chat(PostAiChatRequestDTO dto);
}
