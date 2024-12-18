package com.innercicle.handler;

import com.innercicle.advice.exceptions.RateLimitException;
import com.innercicle.cache.CacheTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.FixedWindowCountInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FixedWindowCounterHandler implements RateLimitHandler {

    private final CacheTemplate cacheTemplate;

    @Override
    public FixedWindowCountInfo allowRequest(String key) {
        FixedWindowCountInfo fixedWindowCounterInfo = (FixedWindowCountInfo)cacheTemplate.getOrDefault(key, FixedWindowCountInfo.class);
        if (fixedWindowCounterInfo.isUnavailable()) {
            log.error("허용되지 않은 요청입니다.");
            throw new RateLimitException("You have reached the limit",
                                         fixedWindowCounterInfo.getRemaining(),
                                         fixedWindowCounterInfo.getLimit(),
                                         fixedWindowCounterInfo.getRetryAfter());
        }
        fixedWindowCounterInfo.plusCount();

        return fixedWindowCounterInfo;
    }

    @Override
    public void endRequest(String cacheKey, AbstractTokenInfo tokenBucketInfo) {
        cacheTemplate.save(cacheKey, tokenBucketInfo);
    }

}
