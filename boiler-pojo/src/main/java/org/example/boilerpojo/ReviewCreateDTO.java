package org.example.boilerpojo;

import lombok.Data;

/**
 * 提交评论请求DTO
 */
@Data
public class ReviewCreateDTO {
    private String orderId;
    private String reviewerId;
    private Integer rating;
    private String content;
}
