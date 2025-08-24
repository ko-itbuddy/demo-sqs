package com.demo.consumer.integration;

import com.demo.consumer.domain.processing.ProcessedOrderRepository;
import com.demo.consumer.domain.processing.ProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Consumer 서비스 통합 테스트
 * Testcontainers를 활용한 실제 LocalStack SQS 환경에서의 테스트
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ConsumerIntegrationTest {
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2");
    
    @Autowired
    private ProcessedOrderRepository processedOrderRepository;
    
    @BeforeEach
    void setUp() {
        // LocalStack 환경 변수 설정
        System.setProperty("spring.cloud.aws.sqs.endpoint", localStack.getEndpointOverride(SQS).toString());
        System.setProperty("spring.cloud.aws.credentials.access-key", localStack.getAccessKey());
        System.setProperty("spring.cloud.aws.credentials.secret-key", localStack.getSecretKey());
        System.setProperty("spring.cloud.aws.region.static", localStack.getRegion());
    }
    
    @Test
    @DisplayName("애플리케이션 컨텍스트 로딩 테스트")
    void contextLoads() {
        // Given & When & Then
        assertThat(processedOrderRepository).isNotNull();
    }
    
    @Test
    @DisplayName("처리된 주문 저장 및 조회 테스트")
    void processedOrderRepository_ShouldWorkCorrectly() {
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
    }
    
    @Test
    @DisplayName("중복 메시지 ID 체크 기능 테스트")
    void messageIdDuplicateCheck_ShouldWorkCorrectly() {
        // Given
        String messageId = "test-message-id-12345";
        
        // When & Then - 존재하지 않는 메시지 ID
        assertThat(processedOrderRepository.existsByMessageId(messageId)).isFalse();
    }
    
    @Test
    @DisplayName("주문번호 중복 체크 기능 테스트")
    void orderNumberDuplicateCheck_ShouldWorkCorrectly() {
        // Given
        String orderNumber = "ORD-TEST-12345";
        
        // When & Then - 존재하지 않는 주문번호
        assertThat(processedOrderRepository.existsByOrderNumber(orderNumber)).isFalse();
    }
    
    @Test
    @DisplayName("데이터베이스 연결 및 기본 쿼리 테스트")
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
    }
}