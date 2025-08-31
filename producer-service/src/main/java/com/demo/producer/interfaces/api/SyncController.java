package com.demo.producer.interfaces.api;

import com.demo.producer.application.order.OrderService;
import com.demo.producer.application.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 동기화 API 컨트롤러
 * Consumer 서비스에서 Producer 데이터 조회를 위한 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    
    private final OrderService orderService;
    
    /**
     * 주문번호로 주문 정보 조회 (Consumer에서 호출)
     */
    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderForSync(@PathVariable String orderNumber) {
        log.info("동기화 API 호출 - 주문 조회: orderNumber={}", orderNumber);
        
        try {
            OrderResponse order = orderService.getOrder(orderNumber);
            log.info("동기화 API - 주문 조회 성공: orderNumber={}", orderNumber);
            return ResponseEntity.ok(order);
        } catch (OrderService.OrderNotFoundException e) {
            log.warn("동기화 API - 주문 조회 실패: orderNumber={}", orderNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("동기화 API - 주문 조회 오류: orderNumber={}", orderNumber, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 모든 주문 목록 조회 (Consumer에서 호출)
     */
    @PostMapping("/orders")
    public ResponseEntity<String> syncAllOrders() {
        log.info("동기화 API 호출 - 모든 주문 동기화 요청");
        
        try {
            // 모든 주문에 대해 동기화 이벤트 발송
            var allOrders = orderService.getAllOrders();
            int syncCount = 0;
            
            for (OrderResponse order : allOrders) {
                try {
                    // OrderService 내부의 동기화 이벤트 발송 로직 활용
                    orderService.updateOrderStatus(order.orderNumber(), order.status());
                    syncCount++;
                } catch (Exception e) {
                    log.warn("주문 동기화 실패: orderNumber={}", order.orderNumber(), e);
                }
            }
            
            log.info("동기화 API - 주문 동기화 완료: 총 {}개 중 {}개 성공", allOrders.size(), syncCount);
            return ResponseEntity.ok(String.format("동기화 완료: 총 %d개 중 %d개 성공", allOrders.size(), syncCount));
            
        } catch (Exception e) {
            log.error("동기화 API - 주문 동기화 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Producer Sync API is running");
    }
}