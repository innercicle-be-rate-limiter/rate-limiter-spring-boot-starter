package com.innercicle.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiting {

    String name();

    String cacheKey() default "";

    String executeCondition() default "";

    String skipCondition() default "";

    boolean ratePerMethod() default false;

    String fallbackMethodName() default "";

    long waitTime() default 5000L;

    long leaseTime() default 2000L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

}
