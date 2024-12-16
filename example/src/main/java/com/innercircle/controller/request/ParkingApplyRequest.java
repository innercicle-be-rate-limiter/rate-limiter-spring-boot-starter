package com.innercircle.controller.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingApplyRequest {

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

}
