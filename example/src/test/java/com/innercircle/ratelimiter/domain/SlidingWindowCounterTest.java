package com.innercircle.ratelimiter.domain;

import com.innercicle.domain.SlidingWindowCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SlidingWindowCounter.class)
@EnableConfigurationProperties(SlidingWindowCounter.class)
class SlidingWindowCounterTest {

    @Autowired
    private SlidingWindowCounter slidingWindowCounter;

    @Test
    @DisplayName("testConfigurationPropertiesBinding")
    void test_case_1() throws Exception {
        // given: application.yml 또는 application.properties에 설정된 값
        int expectedRequestLimit = 100;

        // then: 값이 제대로 바인딩되었는지 확인
        assertThat(slidingWindowCounter.getRequestLimit()).isEqualTo(expectedRequestLimit);
    }

}
