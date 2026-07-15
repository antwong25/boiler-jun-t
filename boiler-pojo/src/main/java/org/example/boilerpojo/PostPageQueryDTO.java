package org.example.boilerpojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.boilercommon.PageQuery;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostPageQueryDTO extends PageQuery {
    private String keyword;
    private String city;
    private String boilerType;
    private String brand;
    private String fuelType;
    private BigDecimal tonnageMin;
    private BigDecimal tonnageMax;
    private Integer offset;
}
