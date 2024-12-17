package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlidingWindowLoggingInfo extends AbstractTokenInfo {

    private int requestLimit;
    private long currentCount;

    public SlidingWindowLoggingInfo(BucketProperties bucketProperties) {
        super(bucketProperties);
        this.requestLimit = bucketProperties.getSlidingWindowLogging().getRequestLimit();
        this.currentCount = 0;
    }

    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

    public boolean isUnavailable() {
        return !this.isAvailable();
    }

}
