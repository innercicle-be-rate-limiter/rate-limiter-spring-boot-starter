package com.innercircle;

import com.innercircle.controller.request.ParkingApplyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 부하 테스트
 * 
 * <p>이 테스트는 실제 성능을 측정하여 포트폴리오에 사용할 수 있는 데이터를 생성합니다.</p>
 * 
 * <h2>실행 방법:</h2>
 * <pre>
 * ./gradlew :example:test --tests LoadTest
 * </pre>
 * 
 * <h2>주의사항:</h2>
 * <ul>
 *   <li>Redis가 실행 중이어야 합니다</li>
 *   <li>application-test.yml 설정 확인</li>
 *   <li>로컬 환경 성능 측정이므로 프로덕션과 다를 수 있습니다</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @BeforeEach
    void setup() {
        // 429 에러를 예외로 던지지 않도록 설정
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                // 429는 에러로 처리하지 않음
                return false;
            }
        });
    }

    /**
     * 기본 부하 테스트
     * 
     * <p>100 동시 사용자, 각 100개 요청 → 총 10,000개 요청</p>
     */
    @Test
    void measureBasicThroughput() throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BASIC LOAD TEST - Token Bucket Algorithm");
        System.out.println("=".repeat(80));
        
        int threads = 100;
        int requestsPerThread = 100;
        
        runLoadTest(threads, requestsPerThread);
    }

    /**
     * 고부하 테스트
     * 
     * <p>200 동시 사용자, 각 500개 요청 → 총 100,000개 요청</p>
     */
    @Test
    void measureHighLoad() throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HIGH LOAD TEST - Token Bucket Algorithm");
        System.out.println("=".repeat(80));
        
        int threads = 200;
        int requestsPerThread = 500;
        
        runLoadTest(threads, requestsPerThread);
    }

    /**
     * 단일 사용자 부하 테스트 (Rate Limit 검증)
     * 
     * <p>1명 사용자가 빠르게 요청하여 Rate Limit이 정상 동작하는지 확인</p>
     */
    @Test
    void verifySingleUserRateLimit() throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SINGLE USER RATE LIMIT VERIFICATION");
        System.out.println("=".repeat(80));
        
        int threads = 1;
        int requestsPerThread = 100;
        
        runLoadTest(threads, requestsPerThread);
    }

    /**
     * 부하 테스트 실행
     */
    private void runLoadTest(int threads, int requestsPerThread) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * requestsPerThread);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        // 요청 실행
        for (int i = 0; i < threads; i++) {
            final String userId = "user" + i;
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    long requestStart = System.currentTimeMillis();

                    try {
                        ParkingApplyRequest request = ParkingApplyRequest.builder()
                                .userId(userId)
                                .carNo("서울12가3456")
                                .applyDate("2025-10-30")
                                .applyTime("14")
                                .applyMinute("30")
                                .build();
                        
                        ResponseEntity<String> response = restTemplate.postForEntity(
                            "http://localhost:" + port + "/api/v1/car/parking",
                            request,
                            String.class
                        );

                        long latency = System.currentTimeMillis() - requestStart;
                        latencies.add(latency);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        } else if (response.getStatusCode().value() == 429) {
                            rateLimitCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        long latency = System.currentTimeMillis() - requestStart;
                        latencies.add(latency);
                        
                        // RateLimitException은 정상 동작 (429)
                        if (e.getMessage() != null && e.getMessage().contains("429")) {
                            rateLimitCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // 완료 대기
        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        // 결과 출력
        printResults(threads, requestsPerThread, duration, 
                    successCount.get(), rateLimitCount.get(), errorCount.get(), 
                    latencies);

        executor.shutdown();
    }

    /**
     * 결과 출력 및 분석
     */
    private void printResults(int threads, int requestsPerThread, long duration,
                             int success, int rateLimited, int error, 
                             List<Long> latencies) {
        int total = threads * requestsPerThread;
        double tps = (double) total / (duration / 1000.0);
        double durationSec = duration / 1000.0;

        // Latency 계산
        Collections.sort(latencies);
        long min = latencies.isEmpty() ? 0 : latencies.get(0);
        long max = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);
        long p50 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.50));
        long p90 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.90));
        long p95 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99));
        long avg = latencies.isEmpty() ? 0 : 
                   (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);

        // 결과 출력
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST CONFIGURATION");
        System.out.println("=".repeat(80));
        System.out.println(String.format("  %-30s : %d", "Concurrent Users", threads));
        System.out.println(String.format("  %-30s : %d", "Requests per User", requestsPerThread));
        System.out.println(String.format("  %-30s : %,d", "Total Requests", total));
        System.out.println(String.format("  %-30s : %.2f seconds", "Test Duration", durationSec));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(80));
        System.out.println(String.format("  %-30s : %,d (%.1f%%)", "Successful", success, 
                                         success * 100.0 / total));
        System.out.println(String.format("  %-30s : %,d (%.1f%%)", "Rate Limited (429)", 
                                         rateLimited, rateLimited * 100.0 / total));
        System.out.println(String.format("  %-30s : %,d (%.1f%%)", "Errors", error, 
                                         error * 100.0 / total));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE METRICS");
        System.out.println("=".repeat(80));
        System.out.println(String.format("  %-30s : %.2f requests/sec", 
                                         "Throughput (TPS)", tps));
        System.out.println(String.format("  %-30s : %d ms", "Min Latency", min));
        System.out.println(String.format("  %-30s : %d ms", "Max Latency", max));
        System.out.println(String.format("  %-30s : %d ms", "Average Latency", avg));
        System.out.println(String.format("  %-30s : %d ms", "P50 Latency (Median)", p50));
        System.out.println(String.format("  %-30s : %d ms", "P90 Latency", p90));
        System.out.println(String.format("  %-30s : %d ms", "P95 Latency", p95));
        System.out.println(String.format("  %-30s : %d ms", "P99 Latency", p99));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ANALYSIS");
        System.out.println("=".repeat(80));
        
        // Rate Limiting 동작 여부
        if (rateLimited > 0) {
            System.out.println("  ✅ Rate Limiting is WORKING correctly");
            System.out.println(String.format("     - %,d requests were rate limited", rateLimited));
        } else if (threads == 1 && requestsPerThread <= 10) {
            System.out.println("  ℹ️  No rate limiting triggered (requests within limit)");
        } else {
            System.out.println("  ⚠️  WARNING: No rate limiting triggered (check configuration)");
        }

        // 성능 분석
        if (p99 < 50) {
            System.out.println("  ✅ Excellent performance (P99 < 50ms)");
        } else if (p99 < 100) {
            System.out.println("  ✅ Good performance (P99 < 100ms)");
        } else if (p99 < 200) {
            System.out.println("  ⚠️  Acceptable performance (P99 < 200ms)");
        } else {
            System.out.println("  ❌ Poor performance (P99 >= 200ms) - Consider optimization");
        }

        // 에러율 분석
        double errorRate = error * 100.0 / total;
        if (errorRate == 0) {
            System.out.println("  ✅ Zero error rate - Perfect stability");
        } else if (errorRate < 0.1) {
            System.out.println(String.format("  ✅ Very low error rate (%.2f%%)", errorRate));
        } else if (errorRate < 1) {
            System.out.println(String.format("  ⚠️  Acceptable error rate (%.2f%%)", errorRate));
        } else {
            System.out.println(String.format("  ❌ High error rate (%.2f%%) - Investigation needed", 
                                            errorRate));
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("PORTFOLIO SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("  이 결과를 포트폴리오에 다음과 같이 작성하세요:");
        System.out.println();
        System.out.println("  ### 성능 측정 결과");
        System.out.println();
        System.out.println("  **테스트 환경:**");
        System.out.println("  - Hardware: [사용 중인 환경 작성]");
        System.out.println("  - Load: " + threads + " 동시 사용자");
        System.out.println("  - Duration: " + String.format("%.2f", durationSec) + "초");
        System.out.println();
        System.out.println("  **성능 지표:**");
        System.out.println("  - TPS: " + String.format("%.0f", tps) + " requests/sec");
        System.out.println("  - Average Latency: " + avg + "ms");
        System.out.println("  - P99 Latency: " + p99 + "ms");
        System.out.println("  - Success Rate: " + String.format("%.1f", success * 100.0 / total) + "%");
        System.out.println();
        System.out.println("=".repeat(80) + "\n");
    }
}


