package com.prompt.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Integer isRead;
    private String bizType;
    private Long bizId;
    private LocalDateTime createTime;
}
