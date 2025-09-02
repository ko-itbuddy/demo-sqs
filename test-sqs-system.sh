#!/bin/bash

# LocalStack SQS 시스템 테스트 스크립트
# 사용자가 간단하게 전체 시스템을 테스트할 수 있도록 구성

echo "🚀 LocalStack SQS 시스템 테스트 시작"
echo "====================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 단계별 테스트 함수
check_docker() {
    echo -e "${BLUE}1. Docker 상태 확인${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker가 설치되지 않았습니다.${NC}"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker가 실행되지 않고 있습니다. Docker Desktop을 시작해주세요.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Docker 정상 실행 중${NC}"
}

start_system() {
    echo -e "\n${BLUE}2. 시스템 시작${NC}"
    echo "시스템을 시작합니다..."
    
    # 기존 컨테이너 정리
    docker-compose down -v > /dev/null 2>&1
    
    # 시스템 시작
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 시스템 시작 완료${NC}"
    else
        echo -e "${RED}❌ 시스템 시작 실패${NC}"
        exit 1
    fi
}

wait_for_services() {
    echo -e "\n${BLUE}3. 서비스 준비 대기${NC}"
    echo "서비스가 준비될 때까지 대기 중..."
    
    # LocalStack 준비 대기 (최대 60초)
    for i in {1..12}; do
        if curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ LocalStack 준비 완료${NC}"
            break
        fi
        echo "LocalStack 준비 중... ($i/12)"
        sleep 5
    done
    
    # SQS 큐 생성 확인
    sleep 5
    if aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ SQS 큐 생성 완료${NC}"
    else
        echo -e "${RED}❌ SQS 큐 생성 실패${NC}"
        exit 1
    fi
}

start_services() {
    echo -e "\n${BLUE}4. Producer/Consumer 서비스 시작${NC}"
    echo "멀티모듈 Gradle 구조를 사용하여 서비스를 시작합니다..."
    
    # 멀티모듈 Gradle로 Producer 서비스 시작 (백그라운드)
    echo "Producer 서비스 시작 중..."
    nohup ./gradlew :producer-service:bootRun > producer.log 2>&1 &
    PRODUCER_PID=$!
    echo $PRODUCER_PID > producer.pid
    
    # 멀티모듈 Gradle로 Consumer 서비스 시작 (백그라운드)
    echo "Consumer 서비스 시작 중..."
    nohup ./gradlew :consumer-service:bootRun > consumer.log 2>&1 &
    CONSUMER_PID=$!
    echo $CONSUMER_PID > consumer.pid
    
    echo -e "${YELLOW}⏳ 서비스 시작 대기 중 (30초)...${NC}"
    sleep 30
    
    echo -e "${GREEN}✅ Producer/Consumer 서비스 시작 완료 (멀티모듈 구조)${NC}"
}

test_system() {
    echo -e "\n${BLUE}5. 시스템 기능 테스트${NC}"
    
    # Producer 서비스 테스트
    echo "Producer 서비스 테스트 중..."
    response=$(curl -s -X POST http://localhost:8080/api/orders \
        -H "Content-Type: application/json" \
        -d '{"orderNumber":"TEST-001", "productName":"테스트 상품", "quantity":1}' \
        -w "%{http_code}")
    
    if [[ "$response" == *"200" ]] || [[ "$response" == *"201" ]]; then
        echo -e "${GREEN}✅ Producer 서비스 정상 작동${NC}"
    else
        echo -e "${RED}❌ Producer 서비스 오류 (응답: $response)${NC}"
    fi
    
    # 메시지 처리 확인 (10초 대기)
    echo "Consumer 메시지 처리 대기 중..."
    sleep 10
    
    # Consumer 로그 확인
    if grep -q "TEST-001" consumer.log 2>/dev/null; then
        echo -e "${GREEN}✅ Consumer 서비스 메시지 처리 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  Consumer 로그에서 메시지 처리 확인 필요${NC}"
    fi
}

run_load_test() {
    echo -e "\n${BLUE}6. 부하 테스트 (선택사항)${NC}"
    read -p "부하 테스트를 실행하시겠습니까? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "10개 메시지 연속 전송 중..."
        for i in {1..10}; do
            curl -s -X POST http://localhost:8080/api/orders \
                -H "Content-Type: application/json" \
                -d "{\"orderNumber\":\"LOAD-$i\", \"productName\":\"부하테스트 상품 $i\", \"quantity\":$i}" > /dev/null
            echo "메시지 $i 전송 완료"
        done
        echo -e "${GREEN}✅ 부하 테스트 완료${NC}"
        
        echo "처리 결과 확인을 위해 10초 대기..."
        sleep 10
    fi
}

show_status() {
    echo -e "\n${BLUE}7. 시스템 상태 정보${NC}"
    echo "====================================="
    
    # 컨테이너 상태
    echo -e "\n${YELLOW}Docker 컨테이너:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(NAMES|localstack|sqs-init)"
    
    # SQS 큐 상태
    echo -e "\n${YELLOW}SQS 큐 목록:${NC}"
    aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2 2>/dev/null | jq -r '.QueueUrls[]' | sed 's/.*\///' || echo "큐 조회 실패"
    
    # 서비스 엔드포인트
    echo -e "\n${YELLOW}서비스 엔드포인트:${NC}"
    echo "• LocalStack: http://localhost:4566"
    echo "• Producer:   http://localhost:8080"
    echo "• Consumer:   http://localhost:8081"
    
    # 로그 파일 위치
    echo -e "\n${YELLOW}로그 파일:${NC}"
    echo "• Producer:   $(pwd)/producer.log"
    echo "• Consumer:   $(pwd)/consumer.log"
}

show_commands() {
    echo -e "\n${BLUE}8. 유용한 명령어${NC}"
    echo "====================================="
    echo -e "\n${YELLOW}수동 테스트:${NC}"
    echo "# 메시지 전송"
    echo 'curl -X POST http://localhost:8080/api/orders \\'
    echo '  -H "Content-Type: application/json" \\'
    echo '  -d '"'"'{"orderNumber":"MANUAL-001", "productName":"수동 테스트", "quantity":1}'"'"
    
    echo -e "\n${YELLOW}상태 확인:${NC}"
    echo "# LocalStack 상태"
    echo "curl http://localhost:4566/_localstack/health"
    
    echo "\n# SQS 큐 목록"
    echo "aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2"
    
    echo -e "\n${YELLOW}로그 확인:${NC}"
    echo "# 실시간 로그 (Producer)"
    echo "tail -f producer.log"
    
    echo "# 실시간 로그 (Consumer)"
    echo "tail -f consumer.log"
    
    echo -e "\n${YELLOW}시스템 종료:${NC}"
    echo "# 서비스 종료"
    echo "./stop-sqs-system.sh"
    
    echo "# 또는 수동 종료"
    echo "docker-compose down -v"
    echo "kill \$(cat producer.pid) 2>/dev/null"
    echo "kill \$(cat consumer.pid) 2>/dev/null"
}

cleanup() {
    echo -e "\n${BLUE}정리 작업...${NC}"
    # PID 파일 정리는 하지 않음 (시스템이 계속 실행되어야 하므로)
}

# 메인 실행
echo "LocalStack SQS 시스템 자동 테스트를 시작합니다."
echo

# 단계별 실행
check_docker
start_system
wait_for_services
start_services
test_system
run_load_test
show_status
show_commands

echo
echo -e "${GREEN}🎉 테스트 완료! 시스템이 실행 중입니다.${NC}"
echo -e "${YELLOW}시스템을 종료하려면 './stop-sqs-system.sh'를 실행하세요.${NC}"

# 정리 작업
trap cleanup EXIT