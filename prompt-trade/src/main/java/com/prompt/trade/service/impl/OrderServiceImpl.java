package com.prompt.trade.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.common.constant.CommonConstants;
import com.prompt.common.exception.BusinessException;
import com.prompt.common.exception.NotFoundException;
import com.prompt.common.result.PageResult;
import com.prompt.common.util.RedisLockUtil;
import com.prompt.trade.dto.CreateOrderDTO;
import com.prompt.trade.dto.OrderVO;
import com.prompt.trade.entity.OrderInfo;
import com.prompt.trade.entity.PurchaseRecord;
import com.prompt.trade.entity.TransactionLog;
import com.prompt.trade.entity.UserBalance;
import com.prompt.trade.mapper.OrderMapper;
import com.prompt.trade.mapper.PurchaseRecordMapper;
import com.prompt.trade.mapper.TransactionLogMapper;
import com.prompt.trade.mapper.UserBalanceMapper;
import com.prompt.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderInfo> implements OrderService {

    private final OrderMapper orderMapper;
    private final UserBalanceMapper userBalanceMapper;
    private final TransactionLogMapper transactionLogMapper;
    private final PurchaseRecordMapper purchaseRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public String createOrder(Long buyerId, CreateOrderDTO dto) {
        Long promptId = dto.getPromptId();
        Long sellerId = null;
        BigDecimal price = BigDecimal.ZERO;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String promptUrl = "http://127.0.0.1:9102/api/prompt/" + promptId;
            var response = restTemplate.getForEntity(promptUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSONUtil.parseObj(response.getBody());
                if (json.getInt("code") == 200 && json.get("data") != null) {
                    JSONObject data = json.getJSONObject("data");
                    sellerId = data.getLong("userId");
                    price = data.getBigDecimal("price");
                }
            }
        } catch (Exception ignored) {}

        if (sellerId != null && sellerId.equals(buyerId)) {
            throw new BusinessException("不能购买自己的提示词");
        }

        LambdaQueryWrapper<PurchaseRecord> purchaseWrapper = new LambdaQueryWrapper<>();
        purchaseWrapper.eq(PurchaseRecord::getUserId, buyerId)
                .eq(PurchaseRecord::getPromptId, promptId);
        if (purchaseRecordMapper.selectCount(purchaseWrapper) > 0 && !checkVip(buyerId)) {
            throw new BusinessException("您已购买过该提示词");
        }

        String orderNo = UUID.fastUUID().toString(true).replace("-", "");
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId != null ? sellerId : 0L);
        order.setPromptId(promptId);
        order.setAmount(price != null ? price : BigDecimal.ZERO);
        order.setStatus(CommonConstants.ORDER_STATUS_UNPAID);
        orderMapper.insert(order);

        // 投递延时消息：15分钟后自动取消未支付订单
        try {
            Map<String, Object> delayMsg = new HashMap<>();
            delayMsg.put("orderNo", orderNo);
            delayMsg.put("buyerId", buyerId);
            rabbitTemplate.convertAndSend("trade.exchange", "order.delay", JSONUtil.toJsonStr(delayMsg));
        } catch (Exception ignored) {}

        return orderNo;
    }

    @RabbitListener(queues = "order.cancel.queue")
    @Transactional
    public void autoCancelOrder(String message) {
        try {
            JSONObject json = JSONUtil.parseObj(message);
            String orderNo = json.getStr("orderNo");
            var wrapper = new LambdaQueryWrapper<OrderInfo>();
            wrapper.eq(OrderInfo::getOrderNo, orderNo);
            OrderInfo order = orderMapper.selectOne(wrapper);
            if (order != null && CommonConstants.ORDER_STATUS_UNPAID.equals(order.getStatus())) {
                order.setStatus(CommonConstants.ORDER_STATUS_CANCELLED);
                orderMapper.updateById(order);
            }
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public void pay(Long userId, String orderNo) {
        RedisLockUtil lock = new RedisLockUtil(stringRedisTemplate, "pay:" + orderNo);
        if (!lock.tryLock(5)) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
        try {
            doPay(userId, orderNo);
        } finally {
            lock.unlock();
        }
    }

    private void doPay(Long userId, String orderNo) {
        var wrapper = new LambdaQueryWrapper<OrderInfo>();
        wrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo order = orderMapper.selectOne(wrapper);
        if (order == null) throw new NotFoundException("订单不存在");
        if (!CommonConstants.ORDER_STATUS_UNPAID.equals(order.getStatus()))
            throw new BusinessException("订单状态不正确");
        if (!order.getBuyerId().equals(userId))
            throw new BusinessException("无权支付该订单");

        boolean isVip = checkVip(userId);

        if (!isVip) {
            UserBalance balance = getOrCreateBalance(userId);
            if (balance.getBalance().compareTo(order.getAmount()) < 0)
                throw new BusinessException("余额不足");

            int updated = userBalanceMapper.update(null,
                new LambdaUpdateWrapper<UserBalance>()
                    .eq(UserBalance::getUserId, userId)
                    .eq(UserBalance::getVersion, balance.getVersion())
                    .setSql("balance = balance - " + order.getAmount())
                    .setSql("version = version + 1"));
            if (updated == 0) throw new BusinessException("系统繁忙");

            UserBalance newBalance = userBalanceMapper.selectById(balance.getId());
            TransactionLog log = new TransactionLog();
            log.setUserId(userId); log.setOrderId(order.getId());
            log.setType("PAY"); log.setAmount(order.getAmount().negate());
            log.setBalanceBefore(balance.getBalance());
            log.setBalanceAfter(newBalance.getBalance());
            transactionLogMapper.insert(log);
        }

        order.setStatus(CommonConstants.ORDER_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        try {
            RestTemplate rt = new RestTemplate();
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("type", "ORDER_PAID");
            body.put("title", isVip ? "VIP免费获得" : "订单支付成功");
            body.put("content", isVip ? "超级VIP免费获取" : "订单"+orderNo+"支付 ¥"+order.getAmount());
            body.put("bizType", "ORDER");
            body.put("bizId", order.getId());
            rt.postForEntity("http://127.0.0.1:9106/api/notify/internal/send", body, String.class);
        } catch (Exception ignored) {}

        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(userId);
        record.setPromptId(order.getPromptId());
        record.setOrderId(order.getId());
        purchaseRecordMapper.insert(record);
    }

    private boolean checkVip(Long userId) {
        try {
            RestTemplate rt = new RestTemplate();
            String resp = rt.getForObject("http://127.0.0.1:9101/api/user/" + userId, String.class);
            if (resp != null) {
                JSONObject json = JSONUtil.parseObj(resp);
                if (json.getInt("code") == 200 && json.getJSONObject("data") != null)
                    return Boolean.TRUE.equals(json.getJSONObject("data").getBool("vip"));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public PageResult<OrderVO> listOrders(Long userId, String role) {
        var wrapper = new LambdaQueryWrapper<OrderInfo>();
        if ("ADMIN".equals(role)) {
            wrapper.orderByDesc(OrderInfo::getCreateTime);
        } else {
            wrapper.and(w -> w.eq(OrderInfo::getBuyerId, userId)
                            .or().eq(OrderInfo::getSellerId, userId))
                    .orderByDesc(OrderInfo::getCreateTime);
        }
        Page<OrderInfo> page = new Page<>(1, 20);
        Page<OrderInfo> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> records = result.getRecords().stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setOrderNo(o.getOrderNo()); vo.setAmount(o.getAmount());
            vo.setStatus(o.getStatus()); vo.setCreateTime(o.getCreateTime());
            vo.setPayTime(o.getPayTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<Long> listPurchased(Long userId) {
        var wrapper = new LambdaQueryWrapper<PurchaseRecord>();
        wrapper.eq(PurchaseRecord::getUserId, userId).orderByDesc(PurchaseRecord::getCreateTime);
        Page<PurchaseRecord> page = new Page<>(1, 100);
        var result = purchaseRecordMapper.selectPage(page, wrapper);
        List<Long> ids = result.getRecords().stream().map(PurchaseRecord::getPromptId).collect(Collectors.toList());
        return new PageResult<>(ids, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public UserBalance getBalance(Long userId) {
        return getOrCreateBalance(userId);
    }

    @Override
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("充值金额必须大于0");
        UserBalance balance = getOrCreateBalance(userId);
        int updated = userBalanceMapper.update(null,
            new LambdaUpdateWrapper<UserBalance>()
                .eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getVersion, balance.getVersion())
                .setSql("balance = balance + " + amount)
                .setSql("version = version + 1"));
        if (updated == 0) throw new BusinessException("系统繁忙");
        UserBalance newBalance = userBalanceMapper.selectById(balance.getId());
        TransactionLog log = new TransactionLog();
        log.setUserId(userId); log.setType("RECHARGE");
        log.setAmount(amount);
        log.setBalanceBefore(balance.getBalance());
        log.setBalanceAfter(newBalance.getBalance());
        transactionLogMapper.insert(log);
    }

    private UserBalance getOrCreateBalance(Long userId) {
        var wrapper = new LambdaQueryWrapper<UserBalance>();
        wrapper.eq(UserBalance::getUserId, userId);
        UserBalance balance = userBalanceMapper.selectOne(wrapper);
        if (balance == null) {
            balance = new UserBalance();
            balance.setUserId(userId); balance.setBalance(BigDecimal.ZERO);
            balance.setFreeze(BigDecimal.ZERO); balance.setVersion(1);
            balance.setUpdateTime(LocalDateTime.now());
            userBalanceMapper.insert(balance);
        }
        return balance;
    }
}
