package com.prompt.review.controller;

import com.prompt.common.constant.CommonConstants;
import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import com.prompt.review.dto.ReviewCreateDTO;
import com.prompt.review.dto.ReviewPageQueryDTO;
import com.prompt.review.dto.ReviewVO;
import com.prompt.review.service.FavoriteService;
import com.prompt.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Tag(name = "评价管理", description = "评价CRUD、收藏管理")
public class ReviewController {

    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    @PostMapping
    @Operation(summary = "创建评价")
    public Result<Void> create(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                               @Valid @RequestBody ReviewCreateDTO dto) {
        reviewService.create(userId, dto);
        return Result.success();
    }

    @GetMapping("/list/{promptId}")
    @Operation(summary = "分页查询评价")
    public Result<PageResult<ReviewVO>> list(@PathVariable Long promptId,
                                             @Valid ReviewPageQueryDTO dto) {
        dto.setPromptId(promptId);
        PageResult<ReviewVO> result = reviewService.pageByPrompt(dto);
        return Result.success(result);
    }

    @GetMapping("/rating/{promptId}")
    @Operation(summary = "获取平均评分")
    public Result<Double> rating(@PathVariable Long promptId) {
        Double avgRating = reviewService.getAvgRating(promptId);
        return Result.success(avgRating);
    }

    @GetMapping("/count/{promptId}")
    @Operation(summary = "获取评价数量")
    public Result<Long> count(@PathVariable Long promptId) {
        Long count = reviewService.getCount(promptId);
        return Result.success(count);
    }

    @PostMapping("/favorite/{promptId}")
    @Operation(summary = "添加收藏")
    public Result<Void> addFavorite(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                    @PathVariable Long promptId) {
        favoriteService.add(userId, promptId);
        return Result.success();
    }

    @DeleteMapping("/favorite/{promptId}")
    @Operation(summary = "取消收藏")
    public Result<Void> removeFavorite(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                       @PathVariable Long promptId) {
        favoriteService.remove(userId, promptId);
        return Result.success();
    }

    @GetMapping("/favorite/check/{promptId}")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavorite(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                         @PathVariable Long promptId) {
        boolean favorited = favoriteService.isFavorite(userId, promptId);
        return Result.success(favorited);
    }

    @GetMapping("/favorite/list")
    @Operation(summary = "获取收藏列表")
    public Result<PageResult<Long>> listFavorites(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        PageResult<Long> result = favoriteService.listFavoriteIds(userId, page, size);
        return Result.success(result);
    }
}
