package com.prompt.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.review.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
