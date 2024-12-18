package com.innercircle.service;

import com.innercicle.annotations.RateLimiting;
import com.innercircle.domain.CarInfo;
import com.innercircle.entity.CarEntity;
import com.innercircle.repository.ParkingRepository;
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
