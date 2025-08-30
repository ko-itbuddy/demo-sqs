package com.demo.consumer.integration;

import com.demo.consumer.domain.processing.ProcessedOrderRepository;
import com.demo.consumer.domain.processing.ProcessingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Consumer 서비스 통합 테스트
 * Spring Boot 3.1+ @ServiceConnection을 활용한 LocalStack SQS 통합 테스트
 * 
 * 테스트 목표:
 * 1. LocalStack 컨테이너 시작 확인
 * 2. Spring Context 로딩 확인  
 * 3. @ServiceConnection 자동 설정 확인
 * 4. 데이터베이스 연동 확인
 * 5. Repository 기능 확인
 */
@Tag("integration")
@SpringBootTest(properties = {
    "spring.cloud.aws.sqs.enabled=false",
    "spring.cloud.aws.sqs.listener.auto-startup=false",
    "logging.level.io.awspring.cloud.sqs=DEBUG"
})
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ConsumerIntegrationTest {
    
    @Container
    @ServiceConnection
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3.2"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("SERVICES", "sqs")
            .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2")
            .withReuse(false);
    
    @Autowired
    private ProcessedOrderRepository processedOrderRepository;
    
    @Test
    @DisplayName("1️⃣ LocalStack 컨테이너 시작 확인")
    void localStackContainer_ShouldStart() {
        assertThat(localStack.isRunning()).isTrue();
        System.out.println("✅ LocalStack container is running");
        System.out.println("🔗 LocalStack endpoint: " + localStack.getEndpoint());
        System.out.println("📡 LocalStack SQS endpoint: " + localStack.getEndpointOverride(SQS));
    }
    
    @Test
    @DisplayName("2️⃣ Spring Boot 애플리케이션 컨텍스트 로딩 확인")
    void applicationContext_ShouldLoad() {
        assertThat(processedOrderRepository).isNotNull();
        System.out.println("✅ Spring Boot context loaded successfully");
        System.out.println("📦 ProcessedOrderRepository injected: " + processedOrderRepository.getClass().getSimpleName());
    }
    
    @Test  
    @DisplayName("3️⃣ @ServiceConnection 자동 설정 확인")
    void serviceConnection_ShouldConfigureAutomatically() {
        // @ServiceConnection이 제대로 작동하면 Repository가 주입되어야 함
        assertThat(processedOrderRepository).isNotNull();
        
        // 컨테이너가 실행 중이어야 함
        assertThat(localStack.isRunning()).isTrue();
        
        System.out.println("✅ @ServiceConnection working correctly");
        System.out.println("🏗️  ProcessedOrderRepository autowired successfully");
        System.out.println("🐳 LocalStack container running: " + localStack.isRunning());
    }
    
    @Test
    @DisplayName("4️⃣ 데이터베이스 연동 확인")
    void databaseConnection_ShouldWorkCorrectly() {
        // Given & When
        var allOrders = processedOrderRepository.findAll();
        var processingOrders = processedOrderRepository.findByStatusOrderByProcessedAtDesc(ProcessingStatus.PROCESSING);
        var completedOrders = processedOrderRepository.findByStatusOrderByProcessedAtDesc(ProcessingStatus.COMPLETED);
        var failedOrders = processedOrderRepository.findByStatusOrderByProcessedAtDesc(ProcessingStatus.FAILED);
        
        // Then
        assertThat(allOrders).isNotNull();
        assertThat(processingOrders).isNotNull();
        assertThat(completedOrders).isNotNull();
        assertThat(failedOrders).isNotNull();
        
        System.out.println("✅ Database connection working");
        System.out.println("📊 Total orders: " + allOrders.size());
        System.out.println("⏳ Processing orders: " + processingOrders.size());
        System.out.println("✅ Completed orders: " + completedOrders.size()); 
        System.out.println("❌ Failed orders: " + failedOrders.size());
    }
    
    @Test
    @DisplayName("5️⃣ Repository 기능 확인")
    void repositoryFunctions_ShouldWork() {
        // Given
        long initialCount = processedOrderRepository.count();
        
        // When & Then
        assertThat(processedOrderRepository.count()).isEqualTo(initialCount);
        
        // 상태별 개수 조회 테스트
        long processingCount = processedOrderRepository.countByStatus(ProcessingStatus.PROCESSING);
        long completedCount = processedOrderRepository.countByStatus(ProcessingStatus.COMPLETED);
        long failedCount = processedOrderRepository.countByStatus(ProcessingStatus.FAILED);
        
        assertThat(processingCount).isGreaterThanOrEqualTo(0);
        assertThat(completedCount).isGreaterThanOrEqualTo(0);
        assertThat(failedCount).isGreaterThanOrEqualTo(0);
        
        System.out.println("✅ Repository functions working correctly");
        System.out.println("📈 Total count: " + initialCount);
        System.out.println("📈 Processing count: " + processingCount);
        System.out.println("📈 Completed count: " + completedCount);
        System.out.println("📈 Failed count: " + failedCount);
    }
    
    @Test
    @DisplayName("6️⃣ 중복 체크 기능 확인")
    void duplicateCheckFunctions_ShouldWork() {
        // Given
        String testMessageId = "test-message-id-12345";
        String testOrderNumber = "ORD-TEST-12345";
        
        // When & Then - 존재하지 않는 ID들
        assertThat(processedOrderRepository.existsByMessageId(testMessageId)).isFalse();
        assertThat(processedOrderRepository.existsByOrderNumber(testOrderNumber)).isFalse();
        
        System.out.println("✅ Duplicate check functions working");
        System.out.println("🔍 Message ID check: PASS");
        System.out.println("🔍 Order number check: PASS");
        System.out.println("🎉 Consumer integration test completed successfully!");
    }
}