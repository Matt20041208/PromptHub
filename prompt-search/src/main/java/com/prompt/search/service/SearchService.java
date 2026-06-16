package com.prompt.search.service;

import com.prompt.common.result.PageResult;
import com.prompt.search.document.PromptDocument;
import com.prompt.search.dto.SearchQueryDTO;
import com.prompt.search.dto.SearchResultVO;

import java.util.List;

public interface SearchService {
    PageResult<SearchResultVO> search(SearchQueryDTO dto);
    List<String> suggest(String keyword);
    void indexPrompt(PromptDocument doc);
    void deleteIndex(Long promptId);
    void saveSearchLog(Long userId, String keyword, int resultCount);
    List<String> getHotKeywords();
}
