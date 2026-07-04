package org.example.boilerpojo;

import lombok.Data;

@Data
public class PostFilterSearchDTO {
    private String city;
    private String boilerType;
    private String brand;
    private String fuelType;
    private Integer limit = 5;
}
