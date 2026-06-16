package com.prompt.prompt.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prompt.common.constant.CommonConstants;
import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import com.prompt.prompt.dto.CategoryTreeVO;
import com.prompt.prompt.dto.PromptCreateDTO;
import com.prompt.prompt.dto.PromptDetailVO;
import com.prompt.prompt.dto.PromptListVO;
import com.prompt.prompt.dto.PromptQueryDTO;
import com.prompt.prompt.dto.PromptUpdateDTO;
import com.prompt.prompt.dto.PromptVersionVO;
import com.prompt.prompt.entity.PromptTag;
import com.prompt.prompt.service.PromptCategoryService;
import com.prompt.prompt.service.PromptService;
import com.prompt.prompt.service.PromptTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
@Tag(name = "提示词管理", description = "提示词CRUD、分类、标签")
public class PromptController {

    private final PromptService promptService;
    private final PromptCategoryService promptCategoryService;
    private final PromptTagService promptTagService;

    @PostMapping
    @Operation(summary = "创建提示词")
    public Result<Void> create(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                               @Valid @RequestBody PromptCreateDTO dto) {
        promptService.create(userId, dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取提示词详情")
    public Result<PromptDetailVO> getDetail(@PathVariable Long id,
                                            @RequestHeader(value = CommonConstants.USER_ID_HEADER, required = false) Long userId) {
        PromptDetailVO vo = promptService.getDetail(id, userId);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新提示词")
    public Result<Void> update(@PathVariable Long id,
                               @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                               @RequestBody PromptUpdateDTO dto) {
        promptService.update(id, userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "下架提示词")
    public Result<Void> offline(@PathVariable Long id,
                                @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        promptService.offline(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布提示词")
    public Result<Void> publish(@PathVariable Long id,
                                @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        promptService.publish(id, userId);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询提示词")
    public Result<PageResult<PromptListVO>> list(@Valid PromptQueryDTO dto) {
        Page<PromptListVO> page = promptService.queryPage(dto);
        return PageResult.success(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @PostMapping("/{id}/version")
    @Operation(summary = "添加提示词版本")
    public Result<Void> addVersion(@PathVariable Long id,
                                   @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                   @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String changelog = body.getOrDefault("changelog", "");
        promptService.addVersion(id, userId, content, changelog);
        return Result.success();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "获取提示词版本列表")
    public Result<List<PromptVersionVO>> getVersions(@PathVariable Long id) {
        List<PromptVersionVO> versions = promptService.getVersions(id);
        return Result.success(versions);
    }

    @GetMapping("/category")
    @Operation(summary = "获取分类树")
    public Result<List<CategoryTreeVO>> getCategoryTree() {
        List<CategoryTreeVO> tree = promptCategoryService.getCategoryTree();
        return Result.success(tree);
    }

    @GetMapping("/tag")
    @Operation(summary = "获取所有标签")
    public Result<List<PromptTag>> getTags() {
        List<PromptTag> tags = promptTagService.listAll();
        return Result.success(tags);
    }
}
