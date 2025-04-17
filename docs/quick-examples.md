# 빠른 시작 예제 모음

이 문서는 Rate Limiter의 다양한 사용 예제를 제공합니다. 복사해서 바로 사용할 수 있습니다.

## 목차

-   [기본 사용법](#기본-사용법)
-   [실전 시나리오](#실전-시나리오)
-   [고급 기법](#고급-기법)
-   [테스트 예제](#테스트-예제)

---

## 기본 사용법

### 1. 사용자별 Rate Limiting

```java
@Service
public class UserService {

    /**
     * 사용자별로 분당 10번 제한
     */
    @RateLimiting(
        name = "user-service",
        cacheKey = "#userId"
    )
    public UserProfile getUserProfile(String userId) {
        return userRepository.findById(userId);
    }
}
```

**설정**

```yaml
rate-limiter:
    enabled: true
    rate-type: token_bucket

token-bucket:
    capacity: 10
    rate: 1
    rate-unit: minutes
```

---

### 2. IP 기반 Rate Limiting

```java
@RestController
public class PublicApiController {

    /**
     * IP 주소별로 시간당 100번 제한
     */
    @GetMapping("/api/public/data")
    @RateLimiting(
        name = "public-api",
        cacheKey = "#request.remoteAddr"
    )
    public ResponseEntity<Data> getPublicData(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return ResponseEntity.ok(dataService.getData());
    }
}
```

**설정**

```yaml
rate-limiter:
    rate-type: fixed_window_counter

fixed-window-counter:
    window-size: 3600 # 1시간
    request-limit: 100
```

---

### 3. API Key 기반 Rate Limiting

```java
@RestController
public class ApiController {

    /**
     * API Key별로 분당 1000번 제한
     */
    @PostMapping("/api/v1/orders")
    @RateLimiting(
        name = "order-api",
        cacheKey = "#apiKey"
    )
    public ResponseEntity<Order> createOrder(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.create(request));
    }
}
```

**설정**

```yaml
rate-limiter:
    rate-type: sliding_window_counter

sliding-window-counter:
    window-size: 60
    request-limit: 1000
```

---

## 실전 시나리오

### 시나리오 1: 로그인 보호 (Brute Force 방어)

```java
@Service
public class AuthService {

    /**
     * IP별 로그인 시도 제한
     * - 5분 내 5번 실패 시 차단
     */
    @RateLimiting(
        name = "login-attempts",
        cacheKey = "#request.remoteAddr",
        waitTime = 100L,
        leaseTime = 50L
    )
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        // 로그인 로직
        if (!authenticate(request.getUsername(), request.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return LoginResult.success();
    }
}
```

**설정**

```yaml
rate-limiter:
    enabled: true
    rate-type: fixed_window_counter
    lock-type: redis_redisson
    cache-type: redis

fixed-window-counter:
    window-size: 300 # 5분
    request-limit: 5 # 5번 시도
```

---

### 시나리오 2: 이메일 발송 제한

```java
@Service
public class EmailService {

    /**
     * 사용자별 이메일 발송 제한
     * - 하루 최대 50개
     */
    @RateLimiting(
        name = "email-sending",
        cacheKey = "#userId",
        fallbackMethodName = "emailLimitExceeded"
    )
    public void sendEmail(String userId, Email email) {
        emailProvider.send(email);
        log.info("Email sent to {}", email.getTo());
    }

    /**
     * 제한 초과 시 알림 이메일 발송
     */
    public void emailLimitExceeded(String userId, Email email) {
        log.warn("Email limit exceeded for user: {}", userId);
        notificationService.notifyUser(userId, "Daily email limit reached");
    }
}
```

**설정**

```yaml
rate-limiter:
    rate-type: fixed_window_counter

fixed-window-counter:
    window-size: 86400 # 24시간
    request-limit: 50
```

---

### 시나리오 3: 파일 다운로드 제한

```java
@RestController
public class FileDownloadController {

    /**
     * 사용자별 파일 다운로드 제한
     * - 시간당 10번
     */
    @GetMapping("/files/{fileId}/download")
    @RateLimiting(
        name = "file-download",
        cacheKey = "#userId",
        fallbackMethodName = "downloadLimitExceeded"
    )
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal String userId) {

        Resource file = fileService.getFile(fileId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                   "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file);
    }

    public ResponseEntity<ErrorResponse> downloadLimitExceeded(
            String fileId, String userId) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("Download limit exceeded. Try again later."));
    }
}
```

**설정**

```yaml
rate-limiter:
    rate-type: token_bucket

token-bucket:
    capacity: 10
    rate: 1
    rate-unit: hours
```

---

### 시나리오 4: 결제 API 보호

```java
@Service
public class PaymentService {

    /**
     * 사용자별 결제 요청 제한
     * - 1분당 최대 3번 (정확한 제한)
     */
    @RateLimiting(
        name = "payment-processing",
        cacheKey = "#request.userId",
        waitTime = 3000L,
        leaseTime = 2000L
    )
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        // 결제 로직
        Payment payment = Payment.builder()
            .userId(request.getUserId())
            .amount(request.getAmount())
            .build();

        Payment saved = paymentRepository.save(payment);

        // 외부 결제 게이트웨이 호출
        PaymentGatewayResponse response = paymentGateway.charge(
            request.getAmount(),
            request.getPaymentMethod()
        );

        return PaymentResult.from(saved, response);
    }
}
```

**설정**

```yaml
rate-limiter:
    enabled: true
    rate-type: sliding_window_logging # 정확한 제한
    lock-type: redis_redisson
    cache-type: redis

sliding-window-logging:
    window-size: 60 # 1분
    request-limit: 3 # 최대 3번
```

---

### 시나리오 5: SMS 인증 코드 발송

```java
@Service
public class SmsVerificationService {

    /**
     * 전화번호별 SMS 발송 제한
     * - 5분 내 3번
     */
    @RateLimiting(
        name = "sms-verification",
        cacheKey = "#phoneNumber"
    )
    public void sendVerificationCode(String phoneNumber) {
        String code = generateVerificationCode();

        // Redis에 인증 코드 저장 (5분 TTL)
        verificationCache.set(phoneNumber, code, Duration.ofMinutes(5));

        // SMS 발송
        smsProvider.send(phoneNumber, "Your verification code: " + code);

        log.info("Verification code sent to {}", phoneNumber);
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
```

**설정**

```yaml
rate-limiter:
    rate-type: fixed_window_counter

fixed-window-counter:
    window-size: 300 # 5분
    request-limit: 3
```

---

## 고급 기법

### 1. 티어별 차등 Rate Limiting

```java
@Service
public class TieredApiService {

    @Autowired
    private UserTierService tierService;

    /**
     * 사용자 티어에 따라 다른 Rate Limit 적용
     */
    @PostMapping("/api/search")
    public ResponseEntity<SearchResult> search(
            @AuthenticationPrincipal String userId,
            @RequestBody SearchRequest request) {

        UserTier tier = tierService.getUserTier(userId);

        switch (tier) {
            case FREE:
                return searchWithRateLimit(userId, request, "free-tier");
            case PREMIUM:
                return searchWithRateLimit(userId, request, "premium-tier");
            case ENTERPRISE:
                return searchWithoutRateLimit(userId, request);
            default:
                throw new IllegalStateException("Unknown tier: " + tier);
        }
    }

    @RateLimiting(
        name = "search-free",
        cacheKey = "#userId"
    )
    private ResponseEntity<SearchResult> searchWithRateLimit(
            String userId, SearchRequest request, String tier) {
        return performSearch(request);
    }

    private ResponseEntity<SearchResult> searchWithoutRateLimit(
            String userId, SearchRequest request) {
        return performSearch(request);
    }

    private ResponseEntity<SearchResult> performSearch(SearchRequest request) {
        return ResponseEntity.ok(searchEngine.search(request.getQuery()));
    }
}
```

**설정 (Free Tier)**

```yaml
rate-limiter:
    rate-type: token_bucket

token-bucket:
    capacity: 10 # Free: 10번
    rate: 1
    rate-unit: minutes
```

**설정 (Premium Tier)**

```yaml
token-bucket:
    capacity: 100 # Premium: 100번
    rate: 1
    rate-unit: minutes
```

---

### 2. 조건부 Rate Limiting

```java
@Service
public class ConditionalRateLimitService {

    /**
     * 특정 조건에서만 Rate Limiting 적용
     * - 프리미엄 사용자는 제한 없음
     * - 내부 IP는 제한 없음
     */
    @RateLimiting(
        name = "conditional-api",
        cacheKey = "#userId",
        executeCondition = "#isPremium == false and #isInternalIp == false"
    )
    public ApiResponse processRequest(
            String userId,
            boolean isPremium,
            boolean isInternalIp,
            ApiRequest request) {

        return apiService.process(request);
    }
}
```

---

### 3. 복합 키 Rate Limiting

```java
@Service
public class MultiKeyRateLimitService {

    /**
     * 여러 필드를 조합하여 Rate Limit 키 생성
     * - 사용자 + 액션 조합
     */
    @RateLimiting(
        name = "multi-key-api",
        cacheKey = "#userId + ':' + #action + ':' + #resourceType"
    )
    public ActionResult performAction(
            String userId,
            String action,
            String resourceType,
            ActionRequest request) {

        return actionService.execute(action, resourceType, request);
    }
}
```

**예시 키**

```
user123:create:document
user123:update:document
user123:delete:document
```

---

### 4. 메서드별 독립적 Rate Limiting

```java
@Service
public class ResourceService {

    /**
     * 같은 사용자라도 메서드별로 독립적인 Rate Limit
     */
    @RateLimiting(
        name = "create-resource",
        cacheKey = "#userId",
        ratePerMethod = true
    )
    public Resource createResource(String userId, ResourceRequest request) {
        return resourceRepository.save(Resource.from(request));
    }

    @RateLimiting(
        name = "update-resource",
        cacheKey = "#userId",
        ratePerMethod = true
    )
    public Resource updateResource(String userId, String resourceId, ResourceRequest request) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        resource.update(request);
        return resourceRepository.save(resource);
    }
}
```

---

### 5. 동적 Rate Limit (런타임 설정)

```java
@Service
public class DynamicRateLimitService {

    @Autowired
    private RateLimitConfigService configService;

    /**
     * 데이터베이스에서 Rate Limit 설정을 동적으로 로드
     */
    public ApiResponse processWithDynamicLimit(String userId, ApiRequest request) {
        // DB에서 사용자별 Rate Limit 설정 조회
        RateLimitConfig config = configService.getUserRateLimitConfig(userId);

        // 프로그래밍 방식으로 Rate Limit 체크
        boolean allowed = rateLimitChecker.check(
            userId,
            config.getRequestLimit(),
            config.getWindowSize()
        );

        if (!allowed) {
            throw new RateLimitException("Rate limit exceeded");
        }

        return apiService.process(request);
    }
}
```

---

## 테스트 예제

### 단위 테스트

```java
@SpringBootTest
class RateLimiterServiceTest {

    @Autowired
    private RateLimitingService service;

    @Test
    void shouldAllowRequestsWithinLimit() {
        // Given
        String userId = "test-user-1";
        int limit = 10;

        // When & Then
        for (int i = 0; i < limit; i++) {
            assertDoesNotThrow(() -> service.processRequest(userId));
        }
    }

    @Test
    void shouldDenyRequestsExceedingLimit() {
        // Given
        String userId = "test-user-2";
        int limit = 10;

        // When
        for (int i = 0; i < limit; i++) {
            service.processRequest(userId);
        }

        // Then
        assertThrows(RateLimitException.class,
            () -> service.processRequest(userId));
    }

    @Test
    void shouldResetAfterWindowExpires() throws InterruptedException {
        // Given
        String userId = "test-user-3";
        int limit = 10;
        int windowSize = 2; // 2초

        // When
        for (int i = 0; i < limit; i++) {
            service.processRequest(userId);
        }

        // Then
        assertThrows(RateLimitException.class,
            () -> service.processRequest(userId));

        // Wait for window to expire
        Thread.sleep((windowSize + 1) * 1000);

        // Should allow again
        assertDoesNotThrow(() -> service.processRequest(userId));
    }
}
```

---

### 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RateLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0-alpine")
        .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void shouldEnforceRateLimitAcrossRequests() {
        // Given
        String url = "http://localhost:" + port + "/api/demo/user-limit";
        UserRequest request = new UserRequest("test-user");
        int limit = 10;

        // When & Then
        for (int i = 0; i < limit; i++) {
            ResponseEntity<DemoResponse> response = restTemplate.postForEntity(
                url, request, DemoResponse.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Check rate limit headers
            assertThat(response.getHeaders().get("X-RateLimit-Remaining"))
                .isNotNull();
        }

        // 11th request should fail
        ResponseEntity<DemoResponse> response = restTemplate.postForEntity(
            url, request, DemoResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
```

---

### 부하 테스트

```java
@SpringBootTest
class RateLimiterLoadTest {

    @Autowired
    private RateLimitingService service;

    @Test
    void loadTestWithConcurrentRequests() throws InterruptedException {
        // Given
        int threads = 100;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * requestsPerThread);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < threads; i++) {
            final String userId = "user-" + i;
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        service.processRequest(userId);
                        successCount.incrementAndGet();
                    } catch (RateLimitException e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        int totalRequests = threads * requestsPerThread;
        double tps = (double) totalRequests / (duration / 1000.0);

        System.out.println("=== Load Test Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("TPS: " + String.format("%.2f", tps));

        assertThat(tps).isGreaterThan(1000); // 최소 1000 TPS

        executor.shutdown();
    }
}
```

---

## curl 명령어 예제

### 기본 요청

```bash
# 사용자별 Rate Limiting 테스트
curl -X POST http://localhost:8080/api/demo/user-limit \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123"}'
```

### Rate Limit 초과 테스트

```bash
# 10번 연속 요청 (limit 초과)
for i in {1..15}; do
  echo "Request $i:"
  curl -X POST http://localhost:8080/api/demo/user-limit \
    -H "Content-Type: application/json" \
    -d '{"userId":"user123"}' \
    -v 2>&1 | grep -E "< HTTP|X-RateLimit"
  echo ""
done
```

### HTTP 헤더 확인

```bash
# Rate Limit 헤더 확인
curl -X POST http://localhost:8080/api/demo/user-limit \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123"}' \
  -I
```

**응답 예시**

```
HTTP/1.1 200 OK
X-RateLimit-Remaining: 8
X-RateLimit-Limit: 10
X-RateLimit-Retry-After: 52
Content-Type: application/json
```

### 조건부 Rate Limiting 테스트

```bash
# 일반 사용자 (Rate Limiting 적용)
curl -X POST http://localhost:8080/api/demo/conditional-limit \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","tier":"BASIC"}'

# 프리미엄 사용자 (Rate Limiting 미적용)
curl -X POST http://localhost:8080/api/demo/conditional-limit \
  -H "Content-Type: application/json" \
  -d '{"userId":"user456","tier":"PREMIUM"}'
```

---

## 문제 해결 팁

### 1. Rate Limit이 동작하지 않을 때

```yaml
# application.yml 확인
rate-limiter:
    enabled: true # ← 이 값이 true인지 확인!
```

### 2. Redis 연결 오류

```bash
# Redis 연결 테스트
redis-cli ping
# 응답: PONG

# Docker로 Redis 실행
docker run -d -p 6379:6379 redis:7.0-alpine
```

### 3. 너무 많은 요청이 거부될 때

```yaml
# Capacity와 Rate 조정
token-bucket:
    capacity: 100 # 더 크게
    rate: 10 # 더 빠르게
    rate-unit: seconds
```

---

이 예제들을 참고하여 여러분의 프로젝트에 맞게 커스터마이징하세요! 🚀
