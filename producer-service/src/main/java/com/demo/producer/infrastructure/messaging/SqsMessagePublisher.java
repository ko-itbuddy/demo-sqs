package com.demo.producer.infrastructure.messaging;

import com.demo.producer.application.order.dto.OrderMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import jakarta.annotation.PostConstruct;
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
    private ObjectMapper objectMapper;
    
    @Value("${app.sqs.queue-name}")
    private String queueName;
    
    @PostConstruct
    private void initObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
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
            log.debug("SqsTemplate 객체: {}", sqsTemplate);
            
            // OrderMessage를 JSON 문자열로 직렬화
            String jsonPayload = objectMapper.writeValueAsString(orderMessage);
            
            var message = MessageBuilder
                    .withPayload(jsonPayload)
                    .setHeader("messageType", "ORDER_CREATED")
                    .setHeader("orderNumber", orderMessage.orderNumber())
                    .setHeader("customerName", orderMessage.customerName())
                    .setHeader("correlationId", UUID.randomUUID().toString())
                    .build();
            
            var sendResult = sqsTemplate.send(queueName, message);
            
            log.info("주문 메시지 발송 완료: orderNumber={}, messageId={}", 
                    orderMessage.orderNumber(), sendResult.messageId());
            
            return sendResult.messageId().toString();
            
        } catch (JsonProcessingException e) {
            log.error("주문 메시지 JSON 직렬화 실패: orderNumber={}, error={}", 
                    orderMessage.orderNumber(), e.getMessage(), e);
            throw new MessagePublishException(
                    "주문 메시지 JSON 직렬화에 실패했습니다: " + orderMessage.orderNumber(), e);
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