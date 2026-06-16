package com.prompt.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromptCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    @NotBlank(message = "内容不能为空")
    private String content;
    private String templateSchema;
    private String cover;
    private BigDecimal price;
    private Long categoryId;
    private List<Long> tagIds;
}
