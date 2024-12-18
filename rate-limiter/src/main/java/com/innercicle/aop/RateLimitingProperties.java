package com.innercicle.aop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("rate-limiter")
public class RateLimitingProperties {

    /**
     * 사용 여부
     */
    private boolean enabled;

    /**
     * 처리율 유형
     */
    private LockType lockType = LockType.REDIS_REDISSON;

    /**
     * 처리율 유형
     */
    private RateType rateType = RateType.TOKEN_BUCKET;

    /**
     * 캐시 유형
     */
    private CacheType cacheType = CacheType.REDIS;

}
