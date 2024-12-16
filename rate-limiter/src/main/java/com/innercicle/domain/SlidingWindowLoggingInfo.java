package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlidingWindowLoggingInfo extends AbstractTokenInfo {

    private int windowSize;
    private int requestLimit;
    private int currentCount;

    public SlidingWindowLoggingInfo(BucketProperties bucketProperties) {
        this.windowSize = bucketProperties.getFixedWindowCounter().getWindowSize();
        this.requestLimit = bucketProperties.getFixedWindowCounter().getRequestLimit();
        this.currentCount = 0;
    }

    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

}
