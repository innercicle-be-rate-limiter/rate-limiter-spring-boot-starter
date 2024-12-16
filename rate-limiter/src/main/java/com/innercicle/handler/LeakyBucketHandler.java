package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.LeakyBucketInfo;
import jakarta.annotation.PreDestroy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h2>누출 버킷 알고리즘을 사용하여 요청을 제한하는 핸들러</h2>
 * 해당 알고리즘은 단순 버킷 알고리즘과 다르게, 토큰을 한 번에 모두 채우지 않고, 주기적으로 누출하는 방식으로 동작합니다. <br/>
 * 이를 통해, 토큰을 한 번에 모두 채우지 않고, 주기적으로 누출하는 방식으로 동작합니다.<br/>
 * 따라서 키 값으로 동작하지 않고, 키와 관계 없이 요청이 들어오면 큐에 담아 두고 순서대로 처리합니다.
 */
public class LeakyBucketHandler implements RateLimitHandler {

    private final Deque<LeakyBucketInfo> deque;
    private final int leakRate;                         // 누출 속도
    private final ScheduledExecutorService scheduler;   // 주기적으로 누출을 수행하는 스케줄러
    private final int capacity;
    private final TimeUnit timeUnit;

    // Leaky Bucket 생성자
    public LeakyBucketHandler(BucketProperties bucketProperties) {
        this.deque = new ArrayDeque<>(bucketProperties.getCapacity());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.capacity = bucketProperties.getCapacity();
        this.leakRate = bucketProperties.getRate();
        this.timeUnit = bucketProperties.getRateUnit().toTimeUnit();
        startLeakTask();
    }

    @Override
    public AbstractTokenInfo allowRequest(String key) {
        LeakyBucketInfo bucketInfo = LeakyBucketInfo.of(deque.size(), capacity);
        if (deque.size() < bucketInfo.getCapacity()) {
            deque.add(bucketInfo);
            if (scheduler.isShutdown()) {
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
