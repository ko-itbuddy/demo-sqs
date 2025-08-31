package com.demo.consumer.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 설정
 */
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .build();
    }
}