package org.example.constant;

/**
 * 交易模块常量
 */
public final class TransactionConstant {

    // 交易状态
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // 预约状态
    public static final String BOOKING_STATUS_BOOKED = "BOOKED";
    public static final String BOOKING_STATUS_CANCELLED = "CANCELLED";

    private TransactionConstant() {
    }
}
