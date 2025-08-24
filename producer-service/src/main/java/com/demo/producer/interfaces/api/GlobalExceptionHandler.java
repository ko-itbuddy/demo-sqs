package com.demo.producer.interfaces.api;

import com.demo.producer.application.order.OrderService;
import com.demo.producer.infrastructure.messaging.SqsMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 글로벌 예외 처리기
 * API 계층에서 발생하는 모든 예외를 일관된 형태로 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 유효성 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("유효성 검증 실패: {}", errors);
        
        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("입력 데이터가 유효하지 않습니다")
                .details(errors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 주문 미발견 예외 처리
     */
    @ExceptionHandler(OrderService.OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderService.OrderNotFoundException ex, WebRequest request) {
        
        log.warn("주문 미발견: {}", ex.getMessage());
        
        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Order Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 메시지 발행 실패 예외 처리
     */
    @ExceptionHandler(SqsMessagePublisher.MessagePublishException.class)
    public ResponseEntity<ErrorResponse> handleMessagePublishException(
            SqsMessagePublisher.MessagePublishException ex, WebRequest request) {
        
        log.error("메시지 발행 실패: {}", ex.getMessage(), ex);
        
        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Message Publish Failed")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
    
    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("예상치 못한 오류 발생: {}", ex.getMessage(), ex);
        
        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("서버에서 예상치 못한 오류가 발생했습니다")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
    
    /**
     * 에러 응답 DTO
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, Object> details
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private String path;
            private Map<String, Object> details;
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder status(int status) {
                this.status = status;
                return this;
            }
            
            public Builder error(String error) {
                this.error = error;
                return this;
            }
            
            public Builder message(String message) {
                this.message = message;
                return this;
            }
            
            public Builder path(String path) {
                this.path = path;
                return this;
            }
            
            public Builder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }
            
            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, error, message, path, details);
            }
        }
    }
}