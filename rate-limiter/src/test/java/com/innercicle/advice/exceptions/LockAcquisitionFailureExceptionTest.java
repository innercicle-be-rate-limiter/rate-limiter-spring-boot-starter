package com.innercicle.advice.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockAcquisitionFailureExceptionTest {

    @Test
    void testExceptionMessage() {
        // given
        String message = "Failed to acquire lock";

        // when
        LockAcquisitionFailureException exception = new LockAcquisitionFailureException(message);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testThrowingException() {
        // given
        String message = "Failed to acquire lock";

        // then
        assertThatThrownBy(() -> {
            throw new LockAcquisitionFailureException(message);
        }).isInstanceOf(LockAcquisitionFailureException.class)
            .hasMessage(message);
    }

}