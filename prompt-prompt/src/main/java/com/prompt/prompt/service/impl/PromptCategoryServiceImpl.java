package com.prompt.prompt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.prompt.dto.CategoryTreeVO;
import com.prompt.prompt.entity.PromptCategory;
import com.prompt.prompt.mapper.PromptCategoryMapper;
import com.prompt.prompt.service.PromptCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptCategoryServiceImpl extends ServiceImpl<PromptCategoryMapper, PromptCategory> implements PromptCategoryService {

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        List<PromptCategory> allCategories = baseMapper.selectList(null);
        if (allCategories.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<PromptCategory>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(PromptCategory::getParentId));

        return allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(c -> buildTreeNode(c, childrenMap))
                .collect(Collectors.toList());
    }

    private CategoryTreeVO buildTreeNode(PromptCategory category, Map<Long, List<PromptCategory>> childrenMap) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSort(category.getSort() != null ? category.getSort() : 0);

        List<PromptCategory> children = childrenMap.getOrDefault(category.getId(), Collections.emptyList());
        List<CategoryTreeVO> childVOs = children.stream()
                .sorted((a, b) -> {
                    int sortA = a.getSort() != null ? a.getSort() : 0;
                    int sortB = b.getSort() != null ? b.getSort() : 0;
                    return Integer.compare(sortA, sortB);
                })
                .map(c -> buildTreeNode(c, childrenMap))
                .collect(Collectors.toList());
        vo.setChildren(childVOs);

        return vo;
    }
}
