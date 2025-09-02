# SQS + MySQL 마이크로서비스 데모

## 📋 개요
Producer-Consumer 아키텍처 + SQS 메시징 + 독립 MySQL DB 시스템

## 🚀 간단한 실행 방법 (2단계)

### 1단계: 인프라 실행 (LocalStack + MySQL 2개)
```bash
docker-compose up -d
```

### 2단계: 서비스 병렬 실행
```bash
./gradlew :producer-service:bootRun :consumer-service:bootRun --parallel
```

## 🧪 테스트 방법

### 주문 생성 테스트
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "김철수", 
    "productName": "테스트 상품",
    "quantity": 2,
    "price": 15000.00
  }'
```

**응답 예시:**
```json
{
  "orderNumber": "ORD-20250902-195028-82FA591D",
  "status": "PENDING"
}
```

### 처리 결과 확인 (30초 후)
```bash
ORDER_NUMBER="ORD-20250902-195028-82FA591D"

# Producer에서 주문 상태 확인
curl -s "http://localhost:8080/api/orders/$ORDER_NUMBER"

# Consumer에서 처리 결과 확인  
curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUMBER"
```

## 🏗 아키텍처

### 서비스 구조
- **Producer Service** (포트 8080) + MySQL (포트 3306)
- **Consumer Service** (포트 8081) + MySQL (포트 3307)  
- **LocalStack SQS** (포트 4566)

### 메시지 플로우
```
Producer → SQS → Consumer → 처리완료 → 동기화 이벤트
```

### 데이터베이스
- **Producer DB**: `producer_db` @ localhost:3306
- **Consumer DB**: `consumer_db` @ localhost:3307
- 각 서비스가 독립적인 MySQL 사용

## 📊 처리 시간
- **주문 생성**: 즉시
- **SQS 전송**: ~50ms
- **Consumer 처리**: ~2초 (비즈니스 로직 포함)
- **전체 플로우**: 약 2초

## 🛑 종료 방법
```bash
# Ctrl+C로 서비스 종료 후
docker-compose down
```

## 🔧 기술 스택
- **Spring Boot 3.5.3** + Java 21
- **Spring Cloud AWS SQS 3.2.1**  
- **MySQL 8.0** (각 서비스별 독립 DB)
- **LocalStack 3.4** (AWS 시뮬레이터)
- **JPA/Hibernate** (ORM)
- **Bean Validation** (입력 검증)

## 📁 패턴
- ✅ **DDD**: Domain-Driven Design
- ✅ **EDD**: Event-Driven Design  
- ✅ **CQRS**: Command Query Responsibility Segregation
- ✅ **마이크로서비스**: 독립 배포/스케일링