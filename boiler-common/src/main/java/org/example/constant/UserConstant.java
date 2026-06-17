package org.example.constant;

public final class UserConstant {
    public static final String USER_TYPE_BUYER = "BUYER";
    public static final String USER_TYPE_SELLER = "SELLER";
    public static final String USER_TYPE_ADMIN = "ADMIN";

    public static final String VERIFICATION_STATUS_UNVERIFIED = "UNVERIFIED";
    public static final String VERIFICATION_STATUS_VERIFIED = "VERIFIED";
    public static final String VERIFICATION_STATUS_SUSPENDED = "SUSPENDED";

    public static final String QUALIFICATION_STATUS_PENDING = "PENDING";
    public static final String QUALIFICATION_STATUS_APPROVED = "APPROVED";
    public static final String QUALIFICATION_STATUS_REJECTED = "REJECTED";

    public static final int DEFAULT_CREDIT_SCORE = 60;
    public static final int MAX_CREDIT_SCORE = 100;
    public static final int MIN_CREDIT_SCORE = 0;
    public static final int SELLER_APPROVAL_CREDIT_SCORE = 80;

    public static final String DEFAULT_VERIFICATION_STATUS = VERIFICATION_STATUS_UNVERIFIED;
    public static final String DEFAULT_QUALIFICATION_STATUS = QUALIFICATION_STATUS_PENDING;

    private UserConstant() {
    }
}
