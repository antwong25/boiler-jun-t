package org.example.boilerserver.service;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.ReviewCreateDTO;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerpojo.SellerRatingVO;

public interface ReviewService {

    ReviewVO submitReview(ReviewCreateDTO dto);

    PageResult<ReviewVO> listByPost(String postId, Integer pageNum, Integer pageSize, String sortField, String sortOrder);

    PageResult<ReviewVO> listByOrder(String orderId, Integer pageNum, Integer pageSize, String sortField, String sortOrder);

    PageResult<ReviewVO> listReceivedByUser(String userId, Integer pageNum, Integer pageSize, String sortField, String sortOrder);

    SellerRatingVO getSellerRating(String sellerId);
}
