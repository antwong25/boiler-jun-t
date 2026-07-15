package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.OrderCreateDTO;
import org.example.boilerpojo.OrderQueryDTO;
import org.example.boilerpojo.OrderVO;
import org.example.boilerserver.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderVO> createOrder(@RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @PutMapping("/{orderId}/confirm")
    public Result<OrderVO> confirmOrder(@PathVariable String orderId,
                                        @RequestParam String operatorId) {
        return Result.success(orderService.confirmOrder(orderId, operatorId));
    }

    @PutMapping("/{orderId}/complete")
    public Result<OrderVO> completeOrder(@PathVariable String orderId,
                                         @RequestParam String operatorId) {
        return Result.success(orderService.completeOrder(orderId, operatorId));
    }

    @PutMapping("/{orderId}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable String orderId,
                                       @RequestParam String operatorId) {
        return Result.success(orderService.cancelOrder(orderId, operatorId));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderId) {
        return Result.success(orderService.getOrderDetail(orderId));
    }

    @GetMapping("/my")
    public Result<org.example.boilercommon.PageResult<OrderVO>> listMyOrders(OrderQueryDTO dto) {
        return Result.success(orderService.listMyOrders(dto));
    }
}
