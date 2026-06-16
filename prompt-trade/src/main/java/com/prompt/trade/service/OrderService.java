package com.prompt.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.common.result.PageResult;
import com.prompt.trade.dto.CreateOrderDTO;
import com.prompt.trade.dto.OrderVO;
import com.prompt.trade.entity.OrderInfo;
import com.prompt.trade.entity.UserBalance;

import java.math.BigDecimal;

public interface OrderService extends IService<OrderInfo> {
    String createOrder(Long buyerId, CreateOrderDTO dto);
    void pay(Long userId, String orderNo);
    PageResult<OrderVO> listOrders(Long userId, String role);
    PageResult<Long> listPurchased(Long userId);
    UserBalance getBalance(Long userId);
    void recharge(Long userId, BigDecimal amount);
}
