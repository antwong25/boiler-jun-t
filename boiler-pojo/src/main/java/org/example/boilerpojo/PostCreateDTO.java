package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PostCreateDTO {
    private String sellerId;
    private String title;
    private BigDecimal price;
    private String description;
    private String mediaFiles;
    private String city;
    private BoilerDetailDTO boilerDetail;
}
