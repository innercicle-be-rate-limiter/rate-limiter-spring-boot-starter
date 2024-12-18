package com.innercicle.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("token-bucket.sliding-window-counter")
public class SlidingWindowCounter {

    private int requestLimit = 100;

}
