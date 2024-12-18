package com.innercicle.handler;

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
        SlidingWindowCounterInfo slidingWindowCounterInfo =
            (SlidingWindowCounterInfo)cacheTemplate.getSortedSetOrDefault(key, SlidingWindowCounterInfo.class);
        slidingWindowCounterInfo.setCurrentCount(this.cacheTemplate.getCurrentScore(key));
        slidingWindowCounterInfo.setBeforeFixedWindowCount(this.cacheTemplate.findCountWithinBeforeRange(key));
        slidingWindowCounterInfo.setAfterFixedWindowCount(this.cacheTemplate.findCountWithinAfterRange(key));

        return null;
    }

}
