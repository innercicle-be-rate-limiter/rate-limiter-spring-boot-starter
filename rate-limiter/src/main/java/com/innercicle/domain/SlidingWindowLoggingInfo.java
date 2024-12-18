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

    /**
     * 윈도우 크기
     */
    private int windowSize;
    /**
     * 요청 제한 갯수
     */
    private int requestLimit;
    /**
     * 현재 카운트
     */
    private long currentCount;

    public SlidingWindowLoggingInfo(BucketProperties bucketProperties) {
        super(bucketProperties);
        this.requestLimit = bucketProperties.getSlidingWindowLogging().getRequestLimit();
        this.currentCount = 0;
    }

    /**
     * 현재 카운트가 요청 제한 갯수보다 크면 안된다.
     * @return
     */
    public boolean isAvailable() {
        return this.currentCount < this.requestLimit;
    }

    public boolean isUnavailable() {
        return !this.isAvailable();
    }

}
