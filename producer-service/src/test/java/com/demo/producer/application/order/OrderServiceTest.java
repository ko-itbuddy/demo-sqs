package com.demo.producer.application.order;

import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.application.order.dto.OrderResponse;
import com.demo.producer.domain.order.Order;
import com.demo.producer.domain.order.OrderRepository;
import com.demo.producer.domain.order.OrderStatus;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * OrderService 단위 테스트
 * 애플리케이션 서비스 계층 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private SqsMessagePublisher messagePublisher;
    
    @InjectMocks
    private OrderService orderService;
    
    private CreateOrderRequest validRequest;
    private Order savedOrder;
    
    @BeforeEach
    void setUp() {
        validRequest = CreateOrderRequest.builder()
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build();
                
        savedOrder = Order.builder()
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build();
    }
    
    @Test
    @DisplayName("정상적인 주문 생성 및 메시지 발송")
    void createOrder_WithValidRequest_ShouldCreateOrderAndSendMessage() {
        // Given
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(messagePublisher.publishOrderMessage(any())).willReturn("message-id-123");
        
        // When
        OrderResponse result = orderService.createOrder(validRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.customerName()).isEqualTo("홍길동");
        assertThat(result.productName()).isEqualTo("테스트 상품");
        assertThat(result.quantity()).isEqualTo(5);
        assertThat(result.price()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        
        // Order 저장 검증
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getCustomerName()).isEqualTo("홍길동");
        assertThat(capturedOrder.getProductName()).isEqualTo("테스트 상품");
        
        // 메시지 발송 검증
        verify(messagePublisher).publishOrderMessage(any());
    }
    
    @Test
    @DisplayName("메시지 발송 실패해도 주문은 생성됨")
    void createOrder_WhenMessagePublishFails_ShouldStillCreateOrder() {
        // Given
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(messagePublisher.publishOrderMessage(any()))
                .willThrow(new SqsMessagePublisher.MessagePublishException("메시지 발송 실패", new RuntimeException()));
        
        // When
        OrderResponse result = orderService.createOrder(validRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo(savedOrder.getOrderNumber());
        
        // Order 저장 검증
        verify(orderRepository).save(any(Order.class));
        // 메시지 발송 시도 검증
        verify(messagePublisher).publishOrderMessage(any());
    }
    
    @Test
    @DisplayName("주문번호로 주문 조회 성공")
    void getOrder_WithValidOrderNumber_ShouldReturnOrder() {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(savedOrder));
        
        // When
        OrderResponse result = orderService.getOrder(orderNumber);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo(orderNumber);
        verify(orderRepository).findByOrderNumber(orderNumber);
    }
    
    @Test
    @DisplayName("존재하지 않는 주문번호로 조회시 예외 발생")
    void getOrder_WithNonExistentOrderNumber_ShouldThrowException() {
        // Given
        String orderNumber = "NON-EXISTENT-ORDER";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(orderNumber))
                .isInstanceOf(OrderService.OrderNotFoundException.class)
                .hasMessage("주문을 찾을 수 없습니다: " + orderNumber);
        
        verify(orderRepository).findByOrderNumber(orderNumber);
    }
    
    @Test
    @DisplayName("고객명으로 주문 목록 조회")
    void getOrdersByCustomerName_ShouldReturnOrderList() {
        // Given
        String customerName = "홍길동";
        List<Order> orders = List.of(savedOrder);
        given(orderRepository.findByCustomerNameOrderByCreatedAtDesc(customerName)).willReturn(orders);
        
        // When
        List<OrderResponse> result = orderService.getOrdersByCustomerName(customerName);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerName()).isEqualTo(customerName);
        verify(orderRepository).findByCustomerNameOrderByCreatedAtDesc(customerName);
    }
    
    @Test
    @DisplayName("주문 상태별 주문 목록 조회")
    void getOrdersByStatus_ShouldReturnOrderList() {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        List<Order> orders = List.of(savedOrder);
        given(orderRepository.findByStatusOrderByCreatedAtDesc(status)).willReturn(orders);
        
        // When
        List<OrderResponse> result = orderService.getOrdersByStatus(status);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(status);
        verify(orderRepository).findByStatusOrderByCreatedAtDesc(status);
    }
    
    @Test
    @DisplayName("모든 주문 조회")
    void getAllOrders_ShouldReturnAllOrders() {
        // Given
        List<Order> orders = List.of(savedOrder);
        given(orderRepository.findAll()).willReturn(orders);
        
        // When
        List<OrderResponse> result = orderService.getAllOrders();
        
        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findAll();
    }
}