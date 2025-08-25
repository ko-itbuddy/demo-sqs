package com.demo.producer.integration;

import com.demo.producer.application.order.dto.CreateOrderRequest;
import com.demo.producer.domain.order.OrderRepository;
import com.demo.producer.domain.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Producer 서비스 통합 테스트
 * Testcontainers를 활용한 실제 LocalStack SQS 환경에서의 테스트
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ProducerIntegrationTest {
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4"))
            .withServices(SQS)
            .withEnv("DEBUG", "1")
            .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private CreateOrderRequest validRequest;
    
    @BeforeEach
    void setUp() {
        // LocalStack 환경 변수 설정
        System.setProperty("spring.cloud.aws.sqs.endpoint", localStack.getEndpointOverride(SQS).toString());
        System.setProperty("spring.cloud.aws.credentials.access-key", localStack.getAccessKey());
        System.setProperty("spring.cloud.aws.credentials.secret-key", localStack.getSecretKey());
        System.setProperty("spring.cloud.aws.region.static", localStack.getRegion());
        
        validRequest = CreateOrderRequest.builder()
                .customerName("홍길동")
                .productName("통합테스트 상품")
                .quantity(3)
                .price(new BigDecimal("25000.00"))
                .build();
    }
    
    @Test
    @DisplayName("주문 생성부터 조회까지 전체 플로우 테스트")
    void fullOrderFlow_ShouldWorkEndToEnd() throws Exception {
        // Given
        long initialCount = orderRepository.count();
        
        // When - 주문 생성
        var createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("홍길동"))
                .andExpect(jsonPath("$.productName").value("통합테스트 상품"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.price").value(25000.00))
                .andExpect(jsonPath("$.totalAmount").value(75000.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderNumber").exists())
                .andReturn();
        
        // Then - DB에 주문이 저장되었는지 확인
        assertThat(orderRepository.count()).isEqualTo(initialCount + 1);
        
        // 생성된 주문번호 추출
        String response = createResult.getResponse().getContentAsString();
        String orderNumber = objectMapper.readTree(response).get("orderNumber").asText();
        
        // When - 생성된 주문 조회
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.customerName").value("홍길동"));
        
        // When - 고객명으로 주문 조회
        mockMvc.perform(get("/api/orders/customer/{customerName}", "홍길동"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerName").value("홍길동"));
        
        // When - 상태별 주문 조회
        mockMvc.perform(get("/api/orders/status/{status}", OrderStatus.PENDING))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
    
    @Test
    @DisplayName("여러 주문 생성 및 전체 조회 테스트")
    void multipleOrders_ShouldBeCreatedAndRetrieved() throws Exception {
        // Given
        long initialCount = orderRepository.count();
        
        CreateOrderRequest request1 = CreateOrderRequest.builder()
                .customerName("김철수")
                .productName("상품A")
                .quantity(1)
                .price(new BigDecimal("10000.00"))
                .build();
                
        CreateOrderRequest request2 = CreateOrderRequest.builder()
                .customerName("이영희")
                .productName("상품B")
                .quantity(2)
                .price(new BigDecimal("15000.00"))
                .build();
        
        // When - 첫 번째 주문 생성
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        // When - 두 번째 주문 생성
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());
        
        // Then - DB에 주문들이 저장되었는지 확인
        assertThat(orderRepository.count()).isEqualTo(initialCount + 2);
        
        // When - 전체 주문 조회
        mockMvc.perform(get("/api/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value((int) (initialCount + 2)));
    }
    
    @Test
    @DisplayName("유효하지 않은 데이터로 주문 생성시 검증 오류")
    void invalidOrderData_ShouldReturnValidationError() throws Exception {
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
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isMap());
    }
    
    @Test
    @DisplayName("존재하지 않는 주문 조회시 404 에러")
    void nonExistentOrder_ShouldReturn404() throws Exception {
        // Given
        String nonExistentOrderNumber = "ORD-99999999-999999-NOTFOUND";
        
        // When & Then
        mockMvc.perform(get("/api/orders/{orderNumber}", nonExistentOrderNumber))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("액추에이터 헬스체크 엔드포인트 테스트")
    void healthCheck_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}