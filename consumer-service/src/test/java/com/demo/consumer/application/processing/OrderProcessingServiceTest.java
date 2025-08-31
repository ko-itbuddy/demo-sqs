package com.demo.consumer.application.processing;

import com.demo.consumer.application.messaging.dto.OrderMessage;
import com.demo.consumer.domain.processing.ProcessedOrder;
import com.demo.consumer.domain.processing.ProcessedOrderRepository;
import com.demo.consumer.domain.processing.ProcessingStatus;
import com.demo.consumer.infrastructure.messaging.SyncEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

import java.util.Random;

/**
 * OrderProcessingService 단위 테스트
 * Consumer 애플리케이션 서비스 계층 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {
    
    @Mock
    private ProcessedOrderRepository processedOrderRepository;
    
    @Mock
    private SyncEventPublisher syncEventPublisher;
    
    @InjectMocks
    private OrderProcessingService orderProcessingService;
    
    private OrderMessage validOrderMessage;
    private ProcessedOrder savedProcessedOrder;
    private String messageId;
    
    @BeforeEach
    void setUp() {
        // maxRetryAttempts 설정
        ReflectionTestUtils.setField(orderProcessingService, "maxRetryAttempts", 3);
        
        messageId = "sqs-message-12345";
        
        validOrderMessage = OrderMessage.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .messageId("original-msg-12345")
                .timestamp(LocalDateTime.now())
                .build();
                
        savedProcessedOrder = ProcessedOrder.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId(messageId)
                .originalCreatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("정상적인 주문 메시지 처리")
    void processOrder_WithValidMessage_ShouldProcessSuccessfully() {
        // Given
        given(processedOrderRepository.existsByMessageId(messageId)).willReturn(false);
        given(processedOrderRepository.save(any(ProcessedOrder.class))).willReturn(savedProcessedOrder);
        willDoNothing().given(syncEventPublisher).publishSyncEvent(any());
        
        // Random을 Mock하여 항상 성공하도록 설정 (50은 30보다 크므로 성공)
        Random mockRandom = mock(Random.class);
        given(mockRandom.nextInt(100)).willReturn(50);
        ReflectionTestUtils.setField(orderProcessingService, "random", mockRandom);
        
        // When
        assertThatNoException().isThrownBy(() -> 
            orderProcessingService.processOrder(validOrderMessage, messageId));
        
        // Then
        ArgumentCaptor<ProcessedOrder> orderCaptor = ArgumentCaptor.forClass(ProcessedOrder.class);
        verify(processedOrderRepository, times(2)).save(orderCaptor.capture());
        
        ProcessedOrder capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getOrderNumber()).isEqualTo("ORD-20231201-123456-ABC12345");
    }
    
    @Test
    @DisplayName("중복 메시지 처리 방지")
    void processOrder_WithDuplicateMessage_ShouldSkipProcessing() {
        // Given
        given(processedOrderRepository.existsByMessageId(messageId)).willReturn(true);
        
        // When
        orderProcessingService.processOrder(validOrderMessage, messageId);
        
        // Then
        verify(processedOrderRepository).existsByMessageId(messageId);
        verify(processedOrderRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("DLQ 메시지 처리 - 기존 주문이 존재하는 경우")
    void handleDeadLetterMessage_WithExistingOrder_ShouldMarkAsFailed() {
        // Given
        given(processedOrderRepository.findByOrderNumber(validOrderMessage.orderNumber()))
                .willReturn(Optional.of(savedProcessedOrder));
        
        // When
        orderProcessingService.handleDeadLetterMessage(validOrderMessage, messageId);
        
        // Then
        verify(processedOrderRepository).findByOrderNumber(validOrderMessage.orderNumber());
        verify(processedOrderRepository).save(any(ProcessedOrder.class));
    }
    
    @Test
    @DisplayName("DLQ 메시지 처리 - 새로운 주문인 경우")
    void handleDeadLetterMessage_WithNewOrder_ShouldCreateFailedOrder() {
        // Given
        given(processedOrderRepository.findByOrderNumber(validOrderMessage.orderNumber()))
                .willReturn(Optional.empty());
        
        // When
        orderProcessingService.handleDeadLetterMessage(validOrderMessage, messageId);
        
        // Then
        ArgumentCaptor<ProcessedOrder> orderCaptor = ArgumentCaptor.forClass(ProcessedOrder.class);
        verify(processedOrderRepository).save(orderCaptor.capture());
        
        ProcessedOrder capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.isFailed()).isTrue();
        assertThat(capturedOrder.getErrorMessage()).contains("DLQ로 직접 이동된 메시지");
    }
    
    @Test
    @DisplayName("재시도 가능한 주문 처리 실패")
    void processOrder_WithRetryableFailure_ShouldIncrementRetryCount() {
        // Given
        given(processedOrderRepository.existsByMessageId(messageId)).willReturn(false);
        
        ProcessedOrder processingOrder = ProcessedOrder.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId(messageId)
                .originalCreatedAt(LocalDateTime.now())
                .build();
                
        given(processedOrderRepository.save(any(ProcessedOrder.class)))
                .willReturn(processingOrder);
        
        // Random을 Mock하여 항상 실패하도록 설정 (29는 30보다 작으므로 실패)
        Random mockRandom = mock(Random.class);
        given(mockRandom.nextInt(100)).willReturn(29);
        ReflectionTestUtils.setField(orderProcessingService, "random", mockRandom);
        
        // When & Then
        assertThatThrownBy(() -> orderProcessingService.processOrder(validOrderMessage, messageId))
                .isInstanceOf(OrderProcessingService.OrderProcessingException.class)
                .hasMessageContaining("재시도 예정");
        
        verify(processedOrderRepository, atLeast(1)).save(any(ProcessedOrder.class));
    }
}