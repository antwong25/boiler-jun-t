package org.example.constant;

/**
 * 帖子模块常量
 */
public final class PostConstant {
    // 帖子创建或编辑后回到待审核状态
    public static final String POST_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String POST_STATUS_APPROVED = "APPROVED";
    public static final String POST_STATUS_REJECTED = "REJECTED";

    // 帖子交易状态
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_DELISTED = "DELISTED";
    public static final String STATUS_BANNED = "BANNED";

    // 锅炉类型与需求文档保持一致，由业务层做条件字段校验
    public static final String BOILER_TYPE_HOT_WATER = "HOT_WATER";
    public static final String BOILER_TYPE_STEAM = "STEAM";

    private PostConstant() {
    }
}
