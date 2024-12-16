package com.innercircle.domain;

public record CarInfo(

    String userId,
    /* 차 번호 */
    String carNo,
    /* 신청 일자 */
    String applyDate,

    /* 신청기한 시간 */
    String applyTime,

    /* 신청기한 분 */
    String applyMinute
) {

}
