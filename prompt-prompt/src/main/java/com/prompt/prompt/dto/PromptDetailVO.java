package com.prompt.prompt.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PromptDetailVO extends PromptListVO {
    private String content;
    private String templateSchema;
    private Long categoryId;
    private List<PromptVersionVO> versions;
    private boolean purchased;
    private boolean contentLocked;
}
