package com.demo.producer.integration;

import com.demo.producer.application.order.dto.OrderMessage;
import com.demo.producer.domain.order.OrderStatus;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

/**
 * Producer 서비스 통합 테스트
 * Testcontainers를 활용한 실제 LocalStack SQS 환경에서의 메시지 발송 테스트
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ProducerIntegrationTest {
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3.2"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("SERVICES", "sqs")
            .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2")
            .withReuse(false);
    
    @Autowired
    private SqsMessagePublisher messagePublisher;
    
    @BeforeAll
    static void createQueues() {
        try {
            // LocalStack SQS 클라이언트로 큐 미리 생성
            SqsClient sqsClient = SqsClient.builder()
                    .endpointOverride(localStack.getEndpointOverride(SQS))
                    .credentialsProvider(() -> AwsBasicCredentials.create(
                            localStack.getAccessKey(), localStack.getSecretKey()))
                    .region(Region.of(localStack.getRegion()))
                    .build();
            
            // 큐 생성
            sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName("order-processing-queue")
                    .build());
            
            System.out.println("Queue created successfully in LocalStack");
            sqsClient.close();
        } catch (Exception e) {
            System.err.println("Failed to create queue: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        
        // 큐 자동 생성 활성화
        registry.add("spring.cloud.aws.sqs.fail-on-missing-queue", () -> "false");
    }
    
    @Test
    @DisplayName("애플리케이션 컨텍스트 로딩 테스트")
    void contextLoads() {
        assertThat(messagePublisher).isNotNull();
        assertThat(localStack.isRunning()).isTrue();
    }
    
    @Test
    @DisplayName("SQS 메시지 발송 테스트")
    void publishOrderMessage_ShouldSendMessageSuccessfully() {
        // Given
        OrderMessage orderMessage = OrderMessage.builder()
                .orderNumber("ORD-TEST-12345")
                .customerName("테스트 고객")
                .productName("테스트 상품")
                .quantity(2)
                .price(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("100000"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
        
        // When & Then - 메시지 발송이 예외 없이 완료되어야 함
        assertDoesNotThrow(() -> {
            String messageId = messagePublisher.publishOrderMessage(orderMessage);
            assertThat(messageId).isNotNull();
            System.out.println("Message published successfully with ID: " + messageId);
        });
    }
    
    @Test
    @DisplayName("LocalStack 컨테이너 상태 확인")
    void localStackContainer_ShouldBeRunning() {
        assertThat(localStack.isRunning()).isTrue();
        assertThat(localStack.getEndpointOverride(SQS)).isNotNull();
        System.out.println("LocalStack endpoint: " + localStack.getEndpointOverride(SQS));
    }
    
    @Test
    @DisplayName("메시지 발송 컴포넌트 주입 확인")
    void messagePublisher_ShouldBeInjected() {
        assertThat(messagePublisher).isNotNull();
        System.out.println("SqsMessagePublisher successfully injected");
    }
}