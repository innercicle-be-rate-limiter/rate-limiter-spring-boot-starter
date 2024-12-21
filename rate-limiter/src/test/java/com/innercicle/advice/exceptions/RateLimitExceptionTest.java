package com.innercicle.advice.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitExceptionTest {

    @Test
    void testRateLimitExceptionFields() {
        // given
        String message = "Rate limit exceeded";
        int remaining = 5;
        int limit = 10;
        int retryAfter = 30;

        // when
        RateLimitException exception = new RateLimitException(message, remaining, limit, retryAfter);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getRemaining()).isEqualTo(remaining);
        assertThat(exception.getLimit()).isEqualTo(limit);
        assertThat(exception.getRetryAfter()).isEqualTo(retryAfter);
    }

    @Test
    void testThrowingRateLimitException() {
        // given
        String message = "Rate limit exceeded";
        int remaining = 5;
        int limit = 10;
        int retryAfter = 30;

        // then
        assertThatThrownBy(() -> {
            throw new RateLimitException(message, remaining, limit, retryAfter);
        }).isInstanceOf(RateLimitException.class)
            .hasMessage(message)
            .hasFieldOrPropertyWithValue("remaining", remaining)
            .hasFieldOrPropertyWithValue("limit", limit)
            .hasFieldOrPropertyWithValue("retryAfter", retryAfter);
    }

}