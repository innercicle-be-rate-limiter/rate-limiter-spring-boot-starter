package com.innercicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlidingWindowCounterInfo extends AbstractTokenInfo {

    /**
     * 요청 제한 갯수
     */
    private int requestLimit;
    /**
     * 현재 카운트
     */
    private long currentCount;

    /**
     * 고정 윈도우 카운트 (이전)
     */
    private long beforeFixedWindowCount;

    private long afterFixedWindowCount;

    public SlidingWindowCounterInfo(BucketProperties bucketProperties) {
        super(bucketProperties);
        this.requestLimit = bucketProperties.getSlidingWindowLogging().getRequestLimit();
        this.currentCount = 0;
    }

    /**
     * 현재 카운트가 요청 제한 갯수보다 크면 안된다.
     *
     * @return
     */
    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

    public boolean isUnavailable() {
        return !this.isAvailable();
    }

}
