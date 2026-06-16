package com.prompt.prompt.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTreeVO {
    private Long id;
    private String name;
    private Integer sort;
    private List<CategoryTreeVO> children;
}
