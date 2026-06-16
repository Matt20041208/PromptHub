package com.prompt.search.controller;

import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import com.prompt.search.document.PromptDocument;
import com.prompt.search.dto.SearchQueryDTO;
import com.prompt.search.dto.SearchResultVO;
import com.prompt.search.service.SearchService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索管理", description = "全文搜索、关键词建议、索引管理")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "搜索提示词")
    public Result<PageResult<SearchResultVO>> search(@Valid SearchQueryDTO dto) {
        PageResult<SearchResultVO> result = searchService.search(dto);
        return Result.success(result);
    }

    @GetMapping("/suggest")
    @Operation(summary = "搜索建议")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        List<String> suggestions = searchService.suggest(keyword);
        return Result.success(suggestions);
    }

    @GetMapping("/hot")
    @Operation(summary = "热门搜索词")
    public Result<List<String>> hotKeywords() {
        List<String> hot = searchService.getHotKeywords();
        return Result.success(hot);
    }

    @PostMapping("/index")
    @Operation(summary = "手动索引提示词")
    public Result<Void> index(@RequestBody PromptDocument doc) {
        searchService.indexPrompt(doc);
        return Result.success();
    }

    @DeleteMapping("/index/{promptId}")
    @Operation(summary = "删除索引")
    public Result<Void> deleteIndex(@PathVariable Long promptId) {
        searchService.deleteIndex(promptId);
        return Result.success();
    }
}
