package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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
