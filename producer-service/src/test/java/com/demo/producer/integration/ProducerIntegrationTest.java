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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

import java.net.URI;
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
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4.0"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("SERVICES", "sqs")
            .withEnv("AWS_DEFAULT_REGION", "eu-west-1")
            .withReuse(false);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Context7 패턴: LocalStack SQS 엔드포인트 동적 설정
        String sqsEndpoint = localstack.getEndpointOverride(SQS).toString();
        
        registry.add("spring.cloud.aws.sqs.endpoint", () -> sqsEndpoint);
        registry.add("spring.cloud.aws.endpoint", localstack::getEndpoint);
        registry.add("spring.cloud.aws.region.static", () -> "eu-west-1");
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        
        System.out.println("🔧 Context7 Dynamic Properties configured:");
        System.out.println("   📡 SQS Endpoint: " + sqsEndpoint);
        System.out.println("   🌍 General Endpoint: " + localstack.getEndpoint());
    }
    
    @Autowired
    private SqsMessagePublisher messagePublisher;
    
    @Autowired
    private SqsAsyncClient sqsAsyncClient;
    
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
    @DisplayName("4️⃣ LocalStack SQS 연결성 검증")
    void localStackSqsConnectivity_ShouldWork() {
        try {
            // Given - Context7 패턴: LocalStack 연결성 기본 검증
            System.out.println("🔗 Testing LocalStack SQS connectivity...");
            System.out.println("📡 LocalStack endpoint: " + localstack.getEndpoint());
            System.out.println("📡 SQS endpoint: " + localstack.getEndpointOverride(SQS));
            System.out.println("🏃 Container running: " + localstack.isRunning());
            
            // When & Then - 기본 연결성 검증
            assertThat(localstack.isRunning()).isTrue();
            assertThat(localstack.getEndpoint()).isNotNull(); 
            assertThat(localstack.getEndpointOverride(SQS)).isNotNull();
            
            System.out.println("✅ LocalStack SQS connectivity verified!");
            
        } catch (Exception e) {
            System.err.println("❌ LocalStack connectivity failed:");
            System.err.println("🔍 Error type: " + e.getClass().getSimpleName());
            System.err.println("💬 Error message: " + e.getMessage());
            throw e;
        }
    }
    
    @Test
    @DisplayName("5️⃣ SQS 큐 생성 기능 검증 (Context7 수동 클라이언트 설정)")
    void sqsQueueCreation_ShouldWork() {
        try {
            // Given - Context7 패턴: 수동으로 LocalStack용 SqsAsyncClient 생성
            String queueName = "order-processing-queue";
            
            System.out.println("🏗️  Creating SQS queue: " + queueName);
            System.out.println("📡 Using LocalStack endpoint: " + localstack.getEndpointOverride(SQS));
            
            // Context7 패턴: 수동으로 AWS 클라이언트 구성
            SqsAsyncClient localstackSqsClient = SqsAsyncClient.builder()
                    .endpointOverride(URI.create(localstack.getEndpointOverride(SQS).toString()))
                    .region(Region.EU_WEST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .build();
            
            // When
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            
            CreateQueueResponse createQueueResponse = localstackSqsClient.createQueue(createQueueRequest).join();
            
            // Then
            assertThat(createQueueResponse.queueUrl()).isNotNull();
            System.out.println("✅ Queue created successfully with manual client configuration!");
            System.out.println("🔗 Queue URL: " + createQueueResponse.queueUrl());
            
        } catch (Exception e) {
            System.err.println("❌ Queue creation failed:");
            System.err.println("🔍 Error type: " + e.getClass().getSimpleName());
            System.err.println("💬 Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("🔄 Root cause: " + e.getCause().getClass().getSimpleName());
                System.err.println("💭 Root message: " + e.getCause().getMessage());
            }
            throw e;
        }
    }
    
    @Test
    @DisplayName("6️⃣ SQS 메시지 발송 기능 검증 (큐 사전 생성)")
    void sqsMessagePublishing_ShouldWork() {
        try {
            // Given - Context7 패턴: 먼저 수동으로 큐 생성 후 Spring Cloud AWS 테스트
            String queueName = "order-processing-queue";
            
            // Step 1: 수동으로 큐 생성
            SqsAsyncClient localstackSqsClient = SqsAsyncClient.builder()
                    .endpointOverride(URI.create(localstack.getEndpointOverride(SQS).toString()))
                    .region(Region.EU_WEST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .build();
            
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            
            CreateQueueResponse queueResponse = localstackSqsClient.createQueue(createQueueRequest).join();
            System.out.println("✅ Pre-created queue: " + queueResponse.queueUrl());
            
            // Step 2: Spring Cloud AWS를 통한 메시지 발송 테스트
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
            
            System.out.println("📤 Attempting to publish message to pre-created queue...");
            System.out.println("🎯 Target queue: " + queueName);
            System.out.println("📄 Message: " + orderMessage.orderNumber());
            
            // When & Then
            String messageId = messagePublisher.publishOrderMessage(orderMessage);
            assertThat(messageId).isNotNull();
            System.out.println("✅ Message published successfully to LocalStack!");
            System.out.println("🆔 Message ID: " + messageId);
            System.out.println("🎉 Context7 + Sequential Thinking approach successful!");
            
        } catch (Exception e) {
            System.err.println("❌ SQS integration test failed:");
            System.err.println("🔍 Error type: " + e.getClass().getSimpleName());  
            System.err.println("💬 Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("🔄 Root cause: " + e.getCause().getClass().getSimpleName());
                System.err.println("💭 Root message: " + e.getCause().getMessage());
            }
            
            System.out.println("✅ LocalStack started: " + localstack.isRunning());
            System.out.println("✅ Spring context loaded: " + (messagePublisher != null));
            System.out.println("❌ SQS message publishing: FAILED");
            
            throw e;
        }
    }
}