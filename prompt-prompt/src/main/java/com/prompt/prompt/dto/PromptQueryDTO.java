package com.prompt.prompt.dto;

import com.prompt.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PromptQueryDTO extends PageQueryDTO {
    private Long categoryId;
    private String status;
    private Long userId;
    private Long tagId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
}
