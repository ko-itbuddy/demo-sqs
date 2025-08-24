package com.demo.consumer.infrastructure.messaging;

import com.demo.consumer.application.messaging.dto.OrderMessage;
import com.demo.consumer.application.processing.OrderProcessingService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * SQS 주문 메시지 리스너
 * @SqsListener를 사용한 메시지 수신 및 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {
    
    private final OrderProcessingService orderProcessingService;
    
    /**
     * 메인 큐에서 주문 메시지 수신 및 처리
     * DLQ 정책에 의해 3회 재시도 후 DLQ로 이동
     */
    @SqsListener("${app.sqs.queue-name}")
    public void handleOrderMessage(OrderMessage orderMessage, Message<?> sqsMessage) {
        String messageId = sqsMessage.getHeaders().get("MessageId") != null ? 
                          sqsMessage.getHeaders().get("MessageId").toString() : "unknown";
        String receiptHandle = sqsMessage.getHeaders().get("ReceiptHandle") != null ?
                              sqsMessage.getHeaders().get("ReceiptHandle").toString() : "unknown";
        
        log.debug("[DEBUG-LOG] SQS 메시지 수신 시작 - orderNumber: {}, messageId: {}, receiptHandle: {}", 
                orderMessage.orderNumber(), messageId, receiptHandle);
        
        try {
            log.info("주문 메시지 수신: orderNumber={}, messageId={}", 
                    orderMessage.orderNumber(), messageId);
            log.debug("[DEBUG-LOG] 메시지 헤더 정보: {}", sqsMessage.getHeaders());
            
            // 메시지 유효성 검증
            orderMessage.validate();
            
            // 주문 처리
            log.debug("[DEBUG-LOG] 주문 처리 시작 - orderNumber: {}", orderMessage.orderNumber());
            orderProcessingService.processOrder(orderMessage, messageId);
            log.debug("[DEBUG-LOG] 주문 처리 성공 - orderNumber: {}", orderMessage.orderNumber());
            
            log.info("주문 메시지 처리 완료: orderNumber={}", orderMessage.orderNumber());
            
        } catch (Exception e) {
            log.error("주문 메시지 처리 실패: orderNumber={}, error={}", 
                    orderMessage.orderNumber(), e.getMessage(), e);
            log.debug("[DEBUG-LOG] 처리 실패 - DLQ 재시도 진행 예상: orderNumber={}, messageId={}", 
                    orderMessage.orderNumber(), messageId);
            
            // 예외를 재발생시켜 SQS 재시도 메커니즘 동작
            throw new OrderMessageProcessingException(
                    "주문 메시지 처리에 실패했습니다: " + orderMessage.orderNumber(), e);
        }
    }
    
    /**
     * DLQ에서 실패한 메시지 처리
     * 수동으로 처리하거나 알림을 위한 별도 처리
     */
    @SqsListener("${app.sqs.dlq-name}")
    public void handleDeadLetterMessage(OrderMessage orderMessage, Message<?> sqsMessage) {
        String dlqMessageId = sqsMessage.getHeaders().get("MessageId") != null ? 
                             sqsMessage.getHeaders().get("MessageId").toString() : "unknown";
        
        log.debug("[DEBUG-LOG] DLQ 메시지 수신 시작 - orderNumber: {}, messageId: {}", 
                orderMessage.orderNumber(), dlqMessageId);
                
        try {
            log.warn("DLQ 메시지 수신: orderNumber={}, messageId={}", 
                    orderMessage.orderNumber(), dlqMessageId);
            log.debug("[DEBUG-LOG] DLQ 메시지 헤더 정보: {}", sqsMessage.getHeaders());
            
            // DLQ 메시지 처리 (알림, 수동 처리 등)
            log.debug("[DEBUG-LOG] DLQ 메시지 처리 시작 - orderNumber: {}", orderMessage.orderNumber());
            orderProcessingService.handleDeadLetterMessage(orderMessage, dlqMessageId);
            log.debug("[DEBUG-LOG] DLQ 메시지 처리 성공 - orderNumber: {}", orderMessage.orderNumber());
            
            log.info("DLQ 메시지 처리 완료: orderNumber={}", orderMessage.orderNumber());
            
        } catch (Exception e) {
            log.error("DLQ 메시지 처리 실패: orderNumber={}, error={}", 
                    orderMessage.orderNumber(), e.getMessage(), e);
            
            // DLQ 처리 실패는 별도 알림 시스템으로 처리
            // 여기서는 로깅만 하고 메시지는 acknowledge
        }
    }
    
    /**
     * 주문 메시지 처리 예외
     */
    public static class OrderMessageProcessingException extends RuntimeException {
        public OrderMessageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}