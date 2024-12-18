package com.innercircle.controller.response;

import com.innercircle.entity.CarEntity;
import lombok.Getter;

@Getter
public class ParkingApplyResponse {

    private Long id;
    /**
     * 신청자 식별자
     */
    private String userId;
    /**
     * 차 번호
     */
    private String carNo;

    /**
     * 신청 일자
     */
    private String applyDate;

    /**
     * 신청기한 시간
     */
    private String applyTime;

    /**
     * 신청기한 분
     */
    private String applyMinute;

    public static ParkingApplyResponse from(CarEntity savedEntity) {
        ParkingApplyResponse response = new ParkingApplyResponse();
        response.id = savedEntity.getId();
        response.userId = savedEntity.getUserId();
        response.carNo = savedEntity.getCarNo();
        response.applyMinute = savedEntity.getApplyMinute();
        response.applyTime = savedEntity.getApplyTime();
        response.applyDate = savedEntity.getApplyDate();
        return response;
    }

}
