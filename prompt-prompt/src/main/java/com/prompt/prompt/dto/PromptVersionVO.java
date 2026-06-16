package com.prompt.prompt.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromptVersionVO {
    private Long id;
    private String versionNo;
    private String changelog;
    private LocalDateTime createTime;
}
