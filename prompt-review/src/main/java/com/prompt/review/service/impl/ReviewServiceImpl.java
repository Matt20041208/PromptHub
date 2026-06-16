package com.prompt.review.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.common.exception.BusinessException;
import com.prompt.common.result.PageResult;
import com.prompt.review.dto.ReviewCreateDTO;
import com.prompt.review.dto.ReviewPageQueryDTO;
import com.prompt.review.dto.ReviewVO;
import com.prompt.review.entity.Review;
import com.prompt.review.mapper.ReviewMapper;
import com.prompt.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public void create(Long userId, ReviewCreateDTO dto) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId)
                .eq(Review::getPromptId, dto.getPromptId());
        if (reviewMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已评价");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setPromptId(dto.getPromptId());
        review.setOrderId(dto.getOrderId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        reviewMapper.insert(review);

        Double avgRating = getAvgRating(dto.getPromptId());
        BigDecimal ratingValue = avgRating != null
                ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get prompt author for notification
        Long sellerId = null;
        try {
            RestTemplate rt = new RestTemplate();
            String resp = rt.getForObject("http://127.0.0.1:9102/api/prompt/" + dto.getPromptId(), String.class);
            if (resp != null) {
                JSONObject json = JSONUtil.parseObj(resp).getJSONObject("data");
                if (json != null) sellerId = json.getLong("userId");
            }
        } catch (Exception ignored) {}

        // Build MQ message
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "REVIEW_CREATED");
        msg.put("userId", sellerId);
        msg.put("reviewerId", userId);
        msg.put("promptId", dto.getPromptId());
        msg.put("rating", dto.getRating());
        msg.put("avgRating", ratingValue.doubleValue());

        String jsonMsg = JSONUtil.toJsonStr(msg);
        rabbitTemplate.convertAndSend("review.exchange", "review.created", jsonMsg);
        rabbitTemplate.convertAndSend("prompt.exchange", "prompt.rating", jsonMsg);
    }

    @Override
    public PageResult<ReviewVO> pageByPrompt(ReviewPageQueryDTO dto) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (dto.getPromptId() != null) {
            wrapper.eq(Review::getPromptId, dto.getPromptId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(Review::getUserId, dto.getUserId());
        }
        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> page = new Page<>(dto.getPage(), dto.getSize());
        Page<Review> result = reviewMapper.selectPage(page, wrapper);

        List<ReviewVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        PageResult<ReviewVO> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        return pageResult;
    }

    @Override
    public Double getAvgRating(Long promptId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Review::getRating)
                .eq(Review::getPromptId, promptId);
        List<Object> ratings = reviewMapper.selectObjs(wrapper);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        double avg = ratings.stream()
                .mapToDouble(r -> ((Number) r).doubleValue())
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public Long getCount(Long promptId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getPromptId, promptId);
        return reviewMapper.selectCount(wrapper);
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtil.copyProperties(review, vo);
        if (vo.getUserNickname() == null) {
            vo.setUserNickname("用户" + review.getUserId());
        }
        if (vo.getUserAvatar() == null) {
            vo.setUserAvatar("");
        }
        return vo;
    }
}
