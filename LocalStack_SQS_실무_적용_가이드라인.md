# LocalStack SQS 시스템 실무 적용 가이드라인

> **문서 버전**: v1.0  
> **작성일**: 2025-08-24  
> **대상**: DevOps, Backend 개발자, QA 엔지니어, 운영팀  
> **전제조건**: LocalStack SQS 데모 시스템 검증 완료  

## 📋 목차

- [1. 전체 로드맵 및 타임라인](#1-전체-로드맵-및-타임라인)
- [2. 단계별 상세 TODO 목록](#2-단계별-상세-todo-목록)
- [3. 환경별 설정 가이드](#3-환경별-설정-가이드)
- [4. 보안 체크리스트](#4-보안-체크리스트)
- [5. 성능 최적화 가이드](#5-성능-최적화-가이드)
- [6. 모니터링 및 알림 설정](#6-모니터링-및-알림-설정)
- [7. 장애 대응 프로세스](#7-장애-대응-프로세스)
- [8. 운영 매뉴얼](#8-운영-매뉴얼)
- [9. 부록](#9-부록)

---

## 1. 실무 적용 단계 개요

### 1.1 적용 단계별 분류

실무 환경 적용을 위한 주요 단계는 다음과 같습니다:

```
환경 준비 → 코드 개선 → 운영 체계 → 서비스 배포
   Phase 1    Phase 2     Phase 3      Phase 4
```

### 1.2 각 단계별 핵심 작업

| 단계 | 핵심 작업 | 완료 기준 | 담당팀 |
|------|-----------|-----------|--------|
| **환경 준비** | 개발/스테이징/프로덕션 환경 구축 | 모든 환경에서 SQS 통신 정상 작동 | DevOps |
| **코드 개선** | 보안 강화, 성능 최적화, 테스트 코드 작성 | 보안 스캔 통과, 테스트 커버리지 80% 이상 | Backend |
| **운영 체계** | 모니터링, CI/CD, 알림 시스템 구축 | 모니터링 대시보드, 알림 시스템 완성 | SRE |
| **서비스 배포** | 스테이징/프로덕션 배포 및 안정화 | 프로덕션 안정 운영 달성 | 전체팀 |

---

## 2. 단계별 상세 TODO 목록

### Phase 1: 환경 준비 단계

#### 🔧 개발 환경 구축

- [ ] **TODO-ENV-001: LocalStack 개발 환경 표준화**
  - **우선순위**: P1 (Critical)
  - **담당자**: DevOps Engineer
  - **완료 기준**: 
    - Docker Compose 설정 표준화
    - 개발팀 전체 동일한 환경에서 실행 가능
    - 환경 변수 관리 체계 확립
  - **의존성**: 없음
  - **위험 요소**: 
    - 개발자별 로컬 환경 차이
    - Docker 버전 호환성 문제
  - **검증 방법**:
    ```bash
    # 각 개발자 머신에서 실행
    docker-compose up -d
    curl http://localhost:4566/_localstack/health
    # Expected: {"status": "ok"}
    ```

- [ ] **TODO-ENV-002: SQS 큐 관리 스크립트 개선**
  - **우선순위**: P2 (High)
  - **담당자**: Backend Developer
  - **완료 기준**:
    - 큐 생성/삭제/모니터링 스크립트 작성
    - 환경별 큐 설정 자동화
    - 오류 처리 및 로깅 강화
  - **의존성**: TODO-ENV-001
  - **검증 방법**:
    ```bash
    ./scripts/manage-queues.sh create dev
    ./scripts/manage-queues.sh status dev
    ```

#### 🏢 스테이징 환경 구축

- [ ] **TODO-ENV-003: AWS 스테이징 환경 설정**
  - **우선순위**: P1 (Critical)
  - **담당자**: DevOps Engineer
  - **완료 기준**:
    - 실제 AWS SQS 환경 구축
    - VPC, 보안 그룹 설정
    - IAM 역할 및 정책 구성
  - **의존성**: TODO-ENV-001
  - **위험 요소**:
    - AWS 비용 발생
    - 네트워크 보안 설정 오류
  - **검증 방법**:
    ```bash
    aws sqs list-queues --region ap-northeast-2
    aws sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names All
    ```

#### 🚀 프로덕션 환경 준비

- [ ] **TODO-ENV-004: 프로덕션 인프라 아키텍처 설계**
  - **우선순위**: P1 (Critical)
  - **담당자**: Solutions Architect
  - **완료 기준**:
    - 고가용성 아키텍처 설계 완료
    - 재해 복구 계획 수립
    - 용량 계획 및 오토 스케일링 설정
  - **의존성**: TODO-ENV-003
  - **위험 요소**:
    - 트래픽 예측 부정확
    - 단일 장애점 존재 가능성

### Phase 2: 코드 개선 단계

#### 🔐 보안 강화

- [ ] **TODO-SEC-001: AWS 크리덴셜 관리 개선**
  - **우선순위**: P1 (Critical)
  - **담당자**: Security Engineer + Backend Developer
  - **완료 기준**:
    - AWS Secrets Manager 또는 Parameter Store 적용
    - 하드코딩된 크리덴셜 완전 제거
    - 개발/스테이징/프로덕션별 격리
  - **의존성**: TODO-ENV-003
  - **위험 요소**:
    - 기존 설정 파일에 크리덴셜 노출
    - 로그 파일에 민감 정보 기록
  - **검증 방법**:
    ```bash
    # 코드베이스에서 크리덴셜 검색
    grep -r "AKIA" . || echo "No AWS Access Keys found"
    grep -r "aws_secret_access_key" . || echo "No hardcoded secrets"
    ```

- [ ] **TODO-SEC-002: 네트워크 보안 강화**
  - **우선순위**: P2 (High)
  - **담당자**: DevOps Engineer
  - **완료 기준**:
    - VPC 엔드포인트 설정으로 인터넷 트래픽 차단
    - 보안 그룹 최소 권한 원칙 적용
    - SSL/TLS 암호화 강제
  - **검증 방법**:
    ```bash
    # VPC 엔드포인트 확인
    aws ec2 describe-vpc-endpoints --filters Name=service-name,Values=com.amazonaws.ap-northeast-2.sqs
    ```

#### ⚡ 성능 최적화

- [ ] **TODO-PERF-001: 메시지 처리 성능 최적화**
  - **우선순위**: P2 (High)
  - **담당자**: Backend Developer
  - **완료 기준**:
    - 배치 메시지 처리 구현
    - 연결 풀링 최적화
    - 메시지 압축 적용
  - **벤치마킹 기준**:
    - 초당 1000 메시지 처리 가능
    - 평균 응답 시간 100ms 이하
    - 메모리 사용량 512MB 이하
  - **검증 방법**:
    ```bash
    # 성능 테스트 실행
    ./scripts/performance-test.sh --messages=10000 --concurrent=50
    ```

#### 🧪 테스트 코드 작성

- [ ] **TODO-TEST-001: 단위 테스트 작성**
  - **우선순위**: P2 (High)
  - **담당자**: Backend Developer
  - **완료 기준**:
    - 테스트 커버리지 80% 이상
    - 모든 비즈니스 로직 테스트 포함
    - CI/CD 파이프라인 통합
  - **검증 방법**:
    ```bash
    ./gradlew test jacocoTestReport
    # 커버리지 리포트 확인: build/reports/jacoco/test/html/index.html
    ```

- [ ] **TODO-TEST-002: 통합 테스트 작성**
  - **우선순위**: P2 (High)
  - **담당자**: QA Engineer + Backend Developer
  - **완료 기준**:
    - TestContainers 기반 통합 테스트
    - 실제 SQS 통신 시나리오 검증
    - 장애 시나리오 테스트 포함

### Phase 3: 운영 준비 단계

#### 📊 모니터링 시스템 구축

- [ ] **TODO-MON-001: CloudWatch 메트릭 설정**
  - **우선순위**: P1 (Critical)
  - **담당자**: SRE Engineer
  - **완료 기준**:
    - SQS 큐별 메트릭 수집
    - 커스텀 메트릭 생성
    - 대시보드 구축
  - **모니터링 지표**:
    - 메시지 수신/발신 속도
    - 큐 깊이 (Queue Depth)
    - DLQ 메시지 수
    - 처리 지연 시간
    - 오류율

- [ ] **TODO-MON-002: 알림 시스템 구축**
  - **우선순위**: P1 (Critical)
  - **담당자**: SRE Engineer
  - **완료 기준**:
    - Slack/이메일 알림 연동
    - 심각도별 알림 규칙 설정
    - 에스컬레이션 정책 구현
  - **알림 규칙**:
    ```
    Critical: DLQ 메시지 > 10개, 처리 지연 > 5분
    Warning: 큐 깊이 > 100개, 오류율 > 5%
    Info: 서비스 재시작, 배포 완료
    ```

#### 🔄 CI/CD 파이프라인

- [ ] **TODO-CICD-001: 빌드 파이프라인 구축**
  - **우선순위**: P2 (High)
  - **담당자**: DevOps Engineer
  - **완료 기준**:
    - GitHub Actions 또는 Jenkins 설정
    - 자동 테스트 실행
    - 보안 스캔 통합
    - 이미지 빌드 및 푸시

- [ ] **TODO-CICD-002: 배포 파이프라인 구축**
  - **우선순위**: P2 (High)
  - **담당자**: DevOps Engineer
  - **완료 기준**:
    - 블루-그린 배포 방식 적용
    - 롤백 메커니즘 구현
    - 배포 후 헬스체크 자동화

### Phase 4: 배포 단계

#### 🎯 스테이징 배포

- [ ] **TODO-DEPLOY-001: 스테이징 배포 실행**
  - **우선순위**: P1 (Critical)
  - **담당자**: DevOps Engineer + QA
  - **완료 기준**:
    - 자동 배포 성공
    - 모든 테스트 시나리오 통과
    - 성능 기준 만족

#### 🚀 프로덕션 배포

- [ ] **TODO-DEPLOY-002: 프로덕션 배포 실행**
  - **우선순위**: P1 (Critical)
  - **담당자**: 전체 팀
  - **완료 기준**:
    - 점진적 배포 성공
    - 실시간 모니터링 정상
    - 비즈니스 메트릭 안정

---

## 3. 환경별 설정 가이드

### 3.1 개발 환경 (LocalStack)

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  localstack:
    image: localstack/localstack:3.4
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sqs
      - DEBUG=1
      - PERSISTENCE=1
      - LAMBDA_EXECUTOR=docker
    volumes:
      - "./localstack-data:/tmp/localstack"
      - "/var/run/docker.sock:/var/run/docker.sock"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 3.2 스테이징 환경 (AWS)

```yaml
# application-staging.yml
spring:
  cloud:
    aws:
      credentials:
        use-default-aws-credentials-chain: true
      region:
        static: ap-northeast-2
      sqs:
        enabled: true
        endpoint: https://sqs.ap-northeast-2.amazonaws.com

logging:
  level:
    com.amazonaws: INFO
    org.springframework.cloud.aws: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 3.3 프로덕션 환경 (AWS)

```yaml
# application-prod.yml
spring:
  cloud:
    aws:
      credentials:
        use-default-aws-credentials-chain: true
      region:
        static: ap-northeast-2
      sqs:
        enabled: true
        endpoint: https://sqs.ap-northeast-2.amazonaws.com

logging:
  level:
    root: WARN
    com.demo: INFO
    org.springframework.cloud.aws: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

---

## 4. 보안 체크리스트

### 4.1 인증 및 권한 관리

- [ ] **IAM 역할 최소 권한 원칙 적용**
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ],
        "Resource": [
          "arn:aws:sqs:ap-northeast-2:ACCOUNT-ID:order-queue",
          "arn:aws:sqs:ap-northeast-2:ACCOUNT-ID:order-dlq"
        ]
      }
    ]
  }
  ```

- [ ] **크리덴셜 관리**
  - AWS Systems Manager Parameter Store 사용
  - 환경별 크리덴셜 격리
  - 정기적 로테이션 (90일)

### 4.2 네트워크 보안

- [ ] **VPC 엔드포인트 설정**
  ```bash
  aws ec2 create-vpc-endpoint \
    --vpc-id vpc-12345678 \
    --service-name com.amazonaws.ap-northeast-2.sqs \
    --route-table-ids rtb-12345678
  ```

- [ ] **보안 그룹 설정**
  - 인바운드: 필요한 포트만 허용 (8080, 8081)
  - 아웃바운드: HTTPS(443)만 허용
  - Source/Destination 명시적 지정

### 4.3 데이터 보안

- [ ] **메시지 암호화**
  - 전송 중 암호화: TLS 1.2 이상
  - 저장 시 암호화: AWS KMS 사용
  - 클라이언트측 암호화 적용

- [ ] **감사 로깅**
  - CloudTrail 활성화
  - 모든 SQS API 호출 기록
  - 로그 무결성 보장

---

## 5. 성능 최적화 가이드

### 5.1 메시지 처리 최적화

#### 배치 처리 구현
```java
@Component
public class OptimizedOrderMessageListener {
    
    @SqsListener(value = "${app.sqs.order-queue}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void handleMessages(List<OrderMessage> messages, Acknowledgment ack) {
        try {
            // 배치 처리로 성능 향상
            processBatch(messages);
            ack.acknowledge();
        } catch (Exception e) {
            // 개별 메시지별 오류 처리
            handleIndividualMessages(messages);
        }
    }
    
    private void processBatch(List<OrderMessage> messages) {
        // 배치 단위로 데이터베이스 업데이트
        orderRepository.saveAll(messages.stream()
            .map(this::convertToOrder)
            .collect(Collectors.toList()));
    }
}
```

#### 연결 풀 최적화
```yaml
# application.yml
spring:
  cloud:
    aws:
      sqs:
        listener:
          max-concurrent-messages: 10
          max-messages-per-poll: 10
          poll-timeout: 20
          back-off-time: 1000
          max-back-off-time: 10000
```

### 5.2 성능 벤치마킹

#### 부하 테스트 스크립트
```bash
#!/bin/bash
# performance-test.sh

MESSAGE_COUNT=${1:-1000}
CONCURRENT_USERS=${2:-10}

echo "Starting performance test..."
echo "Messages: $MESSAGE_COUNT"
echo "Concurrent Users: $CONCURRENT_USERS"

# JMeter를 사용한 부하 테스트
jmeter -n -t sqs-load-test.jmx \
  -Jmessage.count=$MESSAGE_COUNT \
  -Jconcurrent.users=$CONCURRENT_USERS \
  -l results.jtl

# 결과 분석
echo "Performance Test Results:"
cat results.jtl | awk -F',' 'NR>1{sum+=$2; count++} END{print "Average Response Time:", sum/count, "ms"}'
```

### 5.3 성능 모니터링 지표

| 지표 | 목표값 | 경고 임계값 | 위험 임계값 |
|------|--------|-------------|------------|
| 메시지 처리 속도 | >1000/sec | <800/sec | <500/sec |
| 평균 응답 시간 | <100ms | >200ms | >500ms |
| 큐 깊이 | <10 | >50 | >100 |
| 메모리 사용률 | <70% | >80% | >90% |
| CPU 사용률 | <60% | >75% | >85% |

---

## 6. 모니터링 및 알림 설정

### 6.1 CloudWatch 대시보드

#### 메트릭 설정
```json
{
  "metrics": [
    {
      "metricName": "ApproximateNumberOfMessages",
      "namespace": "AWS/SQS",
      "dimensions": {
        "QueueName": "order-queue"
      }
    },
    {
      "metricName": "NumberOfMessagesSent",
      "namespace": "AWS/SQS",
      "dimensions": {
        "QueueName": "order-queue"
      }
    }
  ]
}
```

#### 대시보드 위젯 구성
1. **실시간 메트릭**
   - 초당 메시지 수신/발신 수
   - 현재 큐 깊이
   - DLQ 메시지 수

2. **성능 지표**
   - 평균 처리 시간
   - 처리량 트렌드
   - 오류율

3. **시스템 리소스**
   - CPU/메모리 사용률
   - 네트워크 I/O
   - 디스크 사용량

### 6.2 알림 규칙

#### CloudWatch 알람 설정
```bash
# DLQ 메시지 수 알람
aws cloudwatch put-metric-alarm \
  --alarm-name "SQS-DLQ-Messages-High" \
  --alarm-description "DLQ messages exceed threshold" \
  --metric-name ApproximateNumberOfMessages \
  --namespace AWS/SQS \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=QueueName,Value=order-dlq \
  --alarm-actions arn:aws:sns:ap-northeast-2:ACCOUNT-ID:sqs-alerts

# 큐 깊이 알람
aws cloudwatch put-metric-alarm \
  --alarm-name "SQS-Queue-Depth-High" \
  --alarm-description "Queue depth is too high" \
  --metric-name ApproximateNumberOfMessages \
  --namespace AWS/SQS \
  --statistic Average \
  --period 300 \
  --threshold 100 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=QueueName,Value=order-queue \
  --alarm-actions arn:aws:sns:ap-northeast-2:ACCOUNT-ID:sqs-alerts
```

#### Slack 연동
```python
# lambda_function.py (SNS → Slack)
import json
import urllib3

def lambda_handler(event, context):
    message = json.loads(event['Records'][0]['Sns']['Message'])
    
    slack_message = {
        "channel": "#sqs-alerts",
        "username": "AWS CloudWatch",
        "text": f"🚨 SQS Alert: {message['AlarmName']}",
        "attachments": [
            {
                "color": "danger" if message['NewStateValue'] == 'ALARM' else "good",
                "fields": [
                    {"title": "Alarm", "value": message['AlarmName'], "short": True},
                    {"title": "Status", "value": message['NewStateValue'], "short": True},
                    {"title": "Reason", "value": message['NewStateReason'], "short": False}
                ]
            }
        ]
    }
    
    http = urllib3.PoolManager()
    response = http.request('POST', SLACK_WEBHOOK_URL, 
                           body=json.dumps(slack_message),
                           headers={'Content-Type': 'application/json'})
    
    return {'statusCode': 200}
```

---

## 7. 장애 대응 프로세스

### 7.1 장애 유형별 대응 절차

#### 🔴 Critical: 서비스 완전 중단

**증상**:
- 모든 메시지 처리 중단
- 애플리케이션 응답 없음
- 헬스체크 실패

**대응 절차**:
1. **즉시 조치** (5분 이내)
   ```bash
   # 서비스 상태 확인
   kubectl get pods -n production
   
   # 로그 확인
   kubectl logs -f deployment/order-consumer -n production
   
   # 즉시 롤백 결정
   kubectl rollout undo deployment/order-consumer -n production
   ```

2. **임시 우회** (10분 이내)
   - 수동 메시지 처리 스크립트 실행
   - 트래픽을 백업 시스템으로 라우팅
   - 고객 공지 (상황에 따라)

3. **근본 원인 분석** (1시간 이내)
   - 로그 분석
   - 메트릭 검토
   - 코드 변경 이력 확인

#### 🟡 Warning: 성능 저하

**증상**:
- 메시지 처리 지연
- 큐 깊이 증가
- 응답 시간 증가

**대응 절차**:
1. **모니터링 강화**
   ```bash
   # 실시간 모니터링
   watch -n 5 'aws sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names ApproximateNumberOfMessages'
   ```

2. **스케일링 조치**
   ```bash
   # 인스턴스 수 증가
   kubectl scale deployment order-consumer --replicas=5 -n production
   ```

3. **성능 분석**
   - 처리 시간 프로파일링
   - 데이터베이스 성능 확인
   - 네트워크 지연 측정

### 7.2 장애 복구 체크리스트

- [ ] **서비스 복구 확인**
  - [ ] 헬스체크 통과
  - [ ] 메시지 처리 정상화
  - [ ] 큐 깊이 정상 범위
  - [ ] 응답 시간 목표값 달성

- [ ] **데이터 정합성 확인**
  - [ ] 누락된 메시지 확인
  - [ ] DLQ 메시지 처리
  - [ ] 중복 처리 여부 확인

- [ ] **모니터링 정상화**
  - [ ] 모든 알람 해제
  - [ ] 대시보드 정상 표시
  - [ ] 로그 수집 정상화

### 7.3 사후 분석 (Post-Mortem)

#### 분석 보고서 템플릿
```markdown
# SQS 시스템 장애 보고서

## 사고 개요
- **발생 시간**: YYYY-MM-DD HH:MM:SS
- **복구 시간**: YYYY-MM-DD HH:MM:SS
- **영향 범위**: 영향받은 서비스 및 사용자 수
- **심각도**: Critical/High/Medium/Low

## 타임라인
- 발견: 알람 또는 사용자 신고
- 대응: 초기 대응 조치
- 복구: 서비스 정상화
- 완료: 근본 원인 해결

## 근본 원인
- 기술적 원인
- 프로세스 원인
- 인적 원인

## 개선 계획
- 단기 개선 사항 (1주일 내)
- 중기 개선 사항 (1개월 내)
- 장기 개선 사항 (3개월 내)

## 액션 아이템
- [ ] 액션 1 (담당자, 완료일)
- [ ] 액션 2 (담당자, 완료일)
```

---

## 8. 운영 매뉴얼

### 8.1 일일 운영 체크리스트

#### 오전 점검 (09:00)
- [ ] **시스템 상태 확인**
  ```bash
  # 헬스체크
  curl -f http://producer:8080/actuator/health
  curl -f http://consumer:8081/actuator/health
  
  # 큐 상태
  aws sqs get-queue-attributes --queue-url $ORDER_QUEUE_URL --attribute-names All
  aws sqs get-queue-attributes --queue-url $ORDER_DLQ_URL --attribute-names All
  ```

- [ ] **메트릭 검토**
  - 전일 메시지 처리량
  - 평균 응답 시간
  - 오류율
  - DLQ 메시지 수

- [ ] **로그 검토**
  ```bash
  # 오류 로그 확인
  kubectl logs --since=24h deployment/order-consumer -n production | grep ERROR
  kubectl logs --since=24h deployment/order-producer -n production | grep ERROR
  ```

#### 주간 점검 (월요일)
- [ ] **성능 트렌드 분석**
  - 주간 처리량 변화
  - 응답 시간 트렌드
  - 리소스 사용률 변화

- [ ] **용량 계획 검토**
  - 예상 트래픽 증가
  - 스케일링 필요성
  - 비용 최적화 기회

### 8.2 운영 명령어 모음

#### SQS 관리
```bash
# 큐 목록 조회
aws sqs list-queues --region ap-northeast-2

# 메시지 수 확인
aws sqs get-queue-attributes \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/ACCOUNT-ID/order-queue \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible

# DLQ 메시지 확인
aws sqs receive-message \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/ACCOUNT-ID/order-dlq \
  --max-number-of-messages 10

# 큐 비우기 (주의!)
aws sqs purge-queue \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/ACCOUNT-ID/order-queue
```

#### Kubernetes 관리
```bash
# 파드 상태 확인
kubectl get pods -n production -l app=order-consumer

# 로그 실시간 확인
kubectl logs -f deployment/order-consumer -n production

# 스케일링
kubectl scale deployment order-consumer --replicas=3 -n production

# 롤아웃 상태 확인
kubectl rollout status deployment/order-consumer -n production

# 배포 히스토리
kubectl rollout history deployment/order-consumer -n production
```

#### 모니터링
```bash
# 메트릭 수집
curl http://consumer:8081/actuator/prometheus | grep sqs

# 헬스체크
curl -s http://consumer:8081/actuator/health | jq .

# JVM 메트릭
curl -s http://consumer:8081/actuator/metrics/jvm.memory.used | jq .
```

### 8.3 정기 유지보수

#### 월간 작업
- [ ] **보안 업데이트**
  - 의존성 라이브러리 업데이트
  - 베이스 이미지 업데이트
  - 보안 패치 적용

- [ ] **성능 검토**
  - 월간 성능 리포트 작성
  - 병목 지점 분석
  - 최적화 기회 식별

#### 분기별 작업
- [ ] **재해 복구 테스트**
  - 백업 복원 테스트
  - 장애 시뮬레이션
  - 복구 절차 검증

- [ ] **용량 계획 업데이트**
  - 트래픽 예측 업데이트
  - 인프라 용량 검토
  - 비용 최적화 계획

---

## 9. 부록

### 9.1 트러블슈팅 가이드

#### 일반적인 문제와 해결방법

**문제**: 메시지가 DLQ로 이동됨
```bash
# 원인 분석
1. DLQ 메시지 확인
aws sqs receive-message --queue-url $DLQ_URL

2. 에러 로그 확인
kubectl logs deployment/order-consumer | grep ERROR

# 해결방법
- 메시지 형식 검증
- 예외 처리 로직 개선
- DLQ 메시지 수동 재처리
```

**문제**: 메시지 처리 지연
```bash
# 원인 분석
1. 큐 깊이 확인
aws sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names ApproximateNumberOfMessages

2. 컨슈머 인스턴스 수 확인
kubectl get pods -l app=order-consumer

# 해결방법
- 인스턴스 수 증가
- 배치 처리 크기 조정
- 데이터베이스 성능 최적화
```

### 9.2 참고 자료

#### 공식 문서
- [AWS SQS Developer Guide](https://docs.aws.amazon.com/sqs/)
- [Spring Cloud AWS Documentation](https://docs.spring.io/spring-cloud-aws/docs/current/reference/html/)
- [LocalStack Documentation](https://docs.localstack.cloud/)

#### 모니터링 도구
- [CloudWatch Dashboards](https://console.aws.amazon.com/cloudwatch/)
- [Grafana SQS Dashboard](https://grafana.com/grafana/dashboards/)
- [Prometheus SQS Exporter](https://github.com/jmal98/sqs_exporter)

#### 성능 테스트 도구
- [Apache JMeter](https://jmeter.apache.org/)
- [Artillery.io](https://artillery.io/)
- [AWS Load Testing Solution](https://aws.amazon.com/solutions/load-testing-solution/)

### 9.3 연락처 및 에스컬레이션

#### 운영팀 연락처
- **1차 대응**: DevOps팀 (Slack: #devops-alerts)
- **2차 대응**: Backend팀 (Slack: #backend-support)
- **3차 대응**: 아키텍트 (이메일: architect@company.com)

#### 에스컬레이션 매트릭스
| 심각도 | 1차 대응 시간 | 에스컬레이션 시간 | 대상 |
|--------|---------------|-------------------|------|
| Critical | 5분 | 15분 | CTO, VP Engineering |
| High | 15분 | 1시간 | Engineering Manager |
| Medium | 1시간 | 4시간 | Team Lead |
| Low | 4시간 | 24시간 | 담당자 |

---

**📝 문서 관리**
- 최종 업데이트: 2025-08-24
- 다음 검토일: 2025-09-24
- 문서 소유자: DevOps Team
- 승인자: Engineering Manager

**🔄 변경 이력**
- v1.0 (2025-08-24): 초기 버전 작성

---

> ⚠️ **중요 알림**: 이 문서는 실무 환경 적용을 위한 가이드라인이며, 실제 적용 전에는 반드시 보안 검토와 성능 테스트를 완료해야 합니다.