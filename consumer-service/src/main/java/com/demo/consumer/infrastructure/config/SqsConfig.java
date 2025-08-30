package com.demo.consumer.infrastructure.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;
import java.time.Duration;

/**
 * Consumer SQS 설정 클래스
 * LocalStack 환경에서 SQS 클라이언트 및 리스너 설정
 */
@Slf4j
@Configuration
public class SqsConfig {
    
    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String sqsEndpoint;
    
    @Value("${spring.cloud.aws.region.static}")
    private String region;
    
    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    /**
     * SQS 비동기 클라이언트 설정
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        log.info("Consumer SQS 클라이언트 초기화: endpoint={}, region={}", sqsEndpoint, region);
        
        try {
            SqsAsyncClient client = SqsAsyncClient.builder()
                    .endpointOverride(URI.create(sqsEndpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            
            // SQS 연결 테스트
            try {
                client.listQueues().get();
                log.info("Consumer SQS 클라이언트 초기화 성공 - 연결 테스트 통과");
            } catch (Exception e) {
                log.warn("Consumer SQS 초기 연결 테스트 실패, 실제 사용 시 재시도 됩니다: {}", e.getMessage());
            }
            
            return client;
        } catch (Exception e) {
            log.error("Consumer SQS 클라이언트 초기화 실패: {}", e.getMessage(), e);
            throw new IllegalStateException("Consumer SQS 클라이언트 초기화에 실패했습니다", e);
        }
    }
    
    /**
     * SQS 템플릿 설정
     */
    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }
    
    /**
     * SQS 메시지 리스너 컨테이너 팩토리 설정
     * Consumer에서 메시지 수신을 위한 설정
     */
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory
                .builder()
                .configure(options -> options
                        .maxConcurrentMessages(10) // 동시 처리 메시지 수
                        .maxMessagesPerPoll(10)   // 한번에 가져올 메시지 수
                        .pollTimeout(Duration.ofSeconds(10)) // 폴링 타임아웃
                        .maxDelayBetweenPolls(Duration.ofSeconds(5)) // 폴링 간격
                        .acknowledgementInterval(Duration.ofSeconds(1)) // ACK 배치 간격
                        .acknowledgementThreshold(5) // ACK 배치 임계값
                )
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}