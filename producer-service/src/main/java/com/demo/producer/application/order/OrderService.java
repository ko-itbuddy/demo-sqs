package com.demo.producer.application.order;

import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.application.order.dto.OrderMessage;
import com.demo.producer.application.order.dto.OrderResponse;
import com.demo.producer.domain.order.Order;
import com.demo.producer.domain.order.OrderRepository;
import com.demo.producer.domain.order.OrderStatus;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
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
        try {
            var messageId = UUID.randomUUID().toString();
            var orderMessage = OrderMessage.from(savedOrder, messageId);
            messagePublisher.publishOrderMessage(orderMessage);
            
            log.info("주문 메시지 발송 완료: orderNumber={}", savedOrder.getOrderNumber());
        } catch (Exception e) {
            log.error("주문 메시지 발송 실패하였지만 주문은 생성됨: orderNumber={}", 
                    savedOrder.getOrderNumber(), e);
            // 메시지 발송 실패해도 주문은 이미 생성되어 있으므로 별도 처리 필요
        }
        
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