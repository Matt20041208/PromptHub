package com.prompt.search.dto;

import com.prompt.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchQueryDTO extends PageQueryDTO {
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy = "createTime";
}
