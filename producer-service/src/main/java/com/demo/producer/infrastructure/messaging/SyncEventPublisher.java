package com.demo.producer.infrastructure.messaging;

import com.demo.producer.application.order.dto.SyncEvent;
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

/**
 * 동기화 이벤트 발행자
 * Producer에서 Consumer로 동기화 이벤트 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncEventPublisher {
    
    private final SqsTemplate sqsTemplate;
    private ObjectMapper objectMapper;
    
    @Value("${app.sqs.sync-queue-name}")
    private String syncQueueName;
    
    @PostConstruct
    private void initObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * 동기화 이벤트 발송
     */
    public void publishSyncEvent(SyncEvent syncEvent) {
        try {
            String messageBody = objectMapper.writeValueAsString(syncEvent);
            
            var message = MessageBuilder.withPayload(messageBody)
                    .setHeader("messageId", syncEvent.messageId())
                    .setHeader("eventType", syncEvent.eventType())
                    .build();
            
            sqsTemplate.send(syncQueueName, message);
            
            log.info("동기화 이벤트 발송 완료: eventType={}, entityKey={}, messageId={}", 
                    syncEvent.eventType(), syncEvent.entityKey(), syncEvent.messageId());
                    
        } catch (JsonProcessingException e) {
            log.error("동기화 이벤트 직렬화 실패: eventType={}, entityKey={}", 
                    syncEvent.eventType(), syncEvent.entityKey(), e);
            throw new SyncEventPublishException("동기화 이벤트 발송 실패", e);
        } catch (Exception e) {
            log.error("동기화 이벤트 발송 실패: eventType={}, entityKey={}", 
                    syncEvent.eventType(), syncEvent.entityKey(), e);
            throw new SyncEventPublisher.SyncEventPublishException("동기화 이벤트 발송 실패", e);
        }
    }
    
    public static class SyncEventPublishException extends RuntimeException {
        public SyncEventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}