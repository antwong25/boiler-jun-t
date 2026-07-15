package org.example.boilerpojo;

import lombok.Data;
import org.example.boilercommon.PageQuery;

/**
 * 订单查询DTO（我的订单列表）
 */
@Data
public class OrderQueryDTO extends PageQuery {
    private String userId;
    private String orderStatus;
}
