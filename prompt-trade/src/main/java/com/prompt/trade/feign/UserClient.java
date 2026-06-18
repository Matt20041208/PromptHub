package com.prompt.trade.feign;

import com.prompt.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "prompt-user", path = "/api/user")
public interface UserClient {

    @GetMapping("/{id}")
    Result<Map<String, Object>> getUser(@PathVariable("id") Long id);
}
