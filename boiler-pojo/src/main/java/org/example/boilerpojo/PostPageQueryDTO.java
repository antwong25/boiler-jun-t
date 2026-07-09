package org.example.boilerpojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.boilercommon.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostPageQueryDTO extends PageQuery {
    private String city;
    private String boilerType;
    private String brand;
    private String fuelType;
    private Integer offset;
}
