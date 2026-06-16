package com.prompt.review.dto;

import com.prompt.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewPageQueryDTO extends PageQueryDTO {
    private Long promptId;
    private Long userId;
}
