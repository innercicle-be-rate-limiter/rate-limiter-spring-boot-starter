package com.innercicle.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("token-bucket.sliding-window-logging")
public class SlidingWindowLogging {

    /**
     * 윈도우 크기
     */
    private int windowSize;

    /**
     * 요청 제한 갯수
     */
    private int requestLimit;

}
