package com.innercircle.ratelimiter.domain;

import com.innercicle.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이동 윈도우 카운터 properties 설정
 */
class FixedWindowCounterInfoTest {

    private BucketProperties bucketProperties;

    @BeforeEach
    void setUp() {
        // BucketProperties Mock 객체 생성
        bucketProperties = new DummyBucketProperties();
    }

    @Test
    void testConstructor() {
        // given
        FixedWindowCountInfo info = new FixedWindowCountInfo(bucketProperties);

        // then
        assertThat(info.getRequestLimit()).isEqualTo(10);
        assertThat(info.getCurrentCount()).isEqualTo(0);
    }

    @Test
    void testIsAvailable() {
        // given
        FixedWindowCountInfo info = new FixedWindowCountInfo(bucketProperties);

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
        FixedWindowCountInfo info = new FixedWindowCountInfo(bucketProperties);

        // when
        info.setCurrentCount(10);

        // then
        assertThat(info.isUnavailable()).isTrue(); // 현재 카운트(10)가 제한에 도달
    }

    static class DummyBucketProperties extends BucketProperties {

        public DummyBucketProperties() {
            FixedWindowCounter fixedWindowCounter = new FixedWindowCounter();
            fixedWindowCounter.setRequestLimit(10);
            setFixedWindowCounter(fixedWindowCounter);
        }

    }

}
