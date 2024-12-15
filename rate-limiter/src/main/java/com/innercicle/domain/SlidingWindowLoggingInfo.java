package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.innercicle.cache.CacheTemplate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlidingWindowLoggingInfo extends AbstractTokenInfo {

    private int windowSize;
    private int requestLimit;
    private int currentCount;
    private CacheTemplate cacheTemplate;

    public SlidingWindowLoggingInfo(BucketProperties bucketProperties, CacheTemplate cacheTemplate) {
        this.windowSize = bucketProperties.getFixedWindowCounter().getWindowSize();
        this.requestLimit = bucketProperties.getFixedWindowCounter().getRequestLimit();
        this.cacheTemplate = cacheTemplate;
        this.currentCount = 0;
    }

    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

    /**
     * 호출 후 redis에 쌓인 데이터 제거
     */
    @Override
    public void endProcess() {
        cacheTemplate.removeSortedSet();
    }

}
