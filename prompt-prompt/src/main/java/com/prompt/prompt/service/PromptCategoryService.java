package com.prompt.prompt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.prompt.dto.CategoryTreeVO;
import com.prompt.prompt.entity.PromptCategory;

import java.util.List;

public interface PromptCategoryService extends IService<PromptCategory> {
    List<CategoryTreeVO> getCategoryTree();
}
