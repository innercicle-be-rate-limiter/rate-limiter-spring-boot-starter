package com.innercicle.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BucketPropertiesTest {

    @Test
    void testDefaultValues() {
        // given
        BucketProperties bucketProperties = new BucketProperties();

        // then
        assertThat(bucketProperties.getRateUnit()).isEqualTo(RateUnit.SECONDS);
        assertThat(bucketProperties.getCapacity()).isZero(); // 기본값
        assertThat(bucketProperties.getRate()).isZero(); // 기본값
        assertThat(bucketProperties.getFixedWindowCounter()).isNull();
        assertThat(bucketProperties.getSlidingWindowLogging()).isNull();
        assertThat(bucketProperties.getSlidingWindowCounter()).isNull();
    }

    @Test
    void testSetAndGetProperties() {
        // given
        BucketProperties bucketProperties = new BucketProperties();

        FixedWindowCounter fixedWindowCounter = new FixedWindowCounter();
        SlidingWindowLogging slidingWindowLogging = new SlidingWindowLogging();
        SlidingWindowCounter slidingWindowCounter = new SlidingWindowCounter();

        // when
        bucketProperties.setCapacity(100);
        bucketProperties.setRate(10);
        bucketProperties.setRateUnit(RateUnit.MINUTE);
        bucketProperties.setFixedWindowCounter(fixedWindowCounter);
        bucketProperties.setSlidingWindowLogging(slidingWindowLogging);
        bucketProperties.setSlidingWindowCounter(slidingWindowCounter);

        // then
        assertThat(bucketProperties.getCapacity()).isEqualTo(100);
        assertThat(bucketProperties.getRate()).isEqualTo(10);
        assertThat(bucketProperties.getRateUnit()).isEqualTo(RateUnit.MINUTE);
        assertThat(bucketProperties.getFixedWindowCounter()).isEqualTo(fixedWindowCounter);
        assertThat(bucketProperties.getSlidingWindowLogging()).isEqualTo(slidingWindowLogging);
        assertThat(bucketProperties.getSlidingWindowCounter()).isEqualTo(slidingWindowCounter);
    }

}
