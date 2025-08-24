# LocalStack SQS 메시지 큐 시스템

> **LocalStack을 활용한 SQS 메시지 큐 데모 프로젝트**  
> **상태**: ✅ 모든 구성 요소 정상 작동 검증 완료  
> **작성일**: 2025년 8월 24일  

## 📋 프로젝트 개요

이 프로젝트는 LocalStack을 사용하여 AWS SQS(Simple Queue Service)를 로컬 환경에서 시뮬레이션하는 완전한 메시지 큐 시스템입니다.

### 시스템 구성
- **Producer Service**: 주문 메시지를 SQS로 전송 (포트 8080)
- **Consumer Service**: SQS에서 메시지를 수신하여 처리 (포트 8081)
- **LocalStack**: AWS SQS 서비스 시뮬레이션 (포트 4566)
- **Docker**: 컨테이너 기반 실행 환경

### 메시지 플로우
```
[주문 API] → [Producer] → [SQS Queue] → [Consumer] → [주문 처리]
```

## 🚀 빠른 시작

### 원클릭 테스트 (추천)
```bash
# 전체 시스템 자동 테스트
./test-sqs-system.sh

# 시스템 종료
./stop-sqs-system.sh
```

### 수동 시작

### 1. 시스템 시작
```bash
# 프로젝트 디렉토리로 이동
cd /path/to/demo

# 전체 시스템 시작
docker-compose up -d
```

### 2. 상태 확인
```bash
# LocalStack 상태 확인
curl http://localhost:4566/_localstack/health

# SQS 큐 목록 확인
aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2
```

### 3. 테스트 메시지 전송
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderNumber":"ORDER-001", "productName":"노트북", "quantity":1}'
```

### 4. 처리 결과 확인
```bash
# Consumer 로그 확인
docker logs consumer-service
```

## 🏗️ 시스템 아키텍처

### 주요 구성 요소

| 구성요소 | 역할 | 기술스택 | 상태 |
|----------|------|----------|------|
| **LocalStack** | AWS 서비스 시뮬레이션 | LocalStack 3.4 | ✅ 정상 |
| **Producer Service** | 메시지 송신 | Spring Boot 3.5.3 | ✅ 정상 |
| **Consumer Service** | 메시지 수신/처리 | Spring Boot 3.5.3 + Spring Cloud AWS | ✅ 정상 |
| **SQS Queues** | 메시지 대기열 | order-processing-queue + DLQ | ✅ 정상 |

### 생성된 큐
- **order-processing-queue**: 메인 처리 큐
- **order-processing-dlq**: 데드 레터 큐 (실패 메시지용, maxReceiveCount=3)

## 📖 SQS 기본 개념

### SQS란?
**SQS(Simple Queue Service)**는 메시지 대기열 서비스로, 시스템 간 비동기 통신을 위한 AWS 서비스입니다.

**실생활 비유**: 은행 번호표 시스템
- **고객** = Producer (메시지 발송자)
- **번호표 발급기** = SQS Queue
- **은행 직원** = Consumer (메시지 처리자)

### 주요 특징
1. **안정성**: 메시지 손실 방지
2. **확장성**: 대량 메시지 처리 가능
3. **분리**: 시스템 간 느슨한 결합
4. **순서**: FIFO 방식 지원

## 🛠️ 개발 가이드

### 환경 요구사항
- Docker Desktop
- AWS CLI
- Java 21 이상
- Gradle

### 로컬 개발 설정
```bash
# 개발용 컨테이너 시작
docker-compose up -d localstack

# Producer 서비스 빌드 및 실행
cd producer-service
./gradlew bootRun

# Consumer 서비스 빌드 및 실행
cd consumer-service
./gradlew bootRun
```

### API 엔드포인트
#### Producer Service (포트 8080)
- `POST /api/orders` - 주문 생성
- `GET /actuator/health` - 헬스체크

#### Consumer Service (포트 8081)
- `GET /actuator/health` - 헬스체크
- 자동 메시지 폴링 (10초 간격)

## 🔧 문제 해결

### 일반적인 문제

#### 1. 시스템 시작 실패
```bash
# 기존 컨테이너 정리
docker-compose down -v

# 재시작
docker-compose up -d
```

#### 2. 메시지 처리 지연
- **원인**: Consumer 폴링 주기 (10초)
- **확인**: 큐에 대기 중인 메시지 수 체크

```bash
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url "http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/order-processing-queue" \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2
```

#### 3. 메시지가 DLQ로 이동
```bash
# DLQ 메시지 수 확인
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url "http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/order-processing-dlq" \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2

# Consumer 에러 로그 확인
docker logs consumer-service | grep ERROR
```

### 로그 모니터링
```bash
# 실시간 로그 확인
docker logs -f producer-service    # Producer 로그
docker logs -f consumer-service    # Consumer 로그
docker logs -f localstack          # LocalStack 로그
```

## 🧪 테스트 가이드

### 기본 테스트
```bash
# 단일 메시지 테스트
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderNumber":"TEST-001", "productName":"테스트 상품", "quantity":1}'
```

### 부하 테스트
```bash
# 연속 메시지 전송 (50개)
for i in {1..50}
do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{\"orderNumber\":\"LOAD-$i\", \"productName\":\"상품 $i\", \"quantity\":$(($i % 10 + 1))}"
  echo "메시지 $i 전송 완료"
done
```

### 장애 시뮬레이션
```bash
# Consumer 중단
docker stop consumer-service

# 메시지 전송 (큐에 축적됨)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderNumber":"FAIL-TEST", "productName":"장애 테스트", "quantity":1}'

# Consumer 재시작
docker start consumer-service
```

## 📊 모니터링

### 시스템 상태 확인
```bash
# 전체 서비스 헬스체크
curl http://localhost:4566/_localstack/health    # LocalStack
curl http://localhost:8080/actuator/health       # Producer
curl http://localhost:8081/actuator/health       # Consumer
```

### SQS 메트릭
```bash
# 큐 통계 확인
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url "http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/order-processing-queue" \
  --attribute-names All \
  --region ap-northeast-2
```

## 🚀 실무 적용

실무 환경에 적용하고자 할 때는 [`LocalStack_SQS_실무_적용_가이드라인.md`](LocalStack_SQS_실무_적용_가이드라인.md)를 참조하세요.

주요 고려사항:
- AWS 실제 환경 설정
- 보안 강화 (IAM, VPC, 암호화)
- 성능 최적화
- 모니터링 및 알림
- CI/CD 파이프라인

## 📖 기술 용어집

| 용어 | 설명 |
|------|------|
| **Message Queue** | 메시지를 임시 저장하는 대기열, 시스템 간 비동기 통신 |
| **Producer** | 메시지를 생성하여 큐에 전송하는 애플리케이션 |
| **Consumer** | 큐에서 메시지를 수신하여 처리하는 애플리케이션 |
| **Dead Letter Queue** | 처리 실패한 메시지를 저장하는 특별한 큐 |
| **Polling** | 주기적으로 큐를 확인하여 메시지를 가져오는 방식 |
| **LocalStack** | AWS 서비스의 로컬 시뮬레이터 |

## 🔗 참고 자료

- [AWS SQS Developer Guide](https://docs.aws.amazon.com/sqs/)
- [Spring Cloud AWS Documentation](https://docs.spring.io/spring-cloud-aws/docs/current/reference/html/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## ✅ 시스템 검증 완료

- ✅ LocalStack SQS 서비스 정상 동작
- ✅ Producer/Consumer 서비스 정상 통신
- ✅ SQS 큐 생성 및 DLQ 정책 적용
- ✅ 메시지 송수신 및 처리 완료
- ✅ Docker 컨테이너 오케스트레이션
- ✅ init-sqs.sh 스크립트 정상 동작

---

**프로젝트 상태**: 모든 구성 요소 정상 작동 ✅  
**마지막 업데이트**: 2025년 8월 24일
