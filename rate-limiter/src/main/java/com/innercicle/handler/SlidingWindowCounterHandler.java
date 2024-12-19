package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.cache.CacheTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.SlidingWindowCounterInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이동 윈도우 카운터 핸들러
 */
@Slf4j
@RequiredArgsConstructor
public class SlidingWindowCounterHandler implements RateLimitHandler {

    private final CacheTemplate cacheTemplate;

    @Override
    public AbstractTokenInfo allowRequest(String key) {
        long currentTimeMillis = System.currentTimeMillis();
        SlidingWindowCounterInfo slidingWindowCounterInfo =
            (SlidingWindowCounterInfo)cacheTemplate.getSortedSetOrDefault(key, SlidingWindowCounterInfo.class);
        slidingWindowCounterInfo.setCurrentCount(this.cacheTemplate.getCurrentScore(key, currentTimeMillis));
        slidingWindowCounterInfo.setBeforeFixedWindowCount(this.cacheTemplate.findCountWithinBeforeRange(key, currentTimeMillis));
        slidingWindowCounterInfo.setAfterFixedWindowCount(this.cacheTemplate.findCountWithinAfterRange(key, currentTimeMillis));
        slidingWindowCounterInfo.setBetweenRateCount(this.cacheTemplate.betweenRateInSlidingWindowCounter(key, currentTimeMillis));
        log.info("capacity :: {}, requestLimit :: {}, currentCount :: {}",
                 slidingWindowCounterInfo.getCapacity(),
                 slidingWindowCounterInfo.getRequestLimit(),
                 slidingWindowCounterInfo.getCurrentCount());
        if (slidingWindowCounterInfo.isUnavailable()) {
            log.info("허용 범위를 넘어갔습니다.");
            throw new RateLimitException("You have reached the limit",
                                         slidingWindowCounterInfo.getRemaining(),
                                         slidingWindowCounterInfo.getLimit(),
                                         slidingWindowCounterInfo.getRetryAfter());
        }

        return slidingWindowCounterInfo;
    }

    @Override
    public void endRequest(String cacheKey, AbstractTokenInfo tokenBucketInfo) {
        this.cacheTemplate.removeSortedSet(cacheKey, tokenBucketInfo);
        this.cacheTemplate.saveSortedSet(cacheKey, tokenBucketInfo);
    }

}
