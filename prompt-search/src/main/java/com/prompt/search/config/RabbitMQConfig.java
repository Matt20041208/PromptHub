package com.prompt.search.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue promptIndexQueue() {
        return new Queue("prompt.index.queue", true);
    }

    @Bean
    public TopicExchange promptExchange() {
        return new TopicExchange("prompt.exchange", true, false);
    }

    @Bean
    public Binding promptIndexBinding() {
        return BindingBuilder.bind(promptIndexQueue()).to(promptExchange()).with("prompt.index");
    }
}
