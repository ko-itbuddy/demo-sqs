package com.demo.consumer.domain.processing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 처리된 주문 도메인 엔티티
 * Consumer에서 처리한 주문 정보를 저장
 */
@Entity
@Table(name = "processed_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String orderNumber;
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;
    
    @Column(nullable = false)
    private String messageId;
    
    @Column(nullable = false)
    private LocalDateTime originalCreatedAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private String errorMessage;
    
    @Column
    private Integer retryCount = 0;
    
    @Builder
    private ProcessedOrder(String orderNumber, String customerName, String productName,
                          Integer quantity, BigDecimal price, BigDecimal totalAmount,
                          String messageId, LocalDateTime originalCreatedAt) {
        this.orderNumber = Objects.requireNonNull(orderNumber, "주문번호는 필수입니다");
        this.customerName = Objects.requireNonNull(customerName, "고객명은 필수입니다");
        this.productName = Objects.requireNonNull(productName, "상품명은 필수입니다");
        this.quantity = Objects.requireNonNull(quantity, "수량은 필수입니다");
        this.price = Objects.requireNonNull(price, "가격은 필수입니다");
        this.totalAmount = Objects.requireNonNull(totalAmount, "총금액은 필수입니다");
        this.messageId = Objects.requireNonNull(messageId, "메시지ID는 필수입니다");
        this.originalCreatedAt = Objects.requireNonNull(originalCreatedAt, "원본 생성일시는 필수입니다");
        this.status = ProcessingStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
        
        validateQuantity();
        validatePrice();
        validateTotalAmount();
    }
    
    private void validateQuantity() {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다");
        }
    }
    
    private void validatePrice() {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다");
        }
    }
    
    private void validateTotalAmount() {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("총금액은 0보다 커야 합니다");
        }
    }
    
    /**
     * 처리 완료로 상태 변경
     */
    public void markAsCompleted() {
        if (!canTransitionTo(ProcessingStatus.COMPLETED)) {
            throw new IllegalStateException(
                    "현재 상태에서 완료 상태로 변경할 수 없습니다: " + status);
        }
        this.status = ProcessingStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 처리 실패로 상태 변경
     */
    public void markAsFailed(String errorMessage) {
        if (!canTransitionTo(ProcessingStatus.FAILED)) {
            throw new IllegalStateException(
                    "현재 상태에서 실패 상태로 변경할 수 없습니다: " + status);
        }
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 상태 전이 가능 여부 확인
     */
    private boolean canTransitionTo(ProcessingStatus newStatus) {
        return switch (this.status) {
            case PROCESSING -> newStatus == ProcessingStatus.COMPLETED || newStatus == ProcessingStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
    
    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry(int maxRetryAttempts) {
        return status == ProcessingStatus.PROCESSING && retryCount < maxRetryAttempts;
    }
    
    public boolean isProcessing() {
        return ProcessingStatus.PROCESSING.equals(status);
    }
    
    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(status);
    }
    
    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(status);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedOrder that = (ProcessedOrder) o;
        return Objects.equals(orderNumber, that.orderNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderNumber);
    }
    
    @Override
    public String toString() {
        return "ProcessedOrder{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", status=" + status +
                ", processedAt=" + processedAt +
                "}";
    }
}