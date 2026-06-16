package com.prompt.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.common.result.PageResult;
import com.prompt.review.entity.Favorite;

public interface FavoriteService extends IService<Favorite> {
    void add(Long userId, Long promptId);
    void remove(Long userId, Long promptId);
    boolean isFavorite(Long userId, Long promptId);
    PageResult<Long> listFavoriteIds(Long userId, long page, long size);
}
