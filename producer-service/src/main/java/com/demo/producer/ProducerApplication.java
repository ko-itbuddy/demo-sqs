package com.demo.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Producer 서비스 메인 애플리케이션 클래스
 * SQS 메시지 발송을 담당하는 Spring Boot 애플리케이션
 */
@SpringBootApplication
@EnableAsync
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}