package org.example.boilerpojo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 评论实体类，对应 review 表
 */
@Data
public class ReviewEntity {
    private String reviewId;
    private String reviewerId;
    private String revieweeId;
    private String postId;
    private String orderId;
    private Integer rating;
    private String content;
    private LocalDate reviewTime;
}
