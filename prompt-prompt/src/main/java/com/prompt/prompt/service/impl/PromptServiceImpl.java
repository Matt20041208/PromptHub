package com.prompt.prompt.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.common.constant.CommonConstants;
import com.prompt.common.exception.BusinessException;
import com.prompt.common.exception.NotFoundException;
import com.prompt.prompt.dto.CategoryTreeVO;
import com.prompt.prompt.dto.PromptCreateDTO;
import com.prompt.prompt.dto.PromptDetailVO;
import com.prompt.prompt.dto.PromptListVO;
import com.prompt.prompt.dto.PromptQueryDTO;
import com.prompt.prompt.dto.PromptUpdateDTO;
import com.prompt.prompt.dto.PromptVersionVO;
import com.prompt.prompt.entity.Prompt;
import com.prompt.prompt.entity.PromptCategory;
import com.prompt.prompt.entity.PromptTag;
import com.prompt.prompt.entity.PromptTagRel;
import com.prompt.prompt.entity.PromptVersion;
import com.prompt.prompt.mapper.PromptCategoryMapper;
import com.prompt.prompt.mapper.PromptMapper;
import com.prompt.prompt.mapper.PromptTagMapper;
import com.prompt.prompt.mapper.PromptTagRelMapper;
import com.prompt.prompt.mapper.PromptVersionMapper;
import com.prompt.prompt.service.PromptService;
import com.prompt.prompt.feign.TradeClient;
import com.prompt.prompt.feign.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptServiceImpl extends ServiceImpl<PromptMapper, Prompt> implements PromptService {

    private final PromptMapper promptMapper;
    private final PromptTagRelMapper promptTagRelMapper;
    private final PromptTagMapper promptTagMapper;
    private final PromptCategoryMapper promptCategoryMapper;
    private final PromptVersionMapper promptVersionMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TradeClient tradeClient;
    private final UserClient userClient;

    @Override
    @Cacheable(value = "prompt_list", key = "'page_' + #dto.page + '_' + #dto.size + '_' + #dto.keyword + '_' + #dto.categoryId + '_' + #dto.sortBy", unless = "#result == null || #result.total == 0")
    public Page<PromptListVO> queryPage(PromptQueryDTO dto) {
        LambdaQueryWrapper<Prompt> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(Prompt::getTitle, dto.getKeyword())
                    .or().like(Prompt::getDescription, dto.getKeyword()));
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(Prompt::getCategoryId, dto.getCategoryId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(Prompt::getUserId, dto.getUserId());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            wrapper.eq(Prompt::getStatus, dto.getStatus());
        }
        if (dto.getMinPrice() != null) {
            wrapper.ge(Prompt::getPrice, dto.getMinPrice());
        }
        if (dto.getMaxPrice() != null) {
            wrapper.le(Prompt::getPrice, dto.getMaxPrice());
        }

        if (dto.getTagId() != null) {
            LambdaQueryWrapper<PromptTagRel> relWrapper = new LambdaQueryWrapper<>();
            relWrapper.eq(PromptTagRel::getTagId, dto.getTagId());
            List<PromptTagRel> rels = promptTagRelMapper.selectList(relWrapper);
            if (rels.isEmpty()) {
                return new Page<>();
            }
            List<Long> promptIds = rels.stream()
                    .map(PromptTagRel::getPromptId)
                    .distinct()
                    .collect(Collectors.toList());
            wrapper.in(Prompt::getId, promptIds);
        }

        String sortBy = StringUtils.hasText(dto.getSortBy()) ? dto.getSortBy() : "create_time";
        String sortOrder = StringUtils.hasText(dto.getSortOrder()) ? dto.getSortOrder() : "desc";

        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortBy) {
            case "price":
                wrapper.orderBy(true, isAsc, Prompt::getPrice);
                break;
            case "view_count":
                wrapper.orderBy(true, isAsc, Prompt::getViewCount);
                break;
            case "download_count":
                wrapper.orderBy(true, isAsc, Prompt::getDownloadCount);
                break;
            case "avg_rating":
                wrapper.orderBy(true, isAsc, Prompt::getAvgRating);
                break;
            default:
                wrapper.orderBy(true, isAsc, Prompt::getCreateTime);
                break;
        }

        Page<Prompt> page = new Page<>(dto.getPage(), dto.getSize());
        Page<Prompt> result = promptMapper.selectPage(page, wrapper);

        List<PromptListVO> records = result.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        Page<PromptListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Cacheable(value = "prompt_detail", key = "#id")
    public PromptDetailVO getDetail(Long id, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new NotFoundException("提示词不存在");
        }

        incrementViewCount(id);

        PromptDetailVO vo = new PromptDetailVO();
        BeanUtil.copyProperties(prompt, vo);
        vo.setCategoryId(prompt.getCategoryId());
        vo.setTags(getPromptTags(prompt.getId()));

        if (prompt.getCategoryId() != null) {
            PromptCategory category = promptCategoryMapper.selectById(prompt.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        vo.setVersions(getVersions(prompt.getId()));

        boolean isOwner = false;
        boolean isVip = false;
        if (userId != null) {
            isOwner = Objects.equals(prompt.getUserId(), userId);
            isVip = checkVip(userId);
        }

        boolean hasPaid = isVip || isOwner || prompt.getPrice().compareTo(BigDecimal.ZERO) <= 0;
        if (!hasPaid && userId != null) {
            hasPaid = checkPurchased(userId, id);
        }

        vo.setPurchased(hasPaid);

        if (!hasPaid && prompt.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            vo.setContentLocked(true);
            String truncated = prompt.getContent();
            if (truncated != null && truncated.length() > 150) {
                truncated = truncated.substring(0, 150) + "\n\n...（购买后解锁完整内容）";
            }
            vo.setContent(truncated);
        }

        return vo;
    }

    private boolean checkPurchased(Long userId, Long promptId) {
        try {
            var result = tradeClient.listPurchased(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData().getRecords().contains(promptId);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean checkVip(Long userId) {
        try {
            var result = userClient.getUser(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return Boolean.TRUE.equals(result.getData().get("vip"));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    @Transactional
    @CacheEvict(value = "prompt_list", allEntries = true)
    public void create(Long userId, PromptCreateDTO dto) {
        Prompt prompt = new Prompt();
        prompt.setUserId(userId);
        prompt.setTitle(dto.getTitle());
        prompt.setDescription(dto.getDescription());
        prompt.setContent(dto.getContent());
        prompt.setTemplateSchema(dto.getTemplateSchema());
        prompt.setCover(dto.getCover());
        prompt.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        prompt.setCategoryId(dto.getCategoryId());
        prompt.setStatus(CommonConstants.PROMPT_STATUS_DRAFT);
        prompt.setViewCount(0);
        prompt.setDownloadCount(0);
        prompt.setAvgRating(BigDecimal.ZERO);
        promptMapper.insert(prompt);

        saveTagRelations(prompt.getId(), dto.getTagIds());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"prompt_list", "prompt_detail"}, allEntries = true)
    public void update(Long id, Long userId, PromptUpdateDTO dto) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new NotFoundException("提示词不存在");
        }
        if (!Objects.equals(prompt.getUserId(), userId)) {
            throw new BusinessException("无权修改该提示词");
        }

        if (StringUtils.hasText(dto.getTitle())) {
            prompt.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            prompt.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getContent())) {
            prompt.setContent(dto.getContent());
        }
        if (dto.getTemplateSchema() != null) {
            prompt.setTemplateSchema(dto.getTemplateSchema());
        }
        if (dto.getCover() != null) {
            prompt.setCover(dto.getCover());
        }
        if (dto.getPrice() != null) {
            prompt.setPrice(dto.getPrice());
        }
        if (dto.getCategoryId() != null) {
            prompt.setCategoryId(dto.getCategoryId());
        }
        promptMapper.updateById(prompt);

        if (dto.getTagIds() != null) {
            LambdaQueryWrapper<PromptTagRel> relWrapper = new LambdaQueryWrapper<>();
            relWrapper.eq(PromptTagRel::getPromptId, id);
            promptTagRelMapper.delete(relWrapper);

            saveTagRelations(id, dto.getTagIds());
        }
    }

    @Override
    public void offline(Long id, Long userId) {
        Prompt prompt = getPromptWithOwnerCheck(id, userId);
        prompt.setStatus(CommonConstants.PROMPT_STATUS_OFFLINE);
        promptMapper.updateById(prompt);
    }

    @Override
    public void publish(Long id, Long userId) {
        Prompt prompt = getPromptWithOwnerCheck(id, userId);
        prompt.setStatus(CommonConstants.PROMPT_STATUS_PUBLISHED);
        promptMapper.updateById(prompt);
    }

    @Override
    @Transactional
    public void addVersion(Long promptId, Long userId, String content, String changelog) {
        Prompt prompt = getPromptWithOwnerCheck(promptId, userId);

        LambdaQueryWrapper<PromptVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptVersion::getPromptId, promptId);
        long count = promptVersionMapper.selectCount(wrapper);

        int versionNum = (int) count + 1;
        String versionNo = "1.0." + versionNum;

        PromptVersion version = new PromptVersion();
        version.setPromptId(promptId);
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setChangelog(changelog);
        promptVersionMapper.insert(version);

        prompt.setContent(content);
        promptMapper.updateById(prompt);
    }

    @Override
    public List<PromptVersionVO> getVersions(Long promptId) {
        LambdaQueryWrapper<PromptVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptVersion::getPromptId, promptId)
                .orderByDesc(PromptVersion::getCreateTime);
        List<PromptVersion> versions = promptVersionMapper.selectList(wrapper);

        return versions.stream().map(v -> {
            PromptVersionVO vo = new PromptVersionVO();
            vo.setId(v.getId());
            vo.setVersionNo(v.getVersionNo());
            vo.setChangelog(v.getChangelog());
            vo.setCreateTime(v.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void incrementViewCount(Long id) {
        stringRedisTemplate.opsForValue().increment("prompt:views:" + id);
    }

    @Override
    public void incrementDownloadCount(Long id) {
        stringRedisTemplate.opsForValue().increment("prompt:downloads:" + id);
    }

    @Scheduled(fixedRate = 60000)
    public void flushViewCountsToDb() {
        var keys = stringRedisTemplate.keys("prompt:views:*");
        if (keys != null) {
            for (String key : keys) {
                String countStr = stringRedisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    try {
                        Long promptId = Long.parseLong(key.replace("prompt:views:", ""));
                        int count = Integer.parseInt(countStr);
                        LambdaUpdateWrapper<Prompt> wrapper = new LambdaUpdateWrapper<>();
                        wrapper.eq(Prompt::getId, promptId)
                                .setSql("view_count = view_count + " + count);
                        promptMapper.update(null, wrapper);
                        stringRedisTemplate.delete(key);
                    } catch (Exception ignored) {}
                }
            }
        }

        keys = stringRedisTemplate.keys("prompt:downloads:*");
        if (keys != null) {
            for (String key : keys) {
                String countStr = stringRedisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    try {
                        Long promptId = Long.parseLong(key.replace("prompt:downloads:", ""));
                        int count = Integer.parseInt(countStr);
                        LambdaUpdateWrapper<Prompt> wrapper = new LambdaUpdateWrapper<>();
                        wrapper.eq(Prompt::getId, promptId)
                                .setSql("download_count = download_count + " + count);
                        promptMapper.update(null, wrapper);
                        stringRedisTemplate.delete(key);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public void updateRating(Long id, BigDecimal avgRating) {
        LambdaUpdateWrapper<Prompt> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Prompt::getId, id)
                .set(Prompt::getAvgRating, avgRating.setScale(2, RoundingMode.HALF_UP));
        promptMapper.update(null, wrapper);
    }

    private Prompt getPromptWithOwnerCheck(Long id, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new NotFoundException("提示词不存在");
        }
        if (!Objects.equals(prompt.getUserId(), userId)) {
            throw new BusinessException("无权操作该提示词");
        }
        return prompt;
    }

    private void saveTagRelations(Long promptId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            PromptTagRel rel = new PromptTagRel();
            rel.setPromptId(promptId);
            rel.setTagId(tagId);
            promptTagRelMapper.insert(rel);
        }
    }

    private List<String> getPromptTags(Long promptId) {
        LambdaQueryWrapper<PromptTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(PromptTagRel::getPromptId, promptId);
        List<PromptTagRel> rels = promptTagRelMapper.selectList(relWrapper);
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = rels.stream().map(PromptTagRel::getTagId).collect(Collectors.toList());
        List<PromptTag> tags = promptTagMapper.selectBatchIds(tagIds);
        return tags.stream().map(PromptTag::getName).collect(Collectors.toList());
    }

    private PromptListVO toListVO(Prompt prompt) {
        PromptListVO vo = new PromptListVO();
        BeanUtil.copyProperties(prompt, vo);
        vo.setTags(getPromptTags(prompt.getId()));

        if (prompt.getCategoryId() != null) {
            PromptCategory category = promptCategoryMapper.selectById(prompt.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }
}
