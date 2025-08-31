package com.demo.producer.application.order.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 동기화 이벤트 DTO
 * 서비스간 동기화를 위한 키 기반 이벤트 메시지
 */
@Builder
public record SyncEvent(
        String eventType,        // 이벤트 타입 (ORDER_UPDATED, PROCESSING_COMPLETED 등)
        String sourceService,    // 이벤트 발생 서비스 (producer/consumer)
        String targetService,    // 이벤트 대상 서비스 (consumer/producer) 
        String entityKey,        // 엔티티 키 (주문번호 등)
        String entityType,       // 엔티티 타입 (ORDER, PROCESSED_ORDER)
        LocalDateTime timestamp, // 이벤트 발생 시간
        String messageId         // 메시지 고유 ID
) {
    
    public static SyncEvent createOrderSync(String orderNumber, String messageId) {
        return SyncEvent.builder()
                .eventType("ORDER_UPDATED")
                .sourceService("producer")
                .targetService("consumer")
                .entityKey(orderNumber)
                .entityType("ORDER")
                .timestamp(LocalDateTime.now())
                .messageId(messageId)
                .build();
    }
    
    public static SyncEvent createProcessingSync(String orderNumber, String messageId) {
        return SyncEvent.builder()
                .eventType("PROCESSING_COMPLETED")
                .sourceService("consumer")
                .targetService("producer")
                .entityKey(orderNumber)
                .entityType("PROCESSED_ORDER")
                .timestamp(LocalDateTime.now())
                .messageId(messageId)
                .build();
    }
}