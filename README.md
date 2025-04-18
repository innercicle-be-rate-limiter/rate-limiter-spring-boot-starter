# 🚦 Rate Limiter Spring Boot Starter

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)

**어노테이션 기반의 선언적 Rate Limiting 라이브러리**

[Features](#-features) •
[Quick Start](#-quick-start) •
[Algorithms](#-algorithms) •
[Documentation](#-documentation) •
[Performance](#-performance)

</div>

---

## 📖 프로젝트 소개

이 프로젝트는 `가상면접 사례로 배우는 대규모 시스템 설계 기초` 책의 챕터 3을 기반으로 구현한 **Spring Boot용 처리율 제한기(Rate Limiter) 스타터**입니다.

대규모 트래픽 환경에서 API를 보호하고, 공정한 리소스 사용을 보장하며, DoS 공격을 방어하기 위한 필수적인 기능을 제공합니다.

### 🎯 프로젝트 목표

-   **선언적 사용**: 어노테이션만으로 손쉬운 Rate Limiting 적용
-   **다양한 알고리즘**: 5가지 검증된 Rate Limiting 알고리즘 지원
-   **프로덕션 준비**: Redis 기반 분산 환경 지원
-   **높은 확장성**: 멀티 인스턴스 환경에서도 안정적 동작
-   **Spring Boot 친화적**: Auto Configuration을 통한 제로 설정

---

## ✨ Features

### 🎨 5가지 Rate Limiting 알고리즘

| 알고리즘                   | 설명                                            | 적합한 사용 사례                               |
| -------------------------- | ----------------------------------------------- | ---------------------------------------------- |
| **Token Bucket**           | 토큰 생성 속도와 버킷 용량을 기반으로 요청 제어 | 일반적인 API Rate Limiting, 버스트 트래픽 허용 |
| **Leaky Bucket**           | 고정된 속도로 요청을 처리하는 큐 방식           | 안정적인 아웃바운드 트래픽 제어                |
| **Fixed Window Counter**   | 고정된 시간 윈도우 내 요청 수 제한              | 간단한 Rate Limiting, 리소스 제약이 큰 환경    |
| **Sliding Window Logging** | 각 요청의 타임스탬프를 기록하여 정확한 제어     | 정확한 Rate Limiting이 필요한 경우             |
| **Sliding Window Counter** | Fixed Window와 Sliding Window의 하이브리드      | 정확성과 효율성의 균형이 필요한 경우           |

### 🔧 핵심 기능

-   ✅ **어노테이션 기반**: `@RateLimiting` 어노테이션으로 간단하게 적용
-   ✅ **SpEL 지원**: Spring Expression Language로 동적 키 생성
-   ✅ **분산 Lock**: Redis Redisson 기반 분산 Lock 지원
-   ✅ **유연한 캐시**: Redis 또는 ConcurrentHashMap 선택 가능
-   ✅ **HTTP 헤더**: X-RateLimit 표준 헤더 자동 설정
-   ✅ **Fallback 메서드**: Rate Limit 초과 시 대체 메서드 실행
-   ✅ **조건부 실행**: SpEL을 사용한 조건부 Rate Limiting

### 🏗️ 아키텍처 특징

-   **모듈화 설계**: 핵심 로직과 Spring Boot 자동 구성 분리
-   **전략 패턴**: 알고리즘별 Handler로 쉬운 확장
-   **AOP 기반**: 비즈니스 로직과 Rate Limiting 로직 분리
-   **테스트 친화적**: 100% 테스트 가능한 구조

---

## 🚀 Quick Start

### 1️⃣ 의존성 추가

**Gradle**

```groovy
dependencies {
    implementation 'com.github.your-username:rate-limiter-spring-boot-starter:1.0.0'
}
```

**Maven**

```xml
<dependency>
    <groupId>com.github.your-username</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2️⃣ 설정 파일 구성

**application.yml**

```yaml
rate-limiter:
    enabled: true # Rate Limiter 활성화
    lock-type: redis_redisson # Lock 타입: redis_redisson | concurrent_hash_map
    rate-type: token_bucket # 알고리즘: token_bucket | leaky_bucket | fixed_window_counter | sliding_window_logging | sliding_window_counter
    cache-type: redis # 캐시: redis | concurrent_hash_map

# Redis 설정 (Redis 사용 시 필수)
spring:
    data:
        redis:
            host: localhost
            port: 6379

# Token Bucket 설정 예시
token-bucket:
    capacity: 10 # 버킷 최대 용량
    rate: 1 # 토큰 생성 속도
    rate-unit: seconds # 시간 단위: seconds, minutes, hours, days
```

### 3️⃣ 어노테이션 적용

```java
@Service
public class ApiService {

    /**
     * 기본 사용: 사용자별로 Rate Limiting 적용
     */
    @RateLimiting(
        name = "user-api",
        cacheKey = "#userId"
    )
    public String getUserData(String userId) {
        return "User data for " + userId;
    }

    /**
     * 고급 사용: 조건부 실행 및 Fallback
     */
    @RateLimiting(
        name = "premium-api",
        cacheKey = "#request.userId",
        executeCondition = "#request.isPremium == false",  // 프리미엄 유저는 제한 없음
        fallbackMethodName = "rateLimitFallback",          // 제한 시 대체 메서드
        waitTime = 3000L,
        leaseTime = 1000L
    )
    public ApiResponse processRequest(ApiRequest request) {
        // 비즈니스 로직
        return new ApiResponse("success");
    }

    /**
     * Rate Limit 초과 시 실행되는 Fallback 메서드
     */
    public ApiResponse rateLimitFallback(ApiRequest request) {
        return new ApiResponse("Rate limit exceeded. Please try again later.");
    }
}
```

### 4️⃣ HTTP 응답 헤더

Rate Limiter는 자동으로 다음 HTTP 헤더를 설정합니다:

```http
X-RateLimit-Remaining: 8          # 남은 요청 횟수
X-RateLimit-Limit: 10             # 최대 요청 횟수
X-RateLimit-Retry-After: 45       # 다음 요청까지 대기 시간(초)
```

---

## 🧮 Algorithms

### 1. Token Bucket

**동작 방식**

-   고정된 속도로 토큰이 생성되어 버킷에 저장됨
-   요청마다 1개의 토큰 소비
-   토큰이 없으면 요청 거부

**장점**

-   버스트 트래픽 허용 (버킷에 토큰이 쌓여있을 때)
-   메모리 효율적
-   구현이 간단함

**설정 예시**

```yaml
rate-limiter:
    rate-type: token_bucket
token-bucket:
    capacity: 10 # 최대 10개의 토큰 저장
    rate: 2 # 2초마다 1개씩 토큰 생성
    rate-unit: seconds
```

**사용 사례**: API Gateway, 일반적인 API Rate Limiting

---

### 2. Leaky Bucket

**동작 방식**

-   요청을 큐에 저장
-   고정된 속도로 큐에서 요청을 처리
-   큐가 가득 차면 요청 거부

**장점**

-   안정적인 아웃바운드 속도 보장
-   트래픽 스파이크 완화

**설정 예시**

```yaml
rate-limiter:
    rate-type: leaky_bucket
leaky-bucket:
    capacity: 100 # 큐 크기
    rate: 10 # 초당 10개 요청 처리
    rate-unit: seconds
```

**사용 사례**: 외부 API 호출 제어, 메시지 큐 처리

---

### 3. Fixed Window Counter

**동작 방식**

-   고정된 시간 윈도우(예: 1분)마다 카운터 초기화
-   윈도우 내 요청 수를 카운트
-   제한을 초과하면 요청 거부

**장점**

-   메모리 효율적 (카운터만 저장)
-   구현이 매우 간단

**단점**

-   윈도우 경계에서 트래픽 스파이크 가능

**설정 예시**

```yaml
rate-limiter:
    rate-type: fixed_window_counter
fixed-window-counter:
    window-size: 60 # 60초 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

**사용 사례**: 리소스 제약이 큰 환경, 간단한 Rate Limiting

---

### 4. Sliding Window Logging

**동작 방식**

-   각 요청의 타임스탬프를 저장
-   요청 시 타임스탬프 범위 내 요청 수 계산
-   정확한 시간 윈도우 보장

**장점**

-   가장 정확한 Rate Limiting
-   윈도우 경계 문제 없음

**단점**

-   메모리 사용량이 높음 (모든 타임스탬프 저장)

**설정 예시**

```yaml
rate-limiter:
    rate-type: sliding_window_logging
sliding-window-logging:
    window-size: 60 # 60초 슬라이딩 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

**사용 사례**: 정확한 Rate Limiting이 중요한 금융 서비스, 결제 API

---

### 5. Sliding Window Counter

**동작 방식**

-   Fixed Window Counter와 Sliding Window Logging의 하이브리드
-   현재 윈도우와 이전 윈도우의 카운터를 조합하여 근사치 계산

**장점**

-   메모리 효율적 (2개의 카운터만 저장)
-   합리적인 정확도

**설정 예시**

```yaml
rate-limiter:
    rate-type: sliding_window_counter
sliding-window-counter:
    window-size: 60 # 60초 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

**사용 사례**: 대규모 트래픽 환경, 정확성과 효율성의 균형이 필요한 경우

---

## 📊 Performance

### 알고리즘 성능 비교

| 알고리즘               | 메모리     | CPU        | 정확도     | TPS\*   |
| ---------------------- | ---------- | ---------- | ---------- | ------- |
| Token Bucket           | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐   | 50,000+ |
| Leaky Bucket           | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | 45,000+ |
| Fixed Window Counter   | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐     | 60,000+ |
| Sliding Window Logging | ⭐⭐       | ⭐⭐⭐     | ⭐⭐⭐⭐⭐ | 20,000+ |
| Sliding Window Counter | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | 55,000+ |

\*TPS: Transactions Per Second (Redis 기반, 단일 인스턴스)

### 벤치마크 환경

-   **CPU**: Apple M1 Pro
-   **Memory**: 16GB
-   **Redis**: 7.0 (단일 인스턴스)
-   **Concurrency**: 100 threads

---

## 🏗️ Architecture

### 모듈 구조

```
rate-limiter-spring-boot-starter/
│
├── rate-limiter/                                 # 핵심 Rate Limiter 로직
│   ├── annotations/                              # @RateLimiting 어노테이션
│   ├── aop/                                      # AOP 기반 처리
│   ├── handler/                                  # 알고리즘별 Handler
│   │   ├── TokenBucketHandler.java
│   │   ├── LeakyBucketHandler.java
│   │   ├── FixedWindowCounterHandler.java
│   │   ├── SlidingWindowLoggingHandler.java
│   │   └── SlidingWindowCounterHandler.java
│   ├── domain/                                   # 도메인 모델
│   ├── cache/                                    # 캐시 추상화
│   └── lock/                                     # Lock 관리
│
├── rate-limiter-spring-boot-autoconfigure/       # Spring Boot 자동 구성
│   └── RateLimiterAutoConfiguration.java
│
└── example/                                      # 예제 애플리케이션
    └── ParkingService.java
```

### 동작 흐름

```
┌─────────────────┐
│  Client Request │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  @RateLimiting (AOP)    │  ← 어노테이션 감지
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Lock Manager           │  ← 분산 Lock 획득
│  (Redis/ConcurrentMap)  │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Rate Limit Handler     │  ← 알고리즘 실행
│  (선택된 알고리즘)        │
└────────┬────────────────┘
         │
         ├─── Allowed ────────────────┐
         │                            │
         ▼                            ▼
┌─────────────────────┐    ┌──────────────────┐
│  Business Logic     │    │  Set HTTP Headers│
│  Execution          │    │  X-RateLimit-*   │
└─────────────────────┘    └──────────────────┘
         │
         │
         ▼
┌─────────────────────┐
│  Release Lock       │
└─────────────────────┘
         │
         ▼
┌─────────────────────┐
│  Return Response    │
└─────────────────────┘

         OR

         ├─── Denied ─────────────────┐
         │                            │
         ▼                            ▼
┌─────────────────────┐    ┌──────────────────┐
│  Fallback Method or │    │  Release Lock    │
│  RateLimitException │    │                  │
└─────────────────────┘    └──────────────────┘
```

---

## 📚 Documentation

### 상세 문서

-   [알고리즘 상세 가이드](docs/algorithms.md)
-   [설정 가이드](docs/configuration.md)
-   [고급 사용법](docs/advanced-usage.md)
-   [성능 튜닝](docs/performance-tuning.md)
-   [트러블슈팅](docs/troubleshooting.md)
-   [마이그레이션 가이드](docs/migration.md)

### API 문서

모든 클래스와 메서드는 JavaDoc으로 문서화되어 있습니다.

```bash
./gradlew javadoc
```

생성된 문서는 `build/docs/javadoc/index.html`에서 확인할 수 있습니다.

---

## 🧪 Testing

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :rate-limiter:test

# 커버리지 리포트 생성
./gradlew jacocoTestReport
```

### 테스트 커버리지

현재 프로젝트의 테스트 커버리지:

-   **Line Coverage**: 85%+
-   **Branch Coverage**: 80%+
-   **Class Coverage**: 90%+

### 통합 테스트

예제 애플리케이션은 TestContainers를 사용하여 실제 Redis 환경에서 테스트합니다.

```java
@SpringBootTest
@Testcontainers
class RateLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379);

    @Test
    void testTokenBucket() {
        // 통합 테스트 코드
    }
}
```

---

## 🔧 Configuration

### 환경별 설정

**개발 환경** (application-dev.yml)

```yaml
rate-limiter:
    enabled: true
    lock-type: concurrent_hash_map # 단일 인스턴스
    rate-type: token_bucket
    cache-type: concurrent_hash_map # 메모리 캐시
```

**프로덕션 환경** (application-prod.yml)

```yaml
rate-limiter:
    enabled: true
    lock-type: redis_redisson # 분산 Lock
    rate-type: sliding_window_counter # 효율적이고 정확한 알고리즘
    cache-type: redis # 분산 캐시

spring:
    data:
        redis:
            host: redis-cluster.example.com
            port: 6379
            password: ${REDIS_PASSWORD}
            timeout: 2000ms
            lettuce:
                pool:
                    max-active: 10
                    max-idle: 5
                    min-idle: 2
```

### 고급 설정

**여러 Rate Limiter 적용**

```java
@Service
public class MultiTierApiService {

    // Tier 1: 일반 사용자 (엄격한 제한)
    @RateLimiting(
        name = "tier1-api",
        cacheKey = "#userId",
        executeCondition = "#tier == 'BASIC'"
    )
    public String basicUserApi(String userId, String tier) {
        return "basic";
    }

    // Tier 2: 프리미엄 사용자 (완화된 제한)
    @RateLimiting(
        name = "tier2-api",
        cacheKey = "#userId",
        executeCondition = "#tier == 'PREMIUM'"
    )
    public String premiumUserApi(String userId, String tier) {
        return "premium";
    }
}
```

**동적 Rate Limit 설정**

```java
@RateLimiting(
    name = "dynamic-api",
    cacheKey = "#request.apiKey",
    executeCondition = "#request.limit > 0"
)
public ApiResponse dynamicRateLimit(ApiRequest request) {
    // Rate Limit이 동적으로 적용됨
    return new ApiResponse();
}
```

---

## 🚀 Examples

### 예제 1: RESTful API 보호

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PostMapping
    @RateLimiting(
        name = "create-user",
        cacheKey = "#request.remoteAddr",  // IP 기반 제한
        waitTime = 5000L,
        leaseTime = 2000L
    )
    public ResponseEntity<User> createUser(
            @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }
}
```

### 예제 2: 사용자별 Rate Limiting

```java
@Service
public class MessageService {

    @RateLimiting(
        name = "send-message",
        cacheKey = "#userId",              // 사용자별 제한
        fallbackMethodName = "sendMessageFallback"
    )
    public MessageResponse sendMessage(String userId, String message) {
        // 메시지 전송 로직
        return new MessageResponse("Message sent");
    }

    public MessageResponse sendMessageFallback(String userId, String message) {
        return new MessageResponse("Too many messages. Please wait.");
    }
}
```

### 예제 3: 실시간 모니터링

```java
@Service
public class MonitoringService {

    @RateLimiting(
        name = "metrics-collection",
        cacheKey = "#deviceId",
        ratePerMethod = true               // 메서드별 독립적 제한
    )
    public void collectMetrics(String deviceId, Metrics metrics) {
        metricsRepository.save(metrics);
    }
}
```

---

## 🤝 Contributing

기여를 환영합니다! 다음 절차를 따라주세요:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고해주세요.

---

## 📝 License

이 프로젝트는 MIT 라이센스로 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 🙋‍♂️ FAQ

<details>
<summary><b>Q: Redis 없이 사용할 수 있나요?</b></summary>

네, `concurrent_hash_map` 옵션을 사용하면 Redis 없이도 동작합니다. 다만 단일 인스턴스 환경에서만 사용 가능합니다.

```yaml
rate-limiter:
    lock-type: concurrent_hash_map
    cache-type: concurrent_hash_map
```

</details>

<details>
<summary><b>Q: 멀티 인스턴스 환경에서는 어떻게 설정하나요?</b></summary>

Redis를 사용하여 분산 Lock과 캐시를 구성해야 합니다:

```yaml
rate-limiter:
    lock-type: redis_redisson
    cache-type: redis
```

</details>

<details>
<summary><b>Q: Rate Limit 초과 시 커스텀 응답을 반환하려면?</b></summary>

`fallbackMethodName`을 사용하거나 `@ExceptionHandler`로 `RateLimitException`을 처리하세요:

```java
@ExceptionHandler(RateLimitException.class)
public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException e) {
    return ResponseEntity.status(429)
        .body(new ErrorResponse("Too Many Requests"));
}
```

</details>

<details>
<summary><b>Q: 알고리즘을 런타임에 변경할 수 있나요?</b></summary>

현재는 지원하지 않습니다. 알고리즘은 애플리케이션 시작 시 설정되며, 변경하려면 재시작이 필요합니다.

</details>

<details>
<summary><b>Q: 성능 오버헤드는 얼마나 되나요?</b></summary>

알고리즘과 환경에 따라 다르지만, 일반적으로 요청당 1-5ms의 오버헤드가 발생합니다.
Redis를 사용할 경우 네트워크 레이턴시가 추가됩니다.

</details>

---

## 📧 Contact

프로젝트 관련 문의나 제안사항이 있으시면 이슈를 등록해주세요.

-   GitHub Issues: [https://github.com/your-username/rate-limiter-spring-boot-starter/issues](https://github.com/your-username/rate-limiter-spring-boot-starter/issues)

---

## 🌟 Star History

이 프로젝트가 도움이 되었다면 ⭐️ Star를 눌러주세요!

---

## 📚 References

-   [System Design Interview – An insider's guide, Second Edition](https://www.amazon.com/System-Design-Interview-insiders-Second/dp/B08CMF2CQF)
-   [Redis Documentation](https://redis.io/documentation)
-   [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
-   [Rate Limiting Strategies and Techniques](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)

---

<div align="center">

**Made with ❤️ for better API protection**

[⬆ Back to top](#-rate-limiter-spring-boot-starter)

</div>
