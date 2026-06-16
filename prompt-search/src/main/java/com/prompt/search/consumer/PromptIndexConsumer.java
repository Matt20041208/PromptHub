package com.prompt.search.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompt.search.document.PromptDocument;
import com.prompt.search.dto.PromptIndexMessage;
import com.prompt.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptIndexConsumer {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "prompt.index.queue")
    public void handlePromptIndexMessage(String message) {
        try {
            PromptIndexMessage indexMessage = objectMapper.readValue(message, PromptIndexMessage.class);

            if ("DELETE".equalsIgnoreCase(indexMessage.getAction())) {
                searchService.deleteIndex(indexMessage.getPromptId());
                log.info("Deleted index for prompt {}", indexMessage.getPromptId());
                return;
            }

            PromptDocument doc = new PromptDocument();
            BeanUtil.copyProperties(indexMessage, doc);
            searchService.indexPrompt(doc);
            log.info("Indexed prompt {}", indexMessage.getPromptId());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse prompt index message: {}", message, e);
        }
    }
}
