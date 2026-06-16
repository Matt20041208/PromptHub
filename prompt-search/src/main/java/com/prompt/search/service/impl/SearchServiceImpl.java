package com.prompt.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.prompt.common.result.PageResult;
import com.prompt.search.document.PromptDocument;
import com.prompt.search.dto.SearchQueryDTO;
import com.prompt.search.dto.SearchResultVO;
import com.prompt.search.entity.SearchLog;
import com.prompt.search.mapper.SearchLogMapper;
import com.prompt.search.repository.PromptDocumentRepository;
import com.prompt.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final PromptDocumentRepository promptDocumentRepository;
    private final SearchLogMapper searchLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<SearchResultVO> search(SearchQueryDTO dto) {
        int page = (int) dto.getPage();
        int size = (int) dto.getSize();

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(dto.getKeyword())) {
            boolBuilder.must(m -> m
                    .multiMatch(mm -> mm
                            .query(dto.getKeyword())
                            .fields("title", "description")
                    ));
        }

        if (StringUtils.hasText(dto.getCategory())) {
            boolBuilder.filter(f -> f
                    .term(t -> t
                            .field("category")
                            .value(dto.getCategory())
                    ));
        }

        if (dto.getMinPrice() != null && dto.getMaxPrice() != null) {
            boolBuilder.filter(f -> f
                    .range(r -> r
                            .field("price")
                            .gte(JsonData.of(dto.getMinPrice().doubleValue()))
                            .lte(JsonData.of(dto.getMaxPrice().doubleValue()))
                    ));
        } else if (dto.getMinPrice() != null) {
            boolBuilder.filter(f -> f
                    .range(r -> r
                            .field("price")
                            .gte(JsonData.of(dto.getMinPrice().doubleValue()))
                    ));
        } else if (dto.getMaxPrice() != null) {
            boolBuilder.filter(f -> f
                    .range(r -> r
                            .field("price")
                            .lte(JsonData.of(dto.getMaxPrice().doubleValue()))
                    ));
        }

        Query query;
        BoolQuery boolQuery = boolBuilder.build();
        if (hasBoolClauses(boolQuery)) {
            query = Query.of(q -> q.bool(boolQuery));
        } else {
            query = Query.of(q -> q.matchAll(m -> m));
        }

        String sortBy = mapSortField(dto.getSortBy());
        boolean isAsc = "asc".equalsIgnoreCase(dto.getSortOrder());
        Sort sort = isAsc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(page - 1, size, sort))
                .build();

        SearchHits<PromptDocument> searchHits = elasticsearchOperations.search(nativeQuery, PromptDocument.class);

        List<SearchResultVO> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> {
                    SearchResultVO vo = new SearchResultVO();
                    BeanUtil.copyProperties(doc, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        long total = searchHits.getTotalHits();

        if (StringUtils.hasText(dto.getKeyword())) {
            saveSearchLog(null, dto.getKeyword(), (int) total);
        }

        PageResult<SearchResultVO> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(total);
        pageResult.setPage(page);
        pageResult.setSize(size);
        return pageResult;
    }

    private boolean hasBoolClauses(BoolQuery boolQuery) {
        return (boolQuery.must() != null && !boolQuery.must().isEmpty())
                || (boolQuery.filter() != null && !boolQuery.filter().isEmpty())
                || (boolQuery.should() != null && !boolQuery.should().isEmpty())
                || (boolQuery.mustNot() != null && !boolQuery.mustNot().isEmpty());
    }

    private String mapSortField(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "createTime";
        }
        switch (sortBy) {
            case "price":
            case "avgRating":
            case "viewCount":
            case "downloadCount":
            case "createTime":
                return sortBy;
            case "create_time":
                return "createTime";
            default:
                return "createTime";
        }
    }

    @Override
    public List<String> suggest(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        QueryWrapper<SearchLog> wrapper = new QueryWrapper<>();
        wrapper.select("keyword", "count(*) as cnt")
                .likeRight("keyword", keyword)
                .groupBy("keyword")
                .orderByDesc("cnt")
                .last("LIMIT 10");
        List<Map<String, Object>> list = searchLogMapper.selectMaps(wrapper);
        return list.stream()
                .map(m -> (String) m.get("keyword"))
                .collect(Collectors.toList());
    }

    @Override
    public void indexPrompt(PromptDocument doc) {
        promptDocumentRepository.save(doc);
    }

    @Override
    public void deleteIndex(Long promptId) {
        promptDocumentRepository.deleteById(promptId);
    }

    @Override
    public void saveSearchLog(Long userId, String keyword, int resultCount) {
        try {
            stringRedisTemplate.opsForZSet().incrementScore("search:hot", keyword, 1);
        } catch (Exception ignored) {}
        SearchLog log = new SearchLog();
        log.setUserId(userId);
        log.setKeyword(keyword);
        log.setResultCount(resultCount);
        searchLogMapper.insert(log);
    }

    @Override
    public List<String> getHotKeywords() {
        var set = stringRedisTemplate.opsForZSet().reverseRange("search:hot", 0, 9);
        if (set != null && !set.isEmpty()) {
            return List.copyOf(set);
        }
        QueryWrapper<SearchLog> wrapper = new QueryWrapper<>();
        wrapper.select("keyword", "count(*) as cnt")
                .groupBy("keyword")
                .orderByDesc("cnt")
                .last("LIMIT 10");
        List<Map<String, Object>> list = searchLogMapper.selectMaps(wrapper);
        return list.stream()
                .map(m -> (String) m.get("keyword"))
                .collect(Collectors.toList());
    }
}
