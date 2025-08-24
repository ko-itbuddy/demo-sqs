package com.demo.producer.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 주문 도메인 엔티티
 * DDD의 Aggregate Root 역할
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Builder
    private Order(String orderNumber, String customerName, String productName, 
                  Integer quantity, BigDecimal price) {
        this.orderNumber = Objects.requireNonNull(orderNumber, "주문번호는 필수입니다");
        this.customerName = Objects.requireNonNull(customerName, "고객명은 필수입니다");
        this.productName = Objects.requireNonNull(productName, "상품명은 필수입니다");
        this.quantity = Objects.requireNonNull(quantity, "수량은 필수입니다");
        this.price = Objects.requireNonNull(price, "가격은 필수입니다");
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        
        validateQuantity();
        validatePrice();
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
    
    public void updateStatus(OrderStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "주문 상태는 필수입니다");
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getTotalAmount() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    
    public boolean isPending() {
        return OrderStatus.PENDING.equals(status);
    }
    
    public boolean isProcessing() {
        return OrderStatus.PROCESSING.equals(status);
    }
    
    public boolean isCompleted() {
        return OrderStatus.COMPLETED.equals(status);
    }
    
    public boolean isFailed() {
        return OrderStatus.FAILED.equals(status);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderNumber, order.orderNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderNumber);
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}