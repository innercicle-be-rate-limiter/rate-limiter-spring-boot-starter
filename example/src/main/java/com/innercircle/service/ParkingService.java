package com.innercircle.service;

import com.innercicle.annotations.RateLimiting;
import com.innercicle.testratelimiter.car.controller.request.ParkingApplyRequest;
import com.innercicle.testratelimiter.car.controller.response.ParkingApplyResponse;
import com.innercicle.testratelimiter.car.entity.CarEntity;
import com.innercicle.testratelimiter.car.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRepository parkingRepository;

    @RateLimiting(
        name = "rate-limiting-service",
        cacheKey = "#request.carNo"
    )
    public ParkingApplyResponse parking(ParkingApplyRequest request) {
        CarEntity savedEntity = parkingRepository.save(CarEntity.from(request));
        return ParkingApplyResponse.from(savedEntity);
    }

}