# 성능 튜닝 가이드

이 문서는 Rate Limiter의 성능을 최적화하는 방법을 설명합니다.

## 목차

-   [성능 측정](#성능-측정)
-   [알고리즘별 성능 특성](#알고리즘별-성능-특성)
-   [Redis 최적화](#redis-최적화)
-   [Lock 최적화](#lock-최적화)
-   [캐시 최적화](#캐시-최적화)
-   [JVM 튜닝](#jvm-튜닝)
-   [모니터링](#모니터링)

---

## 성능 측정

### 벤치마크 설정

```java
@SpringBootTest
public class RateLimiterBenchmark {

    @Autowired
    private RateLimitingService service;

    @Test
    void benchmarkTokenBucket() {
        int iterations = 10000;
        int threads = 100;

        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(iterations);

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    service.processRequest("user-" + UUID.randomUUID());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        double tps = (double) iterations / (duration / 1000.0);
        System.out.println("TPS: " + tps);
        System.out.println("Average latency: " + (duration / (double) iterations) + "ms");
    }
}
```

### 주요 성능 지표

| 지표              | 설명                     | 목표    |
| ----------------- | ------------------------ | ------- |
| **TPS**           | Transactions Per Second  | 50,000+ |
| **P50 Latency**   | 50th percentile 응답시간 | < 1ms   |
| **P99 Latency**   | 99th percentile 응답시간 | < 5ms   |
| **CPU 사용률**    | CPU 사용률               | < 50%   |
| **메모리 사용량** | Heap 메모리 사용량       | < 2GB   |
| **에러율**        | Lock 획득 실패율         | < 0.1%  |

---

## 알고리즘별 성능 특성

### Token Bucket

**성능 특성**

-   시간 복잡도: O(1)
-   공간 복잡도: O(1)
-   TPS: 50,000+

**최적화 팁**

```yaml
# 1. Capacity를 적절히 설정
token-bucket:
  capacity: 100          # 너무 크면 메모리 낭비, 너무 작으면 버스트 불가
  rate: 10
  rate-unit: seconds

# 2. Rate unit을 적절히 선택
token-bucket:
  rate: 1
  rate-unit: seconds     # seconds 권장 (밀리초는 오버헤드 큼)
```

### Leaky Bucket

**성능 특성**

-   시간 복잡도: O(1)
-   공간 복잡도: O(n) - 큐 크기
-   TPS: 45,000+

**최적화 팁**

```yaml
# 큐 크기를 적절히 제한
leaky-bucket:
    capacity: 1000 # 큐 크기 제한으로 메모리 관리
    rate: 100
    rate-unit: seconds
```

### Fixed Window Counter

**성능 특성**

-   시간 복잡도: O(1)
-   공간 복잡도: O(1)
-   TPS: 60,000+ (가장 빠름)

**최적화 팁**

```yaml
# 윈도우 크기를 크게 설정 (Redis 부하 감소)
fixed-window-counter:
    window-size: 60 # 60초 (1분)
    request-limit: 6000 # 분당 6000개 = 초당 100개
```

### Sliding Window Logging

**성능 특성**

-   시간 복잡도: O(n)
-   공간 복잡도: O(n)
-   TPS: 20,000+

**최적화 팁**

```yaml
# Request limit을 낮게 유지 (메모리 절약)
sliding-window-logging:
    window-size: 60
    request-limit: 100 # 낮은 limit 권장 (타임스탬프 저장 부담)
```

**코드 최적화**

```java
// 1. 주기적인 타임스탬프 정리
@Scheduled(fixedRate = 60000)
public void cleanupOldTimestamps() {
    long cutoff = System.currentTimeMillis() - windowSize;
    cache.removeEntriesOlderThan(cutoff);
}

// 2. 효율적인 자료구조 사용
// ArrayList 대신 LinkedList 사용
private LinkedList<Long> timestamps = new LinkedList<>();

// 앞쪽부터 삭제 (O(1))
while (!timestamps.isEmpty() && timestamps.getFirst() < cutoff) {
    timestamps.removeFirst();
}
```

### Sliding Window Counter

**성능 특성**

-   시간 복잡도: O(1)
-   공간 복잡도: O(1)
-   TPS: 55,000+

**최적화 팁**

```yaml
# 균형잡힌 설정 (성능과 정확도)
sliding-window-counter:
    window-size: 60
    request-limit: 1000
```

---

## Redis 최적화

### 1. Connection Pool 설정

```yaml
spring:
    data:
        redis:
            host: localhost
            port: 6379
            timeout: 2000ms
            lettuce:
                pool:
                    max-active: 20 # 최대 연결 수 (서버 스펙에 맞게)
                    max-idle: 10 # 유휴 최대 연결 수
                    min-idle: 5 # 유휴 최소 연결 수 (항상 유지)
                    max-wait: 1000ms # 연결 대기 시간
                shutdown-timeout: 100ms
```

**권장 설정**

| 환경                  | max-active | max-idle | min-idle |
| --------------------- | ---------- | -------- | -------- |
| **개발**              | 5          | 3        | 1        |
| **스테이징**          | 10         | 5        | 2        |
| **프로덕션 (소규모)** | 20         | 10       | 5        |
| **프로덕션 (대규모)** | 50         | 20       | 10       |

### 2. Redis 파이프라이닝

여러 요청을 배치로 처리하여 네트워크 오버헤드 감소:

```java
@Service
public class BatchRateLimitService {

    public Map<String, Boolean> checkMultipleUsers(List<String> userIds) {
        Map<String, Boolean> results = new HashMap<>();

        // 파이프라이닝으로 배치 처리
        redisConnection.sync().multi();

        for (String userId : userIds) {
            String key = "rate-limit:" + userId;
            redisConnection.async().get(key);
        }

        List<Object> responses = redisConnection.sync().exec();

        // 결과 처리
        for (int i = 0; i < userIds.size(); i++) {
            results.put(userIds.get(i), (Boolean) responses.get(i));
        }

        return results;
    }
}
```

### 3. Redis Key 설계

**효율적인 Key 패턴**

```java
// ❌ 비효율적: 너무 긴 키
String key = "rate-limiter:token-bucket:user:12345:api:v1:endpoint:create-order";

// ✅ 효율적: 간결한 키
String key = "rl:tb:u:12345:co";  // rl=rate-limiter, tb=token-bucket, u=user, co=create-order
```

**Key 만료 시간 설정**

```java
// TTL 설정으로 메모리 관리
cache.set(key, value, Duration.ofSeconds(windowSize * 2));
```

### 4. Redis 메모리 최적화

**redis.conf 설정**

```conf
# 1. 최대 메모리 설정
maxmemory 2gb

# 2. 메모리 정책 (LRU 권장)
maxmemory-policy allkeys-lru

# 3. Persistence 설정 (성능 중시)
save ""                    # RDB 비활성화
appendonly no             # AOF 비활성화

# 4. 압축 설정
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
```

**프로덕션 권장 설정**

```conf
# 안정성과 성능의 균형
maxmemory 4gb
maxmemory-policy volatile-lru
save 900 1                # 15분마다 1개 이상 변경 시 저장
appendonly yes
appendfsync everysec      # 1초마다 fsync
```

### 5. Redis Cluster (대규모 환경)

```yaml
spring:
    data:
        redis:
            cluster:
                nodes:
                    - redis-node-1:6379
                    - redis-node-2:6379
                    - redis-node-3:6379
                max-redirects: 3
            lettuce:
                cluster:
                    refresh:
                        adaptive: true # 클러스터 토폴로지 자동 감지
                        period: 60s
```

---

## Lock 최적화

### 1. Lock 대기 시간 조정

```java
@RateLimiting(
    name = "optimized-api",
    cacheKey = "#userId",
    waitTime = 100L,      // ✅ 짧은 대기 시간 (100ms)
    leaseTime = 50L       // ✅ 짧은 유지 시간 (50ms)
)
```

**권장 설정**

| 시나리오      | waitTime | leaseTime |
| ------------- | -------- | --------- |
| **빠른 작업** | 100ms    | 50ms      |
| **일반 작업** | 500ms    | 200ms     |
| **느린 작업** | 2000ms   | 1000ms    |

### 2. Lock Striping

여러 Lock을 사용하여 경합 감소:

```java
public class StripedLockManager {

    private static final int STRIPE_COUNT = 16;  // CPU 코어 수의 2배
    private final Lock[] locks = new Lock[STRIPE_COUNT];

    public StripedLockManager() {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public Lock getLock(String key) {
        int stripe = Math.abs(key.hashCode() % STRIPE_COUNT);
        return locks[stripe];
    }
}
```

### 3. Fair vs Non-Fair Lock

```java
// Non-Fair Lock (기본, 더 빠름)
private final Lock lock = new ReentrantLock(false);

// Fair Lock (공정성 보장, 느림)
private final Lock lock = new ReentrantLock(true);
```

**권장**: 대부분의 경우 Non-Fair Lock 사용 (성능 우선)

### 4. Spin Lock (짧은 대기 시)

```java
public class SpinLockManager {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    public boolean tryLock(int maxSpins) {
        int spins = 0;
        while (!locked.compareAndSet(false, true)) {
            if (++spins > maxSpins) {
                return false;
            }
            // Busy wait (CPU 사용)
            Thread.onSpinWait();  // Java 9+
        }
        return true;
    }
}
```

---

## 캐시 최적화

### 1. ConcurrentHashMap 최적화

```java
// ✅ 초기 용량 설정으로 리사이징 방지
private final ConcurrentHashMap<String, TokenInfo> cache =
    new ConcurrentHashMap<>(1024, 0.75f, 16);
    // capacity, loadFactor, concurrencyLevel

// ✅ computeIfAbsent로 원자적 연산
TokenInfo info = cache.computeIfAbsent(key, k -> {
    return TokenInfo.createDefault();
});

// ❌ 비효율적
if (!cache.containsKey(key)) {
    cache.put(key, TokenInfo.createDefault());
}
```

### 2. 캐시 크기 제한

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheTemplate cacheTemplate() {
        return new CacheTemplate() {

            private final int MAX_SIZE = 10000;
            private final Map<String, Entry> cache =
                Collections.synchronizedMap(new LinkedHashMap<String, Entry>(MAX_SIZE + 1, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry eldest) {
                        return size() > MAX_SIZE;  // LRU 자동 제거
                    }
                });
        };
    }
}
```

### 3. 캐시 워밍

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private CacheTemplate cache;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 자주 사용되는 키 미리 로드
        List<String> popularKeys = loadPopularKeys();

        for (String key : popularKeys) {
            cache.set(key, TokenInfo.createDefault());
        }

        log.info("Cache warmed up with {} entries", popularKeys.size());
    }
}
```

---

## JVM 튜닝

### 1. Heap 메모리 설정

```bash
# Heap 크기 설정
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -jar rate-limiter-example.jar
```

**권장 Heap 크기**

| 환경                  | -Xms | -Xmx |
| --------------------- | ---- | ---- |
| **개발**              | 512m | 1g   |
| **스테이징**          | 1g   | 2g   |
| **프로덕션 (소규모)** | 2g   | 2g   |
| **프로덕션 (대규모)** | 4g   | 4g   |

### 2. GC 튜닝

**G1GC (권장)**

```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:G1HeapRegionSize=16m \
     -jar app.jar
```

**ZGC (대용량 Heap)**

```bash
java -Xms8g -Xmx8g \
     -XX:+UseZGC \
     -XX:ConcGCThreads=2 \
     -jar app.jar
```

### 3. JIT 컴파일러 최적화

```bash
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:+TieredCompilation \
     -XX:TieredStopAtLevel=4 \
     -XX:CompileThreshold=1000 \
     -jar app.jar
```

---

## 모니터링

### 1. Micrometer 메트릭

```java
@Component
public class RateLimiterMetrics {

    private final MeterRegistry registry;

    private final Counter allowedRequests;
    private final Counter deniedRequests;
    private final Timer latency;

    public RateLimiterMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.allowedRequests = Counter.builder("rate_limiter.requests.allowed")
            .description("Number of allowed requests")
            .tag("algorithm", "token_bucket")
            .register(registry);

        this.deniedRequests = Counter.builder("rate_limiter.requests.denied")
            .description("Number of denied requests")
            .tag("algorithm", "token_bucket")
            .register(registry);

        this.latency = Timer.builder("rate_limiter.latency")
            .description("Rate limiter processing latency")
            .register(registry);
    }

    public void recordAllowed() {
        allowedRequests.increment();
    }

    public void recordDenied() {
        deniedRequests.increment();
    }

    public <T> T recordLatency(Supplier<T> supplier) {
        return latency.record(supplier);
    }
}
```

### 2. 커스텀 헬스 체크

```java
@Component
public class RateLimiterHealthIndicator implements HealthIndicator {

    @Autowired
    private CacheTemplate cache;

    @Autowired
    private LockManager lockManager;

    @Override
    public Health health() {
        try {
            // 캐시 연결 확인
            cache.ping();

            // Lock 획득 테스트
            boolean lockable = lockManager.tryLock(
                createTestAnnotation(), 100, TimeUnit.MILLISECONDS
            );

            if (lockable) {
                lockManager.unlock();
                return Health.up()
                    .withDetail("cache", "OK")
                    .withDetail("lock", "OK")
                    .build();
            } else {
                return Health.down()
                    .withDetail("lock", "Unable to acquire")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

### 3. Actuator 엔드포인트

```yaml
management:
    endpoints:
        web:
            exposure:
                include: health,metrics,prometheus
    metrics:
        export:
            prometheus:
                enabled: true
```

**Prometheus 쿼리 예시**

```promql
# 초당 요청 수
rate(rate_limiter_requests_allowed_total[1m])

# P99 레이턴시
histogram_quantile(0.99, rate(rate_limiter_latency_seconds_bucket[5m]))

# 거부율
rate(rate_limiter_requests_denied_total[1m]) /
rate(rate_limiter_requests_allowed_total[1m])
```

---

## 성능 체크리스트

### 개발 단계

-   [ ] 적절한 알고리즘 선택
-   [ ] 테스트 커버리지 80% 이상
-   [ ] 부하 테스트 수행

### 배포 단계

-   [ ] Redis Connection Pool 설정
-   [ ] JVM Heap 크기 설정
-   [ ] GC 로그 활성화
-   [ ] 모니터링 설정

### 운영 단계

-   [ ] 정기적인 성능 측정
-   [ ] 메트릭 대시보드 확인
-   [ ] Alert 설정
-   [ ] 용량 계획

---

## 참고 자료

-   [Redis Best Practices](https://redis.io/topics/best-practices)
-   [Lettuce Performance Tuning](https://github.com/lettuce-io/lettuce-core/wiki/Performance-Tuning)
-   [Java Performance Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
-   [G1GC Tuning](https://www.oracle.com/technical-resources/articles/java/g1gc.html)

---

이 문서는 지속적으로 업데이트됩니다. 성능 관련 질문이나 제안사항은 [GitHub Issues](https://github.com/your-repo/issues)에 등록해주세요.
