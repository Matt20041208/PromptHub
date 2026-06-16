package com.prompt.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
