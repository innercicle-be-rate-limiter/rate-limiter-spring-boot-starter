package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.cache.CacheTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.LeakyBucketInfo;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h2>누출 버킷 알고리즘을 사용하여 요청을 제한하는 핸들러</h2>
 * 해당 알고리즘은 단순 버킷 알고리즘과 다르게, 토큰을 한 번에 모두 채우지 않고, 주기적으로 누출하는 방식으로 동작. <br/>
 * 이를 통해, 토큰을 한 번에 모두 채우지 않고, 주기적으로 누출하는 방식으로 동작.<br/>
 * 따라서 키 값으로 동작하지 않고, 키와 관계 없이 요청이 들어오면 큐에 담아 두고 순서대로 처리.<br/>
 */
@RequiredArgsConstructor
public class LeakyBucketHandler implements RateLimitHandler {

    private final CacheTemplate cacheTemplate;
    private final Deque<LeakyBucketInfo> deque;
    private final int leakRate;                         // 누출 속도
    private final ScheduledExecutorService scheduler;   // 주기적으로 누출을 수행하는 스케줄러
    private final int capacity;
    private final TimeUnit timeUnit;

    // Leaky Bucket 생성자
    public LeakyBucketHandler(CacheTemplate cacheTemplate, BucketProperties bucketProperties) {
        this.cacheTemplate = cacheTemplate;
        LeakyBucketInfo leakyBucketInfo = (LeakyBucketInfo)cacheTemplate.getOrDefault("deque", LeakyBucketInfo.class);
        this.deque = leakyBucketInfo.getDeque();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.capacity = bucketProperties.getCapacity();
        this.leakRate = bucketProperties.getRate();
        this.timeUnit = bucketProperties.getRateUnit().toTimeUnit();
        startLeakTask();
    }

    @Override
    public AbstractTokenInfo allowRequest(String key) {
        LeakyBucketInfo bucketInfo = (LeakyBucketInfo)cacheTemplate.getOrDefault("deque", LeakyBucketInfo.class);
        if (deque.size() < bucketInfo.getCapacity()) {
            deque.add(bucketInfo);
            if (scheduler.isShutdown()) {   // 스케줄러가 종료되었을 경우 재시작
                startLeakTask();
            }
            return bucketInfo;
        }
        throw new RateLimitException("You have reached the limit",
                                     bucketInfo.getRemaining(),
                                     bucketInfo.getLimit(),
                                     bucketInfo.getRetryAfter());
    }

    @Override
    public void endRequest() {
        if (!this.deque.isEmpty()) {
            this.deque.removeLast();
        }
    }

    /**
     * <h2>누출 작업을 시작.</h2>
     * 큐에 있는 모든 요소를 누출 속도에 맞게 제거.
     */
    private void startLeakTask() {
        this.scheduler.scheduleAtFixedRate(() -> {
            synchronized (LeakyBucketHandler.this) {
                while (!this.deque.isEmpty()) {
                    this.deque.poll();
                }
            }
        }, 0, this.leakRate, this.timeUnit);
    }

    @PreDestroy
    public void destroy() {
        this.scheduler.shutdown();
    }

}
