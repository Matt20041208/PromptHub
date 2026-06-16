package com.prompt.review.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange("review.exchange", true, false);
    }

    @Bean
    public Queue reviewCreatedQueue() {
        return new Queue("review.created.queue", true);
    }

    @Bean
    public Binding reviewCreatedBinding() {
        return BindingBuilder.bind(reviewCreatedQueue()).to(reviewExchange()).with("review.created");
    }

    @Bean
    public TopicExchange promptExchange() {
        return new TopicExchange("prompt.exchange", true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
