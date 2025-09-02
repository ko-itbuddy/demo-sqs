#!/bin/bash

# 멀티모듈 SQS 시스템 간편 시작 스크립트
# Producer + Consumer 서비스를 한 번에 실행

echo "🚀 멀티모듈 SQS 시스템 시작"
echo "=============================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Docker 상태 확인
check_docker() {
    echo -e "${BLUE}Docker 상태 확인 중...${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker가 설치되지 않았습니다.${NC}"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker가 실행되지 않고 있습니다.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Docker 정상 실행 중${NC}"
}

# LocalStack 시작
start_localstack() {
    echo -e "\n${BLUE}LocalStack 시작 중...${NC}"
    
    # 기존 컨테이너 정리
    docker-compose down -v > /dev/null 2>&1
    
    # LocalStack 시작
    docker-compose up -d
    
    echo -e "${YELLOW}LocalStack 준비 대기 중...${NC}"
    for i in {1..12}; do
        if curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ LocalStack 준비 완료${NC}"
            break
        fi
        echo "대기 중... ($i/12)"
        sleep 5
    done
    
    # SQS 큐 생성 대기
    sleep 5
}

# 멀티모듈 서비스 시작
start_multimodule_services() {
    echo -e "\n${BLUE}멀티모듈 서비스 시작${NC}"
    
    echo -e "${YELLOW}방법을 선택하세요:${NC}"
    echo "1) Gradle 병렬 실행 (권장)"
    echo "2) 커스텀 startAllServices 태스크"
    echo "3) 개별 백그라운드 실행"
    read -p "선택 (1-3): " choice
    
    case $choice in
        1)
            echo -e "${GREEN}Gradle 병렬 실행으로 시작합니다...${NC}"
            echo -e "${YELLOW}주의: Ctrl+C로 중지할 수 있습니다${NC}"
            ./gradlew :producer-service:bootRun :consumer-service:bootRun --parallel
            ;;
        2) 
            echo -e "${GREEN}startAllServices 태스크로 시작합니다...${NC}"
            ./gradlew startAllServices --parallel
            ;;
        3)
            echo -e "${GREEN}개별 백그라운드 실행으로 시작합니다...${NC}"
            
            # Producer 서비스 시작
            echo "Producer 서비스 시작 중..."
            nohup ./gradlew :producer-service:bootRun > producer.log 2>&1 &
            PRODUCER_PID=$!
            echo $PRODUCER_PID > producer.pid
            echo "Producer PID: $PRODUCER_PID"
            
            # Consumer 서비스 시작  
            echo "Consumer 서비스 시작 중..."
            nohup ./gradlew :consumer-service:bootRun > consumer.log 2>&1 &
            CONSUMER_PID=$!
            echo $CONSUMER_PID > consumer.pid
            echo "Consumer PID: $CONSUMER_PID"
            
            echo -e "${GREEN}✅ 백그라운드 서비스 시작 완료${NC}"
            echo -e "${YELLOW}로그 확인: tail -f producer.log consumer.log${NC}"
            echo -e "${YELLOW}서비스 중지: ./stop-sqs-system.sh${NC}"
            ;;
        *)
            echo -e "${RED}잘못된 선택입니다. 기본값(1)을 사용합니다.${NC}"
            ./gradlew :producer-service:bootRun :consumer-service:bootRun --parallel
            ;;
    esac
}

# 시스템 정보 표시
show_system_info() {
    echo -e "\n${BLUE}시스템 정보${NC}"
    echo "================================="
    echo -e "${YELLOW}서비스 엔드포인트:${NC}"
    echo "• LocalStack:    http://localhost:4566"
    echo "• Producer API:  http://localhost:8080"
    echo "• Consumer API:  http://localhost:8081"
    
    echo -e "\n${YELLOW}테스트 명령어:${NC}"
    echo "# 주문 생성 테스트"
    echo 'curl -X POST http://localhost:8080/api/orders \'
    echo '  -H "Content-Type: application/json" \'
    echo '  -d '\''{"customerName":"테스트 고객", "productName":"테스트 상품", "quantity":1, "price":15000.00}'\'''
    
    echo -e "\n${YELLOW}유용한 명령어:${NC}"
    echo "# 큐 목록 확인"
    echo "aws --endpoint-url=http://localhost:4566 sqs list-queues --region ap-northeast-2"
    
    echo "# 로그 확인 (백그라운드 실행시)"
    echo "tail -f producer.log consumer.log"
    
    echo "# 시스템 중지"
    echo "./stop-sqs-system.sh"
}

# 메인 실행
main() {
    check_docker
    start_localstack
    start_multimodule_services
    show_system_info
}

# 스크립트 실행
main "$@"