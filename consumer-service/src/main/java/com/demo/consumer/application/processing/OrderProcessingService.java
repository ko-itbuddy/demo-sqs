package com.demo.consumer.application.processing;

import com.demo.consumer.application.messaging.dto.OrderMessage;
import com.demo.consumer.application.messaging.dto.SyncEvent;
import com.demo.consumer.domain.processing.ProcessedOrder;
import com.demo.consumer.domain.processing.ProcessedOrderRepository;
import com.demo.consumer.infrastructure.messaging.SyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

/**
 * 주문 처리 애플리케이션 서비스
 * SQS 메시지로 수신된 주문을 처리하는 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderProcessingService {
    
    private final ProcessedOrderRepository processedOrderRepository;
    private final SyncEventPublisher syncEventPublisher;
    private final Random random = new Random();
    
    @Value("${app.sqs.max-retry-attempts}")
    private int maxRetryAttempts;
    
    /**
     * 주문 메시지 처리
     * 
     * @param orderMessage 수신된 주문 메시지
     * @param messageId SQS 메시지 ID
     */
    @Transactional
    public void processOrder(OrderMessage orderMessage, String messageId) {
        log.info("주문 처리 시작: orderNumber={}, messageId={}", 
                orderMessage.orderNumber(), messageId);
        
        // 중복 메시지 처리 방지
        if (processedOrderRepository.existsByMessageId(messageId)) {
            log.warn("이미 처리된 메시지입니다: messageId={}", messageId);
            return;
        }
        
        // 도메인 객체 생성 및 저장
        ProcessedOrder processedOrder = orderMessage.toProcessedOrder(messageId);
        processedOrder = processedOrderRepository.save(processedOrder);
        
        try {
            // 실제 비즈니스 로직 수행 (예: 재고 확인, 결제 처리 등)
            performBusinessLogic(processedOrder);
            
            // 처리 완료로 상태 변경
            processedOrder.markAsCompleted();
            processedOrderRepository.save(processedOrder);
            
            log.info("주문 처리 완료: orderNumber={}, id={}", 
                    processedOrder.getOrderNumber(), processedOrder.getId());
            
            // 동기화 이벤트 발송
            publishProcessingSyncEvent(processedOrder.getOrderNumber());
            
        } catch (Exception e) {
            log.error("주문 처리 중 오류 발생: orderNumber={}, error={}", 
                    processedOrder.getOrderNumber(), e.getMessage());
            
            // 재시도 횟수 증가
            processedOrder.incrementRetryCount();
            
            // 재시도 가능 여부 확인
            if (processedOrder.canRetry(maxRetryAttempts)) {
                log.info("주문 재시도 가능: orderNumber={}, retryCount={}", 
                        processedOrder.getOrderNumber(), processedOrder.getRetryCount());
                processedOrderRepository.save(processedOrder);
                throw new OrderProcessingException("주문 처리 실패 - 재시도 예정: " + 
                        processedOrder.getOrderNumber(), e);
            } else {
                // 최대 재시도 횟수 초과시 실패 처리
                processedOrder.markAsFailed(e.getMessage());
                processedOrderRepository.save(processedOrder);
                
                log.error("주문 처리 최종 실패: orderNumber={}, retryCount={}", 
                        processedOrder.getOrderNumber(), processedOrder.getRetryCount());
                        
                throw new OrderProcessingException("주문 처리 최종 실패: " + 
                        processedOrder.getOrderNumber(), e);
            }
        }
    }
    
    /**
     * DLQ 메시지 처리
     * 최대 재시도 횟수를 초과한 실패 메시지 처리
     * 
     * @param orderMessage DLQ에서 수신된 주문 메시지
     * @param messageId SQS 메시지 ID
     */
    @Transactional
    public void handleDeadLetterMessage(OrderMessage orderMessage, String messageId) {
        log.warn("DLQ 메시지 처리 시작: orderNumber={}, messageId={}", 
                orderMessage.orderNumber(), messageId);
        
        // DLQ 메시지가 이미 처리되었는지 확인
        var existingOrder = processedOrderRepository.findByOrderNumber(orderMessage.orderNumber());
        
        if (existingOrder.isPresent()) {
            var order = existingOrder.get();
            log.info("기존 처리된 주문 발견: orderNumber={}, status={}, retryCount={}", 
                    order.getOrderNumber(), order.getStatus(), order.getRetryCount());
            
            if (!order.isFailed()) {
                // 아직 실패 상태가 아니라면 실패로 변경
                order.markAsFailed("DLQ로 이동된 메시지");
                processedOrderRepository.save(order);
            }
        } else {
            // 새로운 DLQ 메시지인 경우 실패 상태로 저장
            ProcessedOrder failedOrder = orderMessage.toProcessedOrder(messageId);
            failedOrder.markAsFailed("DLQ로 직접 이동된 메시지");
            processedOrderRepository.save(failedOrder);
        }
        
        // DLQ 메시지에 대한 알림이나 모니터링 로직 수행
        sendFailureNotification(orderMessage);
        
        log.warn("DLQ 메시지 처리 완료: orderNumber={}", orderMessage.orderNumber());
    }
    
    /**
     * 실제 비즈니스 로직 수행
     * 데모를 위해 랜덤하게 실패 시뮬레이션
     */
    private void performBusinessLogic(ProcessedOrder processedOrder) {
        log.info("비즈니스 로직 수행 중: orderNumber={}", processedOrder.getOrderNumber());
        
        // 시뮬레이션을 위한 처리 시간
        try {
            Thread.sleep(1000); // 1초 처리 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("처리 중 인터럽트 발생", e);
        }
        
        // 30% 확률로 실패 시뮬레이션 (DLQ 테스트를 위해)
        if (random.nextInt(100) < 30) {
            throw new OrderProcessingException("비즈니스 로직 처리 실패 (시뮬레이션)");
        }
        
        // 재고 확인, 결제 처리 등의 실제 로직이 여기에 구현됨
        log.info("재고 확인 완료: orderNumber={}", processedOrder.getOrderNumber());
        log.info("결제 처리 완료: orderNumber={}", processedOrder.getOrderNumber());
        log.info("배송 준비 완료: orderNumber={}", processedOrder.getOrderNumber());
    }
    
    /**
     * 실패 알림 발송
     */
    private void sendFailureNotification(OrderMessage orderMessage) {
        // 실제 환경에서는 이메일, 슬랙, SMS 등으로 알림
        log.error("=== 주문 처리 실패 알림 ===");
        log.error("주문번호: {}", orderMessage.orderNumber());
        log.error("고객명: {}", orderMessage.customerName());
        log.error("상품명: {}", orderMessage.productName());
        log.error("총금액: {}", orderMessage.totalAmount());
        log.error("========================");
    }
    
    /**
     * 동기화 이벤트 발송
     */
    private void publishProcessingSyncEvent(String orderNumber) {
        try {
            var messageId = UUID.randomUUID().toString();
            var syncEvent = SyncEvent.createProcessingSync(orderNumber, messageId);
            syncEventPublisher.publishSyncEvent(syncEvent);
            
            log.info("처리 완료 동기화 이벤트 발송 완료: orderNumber={}", orderNumber);
        } catch (Exception e) {
            log.error("처리 완료 동기화 이벤트 발송 실패: orderNumber={}", orderNumber, e);
            // 동기화 실패는 비즈니스 로직에 영향 없음
        }
    }
    
    /**
     * 주문 처리 예외
     */
    public static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(String message) {
            super(message);
        }
        
        public OrderProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}