package com.demo.consumer.domain.processing;

/**
 * 주문 처리 상태를 나타내는 열거형
 * Consumer에서 메시지 처리 상태를 관리
 */
public enum ProcessingStatus {
    /**
     * 처리 중
     */
    PROCESSING("처리중"),
    
    /**
     * 처리 완료
     */
    COMPLETED("완료"),
    
    /**
     * 처리 실패
     */
    FAILED("실패");
    
    private final String description;
    
    ProcessingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminalState() {
        return this == COMPLETED || this == FAILED;
    }
    
    public boolean canTransitionTo(ProcessingStatus newStatus) {
        return switch (this) {
            case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}