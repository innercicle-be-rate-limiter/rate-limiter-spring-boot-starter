package com.innercircle.ratelimiter.domain;

import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowCounterInfo;
import com.innercicle.domain.SlidingWindowLogging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이동 윈도우 카운터 properties 설정
 */
class SlidingWindowCounterInfoTest {

    private BucketProperties bucketProperties;

    @BeforeEach
    void setUp() {
        // BucketProperties Mock 객체 생성
        bucketProperties = new DummyBucketProperties();
    }

    @Test
    void testConstructor() {
        // given
        SlidingWindowCounterInfo info = new SlidingWindowCounterInfo(bucketProperties);

        // then
        assertThat(info.getRequestLimit()).isEqualTo(10);
        assertThat(info.getCurrentCount()).isEqualTo(0);
    }

    @Test
    void testIsAvailable() {
        // given
        SlidingWindowCounterInfo info = new SlidingWindowCounterInfo(bucketProperties);

        // when
        info.setCurrentCount(5);

        // then
        assertThat(info.isAvailable()).isTrue(); // 현재 카운트(5)가 요청 제한(10)보다 작음

        // when
        info.setCurrentCount(10);

        // then
        assertThat(info.isAvailable()).isFalse(); // 현재 카운트(10)가 요청 제한(10)과 같음
    }

    @Test
    void testIsUnavailable() {
        // given
        SlidingWindowCounterInfo info = new SlidingWindowCounterInfo(bucketProperties);

        // when
        info.setCurrentCount(10);

        // then
        assertThat(info.isUnavailable()).isTrue(); // 현재 카운트(10)가 제한에 도달
    }

    static class DummyBucketProperties extends BucketProperties {

        public DummyBucketProperties() {
            SlidingWindowLogging slidingWindowLogging = new SlidingWindowLogging();
            slidingWindowLogging.setRequestLimit(10);
            setSlidingWindowLogging(slidingWindowLogging);
        }

    }

}
