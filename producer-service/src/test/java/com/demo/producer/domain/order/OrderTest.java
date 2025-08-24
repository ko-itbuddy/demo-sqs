package com.demo.producer.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Order 도메인 엔티티 단위 테스트
 * DDD 원칙에 따른 도메인 로직 검증
 */
class OrderTest {
    
    @Test
    @DisplayName("정상적인 주문 생성")
    void createOrder_WithValidData_ShouldCreateSuccessfully() {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        String customerName = "홍길동";
        String productName = "테스트 상품";
        Integer quantity = 5;
        BigDecimal price = new BigDecimal("10000.00");
        
        // When
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerName(customerName)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
        
        // Then
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getCustomerName()).isEqualTo(customerName);
        assertThat(order.getProductName()).isEqualTo(productName);
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("50000.00"));
    }
    
    @Test
    @DisplayName("주문번호가 null인 경우 예외 발생")
    void createOrder_WithNullOrderNumber_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> Order.builder()
                .orderNumber(null)
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("주문번호는 필수입니다");
    }
    
    @Test
    @DisplayName("고객명이 null인 경우 예외 발생")
    void createOrder_WithNullCustomerName_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName(null)
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("고객명은 필수입니다");
    }
    
    @Test
    @DisplayName("수량이 0 이하인 경우 예외 발생")
    void createOrder_WithInvalidQuantity_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(0)
                .price(new BigDecimal("10000.00"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("가격이 0 이하인 경우 예외 발생")
    void createOrder_WithInvalidPrice_ShouldThrowException() {
        // Given & When & Then
        assertThatThrownBy(() -> Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(BigDecimal.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("가격은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("주문 상태 업데이트")
    void updateStatus_WithValidStatus_ShouldUpdateSuccessfully() {
        // Given
        Order order = createValidOrder();
        
        // When
        order.updateStatus(OrderStatus.PROCESSING);
        
        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("주문 상태가 null인 경우 예외 발생")
    void updateStatus_WithNullStatus_ShouldThrowException() {
        // Given
        Order order = createValidOrder();
        
        // When & Then
        assertThatThrownBy(() -> order.updateStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("주문 상태는 필수입니다");
    }
    
    @Test
    @DisplayName("총 금액 계산")
    void getTotalAmount_ShouldCalculateCorrectly() {
        // Given
        Order order = Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(3)
                .price(new BigDecimal("15000.50"))
                .build();
        
        // When
        BigDecimal totalAmount = order.getTotalAmount();
        
        // Then
        assertThat(totalAmount).isEqualTo(new BigDecimal("45001.50"));
    }
    
    @Test
    @DisplayName("주문 상태 확인 메서드들")
    void statusCheckMethods_ShouldWorkCorrectly() {
        // Given
        Order order = createValidOrder();
        
        // When & Then - PENDING 상태
        assertTrue(order.isPending());
        assertFalse(order.isProcessing());
        assertFalse(order.isCompleted());
        assertFalse(order.isFailed());
        
        // When - PROCESSING 상태로 변경
        order.updateStatus(OrderStatus.PROCESSING);
        
        // Then
        assertFalse(order.isPending());
        assertTrue(order.isProcessing());
        assertFalse(order.isCompleted());
        assertFalse(order.isFailed());
    }
    
    @Test
    @DisplayName("동등성 비교 - 주문번호 기준")
    void equals_BasedOnOrderNumber_ShouldWorkCorrectly() {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        Order order1 = createOrderWithNumber(orderNumber);
        Order order2 = createOrderWithNumber(orderNumber);
        Order order3 = createOrderWithNumber("ORD-20231201-123456-DEF67890");
        
        // When & Then
        assertThat(order1).isEqualTo(order2);
        assertThat(order1).isNotEqualTo(order3);
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }
    
    private Order createValidOrder() {
        return Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build();
    }
    
    private Order createOrderWithNumber(String orderNumber) {
        return Order.builder()
                .orderNumber(orderNumber)
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build();
    }
}