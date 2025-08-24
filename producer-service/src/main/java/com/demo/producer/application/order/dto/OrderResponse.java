package com.demo.producer.application.order.dto;

import com.demo.producer.domain.order.Order;
import com.demo.producer.domain.order.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 응답 DTO
 * 도메인 엔티티를 API 응답 형태로 변환
 */
@Builder
public record OrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        OrderStatus status,
        String statusDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    
    /**
     * 도메인 엔티티로부터 DTO 생성
     */
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDescription(order.getStatus().getDescription())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}