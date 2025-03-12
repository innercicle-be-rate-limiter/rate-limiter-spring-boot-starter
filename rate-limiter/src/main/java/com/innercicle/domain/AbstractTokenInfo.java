package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TokenBucketInfo.class, name = "TokenBucketInfo"),
    @JsonSubTypes.Type(value = FixedWindowCounter.class, name = "FixedWindowCounter"),
    @JsonSubTypes.Type(value = LeakyBucketInfo.class, name = "LeakyBucketInfo"),
    @JsonSubTypes.Type(value = SlidingWindowLoggingInfo.class, name = "SlidingWindowLoggingInfo"),
    @JsonSubTypes.Type(value = SlidingWindowCounterInfo.class, name = "SlidingWindowCounterInfo")
})
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

}
