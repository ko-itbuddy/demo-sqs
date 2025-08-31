package com.demo.consumer.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producer 서비스 API 클라이언트
 * Producer의 동기화 API 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProducerApiClient {
    
    private final RestClient restClient;
    
    @Value("${app.client.producer-service-url}")
    private String producerServiceUrl;
    
    /**
     * Producer에서 주문 정보 조회
     */
    public OrderResponse getOrder(String orderNumber) {
        try {
            String url = producerServiceUrl + "/api/sync/order/" + orderNumber;
            
            log.info("Producer API 호출 - 주문 조회: orderNumber={}, url={}", orderNumber, url);
            
            OrderResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OrderResponse.class);
                    
            log.info("Producer API 호출 성공: orderNumber={}", orderNumber);
            return response;
            
        } catch (RestClientException e) {
            log.error("Producer API 호출 실패: orderNumber={}, error={}", orderNumber, e.getMessage());
            throw new ProducerApiException("Producer API 호출 실패: " + orderNumber, e);
        }
    }
    
    /**
     * Producer 서비스 헬스체크
     */
    public boolean isProducerHealthy() {
        try {
            String url = producerServiceUrl + "/api/sync/health";
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
                    
            log.debug("Producer 헬스체크 응답: {}", response);
            return true;
            
        } catch (Exception e) {
            log.warn("Producer 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 주문 응답 DTO
     */
    public record OrderResponse(
            Long id,
            String orderNumber,
            String customerName,
            String productName,
            Integer quantity,
            BigDecimal price,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    
    public static class ProducerApiException extends RuntimeException {
        public ProducerApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}