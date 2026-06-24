package org.example.boilerpojo;

import lombok.Data;
import org.example.boilercommon.PageQuery;

/**
 * 交易查询DTO
 */
@Data
public class TransactionQueryDTO extends PageQuery {
    private String userId;
    private String transactionStatus;
}
