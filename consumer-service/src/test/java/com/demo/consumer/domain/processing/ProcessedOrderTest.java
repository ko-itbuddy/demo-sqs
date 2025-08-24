package com.demo.consumer.domain.processing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProcessedOrder 도메인 엔티티 단위 테스트
 * Consumer 도메인 로직 검증
 */
class ProcessedOrderTest {
    
    @Test
    @DisplayName("정상적인 처리된 주문 생성")
    void createProcessedOrder_WithValidData_ShouldCreateSuccessfully() {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        String customerName = "홍길동";
        String productName = "테스트 상품";
        Integer quantity = 5;
        BigDecimal price = new BigDecimal("10000.00");
        BigDecimal totalAmount = new BigDecimal("50000.00");
        String messageId = "msg-12345";
        LocalDateTime originalCreatedAt = LocalDateTime.now();
        
        // When
        ProcessedOrder processedOrder = ProcessedOrder.builder()
                .orderNumber(orderNumber)
                .customerName(customerName)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .messageId(messageId)
                .originalCreatedAt(originalCreatedAt)
                .build();
        
        // Then
        assertThat(processedOrder.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(processedOrder.getCustomerName()).isEqualTo(customerName);
        assertThat(processedOrder.getProductName()).isEqualTo(productName);
        assertThat(processedOrder.getQuantity()).isEqualTo(quantity);
        assertThat(processedOrder.getPrice()).isEqualTo(price);
        assertThat(processedOrder.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(processedOrder.getMessageId()).isEqualTo(messageId);
        assertThat(processedOrder.getOriginalCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(processedOrder.getStatus()).isEqualTo(ProcessingStatus.PROCESSING);
        assertThat(processedOrder.getProcessedAt()).isNotNull();
        assertThat(processedOrder.getRetryCount()).isZero();
    }
    
    @Test
    @DisplayName("주문번호가 null인 경우 예외 발생")
    void createProcessedOrder_WithNullOrderNumber_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> ProcessedOrder.builder()
                .orderNumber(null)
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId("msg-12345")
                .originalCreatedAt(LocalDateTime.now())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("주문번호는 필수입니다");
    }
    
    @Test
    @DisplayName("수량이 0 이하인 경우 예외 발생")
    void createProcessedOrder_WithInvalidQuantity_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> ProcessedOrder.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(0)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId("msg-12345")
                .originalCreatedAt(LocalDateTime.now())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("처리 완료 상태 변경")
    void markAsCompleted_ShouldUpdateStatusAndTimestamp() {
        // Given
        ProcessedOrder processedOrder = createValidProcessedOrder();
        
        // When
        processedOrder.markAsCompleted();
        
        // Then
        assertThat(processedOrder.getStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(processedOrder.getUpdatedAt()).isNotNull();
        assertTrue(processedOrder.isCompleted());
        assertFalse(processedOrder.isProcessing());
        assertFalse(processedOrder.isFailed());
    }
    
    @Test
    @DisplayName("처리 실패 상태 변경")
    void markAsFailed_ShouldUpdateStatusAndErrorMessage() {
        // Given
        ProcessedOrder processedOrder = createValidProcessedOrder();
        String errorMessage = "처리 중 오류 발생";
        
        // When
        processedOrder.markAsFailed(errorMessage);
        
        // Then
        assertThat(processedOrder.getStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(processedOrder.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(processedOrder.getUpdatedAt()).isNotNull();
        assertTrue(processedOrder.isFailed());
        assertFalse(processedOrder.isProcessing());
        assertFalse(processedOrder.isCompleted());
    }
    
    @Test
    @DisplayName("재시도 횟수 증가")
    void incrementRetryCount_ShouldIncreaseRetryCount() {
        // Given
        ProcessedOrder processedOrder = createValidProcessedOrder();
        int initialRetryCount = processedOrder.getRetryCount();
        
        // When
        processedOrder.incrementRetryCount();
        
        // Then
        assertThat(processedOrder.getRetryCount()).isEqualTo(initialRetryCount + 1);
        assertThat(processedOrder.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("재시도 가능 여부 확인")
    void canRetry_ShouldReturnCorrectResult() {
        // Given
        ProcessedOrder processedOrder = createValidProcessedOrder();
        int maxRetryAttempts = 3;
        
        // When & Then - 처리 중이고 재시도 횟수가 최대 미만
        assertTrue(processedOrder.canRetry(maxRetryAttempts));
        
        // When - 재시도 횟수 증가
        processedOrder.incrementRetryCount();
        processedOrder.incrementRetryCount();
        processedOrder.incrementRetryCount();
        
        // Then - 최대 재시도 횟수 도달
        assertFalse(processedOrder.canRetry(maxRetryAttempts));
        
        // When - 완료 상태로 변경
        ProcessedOrder completedOrder = createValidProcessedOrder();
        completedOrder.markAsCompleted();
        
        // Then - 완료된 주문은 재시도 불가
        assertFalse(completedOrder.canRetry(maxRetryAttempts));
    }
    
    @Test
    @DisplayName("완료된 주문에서 실패 상태로 변경 시도시 예외 발생")
    void markAsFailed_FromCompletedStatus_ShouldThrowException() {
        // Given
        ProcessedOrder processedOrder = createValidProcessedOrder();
        processedOrder.markAsCompleted();
        
        // When & Then
        assertThatThrownBy(() -> processedOrder.markAsFailed("오류 메시지"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 상태에서 실패 상태로 변경할 수 없습니다");
    }
    
    @Test
    @DisplayName("동등성 비교 - 주문번호 기준")
    void equals_BasedOnOrderNumber_ShouldWorkCorrectly() {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        ProcessedOrder order1 = createProcessedOrderWithNumber(orderNumber);
        ProcessedOrder order2 = createProcessedOrderWithNumber(orderNumber);
        ProcessedOrder order3 = createProcessedOrderWithNumber("ORD-20231201-123456-DEF67890");
        
        // When & Then
        assertThat(order1).isEqualTo(order2);
        assertThat(order1).isNotEqualTo(order3);
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }
    
    private ProcessedOrder createValidProcessedOrder() {
        return ProcessedOrder.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId("msg-12345")
                .originalCreatedAt(LocalDateTime.now())
                .build();
    }
    
    private ProcessedOrder createProcessedOrderWithNumber(String orderNumber) {
        return ProcessedOrder.builder()
                .orderNumber(orderNumber)
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .messageId("msg-12345")
                .originalCreatedAt(LocalDateTime.now())
                .build();
    }
}