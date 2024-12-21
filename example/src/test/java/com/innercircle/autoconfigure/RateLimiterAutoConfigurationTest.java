package com.innercircle.autoconfigure;

import com.innercicle.aop.RateLimitingProperties;
import com.innercircle.container.RedisTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RateLimiterAutoConfigurationTest extends RedisTestContainer {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되었는지 확인
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void rateLimitingPropertiesBeanExists() {
        // RateLimitingProperties 빈이 등록되었는지 확인
        assertThat(applicationContext.getBean(RateLimitingProperties.class)).isNotNull();
    }

    @Test
    void redisPropertiesConditionalBeanTest() {
        // 특정 조건에 따라 빈이 등록되었는지 확인
        if (applicationContext.getEnvironment().getProperty("rate-limiter.cache-type").equals("redis")) {
            assertThat(applicationContext.getBean(RedisProperties.class)).isNotNull();
        }
    }

}
