#!/bin/bash
# SQS 큐 초기화 스크립트
# 실무 프로젝트 적용 시 이 스크립트를 참고하여 필요한 큐를 추가하세요

set -e

echo 'SQS 큐 생성 시작...'

# AWS CLI 엔드포인트 설정
ENDPOINT_URL=${AWS_ENDPOINT_URL:-http://localstack:4566}
REGION=${AWS_DEFAULT_REGION:-ap-northeast-2}

# 큐 생성 함수
create_queue_with_dlq() {
    local QUEUE_NAME=$1
    local DLQ_NAME=$2
    local MAX_RECEIVE_COUNT=${3:-3}  # 기본값 3회
    
    echo "큐 생성: $QUEUE_NAME, DLQ: $DLQ_NAME"
    
    # 메인 큐 생성
    aws sqs create-queue \
        --queue-name "$QUEUE_NAME" \
        --endpoint-url="$ENDPOINT_URL" \
        --region="$REGION"
    
    # DLQ 생성
    aws sqs create-queue \
        --queue-name "$DLQ_NAME" \
        --endpoint-url="$ENDPOINT_URL" \
        --region="$REGION"
    
    # DLQ ARN 가져오기
    local DLQ_ARN=$(aws sqs get-queue-attributes \
        --queue-url="$ENDPOINT_URL/000000000000/$DLQ_NAME" \
        --attribute-names QueueArn \
        --endpoint-url="$ENDPOINT_URL" \
        --region="$REGION" \
        --query 'Attributes.QueueArn' \
        --output text)
    
    # 메인 큐에 DLQ redrive policy 설정
    aws sqs set-queue-attributes \
        --queue-url="$ENDPOINT_URL/000000000000/$QUEUE_NAME" \
        --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"'$DLQ_ARN'\",\"maxReceiveCount\":'$MAX_RECEIVE_COUNT'}"}' \
        --endpoint-url="$ENDPOINT_URL" \
        --region="$REGION"
    
    echo "✅ $QUEUE_NAME 큐 생성 완료 (DLQ: $DLQ_NAME, 최대 재시도: $MAX_RECEIVE_COUNT)"
}

# 기본 큐 생성
create_queue_with_dlq "order-processing-queue" "order-processing-dlq" 3

# 추가 큐 예시 (필요시 주석 해제)
# create_queue_with_dlq "payment-processing-queue" "payment-processing-dlq" 5
# create_queue_with_dlq "notification-queue" "notification-dlq" 2
# create_queue_with_dlq "user-activity-queue" "user-activity-dlq" 3

echo '🎉 SQS 큐 생성 완료!'

# 생성된 큐 목록 확인
echo '📋 생성된 큐 목록:'
aws sqs list-queues \
    --endpoint-url="$ENDPOINT_URL" \
    --region="$REGION"

echo 'ℹ️ 큐 상세 정보 확인:'
for queue in $(aws sqs list-queues --endpoint-url="$ENDPOINT_URL" --region="$REGION" --query 'QueueUrls[]' --output text); do
    queue_name=$(basename "$queue")
    echo "  - $queue_name"
done