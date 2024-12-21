package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixedWindowCountInfo extends AbstractTokenInfo {

    private int windowSize;
    private int requestLimit;
    @Setter
    private int currentCount;

    public FixedWindowCountInfo(BucketProperties bucketProperties) {
        this.windowSize = bucketProperties.getFixedWindowCounter().getWindowSize();
        this.requestLimit = bucketProperties.getFixedWindowCounter().getRequestLimit();
        this.currentCount = 0;
    }

    @Override
    public int getRemaining() {
        return this.requestLimit - this.currentCount;
    }

    @Override
    public int getLimit() {
        return this.requestLimit;
    }

    @Override
    public int getRetryAfter() {
        return this.windowSize;
    }

    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

    public boolean isUnavailable() {
        return !this.isAvailable();
    }

    public void plusCount() {
        this.currentCount++;
    }

}
