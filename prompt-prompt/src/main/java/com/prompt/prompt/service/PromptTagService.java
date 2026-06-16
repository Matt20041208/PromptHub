package com.prompt.prompt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.prompt.entity.PromptTag;

import java.util.List;

public interface PromptTagService extends IService<PromptTag> {
    List<PromptTag> listAll();
}
