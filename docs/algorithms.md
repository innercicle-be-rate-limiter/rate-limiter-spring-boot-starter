# Rate Limiting 알고리즘 상세 가이드

이 문서에서는 Rate Limiter에서 지원하는 5가지 알고리즘의 동작 원리와 사용법을 상세히 설명합니다.

## 목차

-   [Token Bucket](#token-bucket)
-   [Leaky Bucket](#leaky-bucket)
-   [Fixed Window Counter](#fixed-window-counter)
-   [Sliding Window Logging](#sliding-window-logging)
-   [Sliding Window Counter](#sliding-window-counter)
-   [알고리즘 선택 가이드](#알고리즘-선택-가이드)

---

## Token Bucket

### 개념

Token Bucket은 가장 널리 사용되는 Rate Limiting 알고리즘 중 하나입니다. 버킷에 토큰을 일정한 속도로 채우고, 요청이 들어올 때마다 토큰을 소비하는 방식입니다.

### 동작 원리

1. **토큰 생성**: 설정된 속도(`rate`)로 버킷에 토큰이 추가됩니다
2. **토큰 저장**: 버킷의 최대 용량(`capacity`)까지 토큰이 저장됩니다
3. **토큰 소비**: 요청이 들어오면 1개의 토큰을 소비합니다
4. **요청 거부**: 토큰이 없으면 요청을 거부합니다

```
Initial State:    [🪙🪙🪙🪙🪙] capacity: 5, tokens: 5
                     ↓ (time passes, rate: 1 token/sec)
After 2 requests: [🪙🪙🪙] tokens: 3
                     ↓ (wait 2 seconds)
Refilled:         [🪙🪙🪙🪙🪙] tokens: 5 (max capacity)
```

### 구현 코드 (핵심 로직)

```java
public class TokenBucketHandler implements RateLimitHandler {

    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        TokenBucketInfo info = cache.get(cacheKey);

        if (info == null) {
            // 첫 요청: 새 버킷 생성
            info = TokenBucketInfo.createDefault(properties);
            cache.set(cacheKey, info);
            return info;
        }

        // 토큰 재충전
        long now = System.currentTimeMillis();
        long elapsed = now - info.getLastRefillTime();
        long tokensToAdd = (elapsed / properties.getRefillInterval());

        info.setTokens(Math.min(
            info.getCapacity(),
            info.getTokens() + tokensToAdd
        ));
        info.setLastRefillTime(now);

        // 토큰 소비
        if (info.getTokens() > 0) {
            info.setTokens(info.getTokens() - 1);
            cache.set(cacheKey, info);
            return info;
        }

        // 토큰 부족
        throw new RateLimitException("Rate limit exceeded");
    }
}
```

### 설정 예시

```yaml
rate-limiter:
    rate-type: token_bucket

token-bucket:
    capacity: 100 # 버킷 최대 용량
    rate: 10 # 10초마다 1개씩 토큰 생성
    rate-unit: seconds
```

**실제 처리율**: 초당 0.1개 요청 (10초에 1개)

### 사용 예시

```java
@Service
public class ApiService {

    /**
     * API 호출 제한 (초당 10개 요청)
     * - 평소에는 초당 10개 처리
     * - 버스트 시 최대 100개까지 한번에 처리 가능
     */
    @RateLimiting(
        name = "api-token-bucket",
        cacheKey = "#userId"
    )
    public ApiResponse callApi(String userId, ApiRequest request) {
        return externalApi.call(request);
    }
}
```

### 장점

✅ **버스트 트래픽 허용**: 버킷에 토큰이 쌓여있으면 순간적으로 많은 요청 처리 가능  
✅ **구현 간단**: 토큰 카운트와 타임스탬프만 관리  
✅ **메모리 효율적**: O(1) 공간 복잡도  
✅ **예측 가능**: 평균 처리율을 명확히 제어

### 단점

❌ **정확도 낮음**: 버스트로 인해 순간적으로 제한을 초과할 수 있음  
❌ **시간 동기화**: 분산 환경에서 시간 동기화 필요  
❌ **초기 버스트**: 초기에 전체 capacity만큼 한번에 처리 가능

### 적합한 사용 사례

-   ✅ API Gateway Rate Limiting
-   ✅ 일반적인 REST API 보호
-   ✅ 외부 서비스 호출 제한
-   ✅ 사용자별 요청 제한

---

## Leaky Bucket

### 개념

Leaky Bucket은 FIFO 큐와 같은 개념으로, 고정된 속도로 요청을 처리합니다. 물이 새는 양동이에 물을 부으면 일정한 속도로 물이 빠지는 것과 같습니다.

### 동작 원리

1. **큐에 저장**: 들어오는 요청을 큐에 저장
2. **고정 속도 처리**: 설정된 속도로 큐에서 요청을 꺼내 처리
3. **큐 오버플로우**: 큐가 가득 차면 새 요청 거부

```
Requests arrive:  ⬇️⬇️⬇️⬇️⬇️⬇️⬇️ (burst)
                    ↓
Queue:            [R1][R2][R3][R4][R5]
                    ↓↓↓ (leak at fixed rate)
Processed:        ✅ → ✅ → ✅ (constant rate)
```

### 구현 코드 (핵심 로직)

```java
public class LeakyBucketHandler implements RateLimitHandler {

    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        LeakyBucketInfo info = cache.get(cacheKey);

        if (info == null) {
            info = LeakyBucketInfo.createDefault(properties);
        }

        long now = System.currentTimeMillis();

        // 누수 처리 (일정 시간마다 큐에서 요청 제거)
        long elapsed = now - info.getLastLeakTime();
        long leaks = elapsed / properties.getLeakInterval();

        if (leaks > 0) {
            info.setQueueSize(Math.max(0, info.getQueueSize() - leaks));
            info.setLastLeakTime(now);
        }

        // 큐에 공간이 있으면 추가
        if (info.getQueueSize() < properties.getCapacity()) {
            info.setQueueSize(info.getQueueSize() + 1);
            cache.set(cacheKey, info);
            return info;
        }

        // 큐가 가득 참
        throw new RateLimitException("Queue is full");
    }
}
```

### 설정 예시

```yaml
rate-limiter:
    rate-type: leaky_bucket

leaky-bucket:
    capacity: 100 # 큐 크기
    rate: 10 # 10초마다 1개씩 처리
    rate-unit: seconds
```

### 사용 예시

```java
@Service
public class MessageQueueService {

    /**
     * 메시지 큐 처리
     * - 요청을 큐에 저장
     * - 일정한 속도로 처리하여 다운스트림 보호
     */
    @RateLimiting(
        name = "message-queue",
        cacheKey = "#queueName"
    )
    public void enqueueMessage(String queueName, Message message) {
        messageQueue.add(message);
    }
}
```

### 장점

✅ **안정적인 출력**: 항상 일정한 속도로 처리  
✅ **다운스트림 보호**: 백엔드 시스템에 일정한 부하만 전달  
✅ **버퍼링**: 일시적인 버스트를 큐로 흡수

### 단점

❌ **응답 지연**: 큐에서 대기하는 시간 발생  
❌ **메모리 사용**: 큐 크기만큼 메모리 필요  
❌ **오래된 요청**: 큐에 오래 머물러 있는 요청이 최신 요청보다 늦게 처리될 수 있음

### 적합한 사용 사례

-   ✅ 외부 API 호출 속도 제어
-   ✅ 메시지 큐 처리
-   ✅ 백엔드 시스템 보호
-   ✅ 안정적인 아웃바운드 트래픽 필요

---

## Fixed Window Counter

### 개념

고정된 시간 윈도우(예: 1분) 내에서 허용되는 요청 수를 카운트하는 가장 간단한 알고리즘입니다.

### 동작 원리

1. **윈도우 생성**: 고정된 시간 윈도우 시작 (예: 14:00:00 ~ 14:01:00)
2. **카운트 증가**: 요청마다 카운터 증가
3. **제한 확인**: 카운터가 제한을 초과하면 거부
4. **윈도우 리셋**: 다음 윈도우가 시작되면 카운터 초기화

```
Window 1 (14:00:00-14:01:00): [✅✅✅✅✅] Count: 5/10
Window 2 (14:01:00-14:02:00): [✅✅✅] Count: 3/10
                                ↑ (reset at window boundary)
```

### 구현 코드 (핵심 로직)

```java
public class FixedWindowCounterHandler implements RateLimitHandler {

    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        long now = System.currentTimeMillis();
        long windowSize = properties.getWindowSize() * 1000; // 초를 밀리초로
        long currentWindow = now / windowSize;

        String windowKey = cacheKey + ":" + currentWindow;
        FixedWindowCountInfo info = cache.get(windowKey);

        if (info == null) {
            // 새 윈도우 시작
            info = new FixedWindowCountInfo();
            info.setWindowStart(currentWindow * windowSize);
            info.setCount(1);
            info.setLimit(properties.getRequestLimit());
            cache.set(windowKey, info, windowSize);
            return info;
        }

        // 현재 윈도우 내에서 카운트 증가
        if (info.getCount() < properties.getRequestLimit()) {
            info.setCount(info.getCount() + 1);
            cache.set(windowKey, info);
            return info;
        }

        // 제한 초과
        throw new RateLimitException("Request limit exceeded");
    }
}
```

### 설정 예시

```yaml
rate-limiter:
    rate-type: fixed_window_counter

fixed-window-counter:
    window-size: 60 # 60초 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

### 사용 예시

```java
@Service
public class DownloadService {

    /**
     * 파일 다운로드 제한
     * - 1분당 최대 10번 다운로드
     */
    @RateLimiting(
        name = "file-download",
        cacheKey = "#userId"
    )
    public byte[] downloadFile(String userId, String fileId) {
        return fileRepository.getFile(fileId);
    }
}
```

### 장점

✅ **매우 간단**: 구현과 이해가 쉬움  
✅ **메모리 효율**: 윈도우당 1개의 카운터만 저장  
✅ **빠른 성능**: O(1) 시간 복잡도  
✅ **리셋 명확**: 윈도우 경계에서 명확한 리셋

### 단점

❌ **경계 문제**: 윈도우 경계에서 2배의 트래픽 발생 가능

```
Window 1: [................................✅✅✅✅✅] 14:00:59 - 5 requests
Window 2: [✅✅✅✅✅..............................]14:01:00 - 5 requests
           ↑ Within 1 second, 10 requests (2x limit!)
```

❌ **정확도 낮음**: 실제 처리율이 설정값의 2배까지 가능

### 적합한 사용 사례

-   ✅ 간단한 Rate Limiting 요구사항
-   ✅ 리소스가 제한적인 환경
-   ✅ 대략적인 제한만 필요한 경우
-   ✅ 내부 API 보호

---

## Sliding Window Logging

### 개념

각 요청의 타임스탬프를 로그로 저장하고, 슬라이딩 윈도우 내의 요청 수를 계산하는 정확한 알고리즘입니다.

### 동작 원리

1. **타임스탬프 저장**: 각 요청의 타임스탬프를 리스트에 저장
2. **오래된 로그 제거**: 윈도우 밖의 타임스탬프 제거
3. **카운트**: 윈도우 내의 타임스탬프 개수 계산
4. **제한 확인**: 개수가 제한을 초과하면 거부

```
Current Time: 14:01:30
Window: 60 seconds (14:00:30 ~ 14:01:30)

Timestamps: [14:00:35, 14:00:45, 14:01:10, 14:01:20, 14:01:25]
                ✅        ✅        ✅        ✅        ✅
            All within window → Count: 5

Old timestamp removed: [14:00:25] ❌ (outside window)
```

### 구현 코드 (핵심 로직)

```java
public class SlidingWindowLoggingHandler implements RateLimitHandler {

    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        long now = System.currentTimeMillis();
        long windowStart = now - (properties.getWindowSize() * 1000);

        SlidingWindowLoggingInfo info = cache.get(cacheKey);

        if (info == null) {
            info = new SlidingWindowLoggingInfo();
            info.setTimestamps(new ArrayList<>());
        }

        // 윈도우 밖의 오래된 타임스탬프 제거
        info.getTimestamps().removeIf(timestamp -> timestamp < windowStart);

        // 현재 요청 수 확인
        if (info.getTimestamps().size() < properties.getRequestLimit()) {
            info.getTimestamps().add(now);
            cache.set(cacheKey, info);
            return info;
        }

        // 제한 초과
        throw new RateLimitException("Request limit exceeded");
    }
}
```

### 설정 예시

```yaml
rate-limiter:
    rate-type: sliding_window_logging

sliding-window-logging:
    window-size: 60 # 60초 슬라이딩 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

### 사용 예시

```java
@Service
public class PaymentService {

    /**
     * 결제 요청 제한
     * - 정확한 Rate Limiting 필요
     * - 1분당 최대 5번 결제
     */
    @RateLimiting(
        name = "payment-request",
        cacheKey = "#userId"
    )
    public PaymentResult processPayment(String userId, PaymentRequest request) {
        return paymentGateway.charge(request);
    }
}
```

### 장점

✅ **가장 정확함**: 정확한 슬라이딩 윈도우 구현  
✅ **경계 문제 없음**: Fixed Window의 경계 문제 해결  
✅ **유연한 윈도우**: 어느 시점에서도 정확한 제한 적용

### 단점

❌ **높은 메모리 사용**: 모든 타임스탬프를 저장 (O(n))  
❌ **성능 저하**: 요청마다 리스트 정리 필요  
❌ **확장성 제한**: 대규모 트래픽에서 메모리 부족 가능

### 적합한 사용 사례

-   ✅ 금융 서비스 (정확한 제한 필요)
-   ✅ 결제 API
-   ✅ 민감한 작업 (계정 생성, 비밀번호 변경 등)
-   ✅ 낮은 트래픽 환경

---

## Sliding Window Counter

### 개념

Fixed Window Counter와 Sliding Window Logging의 하이브리드 방식입니다. 현재 윈도우와 이전 윈도우의 카운터를 가중 평균하여 근사치를 계산합니다.

### 동작 원리

1. **두 윈도우**: 현재 윈도우와 이전 윈도우의 카운터 유지
2. **가중 평균**: 현재 시간의 윈도우 내 위치에 따라 가중치 계산
3. **근사 카운트**: `prevCount * overlap + currCount`

```
Previous Window (14:00-14:01): Count = 80
Current Window (14:01-14:02):  Count = 30

Current Time: 14:01:30 (50% into current window)

Estimated Count = 80 * 50% + 30 = 40 + 30 = 70
                  └─ previous  └─ current
```

### 구현 코드 (핵심 로직)

```java
public class SlidingWindowCounterHandler implements RateLimitHandler {

    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        long now = System.currentTimeMillis();
        long windowSize = properties.getWindowSize() * 1000;
        long currentWindow = now / windowSize;
        long previousWindow = currentWindow - 1;

        // 현재 및 이전 윈도우 카운터 조회
        Integer currentCount = cache.get(cacheKey + ":" + currentWindow);
        Integer previousCount = cache.get(cacheKey + ":" + previousWindow);

        if (currentCount == null) currentCount = 0;
        if (previousCount == null) previousCount = 0;

        // 현재 윈도우 내에서의 경과 비율 계산
        long windowStart = currentWindow * windowSize;
        double elapsed = (double)(now - windowStart) / windowSize;

        // 가중 평균으로 예상 카운트 계산
        double estimatedCount = previousCount * (1 - elapsed) + currentCount;

        // 제한 확인
        if (estimatedCount < properties.getRequestLimit()) {
            cache.increment(cacheKey + ":" + currentWindow);
            return createTokenInfo(estimatedCount);
        }

        throw new RateLimitException("Request limit exceeded");
    }
}
```

### 설정 예시

```yaml
rate-limiter:
    rate-type: sliding_window_counter

sliding-window-counter:
    window-size: 60 # 60초 윈도우
    request-limit: 100 # 윈도우당 최대 100개 요청
```

### 사용 예시

```java
@Service
public class SearchService {

    /**
     * 검색 API Rate Limiting
     * - 효율적이면서 정확한 제한
     * - 1분당 최대 100번 검색
     */
    @RateLimiting(
        name = "search-api",
        cacheKey = "#userId"
    )
    public SearchResult search(String userId, String query) {
        return searchEngine.search(query);
    }
}
```

### 장점

✅ **메모리 효율적**: 2개의 카운터만 저장  
✅ **합리적인 정확도**: Fixed Window보다 정확  
✅ **좋은 성능**: O(1) 시간 복잡도  
✅ **경계 문제 완화**: 가중 평균으로 스파이크 감소

### 단점

❌ **근사값**: Sliding Window Logging만큼 정확하지 않음  
❌ **복잡도**: Fixed Window보다 구현이 복잡  
❌ **오차 범위**: 이전 윈도우의 패턴에 영향받음

### 적합한 사용 사례

-   ✅ 대규모 트래픽 API
-   ✅ 일반적인 Rate Limiting
-   ✅ 정확성과 효율성의 균형 필요
-   ✅ 클라우드 환경 (메모리 비용 중요)

---

## 알고리즘 선택 가이드

### 의사결정 트리

```
시작
 │
 ├─ 버스트 트래픽을 허용해야 하나요?
 │   └─ YES → Token Bucket
 │
 ├─ 정확한 Rate Limiting이 필수인가요?
 │   └─ YES → Sliding Window Logging
 │
 ├─ 메모리가 매우 제한적인가요?
 │   └─ YES → Fixed Window Counter
 │
 ├─ 안정적인 아웃바운드 속도가 중요한가요?
 │   └─ YES → Leaky Bucket
 │
 └─ 기본 추천 → Sliding Window Counter
```

### 비교 표

| 기준            | Token Bucket | Leaky Bucket | Fixed Window | Sliding Log | Sliding Counter |
| --------------- | ------------ | ------------ | ------------ | ----------- | --------------- |
| **메모리**      | ⭐⭐⭐⭐⭐   | ⭐⭐⭐⭐     | ⭐⭐⭐⭐⭐   | ⭐⭐        | ⭐⭐⭐⭐        |
| **정확도**      | ⭐⭐⭐⭐     | ⭐⭐⭐⭐     | ⭐⭐⭐       | ⭐⭐⭐⭐⭐  | ⭐⭐⭐⭐        |
| **성능**        | ⭐⭐⭐⭐⭐   | ⭐⭐⭐⭐     | ⭐⭐⭐⭐⭐   | ⭐⭐⭐      | ⭐⭐⭐⭐        |
| **구현 난이도** | ⭐⭐⭐       | ⭐⭐⭐       | ⭐⭐         | ⭐⭐⭐⭐    | ⭐⭐⭐⭐        |
| **버스트 허용** | ✅           | ❌           | ✅           | ❌          | 부분적          |
| **경계 문제**   | 해당없음     | 해당없음     | ❌           | ✅          | ✅              |

### 산업별 추천

| 산업/용도        | 추천 알고리즘          | 이유                         |
| ---------------- | ---------------------- | ---------------------------- |
| **API Gateway**  | Token Bucket           | 버스트 허용, 높은 성능       |
| **금융 서비스**  | Sliding Window Logging | 정확성 최우선                |
| **소셜 미디어**  | Sliding Window Counter | 대규모 트래픽, 합리적 정확도 |
| **IoT 디바이스** | Leaky Bucket           | 안정적인 아웃바운드          |
| **내부 API**     | Fixed Window Counter   | 간단하고 효율적              |
| **결제 시스템**  | Sliding Window Logging | 정확한 제한 필요             |
| **검색 엔진**    | Sliding Window Counter | 대규모, 정확도 균형          |

### 트래픽 패턴별 추천

**1. 균등한 트래픽 (Steady Traffic)**

-   추천: Fixed Window Counter 또는 Sliding Window Counter
-   이유: 간단하고 효율적

**2. 버스트 트래픽 (Burst Traffic)**

-   추천: Token Bucket
-   이유: 순간적인 트래픽 스파이크 흡수

**3. 예측 불가능한 트래픽 (Unpredictable Traffic)**

-   추천: Sliding Window Counter
-   이유: 유연하면서 정확

**4. 민감한 작업 (Critical Operations)**

-   추천: Sliding Window Logging
-   이유: 가장 정확한 제한

---

## 참고 자료

-   [Token Bucket Algorithm - Wikipedia](https://en.wikipedia.org/wiki/Token_bucket)
-   [Leaky Bucket Algorithm - Wikipedia](https://en.wikipedia.org/wiki/Leaky_bucket)
-   [Rate Limiting Strategies - Google Cloud](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)
-   [System Design Interview Book](https://www.amazon.com/System-Design-Interview-insiders-Second/dp/B08CMF2CQF)

---

이 문서는 지속적으로 업데이트됩니다. 질문이나 제안사항은 [GitHub Issues](https://github.com/your-repo/issues)에 등록해주세요.
