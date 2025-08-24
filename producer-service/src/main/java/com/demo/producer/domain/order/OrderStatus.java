package com.demo.producer.domain.order;

/**
 * 주문 상태를 나타내는 열거형
 * 주문의 생명주기를 명확하게 정의
 */
public enum OrderStatus {
    /**
     * 주문 대기 중
     */
    PENDING("대기중"),
    
    /**
     * 주문 처리 중
     */
    PROCESSING("처리중"),
    
    /**
     * 주문 완료
     */
    COMPLETED("완료"),
    
    /**
     * 주문 실패
     */
    FAILED("실패");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminalState() {
        return this == COMPLETED || this == FAILED;
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PROCESSING || newStatus == FAILED;
            case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}