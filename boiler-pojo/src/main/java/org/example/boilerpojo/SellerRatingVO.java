package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 卖家评分统计视图对象
 */
@Data
public class SellerRatingVO {
    private String sellerId;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private BigDecimal positiveRatingRate;
}
