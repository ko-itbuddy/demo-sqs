package com.demo.producer.infrastructure.config;

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

/**
 * SQS 설정 클래스
 * LocalStack 환경에서 SQS 클라이언트 설정
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
        log.info("SQS 클라이언트 초기화: endpoint={}, region={}", sqsEndpoint, region);
        
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
                log.info("SQS 클라이언트 초기화 성공 - 연결 테스트 통과");
            } catch (Exception e) {
                log.warn("SQS 초기 연결 테스트 실패, 실제 사용 시 재시도 됩니다: {}", e.getMessage());
            }
            
            return client;
        } catch (Exception e) {
            log.error("SQS 클라이언트 초기화 실패: {}", e.getMessage(), e);
            throw new IllegalStateException("SQS 클라이언트 초기화에 실패했습니다", e);
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
     * (Producer에서는 사용하지 않지만 자동설정을 위해 정의)
     */
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}