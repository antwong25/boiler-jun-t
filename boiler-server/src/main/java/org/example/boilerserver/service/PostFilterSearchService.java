package org.example.boilerserver.service;

import org.example.boilerpojo.PostFilterSearchDTO;
import org.example.boilerpojo.PostSearchResultVO;

import java.util.List;

public interface PostFilterSearchService {
    List<PostSearchResultVO> search(PostFilterSearchDTO dto);
}
