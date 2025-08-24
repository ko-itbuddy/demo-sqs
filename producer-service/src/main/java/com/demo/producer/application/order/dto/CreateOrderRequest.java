package com.demo.producer.application.order.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * 주문 생성 요청 DTO
 * API 계층과 애플리케이션 계층 간의 데이터 전송
 */
@Builder
public record CreateOrderRequest(
        
        @NotBlank(message = "고객명은 필수입니다")
        @Size(min = 2, max = 100, message = "고객명은 2-100자 사이여야 합니다")
        String customerName,
        
        @NotBlank(message = "상품명은 필수입니다")
        @Size(min = 1, max = 200, message = "상품명은 1-200자 사이여야 합니다")
        String productName,
        
        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 0보다 커야 합니다")
        @Max(value = 1000, message = "수량은 1000개를 초과할 수 없습니다")
        Integer quantity,
        
        @NotNull(message = "가격은 필수입니다")
        @DecimalMin(value = "0.01", message = "가격은 0.01 이상이어야 합니다")
        @DecimalMax(value = "999999.99", message = "가격은 999,999.99를 초과할 수 없습니다")
        @Digits(integer = 6, fraction = 2, message = "가격 형식이 올바르지 않습니다")
        BigDecimal price
) {
}