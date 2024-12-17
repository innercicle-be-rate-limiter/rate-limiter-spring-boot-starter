package com.innercircle.service;

import com.innercicle.domain.BucketProperties;
import com.innercircle.container.RedisTestContainer;
import com.innercircle.controller.request.ParkingApplyRequest;
import com.innercircle.entity.CarEntity;
import com.innercircle.repository.ParkingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "rate-limiter.enabled=true",
    "rate-limiter.lock-type=redis_redisson",
    "rate-limiter.rate-type=sliding_window_logging",
    "rate-limiter.cache-type=REDIS",
    "token-bucket.sliding-window-logging.request-limit=10"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SlidingWindowLoggingTest extends RedisTestContainer {

    @Autowired
    private ParkingService parkingService;
    @Autowired
    private ParkingRepository parkingRepository;
    @Autowired
    private BucketProperties bucketProperties;

    @BeforeEach
    public void beforeEach() {
        parkingRepository.deleteAll();
    }

    @Test
    @DisplayName("주차권 저장 테스트")
    void lateLimitingTest() {
        // given
        String carNo = "07하3115";
        ParkingApplyRequest parkingApplyRequest = new ParkingApplyRequest("seunggulee", carNo, "20240722", "10", "00");
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        parkingService.parking(parkingApplyRequest);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        // then
        List<CarEntity> allByCarNoIs = parkingRepository.findAllByCarNoIs(carNo);

        assertThat(allByCarNoIs).hasSize(bucketProperties.getCapacity());

    }

}
