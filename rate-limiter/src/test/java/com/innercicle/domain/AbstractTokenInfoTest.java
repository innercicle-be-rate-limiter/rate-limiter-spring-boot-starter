package com.innercicle.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractTokenInfoTest {

    private AbstractTokenInfo abstractTokenInfo;

    @BeforeEach
    void setUp() {
        // BucketProperties 객체 생성 및 설정
        BucketProperties bucketProperties = new BucketProperties();
        bucketProperties.setCapacity(100);
        bucketProperties.setRateUnit(RateUnit.SECONDS);

        abstractTokenInfo = new AbstractTokenInfo(bucketProperties) {
        };
    }

    @Test
    void testConstructorInitialization() {
        // 생성자가 올바르게 초기화했는지 확인
        assertThat(abstractTokenInfo.getCapacity()).isEqualTo(100);
        assertThat(abstractTokenInfo.getCurrentTokens()).isEqualTo(100);
        assertThat(abstractTokenInfo.getLimit()).isEqualTo(100);
        assertThat(abstractTokenInfo.getRemaining()).isEqualTo(100);
        assertThat(abstractTokenInfo.getRate()).isEqualTo(1000); // Seconds -> Milliseconds
        assertThat(abstractTokenInfo.getLastRefillTimestamp()).isNotZero();
    }

    @Test
    void testGetRemaining() {
        // 현재 토큰 수를 설정하고 getRemaining() 확인
        abstractTokenInfo.currentTokens = 50;
        assertThat(abstractTokenInfo.getRemaining()).isEqualTo(50);
    }

    @Test
    void testGetLimit() {
        // getLimit()이 capacity를 반환하는지 확인
        assertThat(abstractTokenInfo.getLimit()).isEqualTo(100);
    }

    @Test
    void testGetRetryAfter() {
        // 임의의 lastRefillTimestamp를 설정하여 getRetryAfter() 확인
        abstractTokenInfo.lastRefillTimestamp = System.currentTimeMillis() - 3000; // 3초 전
        abstractTokenInfo.rate = 1000; // 1초

        int retryAfter = abstractTokenInfo.getRetryAfter();
        assertThat(retryAfter).isEqualTo(3); // 3초 차이
    }

    @Test
    void testGetRetryAfterWithPositiveRate() {
        // given
        abstractTokenInfo.rate = 1000; // 1초 단위 (ms)

        // when
        abstractTokenInfo.lastRefillTimestamp = System.currentTimeMillis() - 3000; // 3초 전
        int retryAfter = abstractTokenInfo.getRetryAfter();

        // then
        assertThat(retryAfter).isEqualTo(3); // 3초 차이
    }

    @Test
    void testGetRetryAfterWithShortElapsedTime() {
        // given
        abstractTokenInfo.rate = 1000; // 1초 단위 (ms)

        // when
        abstractTokenInfo.lastRefillTimestamp = System.currentTimeMillis() - 500; // 0.5초 전
        int retryAfter = abstractTokenInfo.getRetryAfter();

        // then
        assertThat(retryAfter).isEqualTo(0); // 0.5초는 0으로 반올림
    }

    @Test
    void testGetRetryAfterWithLargeElapsedTime() {
        // given
        abstractTokenInfo.rate = 500; // 0.5초 단위 (ms)

        // when
        abstractTokenInfo.lastRefillTimestamp = System.currentTimeMillis() - 5000; // 5초 전
        int retryAfter = abstractTokenInfo.getRetryAfter();

        // then
        assertThat(retryAfter).isEqualTo(10); // 5000ms / 500ms = 10
    }

}