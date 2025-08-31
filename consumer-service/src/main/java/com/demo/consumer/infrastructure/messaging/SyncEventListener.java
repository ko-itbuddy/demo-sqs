package com.demo.consumer.infrastructure.messaging;

import com.demo.consumer.application.messaging.dto.SyncEvent;
import com.demo.consumer.infrastructure.client.ProducerApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * SQS 동기화 이벤트 리스너
 * Producer에서 발송한 동기화 이벤트를 수신하여 Consumer 데이터를 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncEventListener {
    
    private final ProducerApiClient producerApiClient;
    private final ObjectMapper objectMapper;
    
    @PostConstruct
    private void init() {
        log.info("SyncEventListener 초기화 완료 - sync-events-queue 메시지 수신 대기 중");
    }
    
    /**
     * 동기화 큐에서 동기화 이벤트 수신 및 처리
     */
    @SqsListener("${app.sqs.sync-queue-name}")
    public void handleSyncEvent(String messagePayload, Message<?> sqsMessage) {
        String messageId = sqsMessage.getHeaders().get("MessageId") != null ? 
                          sqsMessage.getHeaders().get("MessageId").toString() : "unknown";
        
        log.debug("[SYNC-DEBUG] 동기화 이벤트 수신 시작 - messageId: {}", messageId);
        log.debug("[SYNC-DEBUG] 메시지 페이로드: {}", messagePayload);
        
        try {
            // JSON을 SyncEvent로 역직렬화
            SyncEvent syncEvent = objectMapper.readValue(messagePayload, SyncEvent.class);
            
            log.info("동기화 이벤트 수신: eventType={}, entityKey={}, messageId={}", 
                    syncEvent.eventType(), syncEvent.entityKey(), messageId);
            log.debug("[SYNC-DEBUG] 동기화 이벤트 상세: {}", syncEvent);
            
            // 이벤트 타입별 처리
            processSyncEvent(syncEvent, messageId);
            
            log.info("동기화 이벤트 처리 완료: eventType={}, entityKey={}", 
                    syncEvent.eventType(), syncEvent.entityKey());
            
        } catch (Exception e) {
            log.error("동기화 이벤트 처리 실패: messageId={}, error={}", 
                    messageId, e.getMessage(), e);
            log.debug("[SYNC-DEBUG] 동기화 실패 상세: messagePayload={}", messagePayload);
            
            // 예외를 재발생시켜 SQS 재시도 메커니즘 동작
            throw new SyncEventProcessingException(
                    "동기화 이벤트 처리에 실패했습니다: " + messageId, e);
        }
    }
    
    /**
     * 동기화 이벤트 타입별 처리
     */
    private void processSyncEvent(SyncEvent syncEvent, String messageId) {
        switch (syncEvent.eventType()) {
            case "ORDER_UPDATED" -> handleOrderUpdatedEvent(syncEvent, messageId);
            case "PROCESSING_COMPLETED" -> handleProcessingCompletedEvent(syncEvent, messageId);
            default -> {
                log.warn("알 수 없는 동기화 이벤트 타입: eventType={}, entityKey={}", 
                        syncEvent.eventType(), syncEvent.entityKey());
            }
        }
    }
    
    /**
     * 주문 업데이트 이벤트 처리
     * Producer에서 주문이 업데이트되었을 때 Consumer 데이터 동기화
     */
    private void handleOrderUpdatedEvent(SyncEvent syncEvent, String messageId) {
        String orderNumber = syncEvent.entityKey();
        
        try {
            log.info("주문 동기화 시작: orderNumber={}", orderNumber);
            
            // Producer에서 최신 주문 정보 조회
            log.debug("[SYNC-DEBUG] Producer API 호출 시작: orderNumber={}", orderNumber);
            
            if (!producerApiClient.isProducerHealthy()) {
                log.warn("Producer 서비스 연결 실패 - 동기화 연기: orderNumber={}", orderNumber);
                throw new SyncEventProcessingException("Producer 서비스에 연결할 수 없습니다");
            }
            
            ProducerApiClient.OrderResponse producerOrder = producerApiClient.getOrder(orderNumber);
            log.debug("[SYNC-DEBUG] Producer에서 주문 정보 조회 성공: orderNumber={}, order={}", 
                    orderNumber, producerOrder);
            
            // TODO: Consumer의 주문 정보 업데이트 로직 구현
            // 현재는 로깅으로만 처리하고, 실제 동기화 로직은 별도 서비스에서 구현
            log.info("주문 동기화 성공: orderNumber={}, producerStatus={}, updatedAt={}", 
                    orderNumber, producerOrder.status(), producerOrder.updatedAt());
            
        } catch (ProducerApiClient.ProducerApiException e) {
            if (e.getMessage().contains("주문을 찾을 수 없습니다") || e.getCause().toString().contains("404")) {
                log.warn("Producer에서 주문 정보를 찾을 수 없음: orderNumber={}", orderNumber);
                // 404는 정상적인 케이스로 처리 (재시도하지 않음)
                return;
            }
            log.error("Producer API 호출 실패: orderNumber={}", orderNumber, e);
            throw new SyncEventProcessingException("Producer API 호출 실패", e);
        } catch (Exception e) {
            log.error("주문 동기화 실패: orderNumber={}", orderNumber, e);
            throw new SyncEventProcessingException("주문 동기화 실패", e);
        }
    }
    
    /**
     * 처리 완료 이벤트 처리 (Consumer → Producer 동기화)
     * 현재는 로깅만 수행하고 향후 확장 예정
     */
    private void handleProcessingCompletedEvent(SyncEvent syncEvent, String messageId) {
        String orderNumber = syncEvent.entityKey();
        
        log.info("처리 완료 동기화 이벤트: orderNumber={}", orderNumber);
        log.debug("[SYNC-DEBUG] 처리 완료 이벤트 - 현재는 로깅만 수행: orderNumber={}", orderNumber);
        
        // TODO: Consumer에서 Producer로의 역방향 동기화 로직 구현 예정
    }
    
    /**
     * 동기화 이벤트 처리 예외
     */
    public static class SyncEventProcessingException extends RuntimeException {
        public SyncEventProcessingException(String message) {
            super(message);
        }
        
        public SyncEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}