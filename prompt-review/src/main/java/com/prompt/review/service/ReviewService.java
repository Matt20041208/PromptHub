package com.prompt.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.common.result.PageResult;
import com.prompt.review.dto.ReviewCreateDTO;
import com.prompt.review.dto.ReviewPageQueryDTO;
import com.prompt.review.dto.ReviewVO;
import com.prompt.review.entity.Review;

public interface ReviewService extends IService<Review> {
    void create(Long userId, ReviewCreateDTO dto);
    PageResult<ReviewVO> pageByPrompt(ReviewPageQueryDTO dto);
    Double getAvgRating(Long promptId);
    Long getCount(Long promptId);
}
