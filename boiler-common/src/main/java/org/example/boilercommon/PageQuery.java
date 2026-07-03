package org.example.boilercommon;

import lombok.Data;
import java.io.Serializable;

/**
 * 分页查询基类
 */
@Data
public class PageQuery implements Serializable {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField;
    private String sortOrder = "desc";
}
