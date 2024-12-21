package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.cache.CacheTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.SlidingWindowLoggingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이동 윈도우 로깅 핸들러
 */
@Slf4j
@RequiredArgsConstructor
public class SlidingWindowLoggingHandler implements RateLimitHandler {

    private final CacheTemplate cacheTemplate;

    @Override
    public SlidingWindowLoggingInfo allowRequest(String key) {
        long currentTimeMillis = System.currentTimeMillis();
        SlidingWindowLoggingInfo slidingWindowLoggingInfo =
            (SlidingWindowLoggingInfo)this.cacheTemplate.getSortedSetOrDefault(key, currentTimeMillis, SlidingWindowLoggingInfo.class);
        slidingWindowLoggingInfo.setCurrentCount(this.cacheTemplate.getCurrentScore(key, currentTimeMillis));
        log.info("capacity :: {}, requestLimit :: {}, currentCount :: {}",
                 slidingWindowLoggingInfo.getCapacity(),
                 slidingWindowLoggingInfo.getRequestLimit(),
                 slidingWindowLoggingInfo.getCurrentCount());
        if (slidingWindowLoggingInfo.isUnavailable()) {
            log.info("허용 범위를 넘어갔습니다.");
            throw new RateLimitException("You have reached the limit",
                                         slidingWindowLoggingInfo.getRemaining(),
                                         slidingWindowLoggingInfo.getLimit(),
                                         slidingWindowLoggingInfo.getRetryAfter());
        }

        return slidingWindowLoggingInfo;
    }

    @Override
    public void endRequest(String cacheKey, AbstractTokenInfo tokenBucketInfo) {
        this.cacheTemplate.removeSortedSet(cacheKey, tokenBucketInfo);
        this.cacheTemplate.saveSortedSet(cacheKey, tokenBucketInfo);
    }

}
