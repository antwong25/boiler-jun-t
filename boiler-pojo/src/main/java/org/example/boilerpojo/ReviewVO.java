package org.example.boilerpojo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 评论视图对象
 */
@Data
public class ReviewVO {
    private String reviewId;
    private String reviewerId;
    private String reviewerName;
    private String postId;
    private String orderId;
    private Integer rating;
    private String content;
    private LocalDate reviewTime;
}
