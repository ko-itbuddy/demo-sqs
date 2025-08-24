package com.demo.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Consumer 서비스 메인 애플리케이션 클래스
 * SQS 메시지 수신 및 처리를 담당하는 Spring Boot 애플리케이션
 */
@SpringBootApplication
@EnableAsync
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}