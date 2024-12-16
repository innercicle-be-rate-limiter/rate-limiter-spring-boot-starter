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
        LeakyBucketInfo bucketInfo = LeakyBucketInfo.of(capacity);
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
        if (!deque.isEmpty()) {
            deque.removeLast();
        }
    }

    private void startLeakTask() {
        scheduler.scheduleAtFixedRate(() -> {
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
