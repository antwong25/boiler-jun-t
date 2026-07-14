package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.OrderEntity;

import java.util.List;

@Mapper
public interface OrderMapper {
    OrderEntity getByOrderId(String orderId);

    OrderEntity getByTransactionId(String transactionId);

    int insert(OrderEntity orderEntity);

    int update(OrderEntity orderEntity);

    /**
     * 按用户ID和订单状态分页查询订单（通过 transaction 表关联 buyerId/sellerId，支持排序）
     */
    List<OrderEntity> listByUserIdAndStatus(@Param("userId") String userId,
                                            @Param("orderStatus") String orderStatus,
                                            @Param("offset") int offset,
                                            @Param("size") int size,
                                            @Param("sortField") String sortField,
                                            @Param("sortOrder") String sortOrder);

    int countByUserIdAndStatus(@Param("userId") String userId,
                               @Param("orderStatus") String orderStatus);
}
