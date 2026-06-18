package com.prompt.trade.feign;

import com.prompt.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "prompt-prompt", path = "/api/prompt")
public interface PromptClient {

    @GetMapping("/{id}")
    Result<Map<String, Object>> getDetail(@PathVariable("id") Long id);
}
