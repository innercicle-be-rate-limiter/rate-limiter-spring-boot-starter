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
        SlidingWindowLoggingInfo slidingWindowLoggingInfo = this.cacheTemplate.getSortedSetOrDefault(key, SlidingWindowLoggingInfo.class);
        log.error("capacity :: {}, requestLimit :: {}, currentCount :: {}",
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
        this.cacheTemplate.removeSortedSet(cacheKey);
        this.cacheTemplate.saveSortedSet(cacheKey, tokenBucketInfo);
    }

}
