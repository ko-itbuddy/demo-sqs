package com.demo.producer.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consumer 서비스 API 클라이언트
 * Consumer의 동기화 API 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerApiClient {
    
    private final RestClient restClient;
    
    @Value("${app.client.consumer-service-url}")
    private String consumerServiceUrl;
    
    /**
     * Consumer에서 처리된 주문 정보 조회
     */
    public ProcessedOrderResponse getProcessedOrder(String orderNumber) {
        try {
            String url = consumerServiceUrl + "/api/sync/processed-order/" + orderNumber;
            
            log.info("Consumer API 호출 - 처리된 주문 조회: orderNumber={}, url={}", orderNumber, url);
            
            ProcessedOrderResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ProcessedOrderResponse.class);
                    
            log.info("Consumer API 호출 성공: orderNumber={}", orderNumber);
            return response;
            
        } catch (RestClientException e) {
            log.error("Consumer API 호출 실패: orderNumber={}, error={}", orderNumber, e.getMessage());
            throw new ConsumerApiException("Consumer API 호출 실패: " + orderNumber, e);
        }
    }
    
    /**
     * Consumer 서비스 헬스체크
     */
    public boolean isConsumerHealthy() {
        try {
            String url = consumerServiceUrl + "/api/sync/health";
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
                    
            log.debug("Consumer 헬스체크 응답: {}", response);
            return true;
            
        } catch (Exception e) {
            log.warn("Consumer 헬스체크 실패: {}", e.getMessage());
            return false;
        }
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
    
    public static class ConsumerApiException extends RuntimeException {
        public ConsumerApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}