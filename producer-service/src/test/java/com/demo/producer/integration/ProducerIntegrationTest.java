package com.demo.producer.integration;

import com.demo.producer.application.order.dto.OrderMessage;
import com.demo.producer.domain.order.OrderStatus;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Producer 서비스 통합 테스트
 * Spring Boot 3.1+ @ServiceConnection을 활용한 LocalStack SQS 통합 테스트
 * 
 * 테스트 목표:
 * 1. LocalStack 컨테이너 시작 확인
 * 2. Spring Context 로딩 확인  
 * 3. @ServiceConnection 자동 설정 확인
 * 4. SQS 메시지 발송 기능 검증
 */
@Tag("integration")
@SpringBootTest(properties = {
    "spring.cloud.aws.sqs.queue-not-found-strategy=CREATE",
    "logging.level.io.awspring.cloud.sqs=DEBUG",
    "logging.level.software.amazon.awssdk=DEBUG"
})
@Testcontainers
@ActiveProfiles("test")
class ProducerIntegrationTest {
    
    @Container
    @ServiceConnection
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3.2"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("SERVICES", "sqs")
            .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2")
            .withReuse(false);
    
    @Autowired
    private SqsMessagePublisher messagePublisher;
    
    @Test
    @DisplayName("1️⃣ LocalStack 컨테이너 시작 확인")
    void localStackContainer_ShouldStart() {
        assertThat(localstack.isRunning()).isTrue();
        System.out.println("✅ LocalStack container is running");
        System.out.println("🔗 LocalStack endpoint: " + localstack.getEndpoint());
        System.out.println("📡 LocalStack SQS endpoint: " + localstack.getEndpointOverride(SQS));
    }
    
    @Test
    @DisplayName("2️⃣ Spring Boot 애플리케이션 컨텍스트 로딩 확인")
    void applicationContext_ShouldLoad() {
        assertThat(messagePublisher).isNotNull();
        System.out.println("✅ Spring Boot context loaded successfully");
        System.out.println("📦 SqsMessagePublisher bean injected: " + messagePublisher.getClass().getSimpleName());
    }
    
    @Test  
    @DisplayName("3️⃣ @ServiceConnection 자동 설정 확인")
    void serviceConnection_ShouldConfigureAutomatically() {
        // @ServiceConnection이 제대로 작동하면 SqsMessagePublisher가 주입되어야 함
        assertThat(messagePublisher).isNotNull();
        
        // 컨테이너가 실행 중이어야 함
        assertThat(localstack.isRunning()).isTrue();
        
        System.out.println("✅ @ServiceConnection working correctly");
        System.out.println("🏗️  SqsMessagePublisher autowired successfully");
        System.out.println("🐳 LocalStack container running: " + localstack.isRunning());
    }
    
    @Test
    @DisplayName("4️⃣ SQS 메시지 발송 기능 검증")
    void sqsMessagePublishing_ShouldWork() {
        // Given
        OrderMessage orderMessage = OrderMessage.builder()
                .orderNumber("ORD-SERVICE-CONNECTION-TEST")
                .customerName("@ServiceConnection 테스트 고객")
                .productName("Context7 + Sequential Thinking 테스트 상품")
                .quantity(1)
                .price(new BigDecimal("100000"))
                .totalAmount(new BigDecimal("100000"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
        
        System.out.println("📤 Attempting to publish message...");
        System.out.println("🎯 Target queue: order-processing-queue");
        System.out.println("📄 Message: " + orderMessage.orderNumber());
        
        // When & Then - @ServiceConnection + Spring Cloud AWS가 제대로 작동하면 메시지 발송이 성공해야 함
        try {
            String messageId = messagePublisher.publishOrderMessage(orderMessage);
            assertThat(messageId).isNotNull();
            System.out.println("✅ Message published successfully!");
            System.out.println("🆔 Message ID: " + messageId);
            System.out.println("🎉 @ServiceConnection + Context7 patterns working perfectly!");
        } catch (Exception e) {
            System.err.println("❌ Message publishing failed:");
            System.err.println("🔍 Error type: " + e.getClass().getSimpleName());  
            System.err.println("💬 Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("🔄 Root cause: " + e.getCause().getClass().getSimpleName());
                System.err.println("💭 Root message: " + e.getCause().getMessage());
            }
            
            // 여전히 실패하더라도 테스트가 어느 정도까지는 작동했음을 보여줌
            System.out.println("✅ LocalStack started: " + localstack.isRunning());
            System.out.println("✅ Spring context loaded: " + (messagePublisher != null));
            System.out.println("❌ SQS message publishing: FAILED");
            
            throw e;
        }
    }
}