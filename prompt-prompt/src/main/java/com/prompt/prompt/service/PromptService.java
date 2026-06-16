package com.prompt.prompt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.prompt.dto.PromptCreateDTO;
import com.prompt.prompt.dto.PromptDetailVO;
import com.prompt.prompt.dto.PromptListVO;
import com.prompt.prompt.dto.PromptQueryDTO;
import com.prompt.prompt.dto.PromptUpdateDTO;
import com.prompt.prompt.dto.PromptVersionVO;
import com.prompt.prompt.entity.Prompt;

import java.math.BigDecimal;
import java.util.List;

public interface PromptService extends IService<Prompt> {
    Page<PromptListVO> queryPage(PromptQueryDTO dto);
    PromptDetailVO getDetail(Long id, Long userId);
    void create(Long userId, PromptCreateDTO dto);
    void update(Long id, Long userId, PromptUpdateDTO dto);
    void offline(Long id, Long userId);
    void publish(Long id, Long userId);
    void addVersion(Long promptId, Long userId, String content, String changelog);
    List<PromptVersionVO> getVersions(Long promptId);
    void incrementViewCount(Long id);
    void incrementDownloadCount(Long id);
    void updateRating(Long id, BigDecimal avgRating);
}
