#!/bin/bash

# LocalStack SQS 시스템 종료 스크립트

echo "🛑 LocalStack SQS 시스템 종료"
echo "=============================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

stop_services() {
    echo -e "\n${BLUE}1. Producer/Consumer 서비스 종료${NC}"
    
    # Producer 종료
    if [ -f producer.pid ]; then
        PID=$(cat producer.pid)
        if kill -0 $PID 2>/dev/null; then
            echo "Producer 서비스 종료 중... (PID: $PID)"
            kill $PID
            sleep 2
            if kill -0 $PID 2>/dev/null; then
                echo "강제 종료 중..."
                kill -9 $PID 2>/dev/null
            fi
        fi
        rm -f producer.pid
        echo -e "${GREEN}✅ Producer 서비스 종료 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  Producer PID 파일을 찾을 수 없습니다${NC}"
    fi
    
    # Consumer 종료
    if [ -f consumer.pid ]; then
        PID=$(cat consumer.pid)
        if kill -0 $PID 2>/dev/null; then
            echo "Consumer 서비스 종료 중... (PID: $PID)"
            kill $PID
            sleep 2
            if kill -0 $PID 2>/dev/null; then
                echo "강제 종료 중..."
                kill -9 $PID 2>/dev/null
            fi
        fi
        rm -f consumer.pid
        echo -e "${GREEN}✅ Consumer 서비스 종료 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  Consumer PID 파일을 찾을 수 없습니다${NC}"
    fi
}

stop_docker() {
    echo -e "\n${BLUE}2. Docker 컨테이너 종료${NC}"
    docker-compose down -v
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Docker 컨테이너 종료 완료${NC}"
    else
        echo -e "${RED}❌ Docker 컨테이너 종료 중 오류 발생${NC}"
    fi
}

clean_logs() {
    echo -e "\n${BLUE}3. 로그 파일 정리${NC}"
    
    read -p "로그 파일을 삭제하시겠습니까? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -f producer.log consumer.log
        echo -e "${GREEN}✅ 로그 파일 삭제 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  로그 파일을 유지합니다${NC}"
    fi
}

show_final_status() {
    echo -e "\n${BLUE}4. 종료 상태 확인${NC}"
    
    # 실행 중인 관련 프로세스 확인
    JAVA_PROCESSES=$(ps aux | grep -E "(producer|consumer|bootRun)" | grep -v grep | wc -l)
    if [ $JAVA_PROCESSES -eq 0 ]; then
        echo -e "${GREEN}✅ 모든 Java 프로세스 종료 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  일부 Java 프로세스가 여전히 실행 중일 수 있습니다${NC}"
    fi
    
    # Docker 컨테이너 확인
    LOCALSTACK_CONTAINERS=$(docker ps | grep localstack | wc -l)
    if [ $LOCALSTACK_CONTAINERS -eq 0 ]; then
        echo -e "${GREEN}✅ LocalStack 컨테이너 종료 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  LocalStack 컨테이너가 여전히 실행 중입니다${NC}"
    fi
}

# 메인 실행
echo "시스템을 안전하게 종료합니다..."
echo

stop_services
stop_docker
clean_logs
show_final_status

echo
echo -e "${GREEN}🎉 시스템 종료 완료${NC}"
echo -e "${BLUE}시스템을 다시 시작하려면 './test-sqs-system.sh'를 실행하세요.${NC}"
