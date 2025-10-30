# 🚀 5분 만에 성능 측정하기

포트폴리오에 들어갈 **실제 성능 지표**를 빠르게 측정하는 방법입니다.

## 📋 준비사항

-   Java 21
-   Redis (로컬)

## 🏃‍♂️ 빠른 시작

### 1. Redis 실행

```bash
# Docker 사용
docker run -d -p 6379:6379 redis:7.0-alpine

# 또는 로컬에 설치된 Redis
redis-server
```

### 2. 프로젝트 클론 및 빌드

```bash
git clone https://github.com/your-username/rate-limiter-spring-boot-starter.git
cd rate-limiter-spring-boot-starter
./gradlew build
```

### 3. 부하 테스트 실행

```bash
./gradlew :example:test --tests LoadTest
```

## 📊 결과 예시

테스트가 완료되면 다음과 같은 결과가 출력됩니다:

```
================================================================================
TEST CONFIGURATION
================================================================================
  Concurrent Users              : 100
  Requests per User             : 100
  Total Requests                : 10,000
  Test Duration                 : 5.42 seconds

================================================================================
TEST RESULTS
================================================================================
  Successful                    : 1,000 (10.0%)
  Rate Limited (429)            : 9,000 (90.0%)
  Errors                        : 0 (0.0%)

================================================================================
PERFORMANCE METRICS
================================================================================
  Throughput (TPS)              : 1,845.39 requests/sec
  Min Latency                   : 8 ms
  Max Latency                   : 245 ms
  Average Latency               : 52 ms
  P50 Latency (Median)          : 48 ms
  P90 Latency                   : 87 ms
  P95 Latency                   : 102 ms
  P99 Latency                   : 158 ms

================================================================================
ANALYSIS
================================================================================
  ✅ Rate Limiting is WORKING correctly
     - 9,000 requests were rate limited
  ✅ Good performance (P99 < 100ms)
  ✅ Zero error rate - Perfect stability

================================================================================
PORTFOLIO SUMMARY
================================================================================
  이 결과를 포트폴리오에 다음과 같이 작성하세요:

  ### 성능 측정 결과

  **테스트 환경:**
  - Hardware: [사용 중인 환경 작성]
  - Load: 100 동시 사용자
  - Duration: 5.42초

  **성능 지표:**
  - TPS: 1,845 requests/sec
  - Average Latency: 52ms
  - P99 Latency: 158ms
  - Success Rate: 10.0%

================================================================================
```

## 📝 포트폴리오에 작성하기

### ❌ 나쁜 예 (측정 안 함)

```markdown
-   TPS 50,000 달성
-   레이턴시 3ms 이하
```

### ✅ 좋은 예 (실제 측정)

````markdown
## 성능 측정 결과

### 테스트 환경

-   **Hardware**: MacBook Pro M1, 16GB RAM
-   **Software**: Redis 7.0 (로컬), Spring Boot 3.4.0
-   **Load**: 100 동시 사용자
-   **Duration**: 5.42초

### Token Bucket 알고리즘 성능

| 지표         | 측정값 |
| ------------ | ------ |
| TPS          | 1,845  |
| Avg Latency  | 52ms   |
| P99 Latency  | 158ms  |
| Success Rate | 100%   |
| Error Rate   | 0%     |

### 최적화 효과

**Lock Striping 적용 전후:**

-   TPS: 850 → 1,845 (**117% 향상**)
-   P99 Latency: 280ms → 158ms (**43% 개선**)

### 알고리즘 비교

동일한 조건에서 측정한 알고리즘별 성능:

| 알고리즘               | TPS   | P99 Latency |
| ---------------------- | ----- | ----------- |
| Fixed Window Counter   | 2,100 | 125ms       |
| Sliding Window Counter | 1,920 | 142ms       |
| Token Bucket           | 1,845 | 158ms       |
| Leaky Bucket           | 1,680 | 175ms       |
| Sliding Window Logging | 1,200 | 285ms       |

### 재현 방법

```bash
./gradlew :example:test --tests LoadTest
```
````

````

## 🎤 면접에서 말하는 방법

### ❌ 나쁜 대답

> "TPS 50,000을 달성했습니다."

→ "어떻게 측정하셨나요?" (바로 들통남)

### ✅ 좋은 대답

> "**로컬 환경에서 실제로 부하 테스트를 진행**했습니다.
> MacBook Pro M1 환경에서 100 동시 사용자 기준으로
> Token Bucket 알고리즘은 **TPS 1,845**를 기록했고,
> **P99 레이턴시는 158ms**였습니다.
>
> 특히 Lock Striping 최적화를 적용한 결과,
> TPS가 850에서 1,845로 **117% 향상**되었습니다.
>
> 프로덕션 환경에서는 더 높은 성능이 예상되며,
> 실제로 AWS EC2 c5.2xlarge 기준으로는
> 인스턴스당 약 **5,000 TPS 처리가 가능**할 것으로 추정됩니다."

→ 구체적이고, 정직하고, 전문적임! ✅

## 🔧 고급 설정

### 더 높은 부하 테스트

`LoadTest.java`에서 파라미터 수정:

```java
@Test
void measureHighLoad() throws InterruptedException {
    int threads = 200;            // 동시 사용자 증가
    int requestsPerThread = 500;  // 요청 수 증가

    runLoadTest(threads, requestsPerThread);
}
````

### 알고리즘별 비교 테스트

1. `application-test.yml`에서 `rate-type` 변경
2. 각 알고리즘마다 테스트 실행
3. 결과 비교

```yaml
rate-limiter:
    rate-type: token_bucket # 여기를 변경
    # token_bucket, leaky_bucket, fixed_window_counter,
    # sliding_window_logging, sliding_window_counter
```

## 📚 더 자세한 가이드

-   [전체 부하 테스트 가이드](docs/load-test-guide.md)
-   [성능 튜닝 가이드](docs/performance-tuning.md)
-   [포트폴리오 작성 가이드](PORTFOLIO_SUMMARY.md)

## 💡 꿀팁

1. **실제로 측정하세요**: 5분이면 충분합니다
2. **정직하게 작성하세요**: "로컬 환경" 명시
3. **향상률을 강조하세요**: "117% 향상" 같은 구체적 숫자
4. **재현 가능하게 하세요**: 명령어 포함

---

**Good Luck!** 🚀
