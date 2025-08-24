package com.demo.consumer.domain.processing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 처리된 주문 도메인 리포지토리
 * DDD의 Repository 패턴 구현
 */
@Repository
public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {
    
    /**
     * 주문번호로 처리된 주문 조회
     */
    Optional<ProcessedOrder> findByOrderNumber(String orderNumber);
    
    /**
     * 메시지ID로 처리된 주문 조회
     */
    Optional<ProcessedOrder> findByMessageId(String messageId);
    
    /**
     * 고객명으로 처리된 주문 목록 조회
     */
    List<ProcessedOrder> findByCustomerNameOrderByProcessedAtDesc(String customerName);
    
    /**
     * 처리 상태별 주문 목록 조회
     */
    List<ProcessedOrder> findByStatusOrderByProcessedAtDesc(ProcessingStatus status);
    
    /**
     * 특정 기간 내 처리된 주문 목록 조회
     */
    @Query("SELECT p FROM ProcessedOrder p WHERE p.processedAt BETWEEN :startDate AND :endDate ORDER BY p.processedAt DESC")
    List<ProcessedOrder> findProcessedOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * 상태별 주문 개수 조회
     */
    long countByStatus(ProcessingStatus status);
    
    /**
     * 실패한 주문 중 재시도 가능한 주문 목록 조회
     */
    @Query("SELECT p FROM ProcessedOrder p WHERE p.status = :status AND p.retryCount < :maxRetryCount")
    List<ProcessedOrder> findRetryableOrders(@Param("status") ProcessingStatus status, 
                                           @Param("maxRetryCount") Integer maxRetryCount);
    
    /**
     * 주문번호 존재 여부 확인
     */
    boolean existsByOrderNumber(String orderNumber);
    
    /**
     * 메시지ID 존재 여부 확인 (중복 처리 방지용)
     */
    boolean existsByMessageId(String messageId);
}