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
     * 提交评论（线下交易完成后的双向互评）
     * 校验链：订单存在 → 订单已完成 → 评论者为订单买方或卖方 → 当前方向未评论过
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
        if (dto.getRating() == null
                || (dto.getRating() != ReviewConstant.MIN_RATING && dto.getRating() != ReviewConstant.MAX_RATING)) {
            throw new IllegalArgumentException("评价结果仅支持好评或差评");
        }

        OrderEntity order = getExistingOrder(dto.getOrderId());
        if (!OrderConstant.STATUS_COMPLETED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅已完成的订单可以评论");
        }

        TransactionEntity transaction = getExistingTransaction(order.getTransactionId());
        boolean reviewerIsBuyer = transaction.getBuyerId().equals(dto.getReviewerId());
        boolean reviewerIsSeller = transaction.getSellerId().equals(dto.getReviewerId());
        if (!reviewerIsBuyer && !reviewerIsSeller) {
            throw new IllegalArgumentException("仅订单买卖双方可以互评");
        }

        ReviewEntity existing = reviewMapper.getByOrderIdAndReviewerId(dto.getOrderId(), dto.getReviewerId());
        if (existing != null) {
            throw new IllegalArgumentException("您已对此订单发表过评论");
        }

        String postId = transaction.getLogisticsInfo();
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("无法确定评论关联的帖子");
        }

        String revieweeId = reviewerIsBuyer ? transaction.getSellerId() : transaction.getBuyerId();
        String reviewId = UUID.randomUUID().toString().replace("-", "");
        ReviewEntity review = new ReviewEntity();
        review.setReviewId(reviewId);
        review.setReviewerId(dto.getReviewerId());
        review.setRevieweeId(revieweeId);
        review.setPostId(postId);
        review.setOrderId(dto.getOrderId());
        review.setRating(dto.getRating());
        review.setContent(StringUtils.hasText(dto.getContent()) ? HtmlUtils.htmlEscape(dto.getContent().trim()) : null);
        review.setReviewTime(LocalDate.now());
        reviewMapper.insert(review);
        log.info("评论提交成功, reviewId={}", reviewId);

        if (reviewerIsBuyer) {
            updateSellerPositiveRatingRate(revieweeId);
        }

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

        long total = reviewMapper.countByRevieweeId(userId);
        List<ReviewVO> records = reviewMapper.listByRevieweeId(userId, offset, pageSize, sf, so)
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

        int totalReviews = reviewMapper.countByRevieweeId(sellerId);
        vo.setTotalReviews(totalReviews);

        if (totalReviews == 0) {
            vo.setAverageRating(BigDecimal.ZERO);
            vo.setPositiveRatingRate(BigDecimal.ZERO);
        } else {
            Double avgRating = reviewMapper.avgRatingByRevieweeId(sellerId);
            vo.setAverageRating(avgRating != null
                    ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            int positiveCount = reviewMapper.countPositiveByRevieweeId(sellerId, ReviewConstant.POSITIVE_RATING_THRESHOLD);
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

        int totalReviews = reviewMapper.countByRevieweeId(sellerId);
        if (totalReviews == 0) {
            seller.setPositiveRatingRate(BigDecimal.ZERO);
        } else {
            int positiveCount = reviewMapper.countPositiveByRevieweeId(sellerId, ReviewConstant.POSITIVE_RATING_THRESHOLD);
            BigDecimal rate = BigDecimal.valueOf(positiveCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
            seller.setPositiveRatingRate(rate);
        }
        sellerMapper.update(seller);
    }

    /**
     * 组装评论视图对象，填充评价双方名称
     */
    private ReviewVO buildReviewVO(ReviewEntity review) {
        ReviewVO vo = new ReviewVO();
        vo.setReviewId(review.getReviewId());
        vo.setReviewerId(review.getReviewerId());
        vo.setRevieweeId(review.getRevieweeId());
        vo.setPostId(review.getPostId());
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setRatingLabel(review.getRating() != null && review.getRating() >= ReviewConstant.POSITIVE_RATING_THRESHOLD ? "好评" : "差评");
        vo.setContent(review.getContent());
        vo.setReviewTime(review.getReviewTime());

        UserEntity reviewer = userMapper.getByUserId(review.getReviewerId());
        if (reviewer != null) {
            vo.setReviewerName(reviewer.getUsername());
        }
        UserEntity reviewee = userMapper.getByUserId(review.getRevieweeId());
        if (reviewee != null) {
            vo.setRevieweeName(reviewee.getUsername());
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
