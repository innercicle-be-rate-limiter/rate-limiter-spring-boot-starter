package com.innercircle.service;

import com.innercicle.annotations.RateLimiting;
import com.innercircle.controller.request.ParkingApplyRequest;
import com.innercircle.controller.response.ParkingApplyResponse;
import com.innercircle.entity.CarEntity;
import com.innercircle.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRepository parkingRepository;

    @RateLimiting(
        name = "rate-limiting-service",
        cacheKey = "#request.userId"
    )
    public ParkingApplyResponse parking(ParkingApplyRequest request) {
        CarEntity savedEntity = parkingRepository.save(CarEntity.from(request));
        return ParkingApplyResponse.from(savedEntity);
    }

}