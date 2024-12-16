package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.cache.CacheTemplate;
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

        if (!slidingWindowLoggingInfo.isAvailable()) {
            log.info("허용 범위를 넘어갔습니다.");
            throw new RateLimitException("You have reached the limit",
                                         slidingWindowLoggingInfo.getRemaining(),
                                         slidingWindowLoggingInfo.getLimit(),
                                         slidingWindowLoggingInfo.getRetryAfter());
        }
        
        this.cacheTemplate.saveSortedSet(key, slidingWindowLoggingInfo);
        return slidingWindowLoggingInfo;
    }

    @Override
    public void endRequest() {
        this.cacheTemplate.removeSortedSet();
    }

}
