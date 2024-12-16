package com.innercircle.controller;

import com.innercircle.controller.request.ParkingApplyRequest;
import com.innercircle.controller.response.ParkingApplyResponse;
import com.innercircle.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/car")
public class ParkingV1Controller {

    private final ParkingService parkingService;

    @PostMapping("/parking")
    public ResponseEntity<ParkingApplyResponse> parking(ParkingApplyRequest request) {
        return ResponseEntity.ok(parkingService.parking(request));
    }

}
