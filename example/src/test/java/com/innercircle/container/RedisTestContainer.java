package com.innercircle.container;

import org.junit.ClassRule;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

@ActiveProfiles("test")
public class RedisTestContainer extends PostgresTestContainerInitializer {

    private static final String DOCKER_REDIS_IMAGE = "redis:6-alpine";

    @ClassRule
    public static final GenericContainer<?> REDIS_CONTAINER;

    static {

        REDIS_CONTAINER = new GenericContainer<>(DOCKER_REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

        REDIS_CONTAINER.start();
    }

    // 동적 설정값 매핑
    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {

        // redis
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));

    }

}
