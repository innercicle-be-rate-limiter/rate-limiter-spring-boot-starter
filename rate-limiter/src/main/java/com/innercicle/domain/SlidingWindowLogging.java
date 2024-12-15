package com.innercicle.domain;

import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @NoArgsConstructor
public class SlidingWindowLogging {

    private int windowSize;
    private int requestLimit;

}
