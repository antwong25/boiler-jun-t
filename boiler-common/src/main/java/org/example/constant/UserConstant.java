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

    public static final int INFORMATION_COMPLETENESS_SCORE = 40;
    public static final int MUTUAL_REVIEW_BASE_SCORE = 15;
    public static final int MUTUAL_REVIEW_POSITIVE_BONUS = 3;
    public static final int MUTUAL_REVIEW_NEGATIVE_PENALTY = 5;
    public static final int MUTUAL_REVIEW_MAX_SCORE = 30;
    public static final int TRANSACTION_BEHAVIOR_BASE_SCORE = 10;
    public static final int TRANSACTION_BEHAVIOR_COMPLETED_BONUS = 2;
    public static final int TRANSACTION_BEHAVIOR_MAX_SCORE = 20;
    public static final int COMMUNITY_CONDUCT_SCORE = 10;
    public static final int DEFAULT_CREDIT_SCORE = INFORMATION_COMPLETENESS_SCORE
            + MUTUAL_REVIEW_BASE_SCORE
            + TRANSACTION_BEHAVIOR_BASE_SCORE
            + COMMUNITY_CONDUCT_SCORE;
    public static final int MAX_CREDIT_SCORE = 100;
    public static final int MIN_CREDIT_SCORE = 0;

    public static final String DEFAULT_VERIFICATION_STATUS = VERIFICATION_STATUS_UNVERIFIED;
    public static final String DEFAULT_QUALIFICATION_STATUS = QUALIFICATION_STATUS_PENDING;

    private UserConstant() {
    }
}
