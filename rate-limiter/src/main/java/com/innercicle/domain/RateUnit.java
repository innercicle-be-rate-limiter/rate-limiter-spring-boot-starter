package com.innercicle.domain;

import java.util.concurrent.TimeUnit;

public enum RateUnit {
    SECONDS,
    MINUTE,
    HOUR,
    DAY;

    public int toMillis() {
        return switch (this) {
            case SECONDS -> 1000;
            case MINUTE -> 1000 * 60;
            case HOUR -> 1000 * 60 * 60;
            case DAY -> 1000 * 60 * 60 * 24;
        };
    }

    public TimeUnit toTimeUnit() {
        return switch (this) {
            case SECONDS -> TimeUnit.SECONDS;
            case MINUTE -> TimeUnit.MINUTES;
            case HOUR -> TimeUnit.HOURS;
            case DAY -> TimeUnit.DAYS;
        };
    }
}
