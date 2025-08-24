package com.demo.producer.application.order.dto;

import com.demo.producer.domain.order.Order;
import com.demo.producer.domain.order.OrderStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SQS 메시지로 전송될 주문 정보 DTO
 * 메시지 큐를 통한 서비스 간 통신용
 */
@Builder
public record OrderMessage(
        @JsonProperty("orderNumber") String orderNumber,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("productName") String productName,
        @JsonProperty("quantity") Integer quantity,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
    
    @JsonCreator
    public OrderMessage(
            @JsonProperty("orderNumber") String orderNumber,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("status") OrderStatus status,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("messageId") String messageId,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.messageId = messageId;
        this.timestamp = timestamp;
    }
    
    /**
     * 도메인 엔티티로부터 메시지 DTO 생성
     */
    public static OrderMessage from(Order order, String messageId) {
        return OrderMessage.builder()
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .messageId(messageId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}