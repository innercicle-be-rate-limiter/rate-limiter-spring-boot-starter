package com.innercicle.handler;

import com.innercicle.domain.AbstractTokenInfo;

public interface RateLimitHandler {

    AbstractTokenInfo allowRequest(String key);

    default void endRequest() {
    }

}
