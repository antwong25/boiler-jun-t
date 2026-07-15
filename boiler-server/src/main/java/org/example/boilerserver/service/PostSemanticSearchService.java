package org.example.boilerserver.service;

import org.example.boilerpojo.PostSemanticSearchDTO;
import org.example.boilerpojo.PostSemanticSearchVO;

import java.util.List;

public interface PostSemanticSearchService {
    List<PostSemanticSearchVO> search(PostSemanticSearchDTO dto);
}
