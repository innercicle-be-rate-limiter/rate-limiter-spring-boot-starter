package com.innercicle.domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RateUnitTest {

    @Test
    void testToMillis() {
        // SECONDS
        assertThat(RateUnit.SECONDS.toMillis()).isEqualTo(1000);

        // MINUTE
        assertThat(RateUnit.MINUTE.toMillis()).isEqualTo(1000 * 60);

        // HOUR
        assertThat(RateUnit.HOUR.toMillis()).isEqualTo(1000 * 60 * 60);

        // DAY
        assertThat(RateUnit.DAY.toMillis()).isEqualTo(1000 * 60 * 60 * 24);
    }

    @Test
    void testToTimeUnit() {
        // SECONDS
        assertThat(RateUnit.SECONDS.toTimeUnit()).isEqualTo(TimeUnit.SECONDS);

        // MINUTE
        assertThat(RateUnit.MINUTE.toTimeUnit()).isEqualTo(TimeUnit.MINUTES);

        // HOUR
        assertThat(RateUnit.HOUR.toTimeUnit()).isEqualTo(TimeUnit.HOURS);

        // DAY
        assertThat(RateUnit.DAY.toTimeUnit()).isEqualTo(TimeUnit.DAYS);
    }

}