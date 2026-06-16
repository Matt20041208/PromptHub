package com.prompt.prompt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.prompt")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.prompt.prompt.feign")
@EnableCaching
@EnableScheduling
public class PromptApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromptApplication.class, args);
    }
}
