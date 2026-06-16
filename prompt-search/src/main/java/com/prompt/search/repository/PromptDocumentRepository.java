package com.prompt.search.repository;

import com.prompt.search.document.PromptDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptDocumentRepository extends ElasticsearchRepository<PromptDocument, Long> {
}
