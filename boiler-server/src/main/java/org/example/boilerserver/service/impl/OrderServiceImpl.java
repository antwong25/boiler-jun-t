package org.example.boilerserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.boilercommon.PageResult;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.OrderCreateDTO;
import org.example.boilerpojo.OrderQueryDTO;
import org.example.boilerpojo.OrderVO;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.OrderService;
import org.example.constant.OrderConstant;
import org.example.constant.PostConstant;
import org.example.constant.TransactionConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /** ID 格式校验：仅允许字母、数字、下划线、连字符，长度1-64，防止SQL注入 */
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final OrderMapper orderMapper;
    private final TransactionMapper transactionMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SellerMapper sellerMapper;

    public OrderServiceImpl(OrderMapper orderMapper,
                            TransactionMapper transactionMapper,
                            PostMapper postMapper,
                            UserMapper userMapper,
                            SellerMapper sellerMapper) {
        this.orderMapper = orderMapper;
        this.transactionMapper = transactionMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sellerMapper = sellerMapper;
    }

    /**
     * 创建订单：关联交易ID，校验交易状态，初始化订单状态为 PENDING_CONFIRM
     */
    @Override
    @Transactional
    public OrderVO createOrder(OrderCreateDTO dto) {
        log.info("创建订单, transactionId={}, operatorId={}", dto.getTransactionId(), dto.getOperatorId());
        if (dto == null || !StringUtils.hasText(dto.getTransactionId())) {
            throw new IllegalArgumentException("交易ID不能为空");
        }
        if (!StringUtils.hasText(dto.getOperatorId())) {
            throw new IllegalArgumentException("操作者ID不能为空");
        }
        validateIdFormat(dto.getTransactionId(), "交易ID");
        validateIdFormat(dto.getOperatorId(), "操作者ID");

        // 校验交易存在且状态为 PENDING
        TransactionEntity transaction = getExistingTransaction(dto.getTransactionId());
        if (!TransactionConstant.STATUS_PENDING.equals(transaction.getTransactionStatus())) {
            throw new IllegalArgumentException("仅待处理状态的交易可以创建订单");
        }

        // 校验操作者是交易的买家（仅买家可发起订单）
        if (!transaction.getBuyerId().equals(dto.getOperatorId())) {
            throw new IllegalArgumentException("仅买家可创建订单");
        }

        // 创建订单
        String orderId = UUID.randomUUID().toString().replace("-", "");
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setTransactionId(dto.getTransactionId());
        order.setOrderStatus(OrderConstant.STATUS_PENDING_CONFIRM);
        order.setCreateTime(LocalDate.now());
        order.setUpdateTime(LocalDate.now());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发场景下，唯一约束 uk_transaction_id 防止重复订单
            throw new IllegalArgumentException("该交易已存在订单，请勿重复创建");
        }
        log.info("订单创建成功, orderId={}", orderId);

        return buildOrderVO(orderMapper.getByOrderId(orderId));
    }

    /**
     * 确认订单：PENDING_CONFIRM → IN_PROGRESS
     * 仅卖家可操作，同步更新交易状态为 IN_PROGRESS
     */
    @Override
    @Transactional
    public OrderVO confirmOrder(String orderId, String operatorId) {
        log.info("确认订单, orderId={}, operatorId={}", orderId, operatorId);
        validateIdFormat(orderId, "订单ID");
        validateIdFormat(operatorId, "操作者ID");
        OrderEntity order = getExistingOrder(orderId);
        if (!OrderConstant.STATUS_PENDING_CONFIRM.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅待确认状态的订单可以确认");
        }

        TransactionEntity transaction = getExistingTransaction(order.getTransactionId());

        // 仅卖家可确认订单
        if (!transaction.getSellerId().equals(operatorId)) {
            throw new IllegalArgumentException("仅卖家可确认订单");
        }

        // 更新订单状态
        order.setOrderStatus(OrderConstant.STATUS_IN_PROGRESS);
        order.setUpdateTime(LocalDate.now());
        orderMapper.update(order);

        // 同步更新交易状态
        transaction.setTransactionStatus(TransactionConstant.STATUS_IN_PROGRESS);
        transactionMapper.update(transaction);

        return buildOrderVO(orderMapper.getByOrderId(orderId));
    }

    /**
     * 完成订单：IN_PROGRESS → COMPLETED
     * 买卖双方均可操作，同步更新交易状态、帖子状态、卖家完成交易数
     */
    @Override
    @Transactional
    public OrderVO completeOrder(String orderId, String operatorId) {
        log.info("完成订单, orderId={}, operatorId={}", orderId, operatorId);
        validateIdFormat(orderId, "订单ID");
        validateIdFormat(operatorId, "操作者ID");
        OrderEntity order = getExistingOrder(orderId);
        if (!OrderConstant.STATUS_IN_PROGRESS.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅进行中状态的订单可以完成");
        }

        TransactionEntity transaction = getExistingTransaction(order.getTransactionId());

        // 买卖双方均可完成订单
        validateOperator(transaction, operatorId);

        // 更新订单状态
        order.setOrderStatus(OrderConstant.STATUS_COMPLETED);
        order.setUpdateTime(LocalDate.now());
        orderMapper.update(order);

        // 同步更新交易状态为已完成
        transaction.setTransactionStatus(TransactionConstant.STATUS_COMPLETED);
        transactionMapper.update(transaction);

        // 同步更新帖子状态为已售出
        String postId = transaction.getLogisticsInfo();
        if (StringUtils.hasText(postId)) {
            postMapper.updateStatus(postId, PostConstant.STATUS_SOLD);
        }

        // 更新卖家完成交易数
        SellerEntity seller = sellerMapper.getBySellerId(transaction.getSellerId());
        if (seller != null) {
            int count = seller.getCompletedTransactionCount() == null ? 0 : seller.getCompletedTransactionCount();
            seller.setCompletedTransactionCount(count + 1);
            sellerMapper.update(seller);
        }

        return buildOrderVO(orderMapper.getByOrderId(orderId));
    }

    /**
     * 取消订单：PENDING_CONFIRM/IN_PROGRESS → CANCELLED
     * 买卖双方均可操作，同步更新交易状态、恢复帖子状态
     */
    @Override
    @Transactional
    public OrderVO cancelOrder(String orderId, String operatorId) {
        log.info("取消订单, orderId={}, operatorId={}", orderId, operatorId);
        validateIdFormat(orderId, "订单ID");
        validateIdFormat(operatorId, "操作者ID");
        OrderEntity order = getExistingOrder(orderId);
        String status = order.getOrderStatus();
        if (!OrderConstant.STATUS_PENDING_CONFIRM.equals(status)
                && !OrderConstant.STATUS_IN_PROGRESS.equals(status)) {
            throw new IllegalArgumentException("仅待确认或进行中状态的订单可以取消");
        }

        TransactionEntity transaction = getExistingTransaction(order.getTransactionId());

        // 买卖双方均可取消订单
        validateOperator(transaction, operatorId);

        // 更新订单状态
        order.setOrderStatus(OrderConstant.STATUS_CANCELLED);
        order.setUpdateTime(LocalDate.now());
        orderMapper.update(order);

        // 同步更新交易状态为已取消
        transaction.setTransactionStatus(TransactionConstant.STATUS_CANCELLED);
        transaction.setBookingStatus(TransactionConstant.BOOKING_STATUS_CANCELLED);
        transactionMapper.update(transaction);

        // 恢复帖子状态为已发布
        String postId = transaction.getLogisticsInfo();
        if (StringUtils.hasText(postId)) {
            postMapper.updateStatus(postId, PostConstant.STATUS_PUBLISHED);
        }

        return buildOrderVO(orderMapper.getByOrderId(orderId));
    }

    @Override
    public OrderVO getOrderDetail(String orderId) {
        validateIdFormat(orderId, "订单ID");
        return buildOrderVO(getExistingOrder(orderId));
    }

    @Override
    public PageResult<OrderVO> listMyOrders(OrderQueryDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        validateIdFormat(dto.getUserId(), "用户ID");
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        String sortField = validateSortField(dto.getSortField(), new String[]{"createTime", "updateTime", "orderStatus"});
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        long total = orderMapper.countByUserIdAndStatus(dto.getUserId(), dto.getOrderStatus());
        List<OrderVO> records = orderMapper.listByUserIdAndStatus(
                dto.getUserId(), dto.getOrderStatus(), offset, pageSize, sortField, sortOrder)
                .stream().map(this::buildOrderVO).toList();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    // ==================== 私有辅助方法 ====================

    private OrderEntity getExistingOrder(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        OrderEntity order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

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
     * 校验操作者是交易的买方或卖方
     */
    private void validateOperator(TransactionEntity transaction, String operatorId) {
        if (!transaction.getBuyerId().equals(operatorId)
                && !transaction.getSellerId().equals(operatorId)) {
            throw new IllegalArgumentException("无权操作此订单");
        }
    }

    /**
     * 组装订单视图对象，关联交易、帖子、买卖方信息
     */
    private OrderVO buildOrderVO(OrderEntity order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setTransactionId(order.getTransactionId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        // 填充交易信息
        TransactionEntity transaction = transactionMapper.getByTransactionId(order.getTransactionId());
        if (transaction != null) {
            vo.setBuyerId(transaction.getBuyerId());
            vo.setSellerId(transaction.getSellerId());
            vo.setTransactionAmount(transaction.getTransactionAmount());
            vo.setTransactionStatus(transaction.getTransactionStatus());
            vo.setBookingStatus(transaction.getBookingStatus());

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
                    vo.setPostPrice(post.getPrice());
                }
            }
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
