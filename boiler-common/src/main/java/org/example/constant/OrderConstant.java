package org.example.constant;

/**
 * 订单模块常量
 */
public final class OrderConstant {

    // 订单状态：待确认 → 进行中 → 已完成 / 已取消
    public static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private OrderConstant() {
    }
}
