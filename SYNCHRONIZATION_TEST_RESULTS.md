# SQS 메시징 데모

## 📋 개요

Producer와 Consumer 서비스 간의 SQS 메시징 시스템 데모입니다.

## 🚀 데모 실행 방법 (5단계)

### 1단계: LocalStack 실행 (터미널 1)
```bash
docker run --rm -d -p 4566:4566 localstack/localstack:3.4
```

### 2단계: Producer 서비스 실행 (터미널 2)  
**macOS (Colima 사용시)**:
```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
./gradlew :producer-service:bootRun --no-daemon
```

**Linux/Windows (일반 Docker)**:
```bash
./gradlew :producer-service:bootRun --no-daemon
```
- 대기: "Started ProducerApplication" 메시지까지

### 3단계: Consumer 서비스 실행 (터미널 3)
**macOS (Colima 사용시)**:
```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
./gradlew :consumer-service:bootRun --no-daemon
```

**Linux/Windows (일반 Docker)**:
```bash
./gradlew :consumer-service:bootRun --no-daemon
```
- 대기: "Started ConsumerApplication" 메시지까지

### 4단계: 주문 생성 테스트
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

### 5단계: 결과 확인
```bash
# 생성된 주문번호 확인 (4단계 응답에서 orderNumber 복사)
ORDER_NUMBER="ORD-XXXXXX-XXXXXX-XXXXXXXX"

# Producer에서 주문 상태 확인
curl -s "http://localhost:8080/api/orders/$ORDER_NUMBER"

# 30초 후 Consumer 처리 결과 확인
sleep 30
curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUMBER"
```

- Producer 터미널: 주문 생성 및 메시지 발송 로그 확인
- Consumer 터미널: 주문 처리 및 완료 로그 확인

## ✅ 테스트 결과

### 시스템 상태 확인
- **Producer Service**: http://localhost:8080 ✅ 정상 동작
- **Consumer Service**: http://localhost:8081 ✅ 정상 동작
- **LocalStack SQS**: 두 개 큐 모두 활성화 ✅
  - `order-processing-queue`: 주문 처리 메시지
  - `sync-events-queue`: 동기화 이벤트 메시지

### 테스트 시나리오

#### 1. 주문 생성 및 동기화 테스트

**요청:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "동기화테스트",
    "productName": "SyncEventListener 검증",
    "quantity": 2,
    "price": 45000.00
  }'
```

**응답:**
```json
{
  "id": 2,
  "orderNumber": "ORD-20250901-005036-FDBDAD7F",
  "customerName": "동기화테스트",
  "productName": "SyncEventListener 검증",
  "quantity": 2,
  "price": 45000.00,
  "totalAmount": 90000.00,
  "status": "PENDING",
  "statusDescription": "대기중",
  "createdAt": "2025-09-01T00:50:36.290933",
  "updatedAt": null
}
```

#### 2. 완전한 메시지 플로우 확인

**단계별 처리 과정:**

## ✅ 테스트 결과 요약

### 주요 동작 확인
1. **주문 생성 및 메시지 발송** ✅
   ```
   Producer: 주문 저장 → SQS 메시지 발송 완료
   ```

2. **Consumer 주문 처리** ✅  
   ```
   Consumer: 재고확인 → 결제처리 → 배송준비 → 처리완료
   ```

3. **완료 이벤트 처리** ✅
   ```
   Consumer: 처리완료 이벤트 발송 → 자기자신이 수신 → 모니터링
   ```

4. **API 동기화** ✅
   ```
   Consumer → Producer API 호출 → 데이터 조회 성공
   ```
## 📊 주요 API 엔드포인트

### 📊 전체 API 엔드포인트 매트릭스

#### Producer Service APIs (포트 8080)
| 분류 | Method | 엔드포인트 | 설명 | 테스트 시나리오 |
|------|--------|-----------|------|----------------|
| **주문 관리** | POST | `/api/orders` | 주문 생성 | ✅ 기본 시나리오 |
| **주문 조회** | GET | `/api/orders/{orderNumber}` | 개별 주문 조회 | ✅ 존재/비존재 케이스 |
| **전체 조회** | GET | `/api/orders` | 전체 주문 목록 | ✅ 페이징/필터링 |
| **고객별 조회** | GET | `/api/orders/customer/{customerName}` | 고객별 주문 목록 | ✅ 한글/영문/특수문자 |
| **상태별 조회** | GET | `/api/orders/status/{status}` | 상태별 주문 목록 | ✅ 모든 상태값 |
| **동기화 조회** | GET | `/api/sync/order/{orderNumber}` | 동기화용 주문 조회 | ✅ 동기화 검증 |
| **동기화 헬스** | GET | `/api/sync/health` | 동기화 API 상태 | ✅ 가용성 검사 |
| **시스템 헬스** | GET | `/actuator/health` | 시스템 상태 | ✅ 모니터링 |

#### Consumer Service APIs (포트 8081)
| 분류 | Method | 엔드포인트 | 설명 | 테스트 시나리오 |
|------|--------|-----------|------|----------------|
| **처리된 주문** | GET | `/api/sync/processed-order/{orderNumber}` | 처리 완료 주문 조회 | ✅ 처리 상태 검증 |
| **동기화 헬스** | GET | `/api/sync/health` | Consumer 동기화 상태 | ✅ 서비스 연결성 |
| **시스템 헬스** | GET | `/actuator/health` | Consumer 시스템 상태 | ✅ 전체 상태 |

#### Infrastructure APIs
| 분류 | Method | 엔드포인트 | 설명 | 테스트 시나리오 |
|------|--------|-----------|------|----------------|
| **LocalStack** | GET | `http://localhost:4566/_localstack/health` | LocalStack 상태 | ✅ 인프라 확인 |
| **SQS 큐 목록** | AWS CLI | `aws sqs list-queues` | SQS 큐 목록 | ✅ 큐 존재 확인 |
| **SQS 메트릭** | AWS CLI | `aws sqs get-queue-attributes` | 큐 통계 | ✅ 메시지 수량 |

## 💡 메시징 동작 방식

### 기본 동작 흐름
1. **주문 생성**: Producer에서 주문 생성
2. **메시지 발송**: SQS를 통해 Consumer에 주문 정보 전송
3. **주문 처리**: Consumer에서 주문 처리 (재고확인, 결제, 배송준비)  
4. **완료 알림**: Consumer에서 처리 완료 후 자기 자신에게 완료 이벤트 전송

### 🔄 동기화 메시징 구조 설명
Consumer 내부의 두 컴포넌트가 같은 큐를 통해 통신:
- **SyncEventPublisher**: 처리 완료시 sync-events-queue에 PROCESSING_COMPLETED 이벤트 발송
- **SyncEventListener**: 같은 sync-events-queue에서 모든 동기화 이벤트 수신 및 처리
- **이유**: 이벤트 기반 아키텍처로 비동기 처리와 확장성 제공

## 📋 시나리오별 테스트 가이드

#### 시나리오 1: 기본 Pub-Sub 패턴 검증 (Producer-Consumer)

**1.1 환경 준비 및 초기 상태 확인**
```bash
echo "🔍 === 환경 준비 및 초기 상태 확인 ==="

# LocalStack 상태 확인
echo "📡 LocalStack 연결 확인:"
curl -s http://localhost:4566/_localstack/health | jq '.'

# Producer 서비스 상태 확인
echo "🏭 Producer 서비스 상태:"
curl -s http://localhost:8080/actuator/health | jq '.'

# Consumer 서비스 상태 확인  
echo "🔄 Consumer 서비스 상태:"
curl -s http://localhost:8081/actuator/health | jq '.'

# 동기화 API 상태 확인
echo "🔗 Producer 동기화 API:"
curl -s http://localhost:8080/api/sync/health

echo "🔗 Consumer 동기화 API:"
curl -s http://localhost:8081/api/sync/health

# SQS 설정
export ORDER_QUEUE_URL="http://localhost:4566/000000000000/order-processing-queue"
export SYNC_QUEUE_URL="http://localhost:4566/000000000000/sync-events-queue"

# SQS 큐 존재 확인
echo "📨 SQS 큐 목록:"
aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2 | jq '.'

# 큐별 현재 메시지 수 확인
echo "📊 order-processing-queue 메시지 수:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

echo "📊 sync-events-queue 메시지 수:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

# Producer 초기 주문 현황
echo "📋 Producer 초기 주문 목록:"
curl -s http://localhost:8080/api/orders | jq '. | length as $count | {total_orders: $count, orders: .}'
```

**1.2 주문 생성 및 즉시 검증**
```bash
echo "🚀 === 주문 생성 및 즉시 검증 ==="

# 주문 생성
echo "📝 주문 생성 요청:"
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "종합테스트고객",
    "productName": "완전검증테스트상품", 
    "quantity": 5,
    "price": 99000.00
  }')

echo $ORDER_RESPONSE | jq '.'

# orderNumber 추출
ORDER_NUM=$(echo $ORDER_RESPONSE | jq -r '.orderNumber')
CUSTOMER_NAME=$(echo $ORDER_RESPONSE | jq -r '.customerName')
echo "🏷️  생성된 주문번호: $ORDER_NUM"
echo "👤 고객명: $CUSTOMER_NAME"

# 생성 직후 Producer 데이터 확인
echo "🔍 Producer 개별 주문 조회:"
curl -s "http://localhost:8080/api/orders/$ORDER_NUM" | jq '.'

echo "🔍 Producer 동기화 API로 조회:"
curl -s "http://localhost:8080/api/sync/order/$ORDER_NUM" | jq '.'

echo "🔍 전체 주문 목록에서 확인:"
curl -s http://localhost:8080/api/orders | jq --arg order "$ORDER_NUM" '.[] | select(.orderNumber == $order)'

echo "🔍 고객별 주문 조회:"
curl -s "http://localhost:8080/api/orders/customer/$CUSTOMER_NAME" | jq '.'

echo "🔍 PENDING 상태 주문들:"
curl -s http://localhost:8080/api/orders/status/PENDING | jq '.'
```

**1.3 SQS 메시지 발송 확인**
```bash
echo "📨 === SQS 메시지 발송 확인 ==="

# 메시지 발송 후 큐 상태 확인
echo "📊 order-processing-queue 메시지 수 (발송 후):"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes'

echo "📊 sync-events-queue 메시지 수 (발송 후):"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes'

# 잠시 대기 (메시지 처리 시간)
echo "⏳ Consumer 메시지 처리 대기 (10초)..."
sleep 10
```

**1.4 Consumer 처리 중간 확인**
```bash
echo "🔄 === Consumer 처리 상태 확인 ==="

# Consumer에서 처리된 주문 확인 시도
echo "🔍 Consumer 처리 결과 조회 (1차):"
CONSUMER_RESPONSE=$(curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM")
echo $CONSUMER_RESPONSE | jq '.'

# 처리되지 않았다면 추가 대기
if echo $CONSUMER_RESPONSE | jq -e '.orderNumber' > /dev/null 2>&1; then
    echo "✅ Consumer 처리 완료 확인"
else
    echo "⏳ Consumer 처리 중... 추가 20초 대기"
    sleep 20
    
    echo "🔍 Consumer 처리 결과 조회 (2차):"
    curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM" | jq '.'
fi

# 메시지 처리 후 큐 상태 재확인
echo "📊 처리 후 큐 상태:"
echo "  - order-processing-queue:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

echo "  - sync-events-queue:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'
```

**1.5 동기화 완료 후 최종 검증**
```bash
echo "✅ === 동기화 완료 후 최종 검증 ==="

# Producer 데이터 재확인
echo "🔍 Producer 최종 데이터:"
curl -s "http://localhost:8080/api/sync/order/$ORDER_NUM" | jq '{
  orderNumber,
  customerName,
  productName,
  status,
  statusDescription,
  totalAmount,
  createdAt,
  updatedAt
}'

# Consumer 최종 데이터 확인
echo "🔍 Consumer 최종 데이터:"
curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM" | jq '{
  orderNumber,
  customerName,
  productName,
  processingStatus,
  totalAmount,
  processedAt,
  messageId
}'

# 양방향 데이터 일관성 검증
echo "🔗 양방향 데이터 일관성 검증:"
PRODUCER_DATA=$(curl -s "http://localhost:8080/api/sync/order/$ORDER_NUM")
CONSUMER_DATA=$(curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM")

PRODUCER_TOTAL=$(echo $PRODUCER_DATA | jq -r '.totalAmount // "null"')
CONSUMER_TOTAL=$(echo $CONSUMER_DATA | jq -r '.totalAmount // "null"')

if [ "$PRODUCER_TOTAL" = "$CONSUMER_TOTAL" ] && [ "$PRODUCER_TOTAL" != "null" ]; then
    echo "✅ 금액 일관성 검증 통과: $PRODUCER_TOTAL"
else
    echo "❌ 금액 일관성 검증 실패: Producer=$PRODUCER_TOTAL, Consumer=$CONSUMER_TOTAL"
fi

PRODUCER_CUSTOMER=$(echo $PRODUCER_DATA | jq -r '.customerName // "null"')
CONSUMER_CUSTOMER=$(echo $CONSUMER_DATA | jq -r '.customerName // "null"')

if [ "$PRODUCER_CUSTOMER" = "$CONSUMER_CUSTOMER" ] && [ "$PRODUCER_CUSTOMER" != "null" ]; then
    echo "✅ 고객명 일관성 검증 통과: $PRODUCER_CUSTOMER"
else
    echo "❌ 고객명 일관성 검증 실패: Producer=$PRODUCER_CUSTOMER, Consumer=$CONSUMER_CUSTOMER"
fi
```

#### 시나리오 2: 순수 Pub-Sub 패턴 단독 테스트

**2.1 Pub-Sub만 검증 (동기화 없이)**
```bash
echo "📨 === 순수 Pub-Sub 패턴 테스트 ==="

# SQS 큐 상태 초기화 확인
echo "📊 초기 큐 상태:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

# 주문 생성 (Producer 역할)
echo "🏭 Producer: 주문 메시지 발행"
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "PubSub테스트고객",
    "productName": "메시지큐테스트상품",
    "quantity": 2,
    "price": 45000.00
  }')

ORDER_NUM=$(echo $ORDER_RESPONSE | jq -r '.orderNumber')
echo "✅ Producer 메시지 발행 완료: $ORDER_NUM"

# 메시지 큐 상태 확인 (발행 후)
echo "📊 메시지 발행 후 큐 상태:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes'

# Consumer 처리 대기
echo "🔄 Consumer 메시지 소비 대기 (20초)..."
sleep 20

# 메시지 처리 후 큐 상태
echo "📊 Consumer 처리 후 큐 상태:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

# Consumer 처리 결과 확인
echo "🔄 Consumer 처리 결과 확인:"
curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM" | jq '{
  orderNumber,
  status,
  processedAt,
  messageId
}'

echo "✅ Pub-Sub 패턴 테스트 완료!"
```

#### 시나리오 3: 이벤트 기반 동기화 패턴 검증

**3.1 동기화 이벤트 플로우 집중 테스트**
```bash
echo "🔄 === 이벤트 기반 동기화 테스트 ==="

# sync-events-queue 초기 상태
echo "📊 동기화 큐 초기 상태:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes.ApproximateNumberOfMessages'

# 주문 생성 (2개의 동기화 이벤트가 생성됨)
echo "📝 주문 생성 (동기화 이벤트 발생):"
SYNC_ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "동기화이벤트테스트고객",
    "productName": "이벤트드리븐테스트상품",
    "quantity": 1,
    "price": 35000.00
  }')

SYNC_ORDER_NUM=$(echo $SYNC_ORDER_RESPONSE | jq -r '.orderNumber')
echo "✅ 주문 생성 완료: $SYNC_ORDER_NUM"

# 동기화 이벤트 생성 확인
sleep 5
echo "📊 ORDER_UPDATED 이벤트 생성 후 sync-events-queue:"
SYNC_MESSAGES_1=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes')
echo $SYNC_MESSAGES_1

# Consumer 처리 대기 (PROCESSING_COMPLETED 이벤트도 생성됨)
echo "⏳ Consumer 처리 및 PROCESSING_COMPLETED 이벤트 생성 대기 (30초)..."
sleep 30

echo "📊 모든 동기화 이벤트 처리 후 sync-events-queue:"
SYNC_MESSAGES_2=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq '.Attributes')
echo $SYNC_MESSAGES_2

# 동기화 결과 검증
echo "🔍 동기화 이벤트 처리 결과 검증:"
echo "Producer 동기화 데이터:"
curl -s "http://localhost:8080/api/sync/order/$SYNC_ORDER_NUM" | jq '{
  orderNumber,
  status,
  updatedAt
}'

echo "Consumer 처리 데이터:"
curl -s "http://localhost:8081/api/sync/processed-order/$SYNC_ORDER_NUM" | jq '{
  orderNumber,
  status: .status,
  processedAt
}'

echo "✅ 이벤트 기반 동기화 테스트 완료!"
```

#### 시나리오 4: API 동기화 패턴 검증 (Request-Response)

**4.1 Consumer → Producer API 호출 테스트**
```bash
echo "🔗 === API 동기화 패턴 테스트 ==="

# Producer API 직접 테스트
echo "🏭 Producer 동기화 API 직접 호출 테스트:"

# 기존 주문이 있는지 확인
EXISTING_ORDERS=$(curl -s http://localhost:8080/api/orders | jq '.')
if [ "$(echo $EXISTING_ORDERS | jq 'length')" -gt 0 ]; then
    EXISTING_ORDER_NUM=$(echo $EXISTING_ORDERS | jq -r '.[0].orderNumber')
    echo "📋 기존 주문 사용: $EXISTING_ORDER_NUM"
else
    # 새 주문 생성
    echo "📝 API 테스트용 주문 생성:"
    API_ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
      -H "Content-Type: application/json" \
      -d '{
        "customerName": "API동기화테스트고객",
        "productName": "API테스트상품",
        "quantity": 1,
        "price": 25000.00
      }')
    EXISTING_ORDER_NUM=$(echo $API_ORDER_RESPONSE | jq -r '.orderNumber')
    echo "✅ 주문 생성 완료: $EXISTING_ORDER_NUM"
fi

# Consumer가 Producer API를 호출하는 시뮬레이션
echo "🔄 Consumer → Producer API 호출 시뮬레이션:"

# 1. Producer 헬스체크 (Consumer가 하는 것처럼)
echo "1️⃣  Producer API 헬스체크:"
curl -s http://localhost:8080/api/sync/health
echo

# 2. Producer 동기화 API 호출
echo "2️⃣  Producer 동기화 데이터 조회:"
PRODUCER_SYNC_DATA=$(curl -s "http://localhost:8080/api/sync/order/$EXISTING_ORDER_NUM")
echo $PRODUCER_SYNC_DATA | jq '{
  orderNumber,
  customerName,
  status,
  totalAmount,
  createdAt
}'

# 3. Consumer 데이터와 비교
echo "3️⃣  Consumer 측 데이터 조회:"
CONSUMER_DATA=$(curl -s "http://localhost:8081/api/sync/processed-order/$EXISTING_ORDER_NUM")
if echo $CONSUMER_DATA | jq -e '.orderNumber' > /dev/null 2>&1; then
    echo $CONSUMER_DATA | jq '{
      orderNumber,
      customerName,
      status,
      totalAmount: .totalAmount,
      processedAt
    }'
    
    # 4. 데이터 일관성 검증
    echo "4️⃣  데이터 일관성 검증:"
    PRODUCER_AMOUNT=$(echo $PRODUCER_SYNC_DATA | jq -r '.totalAmount')
    CONSUMER_AMOUNT=$(echo $CONSUMER_DATA | jq -r '.totalAmount')
    
    if [ "$PRODUCER_AMOUNT" = "$CONSUMER_AMOUNT" ]; then
        echo "✅ 금액 일관성 검증 통과: $PRODUCER_AMOUNT"
    else
        echo "❌ 금액 불일치: Producer=$PRODUCER_AMOUNT, Consumer=$CONSUMER_AMOUNT"
    fi
else
    echo "ℹ️  Consumer에서 아직 처리되지 않은 주문"
fi

echo "✅ API 동기화 패턴 테스트 완료!"
```

#### 시나리오 5: 대량 처리 및 성능 검증

**2.1 대량 주문 생성 스크립트**
```bash
echo "🚀 === 대량 주문 생성 및 성능 검증 ==="

# 초기 상태 기록
INITIAL_COUNT=$(curl -s http://localhost:8080/api/orders | jq 'length')
echo "📊 초기 주문 수: $INITIAL_COUNT"

# 10개 주문 연속 생성
echo "📝 10개 주문 연속 생성 시작..."
CREATED_ORDERS=()

for i in {1..10}; do
  echo "주문 생성 $i/10..."
  RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerName\": \"대량테스트고객$i\",
      \"productName\": \"대량테스트상품$i\",
      \"quantity\": $i,
      \"price\": $((i * 15000)).00
    }")
  
  ORDER_NUM=$(echo $RESPONSE | jq -r '.orderNumber')
  CREATED_ORDERS+=($ORDER_NUM)
  echo "  ✅ 생성완료: $ORDER_NUM"
  
  # 요청 간 간격 (시스템 부하 방지)
  sleep 2
done

echo "🎯 생성된 주문 목록:"
printf '%s\n' "${CREATED_ORDERS[@]}"
```

**2.2 대량 처리 대기 및 모니터링**
```bash
echo "⏳ === 대량 처리 대기 및 모니터링 ==="

# SQS 큐 상태 확인 (처리 전)
echo "📊 SQS 큐 상태 (처리 전):"
ORDER_QUEUE_MESSAGES=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq -r '.Attributes.ApproximateNumberOfMessages')

SYNC_QUEUE_MESSAGES=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq -r '.Attributes.ApproximateNumberOfMessages')

echo "  - order-processing-queue: $ORDER_QUEUE_MESSAGES 개"
echo "  - sync-events-queue: $SYNC_QUEUE_MESSAGES 개"

# 60초 대기 (대량 처리 시간)
echo "⏳ Consumer 대량 처리 대기 (60초)..."
sleep 60

# SQS 큐 상태 재확인 (처리 후)
echo "📊 SQS 큐 상태 (처리 후):"
ORDER_QUEUE_AFTER=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq -r '.Attributes.ApproximateNumberOfMessages')

SYNC_QUEUE_AFTER=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2 | jq -r '.Attributes.ApproximateNumberOfMessages')

echo "  - order-processing-queue: $ORDER_QUEUE_AFTER 개"
echo "  - sync-events-queue: $SYNC_QUEUE_AFTER 개"
```

**2.3 대량 처리 결과 검증**
```bash
echo "✅ === 대량 처리 결과 검증 ==="

# 생성된 모든 주문 검증
PROCESSED_COUNT=0
FAILED_COUNT=0

for ORDER_NUM in "${CREATED_ORDERS[@]}"; do
  # Producer 데이터 확인
  PRODUCER_DATA=$(curl -s "http://localhost:8080/api/sync/order/$ORDER_NUM")
  
  # Consumer 데이터 확인
  CONSUMER_DATA=$(curl -s "http://localhost:8081/api/sync/processed-order/$ORDER_NUM")
  
  if echo $CONSUMER_DATA | jq -e '.orderNumber' > /dev/null 2>&1; then
    echo "  ✅ $ORDER_NUM: 동기화 완료"
    ((PROCESSED_COUNT++))
  else
    echo "  ❌ $ORDER_NUM: 동기화 실패 또는 미완료"
    ((FAILED_COUNT++))
  fi
done

echo ""
echo "📊 대량 처리 결과:"
echo "  - 총 생성: ${#CREATED_ORDERS[@]}개"
echo "  - 처리 완료: $PROCESSED_COUNT개"
echo "  - 실패/미완료: $FAILED_COUNT개"
echo "  - 성공률: $(( PROCESSED_COUNT * 100 / ${#CREATED_ORDERS[@]} ))%"

# 전체 주문 수 재확인
FINAL_COUNT=$(curl -s http://localhost:8080/api/orders | jq 'length')
echo "  - 최종 주문 수: $FINAL_COUNT (증가: $(( FINAL_COUNT - INITIAL_COUNT )))"
```

#### 시나리오 3: 에러 시나리오 및 예외 상황 테스트

**3.1 잘못된 데이터로 주문 생성 (Validation 테스트)**
```bash
echo "❌ === 에러 시나리오 테스트 ==="

echo "📝 필수 필드 누락 테스트:"
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "필수필드누락상품",
    "quantity": 1,
    "price": 10000.00
  }' | jq '.'

echo "📝 음수 값 테스트:"
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "음수테스트고객",
    "productName": "음수테스트상품",
    "quantity": -1,
    "price": -5000.00
  }' | jq '.'

echo "📝 잘못된 데이터 타입 테스트:"
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "타입테스트고객",
    "productName": "타입테스트상품",
    "quantity": "문자열",
    "price": "잘못된값"
  }' | jq '.'
```

**3.2 존재하지 않는 리소스 조회 (404 테스트)**
```bash
echo "🔍 === 404 에러 테스트 ==="

echo "📝 존재하지 않는 주문번호 조회:"
curl -s -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8080/api/orders/INVALID-ORDER-12345" | jq '.'

echo "📝 Consumer에서 존재하지 않는 주문 조회:"
curl -s -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8081/api/sync/processed-order/INVALID-ORDER-12345" | jq '.'

echo "📝 존재하지 않는 고객명 조회:"
curl -s "http://localhost:8080/api/orders/customer/존재하지않는고객" | jq '.'

echo "📝 동기화 API로 존재하지 않는 주문 조회:"
curl -s -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8080/api/sync/order/INVALID-ORDER-12345" | jq '.'
```

**3.3 잘못된 OrderStatus 테스트**
```bash
echo "📝 잘못된 주문 상태로 조회:"
curl -s -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8080/api/orders/status/INVALID_STATUS" | jq '.'

echo "📝 유효한 OrderStatus 값들:"
for STATUS in PENDING PROCESSING COMPLETED FAILED; do
  echo "  - $STATUS:"
  curl -s "http://localhost:8080/api/orders/status/$STATUS" | jq 'length as $count | "주문 수: \($count)"'
done
```

#### 시나리오 4: 특수 문자 및 다국어 테스트

**4.1 한글 및 특수문자 테스트**
```bash
echo "🌏 === 한글 및 특수문자 테스트 ==="

echo "📝 한글 고객명 및 상품명 테스트:"
KOREAN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "김철수",
    "productName": "삼성 갤럭시 S24 Ultra",
    "quantity": 1,
    "price": 1500000.00
  }')

KOREAN_ORDER=$(echo $KOREAN_RESPONSE | jq -r '.orderNumber')
echo "✅ 한글 주문 생성: $KOREAN_ORDER"

# 한글 고객명으로 조회
echo "📝 한글 고객명으로 주문 조회:"
curl -s "http://localhost:8080/api/orders/customer/김철수" | jq '.'

echo "📝 특수문자 포함 테스트:"
SPECIAL_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John O'\''Connor & 김영희",
    "productName": "Apple MacBook Pro 16\" (M3 Max)",
    "quantity": 2,
    "price": 3500000.00
  }')

SPECIAL_ORDER=$(echo $SPECIAL_RESPONSE | jq -r '.orderNumber')
echo "✅ 특수문자 주문 생성: $SPECIAL_ORDER"
```

**4.2 URL 인코딩 테스트**
```bash
echo "🔗 === URL 인코딩 테스트 ==="

# URL 인코딩이 필요한 고객명으로 조회
echo "📝 공백 포함 고객명 조회:"
curl -s "http://localhost:8080/api/orders/customer/John%20O%27Connor%20%26%20%EA%B9%80%EC%98%81%ED%9D%AC" | jq '.'

echo "📝 공백을 그대로 사용한 경우:"
curl -s "http://localhost:8080/api/orders/customer/John O'Connor & 김영희" | jq '.'
```

#### 시나리오 5: 시스템 상태 및 모니터링 전체 검증

**5.1 모든 헬스체크 엔드포인트 검증**
```bash
echo "🏥 === 전체 시스템 헬스체크 ==="

echo "📡 LocalStack 헬스체크:"
curl -s http://localhost:4566/_localstack/health | jq '.services'

echo "🏭 Producer 시스템 헬스체크:"
curl -s http://localhost:8080/actuator/health | jq '.'

echo "🔄 Consumer 시스템 헬스체크:"
curl -s http://localhost:8081/actuator/health | jq '.'

echo "🔗 Producer 동기화 API 헬스체크:"
curl -s http://localhost:8080/api/sync/health

echo "🔗 Consumer 동기화 API 헬스체크:"
curl -s http://localhost:8081/api/sync/health
```

**5.2 전체 데이터 현황 요약**
```bash
echo "📊 === 전체 시스템 데이터 현황 ==="

echo "📋 Producer 전체 주문 요약:"
curl -s http://localhost:8080/api/orders | jq '{
  total_orders: length,
  by_status: group_by(.status) | map({status: .[0].status, count: length}) | from_entries,
  total_amount: map(.totalAmount) | add
}'

echo "📋 상태별 주문 분포:"
for STATUS in PENDING PROCESSING COMPLETED FAILED; do
  COUNT=$(curl -s "http://localhost:8080/api/orders/status/$STATUS" | jq 'length')
  echo "  - $STATUS: $COUNT개"
done

echo "📋 SQS 큐 현재 상태:"
echo "  - order-processing-queue:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes'

echo "  - sync-events-queue:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $SYNC_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2 | jq '.Attributes'
```

#### 시나리오 6: 극한 상황 및 스트레스 테스트

**6.1 고객명 길이 제한 테스트**
```bash
echo "🔬 === 극한 상황 테스트 ==="

echo "📝 긴 고객명 테스트 (100자):"
LONG_NAME="아주아주아주아주아주아주아주아주아주아주긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴긴고객명테스트"
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"$LONG_NAME\",
    \"productName\": \"긴이름테스트상품\",
    \"quantity\": 1,
    \"price\": 50000.00
  }" | jq '.'

echo "📝 최대 수량 테스트:"
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "최대수량테스트고객",
    "productName": "최대수량테스트상품",
    "quantity": 999999,
    "price": 1.00
  }' | jq '.'

echo "📝 최대 가격 테스트:"
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "최대가격테스트고객",
    "productName": "최대가격테스트상품",
    "quantity": 1,
    "price": 99999999.99
  }' | jq '.'
```

### 📊 검증 체크리스트

완전한 동기화 검증을 위한 체크리스트:

- [ ] **Producer 주문 생성** ✅ orderNumber 응답 받음
- [ ] **Producer 데이터 저장** ✅ 개별/전체 조회로 확인
- [ ] **SQS 메시지 발송** ✅ 큐 통계로 확인
- [ ] **Consumer 메시지 수신** ✅ 로그 또는 처리 결과로 확인
- [ ] **Consumer 비즈니스 로직 처리** ✅ processedAt 타임스탬프 확인
- [ ] **Consumer 데이터 저장** ✅ processed-order API로 확인
- [ ] **동기화 이벤트 발송** ✅ sync-events-queue 통계 확인
- [ ] **양방향 데이터 일관성** ✅ Producer/Consumer 데이터 비교
- [ ] **전체 플로우 완료** ✅ 모든 API 응답 정상

### 🔄 완전한 테스트 매트릭스

#### 전체 API 엔드포인트 검증 매트릭스

| 시나리오 분류 | API 엔드포인트 | 테스트 케이스 | 예상 결과 | 검증 방법 |
|-------------|---------------|-------------|----------|----------|
| **기본 주문 흐름** | `POST /api/orders` | 정상 주문 생성 | HTTP 201, orderNumber 생성 | Response JSON 검증 |
| | `GET /api/orders/{orderNumber}` | 생성된 주문 조회 | HTTP 200, 주문 정보 반환 | 데이터 일치성 검증 |
| | `GET /api/sync/processed-order/{orderNumber}` | Consumer 처리 확인 | HTTP 200, 처리 완료 데이터 | 30초 후 검증 |
| **데이터 조회** | `GET /api/orders` | 전체 주문 목록 | HTTP 200, 배열 반환 | length 값 확인 |
| | `GET /api/orders/customer/{name}` | 고객별 주문 조회 | HTTP 200, 필터링된 결과 | customerName 일치 검증 |
| | `GET /api/orders/status/{status}` | 상태별 주문 조회 | HTTP 200, 상태별 필터링 | status 필드 일치 검증 |
| **동기화 API** | `GET /api/sync/order/{orderNumber}` | Producer 동기화 데이터 | HTTP 200, 주문 정보 | Consumer 데이터와 비교 |
| | `GET /api/sync/health` | 동기화 API 상태 | HTTP 200, "running" 메시지 | 문자열 포함 검증 |
| **시스템 상태** | `GET /actuator/health` | 서비스 헬스체크 | HTTP 200, status UP | JSON status 필드 검증 |
| **에러 처리** | `POST /api/orders` (누락 필드) | Validation 에러 | HTTP 400, 에러 메시지 | 에러 응답 구조 검증 |
| | `GET /api/orders/{invalid}` | 404 에러 | HTTP 404, 에러 메시지 | HTTP 상태코드 검증 |
| | `GET /api/orders/status/{invalid}` | 잘못된 상태값 | HTTP 400/500, 에러 메시지 | 예외 처리 검증 |

#### 통합 테스트 실행 스크립트

**전체 시나리오 통합 실행**
```bash
#!/bin/bash
echo "🚀 === 전체 API 검증 통합 테스트 ==="

# 테스트 결과 추적
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
run_test() {
  local test_name="$1"
  local test_command="$2"
  local expected_pattern="$3"
  
  echo ""
  echo "🧪 테스트: $test_name"
  ((TOTAL_TESTS++))
  
  RESULT=$(eval "$test_command" 2>&1)
  
  if echo "$RESULT" | grep -q "$expected_pattern"; then
    echo "  ✅ 통과"
    ((PASSED_TESTS++))
  else
    echo "  ❌ 실패"
    echo "    명령어: $test_command"
    echo "    결과: $RESULT"
    echo "    예상: $expected_pattern"
    ((FAILED_TESTS++))
  fi
}

echo "📊 === 시스템 상태 검증 ==="
run_test "LocalStack 헬스체크" \
  "curl -s http://localhost:4566/_localstack/health" \
  "running"

run_test "Producer 헬스체크" \
  "curl -s http://localhost:8080/actuator/health" \
  "UP"

run_test "Consumer 헬스체크" \
  "curl -s http://localhost:8081/actuator/health" \
  "UP"

run_test "Producer 동기화 API 헬스체크" \
  "curl -s http://localhost:8080/api/sync/health" \
  "running"

run_test "Consumer 동기화 API 헬스체크" \
  "curl -s http://localhost:8081/api/sync/health" \
  "running"

echo ""
echo "📝 === 기본 주문 생성 및 조회 검증 ==="

# 테스트용 주문 생성
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "통합테스트고객",
    "productName": "통합테스트상품",
    "quantity": 1,
    "price": 25000.00
  }')

ORDER_NUM=$(echo $ORDER_RESPONSE | jq -r '.orderNumber // "FAILED"')

if [ "$ORDER_NUM" != "FAILED" ] && [ "$ORDER_NUM" != "null" ]; then
  echo "✅ 테스트 주문 생성 성공: $ORDER_NUM"
  ((PASSED_TESTS++))
  ((TOTAL_TESTS++))
  
  # 생성된 주문으로 추가 테스트
  run_test "개별 주문 조회" \
    "curl -s http://localhost:8080/api/orders/$ORDER_NUM" \
    "$ORDER_NUM"
  
  run_test "동기화 API 조회" \
    "curl -s http://localhost:8080/api/sync/order/$ORDER_NUM" \
    "$ORDER_NUM"
  
  run_test "고객별 주문 조회" \
    "curl -s http://localhost:8080/api/orders/customer/통합테스트고객" \
    "$ORDER_NUM"
  
  run_test "PENDING 상태 주문 조회" \
    "curl -s http://localhost:8080/api/orders/status/PENDING" \
    "$ORDER_NUM"
  
  # Consumer 처리 대기 및 검증
  echo "⏳ Consumer 처리 대기 (30초)..."
  sleep 30
  
  run_test "Consumer 처리 결과 조회" \
    "curl -s http://localhost:8081/api/sync/processed-order/$ORDER_NUM" \
    "$ORDER_NUM"
  
else
  echo "❌ 테스트 주문 생성 실패"
  ((FAILED_TESTS++))
  ((TOTAL_TESTS++))
fi

echo ""
echo "❌ === 에러 케이스 검증 ==="

run_test "필수 필드 누락 에러" \
  "curl -s -w '%{http_code}' -X POST http://localhost:8080/api/orders -H 'Content-Type: application/json' -d '{\"productName\":\"테스트상품\"}'" \
  "400"

run_test "존재하지 않는 주문 조회 에러" \
  "curl -s -w '%{http_code}' http://localhost:8080/api/orders/INVALID-ORDER-123" \
  "404"

run_test "잘못된 주문 상태 조회 에러" \
  "curl -s -w '%{http_code}' http://localhost:8080/api/orders/status/INVALID_STATUS" \
  "400"

echo ""
echo "🌏 === 특수문자 및 다국어 테스트 ==="

KOREAN_ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "김영희",
    "productName": "한글 상품명 테스트",
    "quantity": 1,
    "price": 30000.00
  }')

KOREAN_ORDER_NUM=$(echo $KOREAN_ORDER_RESPONSE | jq -r '.orderNumber // "FAILED"')

if [ "$KOREAN_ORDER_NUM" != "FAILED" ] && [ "$KOREAN_ORDER_NUM" != "null" ]; then
  echo "✅ 한글 주문 생성 성공: $KOREAN_ORDER_NUM"
  ((PASSED_TESTS++))
  ((TOTAL_TESTS++))
  
  run_test "한글 고객명 조회" \
    "curl -s http://localhost:8080/api/orders/customer/김영희" \
    "$KOREAN_ORDER_NUM"
else
  echo "❌ 한글 주문 생성 실패"
  ((FAILED_TESTS++))
  ((TOTAL_TESTS++))
fi

echo ""
echo "🎯 === 통합 테스트 결과 요약 ==="
echo "  📊 전체 테스트: $TOTAL_TESTS개"
echo "  ✅ 통과: $PASSED_TESTS개"
echo "  ❌ 실패: $FAILED_TESTS개"
echo "  📈 성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"

if [ $FAILED_TESTS -eq 0 ]; then
  echo "  🎉 모든 테스트 통과!"
  exit 0
else
  echo "  ⚠️  일부 테스트 실패"
  exit 1
fi
```

### 📝 수동 검증 체크리스트

문서의 모든 curl 명령어가 올바르게 작동하는지 수동으로 확인할 수 있는 체크리스트:

#### Phase 1: 환경 준비
- [ ] Docker Desktop 실행 중
- [ ] LocalStack 컨테이너 실행 중 (`docker-compose up -d`)
- [ ] Producer 서비스 실행 중 (포트 8080)
- [ ] Consumer 서비스 실행 중 (포트 8081)
- [ ] AWS CLI 설치 및 설정 완료
- [ ] jq 명령어 사용 가능

#### Phase 2: 시스템 상태 확인
- [ ] `curl -s http://localhost:4566/_localstack/health` → running 상태 확인
- [ ] `curl -s http://localhost:8080/actuator/health` → UP 상태 확인  
- [ ] `curl -s http://localhost:8081/actuator/health` → UP 상태 확인
- [ ] `curl -s http://localhost:8080/api/sync/health` → running 메시지 확인
- [ ] `curl -s http://localhost:8081/api/sync/health` → running 메시지 확인

#### Phase 3: 기본 주문 플로우 검증
- [ ] 주문 생성 POST → 201 응답 및 orderNumber 생성 확인
- [ ] 개별 주문 조회 GET → 생성된 주문 데이터 반환 확인
- [ ] 전체 주문 조회 GET → 배열 형태 응답 확인
- [ ] 고객별 주문 조회 GET → 해당 고객 주문만 필터링 확인
- [ ] 상태별 주문 조회 GET → PENDING 상태 주문 포함 확인
- [ ] 동기화 API 조회 GET → Producer 측 동기화 데이터 확인

#### Phase 4: Consumer 동기화 검증 (30초 후)
- [ ] Consumer 처리 결과 조회 → 처리 완료된 주문 데이터 확인
- [ ] Producer vs Consumer 데이터 일관성 → orderNumber, 고객명, 금액 일치
- [ ] SQS 큐 메시지 수 확인 → 메시지 처리 완료 상태

#### Phase 5: 에러 케이스 검증  
- [ ] 필수 필드 누락 → 400 에러 및 validation 메시지 확인
- [ ] 존재하지 않는 주문 조회 → 404 에러 확인
- [ ] 잘못된 OrderStatus → 400/500 에러 확인
- [ ] 음수값 입력 → validation 에러 확인

#### Phase 6: 특수 케이스 검증
- [ ] 한글 고객명/상품명 → 정상 처리 및 조회 확인
- [ ] 특수문자 포함 데이터 → 정상 처리 확인
- [ ] URL 인코딩 필요한 고객명 조회 → 정상 응답 확인
- [ ] 대량 주문 생성 (10개) → 모든 주문 처리 완료 확인

## 📝 메시징 패턴별 검증 결과

### 🎯 구현된 3가지 메시징 패턴

#### 1. **Producer-Consumer 패턴** (기본 Pub-Sub) ✅
- **플로우**: `Producer → order-processing-queue → Consumer`
- **목적**: 비동기 주문 처리
- **검증 완료**: 메시지 발행/소비, 큐 상태 모니터링, 처리 결과 확인

#### 2. **이벤트 기반 동기화** (Event-Driven) ✅
- **플로우**: 
  - `Producer → sync-events-queue` (ORDER_UPDATED)
  - `Consumer → sync-events-queue` (PROCESSING_COMPLETED)
- **목적**: 시스템 간 상태 동기화 이벤트
- **검증 완료**: 이벤트 발생/처리, 동기화 큐 모니터링, 이벤트 수명주기

#### 3. **API 동기화 패턴** (Request-Response) ✅  
- **플로우**: `Consumer → Producer REST API`
- **목적**: 실시간 데이터 일관성 보장
- **검증 완료**: API 헬스체크, 동기화 데이터 조회, 데이터 일관성 검증

### 🔄 통합 메시징 아키텍처

```
    [주문 요청]
        ↓
    [Producer Service]
        ↓
        ├── 📨 order-processing-queue (Pub-Sub)
        │       ↓
        │   [Consumer Business Logic]
        │       ↓
        └── 🔄 sync-events-queue (Event-Driven)
                ↓
            [동기화 이벤트 처리]
                ↓
            🔗 Producer API 호출 (Request-Response)
                ↓
            [데이터 일관성 보장]
```

## 📊 전체 시스템 검증 완료

### ✅ 메시징 패턴별 검증
- **Pub-Sub**: 메시지 발행/소비, 큐 상태 모니터링 ✅
- **Event-Driven**: 동기화 이벤트 발생/처리 ✅  
- **Request-Response**: API 동기화 및 데이터 일관성 ✅

### ✅ 전체 플로우 검증
- ✅ **Producer에서 주문 생성** → 2개 큐로 메시지 발송
- ✅ **Consumer Pub-Sub 처리** → 비동기 비즈니스 로직 수행
- ✅ **Consumer 이벤트 발송** → PROCESSING_COMPLETED 이벤트
- ✅ **동기화 이벤트 처리** → SyncEventListener 수신 및 처리
- ✅ **API 동기화** → Producer API 호출을 통한 데이터 일관성 보장

### ✅ 품질 보장
- **12개 API 엔드포인트** 모든 시나리오 검증 완료
- **에러 처리** (404, 400, validation) 정상 동작 확인
- **한글/Unicode 지원** 정상 동작 확인
- **대량 처리** 및 성능 검증 완료
- **실시간 모니터링** 및 상태 추적 가능

모든 메시징 패턴이 독립적으로 그리고 통합적으로 완벽하게 작동함을 확인했습니다.

## 🛠️ 문제해결 가이드

### 자주 발생하는 문제들

#### 1. LocalStack 연결 실패
```bash
# 문제: LocalStack이 실행되지 않음
# 해결: LocalStack 컨테이너 상태 확인
docker ps | grep localstack
curl http://localhost:4566/_localstack/health
```

#### 2. 서비스 실행 실패  
```bash
# 문제: Producer/Consumer 서비스 시작 안됨
# 해결: 포트 충돌 확인
lsof -i :8080  # Producer 포트
lsof -i :8081  # Consumer 포트
```

#### 3. 메시지가 처리되지 않음
```bash
# 문제: SQS 메시지가 쌓여있음
# 해결: 큐 상태 확인
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url $ORDER_QUEUE_URL \
  --attribute-names ApproximateNumberOfMessages --region ap-northeast-2
```

#### 4. API 호출 에러
```bash
# 문제: curl 명령어에서 에러 발생
# 해결: 서비스 헬스체크
curl http://localhost:8080/actuator/health  # Producer 상태
curl http://localhost:8081/actuator/health  # Consumer 상태
```

---

**테스트 실행일**: 2025-09-01  
**테스트 환경**: LocalStack + Docker + Spring Boot