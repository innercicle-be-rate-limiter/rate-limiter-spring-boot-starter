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

    private long betweenRateCount;

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
        long requestCount = this.afterFixedWindowCount + (this.beforeFixedWindowCount * getCurrentWindowRequest());
        return this.currentCount < requestCount;
    }

    public boolean isUnavailable() {
        return !this.isAvailable();
    }

    private int getCurrentWindowRequest() {
        if (this.currentCount == 0) {
            throw new IllegalArgumentException("전체 갯수는 0일 수 없습니다.");
        }
        return (int)((this.currentCount / this.betweenRateCount) * 100);
    }

}
