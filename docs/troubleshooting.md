# 트러블슈팅 가이드

Rate Limiter 사용 중 발생할 수 있는 문제와 해결 방법을 안내합니다.

## 목차

-   [일반적인 문제](#일반적인-문제)
-   [Redis 관련 문제](#redis-관련-문제)
-   [Lock 관련 문제](#lock-관련-문제)
-   [성능 문제](#성능-문제)
-   [설정 문제](#설정-문제)
-   [알고리즘 관련 문제](#알고리즘-관련-문제)

---

## 일반적인 문제

### ❌ Rate Limiter가 동작하지 않음

**증상**

```
Rate limiting이 적용되지 않고 모든 요청이 통과됨
```

**원인 및 해결**

**1. Rate Limiter가 비활성화되어 있음**

```yaml
# ❌ 잘못된 설정
rate-limiter:
  enabled: false    # 또는 설정 누락

# ✅ 올바른 설정
rate-limiter:
  enabled: true
```

**2. 어노테이션이 올바르게 적용되지 않음**

```java
// ❌ private 메서드에 적용
@RateLimiting(name = "test", cacheKey = "#id")
private void method(String id) { }

// ✅ public 메서드에 적용
@RateLimiting(name = "test", cacheKey = "#id")
public void method(String id) { }
```

**3. AOP 프록시 문제**

```java
// ❌ 같은 클래스 내에서 직접 호출
public class Service {
    public void caller() {
        this.rateLimitedMethod();  // AOP 미적용
    }

    @RateLimiting(name = "test", cacheKey = "#id")
    public void rateLimitedMethod(String id) { }
}

// ✅ 다른 Bean을 통해 호출
@Service
public class CallerService {
    @Autowired
    private RateLimitedService service;

    public void caller() {
        service.rateLimitedMethod();  // AOP 적용됨
    }
}
```

---

### ❌ RateLimitException이 발생함

**증상**

```
com.innercicle.advice.exceptions.RateLimitException: Rate limit exceeded
```

**원인**

-   Rate limit을 초과한 정상적인 동작입니다

**해결 방법**

**1. Fallback 메서드 사용**

```java
@Service
public class ApiService {

    @RateLimiting(
        name = "api",
        cacheKey = "#userId",
        fallbackMethodName = "fallback"
    )
    public ApiResponse callApi(String userId) {
        return externalApi.call();
    }

    // Fallback 메서드 (동일한 파라미터)
    public ApiResponse fallback(String userId) {
        return ApiResponse.error("Rate limit exceeded. Please try again later.");
    }
}
```

**2. Exception Handler로 처리**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException e) {
        ErrorResponse response = ErrorResponse.builder()
            .code("RATE_LIMIT_EXCEEDED")
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)  // 429
            .header("Retry-After", "60")
            .body(response);
    }
}
```

---

### ❌ SpEL 표현식이 동작하지 않음

**증상**

```
cacheKey가 "#userId"로 그대로 저장됨
```

**원인**

-   SpEL 표현식 문법 오류

**해결 방법**

```java
// ❌ 잘못된 SpEL
@RateLimiting(
    cacheKey = "userId"        // # 누락
)
public void method(String userId) { }

// ❌ 존재하지 않는 필드
@RateLimiting(
    cacheKey = "#request.wrongField"
)
public void method(Request request) { }

// ✅ 올바른 SpEL
@RateLimiting(
    cacheKey = "#userId"
)
public void method(String userId) { }

// ✅ 객체 필드 접근
@RateLimiting(
    cacheKey = "#request.userId"
)
public void method(Request request) { }

// ✅ 여러 필드 조합
@RateLimiting(
    cacheKey = "#userId + ':' + #action"
)
public void method(String userId, String action) { }
```

---

## Redis 관련 문제

### ❌ Redis 연결 실패

**증상**

```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**원인 및 해결**

**1. Redis가 실행되지 않음**

```bash
# Redis 상태 확인
redis-cli ping
# 응답: PONG (정상)

# Redis 시작
docker run -d -p 6379:6379 redis:7.0-alpine

# 또는
redis-server
```

**2. Redis 연결 정보 오류**

```yaml
# ❌ 잘못된 설정
spring:
  data:
    redis:
      host: wrong-host
      port: 6379

# ✅ 올바른 설정
spring:
  data:
    redis:
      host: localhost    # 또는 실제 Redis 호스트
      port: 6379
      password: ${REDIS_PASSWORD}  # 필요한 경우
      timeout: 2000ms
```

**3. 방화벽 문제**

```bash
# Redis 포트 확인
netstat -an | grep 6379

# 텔넷으로 연결 테스트
telnet localhost 6379
```

---

### ❌ Redis 메모리 부족

**증상**

```
OOM command not allowed when used memory > 'maxmemory'
```

**원인**

-   Redis 최대 메모리 도달

**해결 방법**

**1. 메모리 사용량 확인**

```bash
# Redis CLI에서
redis-cli
> INFO memory
> CONFIG GET maxmemory
```

**2. 메모리 정책 설정**

```bash
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru  # LRU로 자동 삭제
```

**3. TTL 설정으로 자동 정리**

```java
// 캐시에 TTL 설정
cache.set(key, value, Duration.ofSeconds(windowSize * 2));
```

**4. 주기적인 정리**

```java
@Scheduled(cron = "0 0 * * * *")  // 매 시간
public void cleanupExpiredKeys() {
    Set<String> keys = cache.keys("rate-limit:*");
    long now = System.currentTimeMillis();

    for (String key : keys) {
        TokenInfo info = cache.get(key);
        if (info != null && isExpired(info, now)) {
            cache.delete(key);
        }
    }
}
```

---

### ❌ Redis Cluster 관련 문제

**증상**

```
io.lettuce.core.RedisCommandExecutionException: MOVED 12345 127.0.0.1:6380
```

**원인**

-   Cluster 재구성 또는 노드 장애

**해결 방법**

**1. Cluster 토폴로지 자동 갱신**

```yaml
spring:
    data:
        redis:
            cluster:
                nodes:
                    - node1:6379
                    - node2:6379
                    - node3:6379
                max-redirects: 3
            lettuce:
                cluster:
                    refresh:
                        adaptive: true # 토폴로지 자동 감지
                        period: 60s
```

**2. Cluster 상태 확인**

```bash
redis-cli --cluster check 127.0.0.1:6379
```

---

## Lock 관련 문제

### ❌ LockAcquisitionFailureException 발생

**증상**

```
com.innercicle.advice.exceptions.LockAcquisitionFailureException: Lock 획득 실패
```

**원인**

-   Lock 대기 시간 내에 획득 실패

**해결 방법**

**1. 대기 시간 증가**

```java
@RateLimiting(
    name = "api",
    cacheKey = "#userId",
    waitTime = 5000L,      // 5초로 증가
    leaseTime = 2000L
)
```

**2. Lock 타임아웃 처리**

```java
@ExceptionHandler(LockAcquisitionFailureException.class)
public ResponseEntity<ErrorResponse> handleLockFailure(LockAcquisitionFailureException e) {
    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)  // 503
        .header("Retry-After", "1")
        .body(ErrorResponse.of("Service temporarily unavailable"));
}
```

**3. Lock Striping 사용**

```java
// Lock 경합 감소
@Configuration
public class LockConfig {

    @Bean
    public LockManager lockManager() {
        return new StripedLockManager(16);  // 16개의 Lock 사용
    }
}
```

---

### ❌ Lock이 해제되지 않음 (Deadlock)

**증상**

```
특정 사용자의 요청이 계속 실패함
Lock이 영구적으로 유지됨
```

**원인**

-   Exception 발생 시 Lock 미해제
-   leaseTime이 너무 김

**해결 방법**

**1. Finally 블록으로 확실한 해제**

AOP에서 자동 처리되지만, 수동으로 Lock을 사용할 경우:

```java
try {
    lockManager.tryLock(annotation);
    // 비즈니스 로직
} finally {
    lockManager.unlock();  // 항상 실행
}
```

**2. leaseTime 단축**

```java
@RateLimiting(
    name = "api",
    cacheKey = "#userId",
    waitTime = 3000L,
    leaseTime = 1000L      // 1초로 단축 (자동 해제)
)
```

**3. 수동으로 Lock 해제**

```bash
# Redis CLI에서 Lock 키 삭제
redis-cli
> DEL rate-limiter:lock:userId123
```

---

## 성능 문제

### ❌ 응답 시간이 느림

**증상**

```
API 응답 시간이 평소보다 2-3배 느림
```

**원인 및 해결**

**1. Redis 네트워크 레이턴시**

```yaml
# Redis 타임아웃 설정
spring:
    data:
        redis:
            timeout: 500ms # 짧게 설정

# Connection Pool 최적화
lettuce:
    pool:
        max-active: 20
        min-idle: 5
```

**2. Lock 대기 시간**

```java
// 대기 시간 단축
@RateLimiting(
    waitTime = 100L,     // 100ms로 단축
    leaseTime = 50L
)
```

**3. 알고리즘 변경**

```yaml
# Fixed Window Counter로 변경 (가장 빠름)
rate-limiter:
    rate-type: fixed_window_counter
```

---

### ❌ 높은 CPU 사용률

**증상**

```
CPU 사용률이 80% 이상
```

**원인 및 해결**

**1. Sliding Window Logging의 오버헤드**

```yaml
# ❌ 메모리 집약적
rate-limiter:
  rate-type: sliding_window_logging

# ✅ 효율적인 알고리즘으로 변경
rate-limiter:
  rate-type: sliding_window_counter
```

**2. Lock 경합**

```java
// Lock Striping으로 경합 감소
@Bean
public LockManager lockManager() {
    return new StripedLockManager(
        Runtime.getRuntime().availableProcessors() * 2
    );
}
```

---

### ❌ 메모리 사용량 증가

**증상**

```
Heap 메모리 사용량이 지속적으로 증가
```

**원인 및 해결**

**1. 캐시 크기 제한**

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheTemplate cacheTemplate() {
        return new BoundedCacheTemplate(
            10000  // 최대 10,000개 엔트리
        );
    }
}
```

**2. TTL 설정**

```java
// 모든 캐시 엔트리에 TTL 설정
cache.set(key, value, Duration.ofMinutes(10));
```

**3. 주기적인 GC**

```bash
# JVM 옵션
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -jar app.jar
```

---

## 설정 문제

### ❌ Auto Configuration이 적용되지 않음

**증상**

```
Bean 생성 오류 또는 Rate Limiter가 초기화되지 않음
```

**원인 및 해결**

**1. 의존성 누락**

```groovy
// build.gradle
dependencies {
    implementation 'com.github.your-username:rate-limiter-spring-boot-starter:1.0.0'
}
```

**2. Auto Configuration 파일 확인**

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

파일에 다음 내용이 있어야 함:

```
com.innercicle.RateLimiterAutoConfiguration
```

**3. Component Scan 경로 확인**

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.yourpackage",
    "com.innercicle"  // Rate Limiter 패키지 포함
})
public class Application {
    // ...
}
```

---

### ❌ 여러 알고리즘을 동시에 사용하고 싶음

**증상**

```
하나의 알고리즘만 설정 가능
```

**해결 방법**

현재는 애플리케이션당 하나의 알고리즘만 지원합니다. 여러 알고리즘을 사용하려면:

**1. Profile 사용**

```yaml
# application-tokenBucket.yml
rate-limiter:
  rate-type: token_bucket

# application-slidingWindow.yml
rate-limiter:
  rate-type: sliding_window_counter
```

```bash
# 실행 시 프로파일 지정
java -Dspring.profiles.active=tokenBucket -jar app.jar
```

**2. 커스텀 구현**

```java
@Configuration
public class MultiAlgorithmConfig {

    @Bean("tokenBucketHandler")
    public RateLimitHandler tokenBucketHandler() {
        return new TokenBucketHandler(/*...*/);
    }

    @Bean("slidingWindowHandler")
    public RateLimitHandler slidingWindowHandler() {
        return new SlidingWindowCounterHandler(/*...*/);
    }
}

// 사용
@Autowired
@Qualifier("tokenBucketHandler")
private RateLimitHandler handler;
```

---

## 알고리즘 관련 문제

### ❌ Fixed Window Counter의 경계 문제

**증상**

```
윈도우 경계에서 2배의 트래픽 발생
```

**예시**

```
14:00:59 - 100개 요청
14:01:00 - 100개 요청
→ 1초 내에 200개 요청 (limit의 2배)
```

**해결 방법**

**1. Sliding Window Counter로 변경**

```yaml
rate-limiter:
    rate-type: sliding_window_counter # 경계 문제 완화

sliding-window-counter:
    window-size: 60
    request-limit: 100
```

**2. Rate limit 조정**

```yaml
# 실제 원하는 limit의 50%로 설정
fixed-window-counter:
    window-size: 60
    request-limit: 50 # 경계에서 최대 100개
```

---

### ❌ Token Bucket의 초기 버스트

**증상**

```
서비스 시작 시 한번에 많은 요청 처리
```

**원인**

-   초기에 버킷이 가득 차있음

**해결 방법**

**1. Capacity 조정**

```yaml
token-bucket:
    capacity: 10 # 초기 burst 크기 제한
    rate: 10
    rate-unit: seconds
```

**2. Warm-up 구현**

```java
@Component
public class TokenBucketWarmer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private CacheTemplate cache;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 초기 토큰을 절반만 설정
        TokenBucketInfo info = TokenBucketInfo.createDefault(properties);
        info.setTokens(info.getCapacity() / 2);

        // 미리 캐시에 저장
        cache.set("warm-up-key", info);
    }
}
```

---

### ❌ Sliding Window Logging의 메모리 문제

**증상**

```
메모리 사용량이 계속 증가
OutOfMemoryError 발생
```

**원인**

-   모든 타임스탬프를 저장하여 메모리 부족

**해결 방법**

**1. Request limit 낮추기**

```yaml
sliding-window-logging:
    window-size: 60
    request-limit: 100 # 낮은 값으로 제한
```

**2. 다른 알고리즘으로 변경**

```yaml
# Sliding Window Counter 권장
rate-limiter:
    rate-type: sliding_window_counter
```

**3. 주기적인 정리**

```java
@Scheduled(fixedRate = 30000)  // 30초마다
public void cleanupTimestamps() {
    long cutoff = System.currentTimeMillis() - windowSize;
    cache.removeOldTimestamps(cutoff);
}
```

---

## 디버깅 팁

### 로그 레벨 조정

```yaml
logging:
    level:
        com.innercicle: DEBUG # Rate Limiter 로그
        com.innercicle.aop: TRACE # AOP 상세 로그
        io.lettuce.core: DEBUG # Redis 클라이언트 로그
        org.redisson: DEBUG # Redisson 로그
```

### Actuator로 상태 확인

```yaml
management:
    endpoints:
        web:
            exposure:
                include: health,metrics,info,beans
```

```bash
# Health 체크
curl http://localhost:8080/actuator/health

# Metrics 확인
curl http://localhost:8080/actuator/metrics/rate_limiter.requests.allowed

# Beans 확인
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans | keys[] | select(contains("rate"))'
```

### Redis 모니터링

```bash
# Redis 명령어 실시간 모니터링
redis-cli MONITOR

# 메모리 사용량
redis-cli INFO memory

# Slow log 확인
redis-cli SLOWLOG GET 10
```

---

## 도움 받기

문제가 해결되지 않으면:

1. **GitHub Issues**: [이슈 등록](https://github.com/your-repo/issues)
2. **Discussions**: [질문하기](https://github.com/your-repo/discussions)
3. **Stack Overflow**: `rate-limiter-spring-boot` 태그 사용

이슈 등록 시 다음 정보를 포함해주세요:

-   Spring Boot 버전
-   Java 버전
-   Rate Limiter 버전
-   설정 파일 (application.yml)
-   에러 로그
-   재현 방법

---

이 문서는 지속적으로 업데이트됩니다. 새로운 문제를 발견하면 [GitHub](https://github.com/your-repo)에 공유해주세요!
