package org.example.boilerserver.service;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.OrderCreateDTO;
import org.example.boilerpojo.OrderQueryDTO;
import org.example.boilerpojo.OrderVO;

public interface OrderService {

    OrderVO createOrder(OrderCreateDTO dto);

    OrderVO confirmOrder(String orderId, String operatorId);

    OrderVO completeOrder(String orderId, String operatorId);

    OrderVO cancelOrder(String orderId, String operatorId);

    OrderVO getOrderDetail(String orderId);

    PageResult<OrderVO> listMyOrders(OrderQueryDTO dto);
}
