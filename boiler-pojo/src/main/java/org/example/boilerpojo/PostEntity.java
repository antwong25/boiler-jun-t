package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 帖子实体类，对应 post 表
 */
@Data
public class PostEntity {
    private String postId;
    private String sellerId;
    private String title;
    private BigDecimal price;
    private String description;
    private String status;
    private LocalDate publishTime;
    private LocalDate updateTime;
    private Integer viewCount;
    private String mediaFiles;
    private String aiValuationRange;
    private String city;
    private String boilerId;
}
