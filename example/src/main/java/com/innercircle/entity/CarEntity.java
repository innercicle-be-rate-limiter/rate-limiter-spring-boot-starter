package com.innercircle.entity;

import com.innercircle.controller.request.ParkingApplyRequest;
import com.innercircle.domain.CarInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "car")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarEntity {

    @Id
    @EntitySequence(name = "id")
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

    public static CarEntity from(CarInfo carInfo) {
        CarEntity car = new CarEntity();
        car.userId = carInfo.userId();
        car.carNo = carInfo.carNo();
        car.applyDate = carInfo.applyDate();
        car.applyTime = carInfo.applyTime();
        car.applyMinute = carInfo.applyMinute();
        return car;
    }

    public static CarEntity from(ParkingApplyRequest request) {
        CarEntity car = new CarEntity();
        car.userId = request.getUserId();
        car.carNo = request.getCarNo();
        car.applyDate = request.getApplyDate();
        car.applyTime = request.getApplyTime();
        car.applyMinute = request.getApplyMinute();
        return car;
    }

}
