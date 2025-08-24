package com.demo.producer.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 도메인 리포지토리
 * DDD의 Repository 패턴 구현
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 주문번호로 주문 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * 고객명으로 주문 목록 조회
     */
    List<Order> findByCustomerNameOrderByCreatedAtDesc(String customerName);
    
    /**
     * 주문 상태별 주문 목록 조회
     */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    /**
     * 특정 기간 내 주문 목록 조회
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * 대기 중인 주문 개수 조회
     */
    long countByStatus(OrderStatus status);
    
    /**
     * 주문번호 존재 여부 확인
     */
    boolean existsByOrderNumber(String orderNumber);
}