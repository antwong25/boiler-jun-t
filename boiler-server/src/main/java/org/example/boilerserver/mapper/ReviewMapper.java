package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.ReviewEntity;

import java.util.List;

@Mapper
public interface ReviewMapper {
    int insert(ReviewEntity reviewEntity);

    /**
     * 查询某订单下某买家是否已评论（单方向仅1次）
     */
    ReviewEntity getByOrderIdAndBuyerId(@Param("orderId") String orderId,
                                        @Param("buyerId") String buyerId);

    /**
     * 按帖子分页查询评论（支持排序）
     */
    List<ReviewEntity> listByPostId(@Param("postId") String postId,
                                    @Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("sortField") String sortField,
                                    @Param("sortOrder") String sortOrder);

    int countByPostId(String postId);

    /**
     * 按订单分页查询评论（支持排序）
     */
    List<ReviewEntity> listByOrderId(@Param("orderId") String orderId,
                                     @Param("offset") int offset,
                                     @Param("size") int size,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);

    int countByOrderId(String orderId);

    /**
     * 查询卖家收到的所有评论（通过 order → transaction 关联，支持分页排序）
     */
    List<ReviewEntity> listBySellerId(@Param("sellerId") String sellerId,
                                      @Param("offset") int offset,
                                      @Param("size") int size,
                                      @Param("sortField") String sortField,
                                      @Param("sortOrder") String sortOrder);

    /**
     * 统计卖家收到的评论总数
     */
    int countBySellerId(String sellerId);

    /**
     * 统计卖家收到的好评数（rating >= threshold）
     */
    int countPositiveBySellerId(@Param("sellerId") String sellerId,
                                @Param("threshold") int threshold);

    /**
     * 计算卖家收到评论的平均评分
     */
    Double avgRatingBySellerId(String sellerId);
}
