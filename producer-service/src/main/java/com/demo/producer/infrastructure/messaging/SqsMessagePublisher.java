package com.demo.producer.infrastructure.messaging;

import com.demo.producer.application.order.dto.OrderMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SQS 메시지 발행자
 * 주문 메시지를 SQS 큐로 전송하는 인프라스트럭처 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqsMessagePublisher {
    
    private final SqsTemplate sqsTemplate;
    
    @Value("${app.sqs.queue-name}")
    private String queueName;
    
    /**
     * 주문 메시지를 SQS 큐로 발송
     * 
     * @param orderMessage 발송할 주문 메시지
     * @return 메시지 ID
     */
    public String publishOrderMessage(OrderMessage orderMessage) {
        try {
            log.info("주문 메시지 발송 시작: orderNumber={}, queueName={}", 
                    orderMessage.orderNumber(), queueName);
            
            var message = MessageBuilder
                    .withPayload(orderMessage)
                    .setHeader("messageType", "ORDER_CREATED")
                    .setHeader("orderNumber", orderMessage.orderNumber())
                    .setHeader("customerName", orderMessage.customerName())
                    .setHeader("correlationId", UUID.randomUUID().toString())
                    .build();
            
            var sendResult = sqsTemplate.send(queueName, message);
            
            log.info("주문 메시지 발송 완료: orderNumber={}, messageId={}", 
                    orderMessage.orderNumber(), sendResult.messageId());
            
            return sendResult.messageId().toString();
            
        } catch (Exception e) {
            log.error("주문 메시지 발송 실패: orderNumber={}, error={}", 
                    orderMessage.orderNumber(), e.getMessage(), e);
            throw new MessagePublishException(
                    "주문 메시지 발송에 실패했습니다: " + orderMessage.orderNumber(), e);
        }
    }
    
    /**
     * 메시지 발행 예외
     */
    public static class MessagePublishException extends RuntimeException {
        public MessagePublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}