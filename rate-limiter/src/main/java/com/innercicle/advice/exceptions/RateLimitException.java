package com.innercicle.advice.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 처리율 제한 예외
 */
@Getter
public class RateLimitException extends RuntimeException {

    private static final Logger log = LoggerFactory.getLogger(RateLimitException.class);
    private final int remaining;
    private final int limit;
    private final int retryAfter;

    public RateLimitException(String message, int remaining, int limit, int retryAfter) {
        super(message);
        log.error("RateLimitException :: message :: {}, remaining :: {}, limit :: {}, retryAfter :: {}",
                  message, remaining, limit, retryAfter);
        this.remaining = remaining;
        this.limit = limit;
        this.retryAfter = retryAfter;
    }

}
