package com.prompt.prompt.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromptUpdateDTO {
    private String title;
    private String description;
    private String content;
    private String templateSchema;
    private String cover;
    private BigDecimal price;
    private Long categoryId;
    private List<Long> tagIds;
}
