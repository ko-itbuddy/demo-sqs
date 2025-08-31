package com.demo.producer.application.order;

import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.application.order.dto.OrderMessage;
import com.demo.producer.application.order.dto.OrderResponse;
import com.demo.producer.application.order.dto.SyncEvent;
import com.demo.producer.domain.order.Order;
import com.demo.producer.domain.order.OrderRepository;
import com.demo.producer.domain.order.OrderStatus;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
import com.demo.producer.infrastructure.messaging.SyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 주문 애플리케이션 서비스
 * 주문 생성 및 메시지 발송을 담당하는 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final SqsMessagePublisher messagePublisher;
    private final SyncEventPublisher syncEventPublisher;
    
    /**
     * 새로운 주문을 생성하고 메시지를 발송
     * 
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("주문 생성 시작: customerName={}, productName={}", 
                request.customerName(), request.productName());
        
        // 도메인 객체 생성
        var order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.customerName())
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();
        
        // 주문 저장
        var savedOrder = orderRepository.save(order);
        log.info("주문 저장 완료: orderNumber={}, id={}", 
                savedOrder.getOrderNumber(), savedOrder.getId());
        
        // 메시지 발송
        boolean messagePublished = false;
        boolean syncEventPublished = false;
        
        try {
            var messageId = UUID.randomUUID().toString();
            var orderMessage = OrderMessage.from(savedOrder, messageId);
            messagePublisher.publishOrderMessage(orderMessage);
            messagePublished = true;
            
            log.info("주문 메시지 발송 완료: orderNumber={}, messageId={}", 
                    savedOrder.getOrderNumber(), messageId);
            
        } catch (Exception e) {
            log.error("주문 메시지 발송 실패: orderNumber={}, errorType={}, errorMessage={}", 
                    savedOrder.getOrderNumber(), e.getClass().getSimpleName(), e.getMessage(), e);
            // 메시지 발송 실패해도 주문은 이미 생성되어 있으므로 별도 처리 필요
        }
        
        // 동기화 이벤트 발송
        try {
            publishOrderSyncEvent(savedOrder.getOrderNumber());
            syncEventPublished = true;
        } catch (Exception e) {
            log.error("동기화 이벤트 발송 실패: orderNumber={}, errorType={}, errorMessage={}", 
                    savedOrder.getOrderNumber(), e.getClass().getSimpleName(), e.getMessage(), e);
        }
        
        // 메시지 발송 상태 로그
        log.warn("메시지 발송 상태 - orderNumber={}, messagePublished={}, syncEventPublished={}", 
                savedOrder.getOrderNumber(), messagePublished, syncEventPublished);
        
        return OrderResponse.from(savedOrder);
    }
    
    /**
     * 주문번호로 주문 조회
     */
    public OrderResponse getOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(
                        "주문을 찾을 수 없습니다: " + orderNumber));
    }
    
    /**
     * 고객명으로 주문 목록 조회
     */
    public List<OrderResponse> getOrdersByCustomerName(String customerName) {
        return orderRepository.findByCustomerNameOrderByCreatedAtDesc(customerName)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
    
    /**
     * 주문 상태별 주문 목록 조회
     */
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
    
    /**
     * 모든 주문 조회
     */
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
    
    /**
     * 주문 상태 업데이트 및 동기화 이벤트 발송
     */
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        var order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderNumber));
        
        order.updateStatus(newStatus);
        var updatedOrder = orderRepository.save(order);
        
        log.info("주문 상태 업데이트: orderNumber={}, newStatus={}", orderNumber, newStatus);
        
        // 동기화 이벤트 발송
        publishOrderSyncEvent(orderNumber);
        
        return OrderResponse.from(updatedOrder);
    }
    
    /**
     * 동기화 이벤트 발송
     */
    private void publishOrderSyncEvent(String orderNumber) {
        try {
            var messageId = UUID.randomUUID().toString();
            var syncEvent = SyncEvent.createOrderSync(orderNumber, messageId);
            syncEventPublisher.publishSyncEvent(syncEvent);
            
            log.info("주문 동기화 이벤트 발송 완료: orderNumber={}", orderNumber);
        } catch (Exception e) {
            log.error("주문 동기화 이벤트 발송 실패: orderNumber={}", orderNumber, e);
            // 동기화 실패는 비즈니스 로직에 영향 없음
        }
    }
    
    /**
     * 주문번호 생성
     * 패턴: ORD-YYYYMMDD-HHMMSS-UUID(8자리)
     */
    private String generateOrderNumber() {
        var now = LocalDateTime.now();
        var dateTime = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        var uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", dateTime, uuid);
    }
    
    /**
     * 주문 미발견 예외
     */
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
}