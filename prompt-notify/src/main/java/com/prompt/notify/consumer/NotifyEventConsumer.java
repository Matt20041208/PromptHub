package com.prompt.notify.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.prompt.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notify.order.queue")
    public void handleOrderEvent(String message) {
        log.info("Received order event: {}", message);
        try {
            JSONObject msg = JSONUtil.parseObj(message);
            Long userId = msg.getLong("userId");
            String type = msg.getStr("type");
            String orderNo = msg.getStr("orderNo", "");

            if (userId == null) return;

            if ("ORDER_PAID".equals(type)) {
                notificationService.send(userId, "ORDER",
                        "订单支付成功",
                        "订单 " + orderNo + " 已支付成功，金额：" + msg.getBigDecimal("amount"),
                        "ORDER", userId);
            } else if ("ORDER_CANCELLED".equals(type)) {
                notificationService.send(userId, "ORDER",
                        "订单已取消",
                        "订单 " + orderNo + " 已取消",
                        "ORDER", userId);
            } else {
                notificationService.send(userId, "ORDER",
                        "订单状态更新",
                        "订单 " + orderNo + " 状态已更新",
                        "ORDER", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "notify.review.queue")
    public void handleReviewEvent(String message) {
        log.info("Received review event: {}", message);
        try {
            JSONObject msg = JSONUtil.parseObj(message);
            String type = msg.getStr("type");
            Long userId = msg.getLong("userId");
            Long promptId = msg.getLong("promptId");

            if (userId == null) {
                log.warn("Review event without userId, skipping");
                return;
            }

            if ("REVIEW_CREATED".equals(type)) {
                notificationService.send(userId, "REVIEW",
                        "收到新评价",
                        "您的提示词收到了一条" + msg.getInt("rating", 5) + "星评价",
                        "REVIEW", promptId);
            } else {
                notificationService.send(userId, "REVIEW",
                        "评价更新",
                        "您的提示词有新的评价动态",
                        "REVIEW", promptId);
            }
        } catch (Exception e) {
            log.error("Failed to process review event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "notify.vip.queue")
    public void handleVipEvent(String message) {
        log.info("Received VIP event: {}", message);
        try {
            JSONObject msg = JSONUtil.parseObj(message);
            Long userId = msg.getLong("userId");
            if (userId == null) return;

            if ("VIP_BUY".equals(msg.getStr("type"))) {
                notificationService.send(userId, "VIP",
                        "🎉 超级VIP购买成功",
                        "恭喜您成为超级VIP！现在所有提示词免费查看，尽情探索吧",
                        "VIP", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process VIP event: {}", e.getMessage());
        }
    }
}
