package com.prompt.trade.controller;

import com.prompt.common.constant.CommonConstants;
import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import com.prompt.trade.dto.CreateOrderDTO;
import com.prompt.trade.dto.OrderVO;
import com.prompt.trade.entity.UserBalance;
import com.prompt.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Tag(name = "交易管理", description = "订单、支付、余额")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/order")
    @Operation(summary = "创建订单")
    public Result<String> createOrder(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                      @Valid @RequestBody CreateOrderDTO dto) {
        String orderNo = orderService.createOrder(userId, dto);
        return Result.success(orderNo);
    }

    @PostMapping("/order/{orderNo}/pay")
    @Operation(summary = "支付订单")
    public Result<Void> pay(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                            @PathVariable String orderNo) {
        orderService.pay(userId, orderNo);
        return Result.success();
    }

    @GetMapping("/order/list")
    @Operation(summary = "查询订单列表")
    public Result<PageResult<OrderVO>> listOrders(
            @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
            @RequestHeader(CommonConstants.USER_ROLE_HEADER) String role) {
        PageResult<OrderVO> result = orderService.listOrders(userId, role);
        return Result.success(result);
    }

    @GetMapping("/purchased")
    @Operation(summary = "查询已购买的提示词ID")
    public Result<PageResult<Long>> listPurchased(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        PageResult<Long> result = orderService.listPurchased(userId);
        return Result.success(result);
    }

    @GetMapping("/balance")
    @Operation(summary = "查询余额")
    public Result<UserBalance> getBalance(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        UserBalance balance = orderService.getBalance(userId);
        return Result.success(balance);
    }

    @PostMapping("/recharge")
    @Operation(summary = "充值")
    public Result<Void> recharge(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                 @RequestParam BigDecimal amount) {
        orderService.recharge(userId, amount);
        return Result.success();
    }
}
