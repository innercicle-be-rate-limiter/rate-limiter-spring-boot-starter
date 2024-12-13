package com.innercicle.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("token-bucket.fixed-window-counter")
public class FixedWindowCounter extends AbstractTokenInfo {

    private int windowSize;
    private int requestLimit;

}
