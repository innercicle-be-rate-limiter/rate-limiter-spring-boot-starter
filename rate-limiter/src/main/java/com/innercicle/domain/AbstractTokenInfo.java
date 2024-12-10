package com.innercicle.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AbstractTokenInfo {

    protected int capacity;
    protected long lastRefillTimestamp;
    protected int currentTokens;
    protected int rate;

    public AbstractTokenInfo(BucketProperties bucketProperties) {
        this.capacity = bucketProperties.getCapacity();
        this.currentTokens = bucketProperties.getCapacity();
        this.lastRefillTimestamp = System.currentTimeMillis();
        this.rate = bucketProperties.getRateUnit().toMillis();
    }

    public int getRemaining() {
        return this.currentTokens;
    }

    public int getLimit() {
        return this.capacity;
    }

    public int getRetryAfter() {
        return (int)(System.currentTimeMillis() - this.lastRefillTimestamp) / this.rate;
    }

    public void endProcess() {

    }

}
