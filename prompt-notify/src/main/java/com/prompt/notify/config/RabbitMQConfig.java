package com.prompt.notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRADE_EXCHANGE = "trade.exchange";
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String NOTIFY_ORDER_QUEUE = "notify.order.queue";
    public static final String NOTIFY_REVIEW_QUEUE = "notify.review.queue";
    public static final String VIP_QUEUE = "notify.vip.queue";

    @Bean
    public TopicExchange tradeExchange() {
        return new TopicExchange(TRADE_EXCHANGE);
    }

    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange(REVIEW_EXCHANGE);
    }

    @Bean
    public Queue notifyOrderQueue() {
        return new Queue(NOTIFY_ORDER_QUEUE);
    }

    @Bean
    public Queue notifyReviewQueue() {
        return new Queue(NOTIFY_REVIEW_QUEUE);
    }

    @Bean
    public Queue vipQueue() {
        return new Queue(VIP_QUEUE);
    }

    @Bean
    public Binding notifyOrderBinding() {
        return BindingBuilder.bind(notifyOrderQueue()).to(tradeExchange()).with("trade.*");
    }

    @Bean
    public Binding notifyReviewBinding() {
        return BindingBuilder.bind(notifyReviewQueue()).to(reviewExchange()).with("review.*");
    }

    @Bean
    public Binding vipBinding() {
        return BindingBuilder.bind(vipQueue()).to(tradeExchange()).with("vip.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
