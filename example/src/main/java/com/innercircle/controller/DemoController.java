package com.innercircle.controller;

import com.innercicle.annotations.RateLimiting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiter 데모 컨트롤러
 * 
 * <p>다양한 Rate Limiting 시나리오를 시연합니다.</p>
 * 
 * <h2>사용 예시:</h2>
 * <pre>
 * # 사용자별 Rate Limiting
 * curl -X POST http://localhost:8080/api/demo/user-limit \
 *   -H "Content-Type: application/json" \
 *   -d '{"userId":"user123"}'
 * 
 * # IP 기반 Rate Limiting
 * curl -X GET http://localhost:8080/api/demo/ip-limit
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    // 통계를 위한 카운터
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> successCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    
    /**
     * 기본 Rate Limiting 데모
     * 
     * <p>사용자별로 Rate Limiting을 적용합니다.</p>
     * 
     * @param request 사용자 요청
     * @return 응답 메시지
     */
    @PostMapping("/user-limit")
    @RateLimiting(
        name = "user-rate-limit",
        cacheKey = "#request.userId"
    )
    public ResponseEntity<DemoResponse> userRateLimit(@RequestBody UserRequest request) {
        log.info("Processing request for user: {}", request.getUserId());
        
        incrementCounter(requestCounts, "user-limit");
        incrementCounter(successCounts, "user-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .userId(request.getUserId())
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * IP 기반 Rate Limiting 데모
     * 
     * <p>클라이언트 IP 주소를 기반으로 Rate Limiting을 적용합니다.</p>
     * 
     * @param ipAddress 클라이언트 IP (헤더에서 추출)
     * @return 응답 메시지
     */
    @GetMapping("/ip-limit")
    @RateLimiting(
        name = "ip-rate-limit",
        cacheKey = "#ipAddress"
    )
    public ResponseEntity<DemoResponse> ipRateLimit(
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ipAddress) {
        log.info("Processing request from IP: {}", ipAddress);
        
        incrementCounter(requestCounts, "ip-limit");
        incrementCounter(successCounts, "ip-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .clientIp(ipAddress)
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * API Key 기반 Rate Limiting 데모
     * 
     * <p>API 키별로 다른 Rate Limit을 적용합니다.</p>
     * 
     * @param apiKey API 키
     * @return 응답 메시지
     */
    @GetMapping("/api-key-limit")
    @RateLimiting(
        name = "api-key-rate-limit",
        cacheKey = "#apiKey"
    )
    public ResponseEntity<DemoResponse> apiKeyRateLimit(
            @RequestHeader("X-API-Key") String apiKey) {
        log.info("Processing request with API key: {}", maskApiKey(apiKey));
        
        incrementCounter(requestCounts, "api-key-limit");
        incrementCounter(successCounts, "api-key-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .apiKey(maskApiKey(apiKey))
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * 조건부 Rate Limiting 데모
     * 
     * <p>프리미엄 사용자는 Rate Limiting을 적용하지 않습니다.</p>
     * 
     * @param request 사용자 요청
     * @return 응답 메시지
     */
    @PostMapping("/conditional-limit")
    @RateLimiting(
        name = "conditional-rate-limit",
        cacheKey = "#request.userId",
        executeCondition = "#request.tier != 'PREMIUM'"  // 프리미엄 유저는 제한 없음
    )
    public ResponseEntity<DemoResponse> conditionalRateLimit(@RequestBody TierRequest request) {
        log.info("Processing request for user: {}, tier: {}", request.getUserId(), request.getTier());
        
        incrementCounter(requestCounts, "conditional-limit");
        incrementCounter(successCounts, "conditional-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .userId(request.getUserId())
            .tier(request.getTier())
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * Fallback 메서드 데모
     * 
     * <p>Rate Limit 초과 시 fallback 메서드가 호출됩니다.</p>
     * 
     * @param request 사용자 요청
     * @return 응답 메시지
     */
    @PostMapping("/fallback-demo")
    @RateLimiting(
        name = "fallback-rate-limit",
        cacheKey = "#request.userId",
        fallbackMethodName = "rateLimitFallback"
    )
    public ResponseEntity<DemoResponse> fallbackDemo(@RequestBody UserRequest request) {
        log.info("Processing request for user: {}", request.getUserId());
        
        incrementCounter(requestCounts, "fallback-demo");
        incrementCounter(successCounts, "fallback-demo");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .userId(request.getUserId())
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * Rate Limit 초과 시 호출되는 Fallback 메서드
     */
    public ResponseEntity<DemoResponse> rateLimitFallback(UserRequest request) {
        log.warn("Rate limit exceeded for user: {}", request.getUserId());
        
        incrementCounter(requestCounts, "fallback-demo");
        incrementCounter(failureCounts, "fallback-demo");
        
        return ResponseEntity.status(429)  // Too Many Requests
            .body(DemoResponse.builder()
                .message("Rate limit exceeded. Please try again later.")
                .userId(request.getUserId())
                .timestamp(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .rateLimitExceeded(true)
                .build());
    }
    
    /**
     * 메서드별 독립적인 Rate Limiting 데모
     * 
     * <p>같은 사용자라도 메서드별로 독립적인 Rate Limit 적용</p>
     */
    @PostMapping("/method-limit")
    @RateLimiting(
        name = "method-rate-limit",
        cacheKey = "#request.userId",
        ratePerMethod = true  // 메서드별 독립적 제한
    )
    public ResponseEntity<DemoResponse> methodRateLimit(@RequestBody UserRequest request) {
        log.info("Processing method-specific request for user: {}", request.getUserId());
        
        incrementCounter(requestCounts, "method-limit");
        incrementCounter(successCounts, "method-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Request processed successfully")
            .userId(request.getUserId())
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * 복합 키 Rate Limiting 데모
     * 
     * <p>여러 필드를 조합하여 Rate Limit 키 생성</p>
     */
    @PostMapping("/composite-key-limit")
    @RateLimiting(
        name = "composite-key-rate-limit",
        cacheKey = "#request.userId + ':' + #request.action"  // 복합 키
    )
    public ResponseEntity<DemoResponse> compositeKeyRateLimit(@RequestBody ActionRequest request) {
        log.info("Processing action '{}' for user: {}", request.getAction(), request.getUserId());
        
        incrementCounter(requestCounts, "composite-key-limit");
        incrementCounter(successCounts, "composite-key-limit");
        
        return ResponseEntity.ok(DemoResponse.builder()
            .message("Action processed successfully")
            .userId(request.getUserId())
            .action(request.getAction())
            .timestamp(LocalDateTime.now())
            .requestId(UUID.randomUUID().toString())
            .build());
    }
    
    /**
     * 통계 조회
     * 
     * <p>각 엔드포인트의 요청 통계를 조회합니다.</p>
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, EndpointStats>> getStats() {
        Map<String, EndpointStats> stats = new ConcurrentHashMap<>();
        
        for (String key : requestCounts.keySet()) {
            stats.put(key, EndpointStats.builder()
                .totalRequests(requestCounts.getOrDefault(key, new AtomicInteger(0)).get())
                .successfulRequests(successCounts.getOrDefault(key, new AtomicInteger(0)).get())
                .failedRequests(failureCounts.getOrDefault(key, new AtomicInteger(0)).get())
                .successRate(calculateSuccessRate(key))
                .build());
        }
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 통계 초기화
     */
    @DeleteMapping("/stats")
    public ResponseEntity<String> resetStats() {
        requestCounts.clear();
        successCounts.clear();
        failureCounts.clear();
        return ResponseEntity.ok("Statistics reset successfully");
    }
    
    /**
     * Health Check (Rate Limiting 적용 안 됨)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "Rate Limiter Demo"
        ));
    }
    
    // ========== Helper Methods ==========
    
    private void incrementCounter(Map<String, AtomicInteger> map, String key) {
        map.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    private double calculateSuccessRate(String key) {
        int total = requestCounts.getOrDefault(key, new AtomicInteger(0)).get();
        int success = successCounts.getOrDefault(key, new AtomicInteger(0)).get();
        return total == 0 ? 0.0 : (double) success / total * 100.0;
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
    
    // ========== Request/Response DTOs ==========
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRequest {
        private String userId;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierRequest {
        private String userId;
        private String tier;  // BASIC, PREMIUM, ENTERPRISE
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionRequest {
        private String userId;
        private String action;  // CREATE, UPDATE, DELETE, etc.
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class DemoResponse {
        private String message;
        private String userId;
        private String clientIp;
        private String apiKey;
        private String tier;
        private String action;
        private LocalDateTime timestamp;
        private String requestId;
        private boolean rateLimitExceeded;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class EndpointStats {
        private int totalRequests;
        private int successfulRequests;
        private int failedRequests;
        private double successRate;
    }
}


