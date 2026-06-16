package com.prompt.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String TRADE_EXCHANGE = "trade.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "order.dead.letter.exchange";

    @Bean
    public TopicExchange tradeExchange() {
        return new TopicExchange(TRADE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE);
    }

    // 延时队列：消息存活 15 分钟，过期后投递到死信交换机
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.cancel");
        args.put("x-message-ttl", 15 * 60 * 1000); // 15 分钟
        return new Queue("order.delay.queue", true, false, false, args);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(tradeExchange()).with("order.delay");
    }

    // 死信消费队列：到期订单在这里被消费
    @Bean
    public Queue orderCancelQueue() {
        return new Queue("order.cancel.queue", true);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(deadLetterExchange()).with("order.cancel");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
