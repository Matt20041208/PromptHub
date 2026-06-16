package com.prompt.prompt.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromptListVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String title;
    private String description;
    private String cover;
    private BigDecimal price;
    private String status;
    private List<String> tags;
    private String categoryName;
    private Integer viewCount;
    private Integer downloadCount;
    private BigDecimal avgRating;
    private LocalDateTime createTime;
}
