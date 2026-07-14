package org.example.boilerserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.boilercommon.PageResult;
import org.example.boilerpojo.BuyerEntity;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.ReviewEntity;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.TransactionCreateDTO;
import org.example.boilerpojo.TransactionQueryDTO;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.auth.AuthContext;
import org.example.boilerserver.mapper.BuyerMapper;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.TransactionService;
import org.example.constant.OrderConstant;
import org.example.constant.PostConstant;
import org.example.constant.TransactionConstant;
import org.example.constant.UserConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    /** ID 格式校验：仅允许字母、数字、下划线、连字符，长度1-64，防止SQL注入 */
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final TransactionMapper transactionMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SellerMapper sellerMapper;
    private final BuyerMapper buyerMapper;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;

    public TransactionServiceImpl(TransactionMapper transactionMapper,
                                  PostMapper postMapper,
                                  UserMapper userMapper,
                                  SellerMapper sellerMapper,
                                  BuyerMapper buyerMapper,
                                  OrderMapper orderMapper,
                                  ReviewMapper reviewMapper) {
        this.transactionMapper = transactionMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sellerMapper = sellerMapper;
        this.buyerMapper = buyerMapper;
        this.orderMapper = orderMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 创建交易（买家预约帖子）
     * 事务边界：插入交易记录 + 更新帖子状态为已预约
     */
    @Override
    @Transactional
    public TransactionVO createTransaction(TransactionCreateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getPostId())) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        if (!StringUtils.hasText(dto.getBuyerId())) {
            throw new IllegalArgumentException("买家ID不能为空");
        }
        validateAuthenticatedBuyer(dto.getBuyerId());

        // 校验帖子存在且状态为已发布
        PostEntity post = postMapper.getByPostId(dto.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!PostConstant.STATUS_PUBLISHED.equals(post.getStatus())) {
            throw new IllegalArgumentException("帖子当前状态不支持预约");
        }

        // 校验买家存在
        BuyerEntity buyer = buyerMapper.getByBuyerId(dto.getBuyerId());
        if (buyer == null) {
            throw new IllegalArgumentException("买家不存在");
        }

        // 校验买家不能是帖子发布者自己
        if (dto.getBuyerId().equals(post.getSellerId())) {
            throw new IllegalArgumentException("不能预约自己的帖子");
        }

        // 创建交易记录，logisticsInfo 字段暂存 postId（schema 无 postId 字段的变通方案）
        String transactionId = UUID.randomUUID().toString().replace("-", "");
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(transactionId);
        transaction.setBuyerId(dto.getBuyerId());
        transaction.setSellerId(post.getSellerId());
        transaction.setTransactionAmount(post.getPrice());
        transaction.setTransactionTime(LocalDate.now());
        transaction.setTransactionStatus(TransactionConstant.STATUS_PENDING);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_BOOKED);
        transaction.setLogisticsInfo(post.getPostId());
        transactionMapper.insert(transaction);
        log.info("交易创建成功, transactionId={}", transactionId);

        // 更新帖子状态为已预约
        postMapper.updateStatus(post.getPostId(), PostConstant.STATUS_RESERVED);

        return buildTransactionVO(transactionMapper.getByTransactionId(transactionId), dto.getBuyerId());
    }

    @Override
    public TransactionVO getTransaction(String transactionId) {
        validateIdFormat(transactionId, "交易ID");
        return buildTransactionVO(getExistingTransaction(transactionId), null);
    }

    @Override
    public PageResult<TransactionVO> listMyTransactions(TransactionQueryDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        validateIdFormat(dto.getUserId(), "用户ID");
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        String sortField = validateSortField(dto.getSortField(), new String[]{"transactionTime", "transactionAmount", "transactionStatus"});
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        long total = transactionMapper.countByUserIdAndStatus(dto.getUserId(), dto.getTransactionStatus());
        List<TransactionVO> records = transactionMapper.listByUserIdAndStatus(
                dto.getUserId(), dto.getTransactionStatus(), offset, pageSize, sortField, sortOrder)
                .stream().map(item -> buildTransactionVO(item, dto.getUserId())).toList();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 取消交易（仅买家可取消，仅 PENDING 状态可取消）
     * 事务边界：更新交易状态 + 恢复帖子状态为已发布
     */
    @Override
    @Transactional
    public TransactionVO cancelTransaction(String transactionId, String userId) {
        log.info("取消交易, transactionId={}, userId={}", transactionId, userId);
        validateIdFormat(transactionId, "交易ID");
        validateIdFormat(userId, "用户ID");
        validateAuthenticatedUser(userId);
        TransactionEntity transaction = getExistingTransaction(transactionId);

        // 仅 PENDING 状态的交易可取消
        if (!TransactionConstant.STATUS_PENDING.equals(transaction.getTransactionStatus())) {
            throw new IllegalArgumentException("仅待处理状态的交易可以取消");
        }

        // 仅买家可取消预约
        if (!transaction.getBuyerId().equals(userId)) {
            throw new IllegalArgumentException("仅买家可取消预约");
        }

        // 更新交易状态
        transaction.setTransactionStatus(TransactionConstant.STATUS_CANCELLED);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_CANCELLED);
        transactionMapper.update(transaction);

        // 恢复帖子状态为已发布
        String postId = transaction.getLogisticsInfo();
        if (StringUtils.hasText(postId)) {
            postMapper.updateStatus(postId, PostConstant.STATUS_PUBLISHED);
        }

        return buildTransactionVO(transactionMapper.getByTransactionId(transactionId), userId);
    }

    /**
     * 卖家确认成交：PENDING -> COMPLETED
     * 事务边界：更新交易状态 + 更新帖子状态为已售 + 补齐评价所需订单
     */
    @Override
    @Transactional
    public TransactionVO completeTransaction(String transactionId, String userId) {
        log.info("卖家成交交易, transactionId={}, userId={}", transactionId, userId);
        validateIdFormat(transactionId, "交易ID");
        validateIdFormat(userId, "用户ID");
        validateAuthenticatedSeller(userId);
        TransactionEntity transaction = getExistingTransaction(transactionId);

        if (!TransactionConstant.STATUS_PENDING.equals(transaction.getTransactionStatus())) {
            throw new IllegalArgumentException("仅已预订状态的交易可以成交");
        }

        if (!transaction.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("仅卖家可确认成交");
        }

        transaction.setTransactionStatus(TransactionConstant.STATUS_COMPLETED);
        transactionMapper.update(transaction);

        OrderEntity order = orderMapper.getByTransactionId(transactionId);
        if (order == null) {
            order = new OrderEntity();
            order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
            order.setTransactionId(transactionId);
            order.setOrderStatus(OrderConstant.STATUS_COMPLETED);
            order.setCreateTime(LocalDate.now());
            order.setUpdateTime(LocalDate.now());
            orderMapper.insert(order);
        } else {
            order.setOrderStatus(OrderConstant.STATUS_COMPLETED);
            order.setUpdateTime(LocalDate.now());
            orderMapper.update(order);
        }

        String postId = transaction.getLogisticsInfo();
        if (StringUtils.hasText(postId)) {
            postMapper.updateStatus(postId, PostConstant.STATUS_SOLD);
        }

        return buildTransactionVO(transactionMapper.getByTransactionId(transactionId), userId);
    }

    // ==================== 私有辅助方法 ====================

    private TransactionEntity getExistingTransaction(String transactionId) {
        if (!StringUtils.hasText(transactionId)) {
            throw new IllegalArgumentException("交易ID不能为空");
        }
        TransactionEntity transaction = transactionMapper.getByTransactionId(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("交易不存在");
        }
        return transaction;
    }

    /**
     * 组装交易视图对象，关联买家/卖家名称、店铺名称、帖子信息和评价状态
     */
    private TransactionVO buildTransactionVO(TransactionEntity transaction, String currentUserId) {
        TransactionVO vo = new TransactionVO();
        vo.setTransactionId(transaction.getTransactionId());
        vo.setBuyerId(transaction.getBuyerId());
        vo.setSellerId(transaction.getSellerId());
        vo.setTransactionAmount(transaction.getTransactionAmount());
        vo.setTransactionTime(transaction.getTransactionTime());
        vo.setTransactionStatus(transaction.getTransactionStatus());
        vo.setBookingStatus(transaction.getBookingStatus());
        vo.setLogisticsInfo(transaction.getLogisticsInfo());

        // 填充买家名称
        UserEntity buyer = userMapper.getByUserId(transaction.getBuyerId());
        if (buyer != null) {
            vo.setBuyerName(buyer.getUsername());
        }

        // 填充卖家名称和店铺名称
        UserEntity seller = userMapper.getByUserId(transaction.getSellerId());
        if (seller != null) {
            vo.setSellerName(seller.getUsername());
        }
        SellerEntity sellerEntity = sellerMapper.getBySellerId(transaction.getSellerId());
        if (sellerEntity != null) {
            vo.setShopName(sellerEntity.getShopName());
        }

        // 从 logisticsInfo 解析 postId，填充帖子信息
        String postId = transaction.getLogisticsInfo();
        if (StringUtils.hasText(postId)) {
            vo.setPostId(postId);
            PostEntity post = postMapper.getByPostId(postId);
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }
        }

        OrderEntity order = orderMapper.getByTransactionId(transaction.getTransactionId());
        if (order != null) {
            vo.setOrderId(order.getOrderId());
        }

        if (StringUtils.hasText(currentUserId)) {
            boolean isBuyer = currentUserId.equals(transaction.getBuyerId());
            boolean isSeller = currentUserId.equals(transaction.getSellerId());
            if (isBuyer || isSeller) {
                String targetId = isBuyer ? transaction.getSellerId() : transaction.getBuyerId();
                vo.setReviewTargetId(targetId);
                vo.setReviewTargetRole(isBuyer ? "SELLER" : "BUYER");
                vo.setReviewTargetName(isBuyer
                        ? (StringUtils.hasText(vo.getSellerName()) ? vo.getSellerName() : vo.getShopName())
                        : vo.getBuyerName());

                boolean completed = TransactionConstant.STATUS_COMPLETED.equals(transaction.getTransactionStatus()) && order != null;
                ReviewEntity existingReview = completed ? reviewMapper.getByOrderIdAndReviewerId(order.getOrderId(), currentUserId) : null;
                vo.setCurrentUserReviewed(existingReview != null);
                vo.setCurrentUserCanReview(completed && existingReview == null);
            }
        }

        return vo;
    }

    /**
     * 校验排序字段（白名单方式，防止SQL注入）
     */
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

    private void validateAuthenticatedUser(String userId) {
        String currentUserId = AuthContext.getRequiredUserId();
        if (!currentUserId.equals(userId)) {
            throw new IllegalArgumentException("当前登录用户与操作用户不一致");
        }
    }

    private void validateAuthenticatedBuyer(String buyerId) {
        validateAuthenticatedUser(buyerId);
        String currentUserType = AuthContext.getRequiredUserType();
        if (!UserConstant.USER_TYPE_BUYER.equals(currentUserType)) {
            throw new IllegalArgumentException("仅买家可预约帖子");
        }
    }

    private void validateAuthenticatedSeller(String sellerId) {
        validateAuthenticatedUser(sellerId);
        String currentUserType = AuthContext.getRequiredUserType();
        if (!UserConstant.USER_TYPE_SELLER.equals(currentUserType)) {
            throw new IllegalArgumentException("仅卖家可确认成交");
        }
    }
}
