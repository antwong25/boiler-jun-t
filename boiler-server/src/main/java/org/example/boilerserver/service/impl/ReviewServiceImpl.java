package org.example.boilerserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.boilercommon.PageResult;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.ReviewCreateDTO;
import org.example.boilerpojo.ReviewEntity;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.SellerRatingVO;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.ReviewService;
import org.example.constant.OrderConstant;
import org.example.constant.ReviewConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    /** ID 格式校验：仅允许字母、数字、下划线、连字符，长度1-64，防止SQL注入 */
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;
    private final SellerMapper sellerMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             OrderMapper orderMapper,
                             TransactionMapper transactionMapper,
                             UserMapper userMapper,
                             SellerMapper sellerMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.transactionMapper = transactionMapper;
        this.userMapper = userMapper;
        this.sellerMapper = sellerMapper;
    }

    /**
     * 提交评论（买家评卖家）
     * 校验链：订单存在 → 订单已完成 → 评论者为订单买方 → 该方向未评论过
     * 事务边界：插入评论 + 更新卖家好评率
     */
    @Override
    @Transactional
    public ReviewVO submitReview(ReviewCreateDTO dto) {
        log.info("提交评论, orderId={}, reviewerId={}, rating={}", dto.getOrderId(), dto.getReviewerId(), dto.getRating());
        // 参数校验
        if (dto == null || !StringUtils.hasText(dto.getOrderId())) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (!StringUtils.hasText(dto.getReviewerId())) {
            throw new IllegalArgumentException("评论者ID不能为空");
        }
        validateIdFormat(dto.getOrderId(), "订单ID");
        validateIdFormat(dto.getReviewerId(), "评论者ID");
        if (dto.getRating() == null || dto.getRating() < ReviewConstant.MIN_RATING
                || dto.getRating() > ReviewConstant.MAX_RATING) {
            throw new IllegalArgumentException("评分必须在" + ReviewConstant.MIN_RATING + "到" + ReviewConstant.MAX_RATING + "之间");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        // 校验订单存在且已完成
        OrderEntity order = getExistingOrder(dto.getOrderId());
        if (!OrderConstant.STATUS_COMPLETED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅已完成的订单可以评论");
        }

        // 校验交易存在
        TransactionEntity transaction = getExistingTransaction(order.getTransactionId());

        // 校验评论者为订单买方（单向评论：仅买家评卖家）
        if (!transaction.getBuyerId().equals(dto.getReviewerId())) {
            throw new IllegalArgumentException("仅订单买方可评价卖家");
        }

        // 校验该买方对此订单仅评论1次
        ReviewEntity existing = reviewMapper.getByOrderIdAndBuyerId(dto.getOrderId(), dto.getReviewerId());
        if (existing != null) {
            throw new IllegalArgumentException("您已对此订单发表过评论");
        }

        // 从交易记录的 logisticsInfo 字段获取 postId
        String postId = transaction.getLogisticsInfo();
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("无法确定评论关联的帖子");
        }

        // 写入评论（提交后不可编辑、不可删除）
        String reviewId = UUID.randomUUID().toString().replace("-", "");
        ReviewEntity review = new ReviewEntity();
        review.setReviewId(reviewId);
        review.setBuyerId(dto.getReviewerId());
        review.setPostId(postId);
        review.setOrderId(dto.getOrderId());
        review.setRating(dto.getRating());
        // HTML 转义防止 XSS 攻击（存储转义后的内容，前端渲染时安全）
        review.setContent(HtmlUtils.htmlEscape(dto.getContent().trim()));
        review.setReviewTime(LocalDate.now());
        reviewMapper.insert(review);
        log.info("评论提交成功, reviewId={}", reviewId);

        // 更新卖家好评率
        updateSellerPositiveRatingRate(transaction.getSellerId());

        return buildReviewVO(review);
    }

    @Override
    public PageResult<ReviewVO> listByPost(String postId, Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        validateIdFormat(postId, "帖子ID");
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;
        String sf = validateSortField(sortField, new String[]{"reviewTime", "rating"});
        String so = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";

        long total = reviewMapper.countByPostId(postId);
        List<ReviewVO> records = reviewMapper.listByPostId(postId, offset, pageSize, sf, so)
                .stream().map(this::buildReviewVO).toList();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ReviewVO> listByOrder(String orderId, Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        validateIdFormat(orderId, "订单ID");
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;
        String sf = validateSortField(sortField, new String[]{"reviewTime", "rating"});
        String so = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";

        long total = reviewMapper.countByOrderId(orderId);
        List<ReviewVO> records = reviewMapper.listByOrderId(orderId, offset, pageSize, sf, so)
                .stream().map(this::buildReviewVO).toList();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ReviewVO> listReceivedByUser(String userId, Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        validateIdFormat(userId, "用户ID");
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;
        String sf = validateSortField(sortField, new String[]{"reviewTime", "rating"});
        String so = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";

        long total = reviewMapper.countBySellerId(userId);
        List<ReviewVO> records = reviewMapper.listBySellerId(userId, offset, pageSize, sf, so)
                .stream().map(this::buildReviewVO).toList();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public SellerRatingVO getSellerRating(String sellerId) {
        if (!StringUtils.hasText(sellerId)) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        validateIdFormat(sellerId, "卖家ID");
        SellerEntity seller = sellerMapper.getBySellerId(sellerId);
        if (seller == null) {
            throw new IllegalArgumentException("卖家不存在");
        }

        SellerRatingVO vo = new SellerRatingVO();
        vo.setSellerId(sellerId);

        int totalReviews = reviewMapper.countBySellerId(sellerId);
        vo.setTotalReviews(totalReviews);

        if (totalReviews == 0) {
            vo.setAverageRating(BigDecimal.ZERO);
            vo.setPositiveRatingRate(BigDecimal.ZERO);
        } else {
            Double avgRating = reviewMapper.avgRatingBySellerId(sellerId);
            vo.setAverageRating(avgRating != null
                    ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            int positiveCount = reviewMapper.countPositiveBySellerId(sellerId, ReviewConstant.POSITIVE_RATING_THRESHOLD);
            BigDecimal rate = BigDecimal.valueOf(positiveCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
            vo.setPositiveRatingRate(rate);
        }

        return vo;
    }

    // ==================== 私有辅助方法 ====================

    private OrderEntity getExistingOrder(String orderId) {
        OrderEntity order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private TransactionEntity getExistingTransaction(String transactionId) {
        TransactionEntity transaction = transactionMapper.getByTransactionId(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("交易不存在");
        }
        return transaction;
    }

    /**
     * 更新卖家好评率（基于所有收到的评论重新计算）
     */
    private void updateSellerPositiveRatingRate(String sellerId) {
        SellerEntity seller = sellerMapper.getBySellerId(sellerId);
        if (seller == null) {
            return;
        }

        int totalReviews = reviewMapper.countBySellerId(sellerId);
        if (totalReviews == 0) {
            seller.setPositiveRatingRate(BigDecimal.ZERO);
        } else {
            int positiveCount = reviewMapper.countPositiveBySellerId(sellerId, ReviewConstant.POSITIVE_RATING_THRESHOLD);
            BigDecimal rate = BigDecimal.valueOf(positiveCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
            seller.setPositiveRatingRate(rate);
        }
        sellerMapper.update(seller);
    }

    /**
     * 组装评论视图对象，填充评论者名称
     */
    private ReviewVO buildReviewVO(ReviewEntity review) {
        ReviewVO vo = new ReviewVO();
        vo.setReviewId(review.getReviewId());
        vo.setReviewerId(review.getBuyerId());
        vo.setPostId(review.getPostId());
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setReviewTime(review.getReviewTime());

        // 填充评论者名称
        UserEntity reviewer = userMapper.getByUserId(review.getBuyerId());
        if (reviewer != null) {
            vo.setReviewerName(reviewer.getUsername());
        }

        return vo;
    }

    private String validateSortField(String sortField, String[] allowedFields) {
        if (!StringUtils.hasText(sortField)) {
            return null;
        }
        for (String allowed : allowedFields) {
            if (allowed.equals(sortField)) {
                return sortField;
            }
        }
        return null;
    }

    /**
     * 校验ID格式（仅允许字母、数字、下划线、连字符，防止SQL注入）
     */
    private void validateIdFormat(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(fieldName + "格式非法");
        }
    }
}
