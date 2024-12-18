package com.innercicle.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("token-bucket")
public class BucketProperties {

    private int capacity;
    private int rate;
    private RateUnit rateUnit = RateUnit.SECONDS;
    private FixedWindowCounter fixedWindowCounter;
    private SlidingWindowLogging slidingWindowLogging;

}
