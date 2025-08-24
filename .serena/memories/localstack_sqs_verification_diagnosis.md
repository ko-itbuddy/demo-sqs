# LocalStack SQS 메시지 통신 시스템 검증 진단

## 분석된 잠재적 문제 소스들 (7가지)

1. **LocalStack 서비스 초기화 실패**
   - 헬스체크 타이밍 문제
   - depends_on 조건 미충족

2. **SQS 큐 생성 스크립트 오류**
   - init-sqs.sh 실행 실패
   - DLQ ARN 획득 실패

3. **Consumer 서비스의 SQS 리스너 설정 충돌** ⭐
   - 메시지 처리 중 예외 발생
   - DLQ 라우팅 실패

4. **Producer-Consumer 간 메시지 직렬화/역직렬화 불일치**
   - JSON 매핑 오류
   - 타입 불일치

5. **DLQ redrive policy 설정 오류** ⭐
   - maxReceiveCount 설정 문제
   - ARN 매핑 실패

6. **네트워크 연결 및 엔드포인트 설정 문제**
   - LocalStack 포트 접근 실패
   - 컨테이너 간 통신 문제

7. **Spring Cloud AWS SQS 의존성 버전 호환성 문제**
   - 라이브러리 버전 충돌
   - 설정 방식 변경

## 가장 가능성이 높은 문제 소스 (1-2개)

### 1. LocalStack과 서비스 간 네트워크 연결 및 헬스체크 타이밍 문제
- Docker Compose의 depends_on과 healthcheck 설정
- 서비스 시작 순서와 준비 상태 동기화

### 2. Consumer 서비스의 SQS 리스너와 메시지 처리 중 예외 발생 시 DLQ 라우팅 문제
- 메시지 처리 실패 시 올바른 DLQ 전송
- 재시도 로직과 maxReceiveCount 설정

## 검증 계획

1. Consumer 메시지 리스너에 상세 로깅 추가
2. DLQ 라우팅 모니터링 로그 추가
3. LocalStack 연결 상태 확인 로그 추가
4. End-to-End 테스트 실행으로 검증
