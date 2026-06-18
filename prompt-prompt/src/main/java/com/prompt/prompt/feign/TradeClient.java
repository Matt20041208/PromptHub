package com.prompt.prompt.feign;

import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "prompt-trade", path = "/api/trade")
public interface TradeClient {

    @GetMapping("/purchased")
    Result<PageResult<Long>> listPurchased(@RequestHeader("X-User-Id") Long userId);
}
