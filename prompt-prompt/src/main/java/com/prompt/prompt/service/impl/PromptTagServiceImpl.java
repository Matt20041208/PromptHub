package com.prompt.prompt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.prompt.entity.PromptTag;
import com.prompt.prompt.mapper.PromptTagMapper;
import com.prompt.prompt.service.PromptTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptTagServiceImpl extends ServiceImpl<PromptTagMapper, PromptTag> implements PromptTagService {

    @Override
    public List<PromptTag> listAll() {
        return baseMapper.selectList(null);
    }
}
