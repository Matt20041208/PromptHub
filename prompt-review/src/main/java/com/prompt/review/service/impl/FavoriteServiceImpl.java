package com.prompt.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.common.result.PageResult;
import com.prompt.review.entity.Favorite;
import com.prompt.review.mapper.FavoriteMapper;
import com.prompt.review.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional
    public void add(Long userId, Long promptId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPromptId, promptId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPromptId(promptId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long promptId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPromptId, promptId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    public boolean isFavorite(Long userId, Long promptId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPromptId, promptId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public PageResult<Long> listFavoriteIds(Long userId, long page, long size) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime);
        Page<Favorite> pageObj = new Page<>(page, size);
        Page<Favorite> result = favoriteMapper.selectPage(pageObj, wrapper);

        List<Long> promptIds = result.getRecords().stream()
                .map(Favorite::getPromptId)
                .collect(Collectors.toList());

        PageResult<Long> pageResult = new PageResult<>();
        pageResult.setRecords(promptIds);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        return pageResult;
    }
}
