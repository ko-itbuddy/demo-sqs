package com.demo.producer.interfaces.api;

import com.demo.producer.application.order.OrderService;
import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.application.order.dto.OrderResponse;
import com.demo.producer.domain.order.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문 REST API 컨트롤러
 * 주문 생성 및 조회 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 새로운 주문 생성
     * 
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("주문 생성 요청: customerName={}, productName={}, quantity={}, price={}", 
                request.customerName(), request.productName(), request.quantity(), request.price());
        
        var order = orderService.createOrder(request);
        
        log.info("주문 생성 응답: orderNumber={}, status={}", 
                order.orderNumber(), order.status());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    /**
     * 주문번호로 주문 조회
     * 
     * @param orderNumber 주문번호
     * @return 주문 정보
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        log.info("주문 조회 요청: orderNumber={}", orderNumber);
        
        var order = orderService.getOrder(orderNumber);
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * 모든 주문 조회
     * 
     * @return 전체 주문 목록
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("전체 주문 조회 요청");
        
        var orders = orderService.getAllOrders();
        
        log.info("전체 주문 조회 응답: count={}", orders.size());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * 고객명으로 주문 조회
     * 
     * @param customerName 고객명
     * @return 해당 고객의 주문 목록
     */
    @GetMapping("/customer/{customerName}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerName) {
        log.info("고객별 주문 조회 요청: customerName={}", customerName);
        
        var orders = orderService.getOrdersByCustomerName(customerName);
        
        log.info("고객별 주문 조회 응답: customerName={}, count={}", customerName, orders.size());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * 주문 상태별 조회
     * 
     * @param status 주문 상태
     * @return 해당 상태의 주문 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        log.info("상태별 주문 조회 요청: status={}", status);
        
        var orders = orderService.getOrdersByStatus(status);
        
        log.info("상태별 주문 조회 응답: status={}, count={}", status, orders.size());
        
        return ResponseEntity.ok(orders);
    }
}