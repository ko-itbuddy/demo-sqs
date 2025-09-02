package com.demo.consumer.application.messaging.dto;

import com.demo.consumer.domain.order.OrderStatus;
import com.demo.consumer.domain.processing.ProcessedOrder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SQS 메시지로 수신된 주문 정보 DTO
 * Producer에서 전송된 메시지를 수신하기 위한 DTO
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
     * 메시지 DTO로부터 도메인 엔티티 생성
     */
    public ProcessedOrder toProcessedOrder(String sqsMessageId) {
        return ProcessedOrder.builder()
                .orderNumber(orderNumber)
                .customerName(customerName)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .messageId(sqsMessageId)
                .originalCreatedAt(createdAt)
                .build();
    }
    
    /**
     * 필수 필드 검증
     */
    public void validate() {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("주문번호는 필수입니다");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("고객명은 필수입니다");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("총금액은 0보다 커야 합니다");
        }
    }
}