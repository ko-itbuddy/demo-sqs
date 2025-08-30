package com.demo.producer.interfaces.api;

import com.demo.producer.application.order.OrderService;
import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.application.order.dto.OrderResponse;
import com.demo.producer.domain.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController 통합 테스트
 * Spring Boot Test Slice를 활용한 웹 계층 테스트
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private OrderService orderService;
    
    private CreateOrderRequest validRequest;
    private OrderResponse orderResponse;
    
    @BeforeEach
    void setUp() {
        validRequest = CreateOrderRequest.builder()
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .build();
                
        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-20231201-123456-ABC12345")
                .customerName("홍길동")
                .productName("테스트 상품")
                .quantity(5)
                .price(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .status(OrderStatus.PENDING)
                .statusDescription("대기중")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("정상적인 주문 생성 API")
    void createOrder_WithValidRequest_ShouldReturn201() throws Exception {
        // Given
        given(orderService.createOrder(any(CreateOrderRequest.class))).willReturn(orderResponse);
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20231201-123456-ABC12345"))
                .andExpect(jsonPath("$.customerName").value("홍길동"))
                .andExpect(jsonPath("$.productName").value("테스트 상품"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.price").value(10000.00))
                .andExpect(jsonPath("$.totalAmount").value(50000.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
        
        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }
    
    @Test
    @DisplayName("유효하지 않은 요청 데이터로 주문 생성시 400 에러")
    void createOrder_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .customerName("") // 빈 문자열
                .productName(null) // null
                .quantity(-1) // 음수
                .price(BigDecimal.ZERO) // 0
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        verifyNoInteractions(orderService);
    }
    
    @Test
    @DisplayName("주문번호로 주문 조회 API")
    void getOrder_WithValidOrderNumber_ShouldReturn200() throws Exception {
        // Given
        String orderNumber = "ORD-20231201-123456-ABC12345";
        given(orderService.getOrder(orderNumber)).willReturn(orderResponse);
        
        // When & Then
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.customerName").value("홍길동"));
        
        verify(orderService).getOrder(orderNumber);
    }
    
    @Test
    @DisplayName("존재하지 않는 주문번호로 조회시 404 에러")
    void getOrder_WithNonExistentOrderNumber_ShouldReturn404() throws Exception {
        // Given
        String orderNumber = "NON-EXISTENT-ORDER";
        given(orderService.getOrder(orderNumber))
                .willThrow(new OrderService.OrderNotFoundException("주문을 찾을 수 없습니다: " + orderNumber));
        
        // When & Then
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andDo(print())
                .andExpect(status().isNotFound());
        
        verify(orderService).getOrder(orderNumber);
    }
    
    @Test
    @DisplayName("모든 주문 조회 API")
    void getAllOrders_ShouldReturn200() throws Exception {
        // Given
        List<OrderResponse> orders = List.of(orderResponse);
        given(orderService.getAllOrders()).willReturn(orders);
        
        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-20231201-123456-ABC12345"));
        
        verify(orderService).getAllOrders();
    }
    
    @Test
    @DisplayName("고객명으로 주문 조회 API")
    void getOrdersByCustomer_ShouldReturn200() throws Exception {
        // Given
        String customerName = "홍길동";
        List<OrderResponse> orders = List.of(orderResponse);
        given(orderService.getOrdersByCustomerName(customerName)).willReturn(orders);
        
        // When & Then
        mockMvc.perform(get("/api/orders/customer/{customerName}", customerName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerName").value(customerName));
        
        verify(orderService).getOrdersByCustomerName(customerName);
    }
    
    @Test
    @DisplayName("주문 상태별 조회 API")
    void getOrdersByStatus_ShouldReturn200() throws Exception {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        List<OrderResponse> orders = List.of(orderResponse);
        given(orderService.getOrdersByStatus(status)).willReturn(orders);
        
        // When & Then
        mockMvc.perform(get("/api/orders/status/{status}", status))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        
        verify(orderService).getOrdersByStatus(status);
    }
}