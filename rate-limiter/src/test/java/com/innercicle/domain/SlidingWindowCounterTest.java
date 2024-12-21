package com.innercicle.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlidingWindowCounterTest {

    @Test
    void testDefaultRequestLimit() {
        // 기본 생성된 SlidingWindowCounter의 requestLimit 값을 확인
        SlidingWindowCounter slidingWindowCounter = new SlidingWindowCounter();
        assertEquals(100, slidingWindowCounter.getRequestLimit(), "Default requestLimit should be 100");
    }

    @Test
    void testSetRequestLimit() {
        // requestLimit 값을 설정하고 확인
        SlidingWindowCounter slidingWindowCounter = new SlidingWindowCounter();
        slidingWindowCounter.setRequestLimit(250);
        assertEquals(250, slidingWindowCounter.getRequestLimit(), "RequestLimit should be updated to 250");
    }

}