package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.TransactionEntity;

import java.util.List;

@Mapper
public interface TransactionMapper {
    TransactionEntity getByTransactionId(String transactionId);

    int insert(TransactionEntity transactionEntity);

    int update(TransactionEntity transactionEntity);

    /**
     * 按用户ID和状态分页查询交易（支持排序）
     */
    List<TransactionEntity> listByUserIdAndStatus(@Param("userId") String userId,
                                                   @Param("transactionStatus") String transactionStatus,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size,
                                                   @Param("sortField") String sortField,
                                                   @Param("sortOrder") String sortOrder);

    /**
     * 按用户ID和状态统计交易总数
     */
    int countByUserIdAndStatus(@Param("userId") String userId,
                               @Param("transactionStatus") String transactionStatus);
}
