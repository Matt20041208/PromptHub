package com.prompt.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long promptId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}
