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
        
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
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