package com.prompt.search.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromptIndexMessage {
    private String action;
    private Long promptId;
    private String title;
    private String description;
    private List<String> tags;
    private String category;
    private BigDecimal price;
    private BigDecimal avgRating;
    private Integer viewCount;
    private Integer downloadCount;
    private LocalDateTime createTime;
}
