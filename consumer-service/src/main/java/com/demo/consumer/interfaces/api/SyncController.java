package com.demo.consumer.interfaces.api;

import com.demo.consumer.domain.processing.ProcessedOrder;
import com.demo.consumer.domain.processing.ProcessedOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 동기화 API 컨트롤러
 * Producer 서비스에서 Consumer 데이터 조회를 위한 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    
    private final ProcessedOrderRepository processedOrderRepository;
    
    /**
     * 주문번호로 처리된 주문 정보 조회 (Producer에서 호출)
     */
    @GetMapping("/processed-order/{orderNumber}")
    public ResponseEntity<ProcessedOrderResponse> getProcessedOrderForSync(@PathVariable String orderNumber) {
        log.info("동기화 API 호출 - 처리된 주문 조회: orderNumber={}", orderNumber);
        
        return processedOrderRepository.findByOrderNumber(orderNumber)
                .map(this::toResponse)
                .map(response -> {
                    log.info("동기화 API - 처리된 주문 조회 성공: orderNumber={}", orderNumber);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("동기화 API - 처리된 주문 조회 실패: orderNumber={}", orderNumber);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Consumer Sync API is running");
    }
    
    private ProcessedOrderResponse toResponse(ProcessedOrder order) {
        return new ProcessedOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getProductName(),
                order.getQuantity(),
                order.getPrice().toString(),
                order.getTotalAmount().toString(),
                order.getStatus().toString(),
                order.getMessageId(),
                order.getOriginalCreatedAt().toString(),
                order.getProcessedAt().toString(),
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null,
                order.getErrorMessage(),
                order.getRetryCount()
        );
    }
    
    /**
     * 처리된 주문 응답 DTO
     */
    public record ProcessedOrderResponse(
            Long id,
            String orderNumber,
            String customerName,
            String productName,
            Integer quantity,
            String price,
            String totalAmount,
            String status,
            String messageId,
            String originalCreatedAt,
            String processedAt,
            String updatedAt,
            String errorMessage,
            Integer retryCount
    ) {}
}