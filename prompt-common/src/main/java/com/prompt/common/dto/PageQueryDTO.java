package com.prompt.common.dto;

import lombok.Data;

@Data
public class PageQueryDTO {
    private long page = 1;
    private long size = 20;
    private String keyword;
    private String sortField;
    private String sortOrder;
}
