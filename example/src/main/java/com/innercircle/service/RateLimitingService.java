package com.innercircle.service;

import com.innercicle.annotations.RateLimiting;
import com.innercicle.testratelimiter.car.domain.CarInfo;
import com.innercicle.testratelimiter.car.entity.CarEntity;
import com.innercicle.testratelimiter.car.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ParkingRepository parkingRepository;

    @RateLimiting(
        name = "rate-limiting-service",
        cacheKey = "#carInfo.carNo"
    )
    public void rateLimitingService(CarInfo carInfo) {
        parkingRepository.save(CarEntity.from(carInfo));
    }

}
