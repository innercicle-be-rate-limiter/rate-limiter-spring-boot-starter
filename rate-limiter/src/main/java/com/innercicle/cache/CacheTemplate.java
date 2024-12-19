package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;

/**
 * <h2>캐시 처리용 템플릿 인터페이스</h2>
 * cache-type 별로 구현체를 만들어서 사용한다.
 *
 * @see com.innercicle.cache.BucketRedisTemplate
 */
public interface CacheTemplate {

    AbstractTokenInfo getOrDefault(final String key, Class<? extends AbstractTokenInfo> tokenBucketInfoClass);

    void save(String key, AbstractTokenInfo tokenInfo);

    AbstractTokenInfo getSortedSetOrDefault(String key, Class<? extends AbstractTokenInfo> clazz);

    void saveSortedSet(String key, AbstractTokenInfo tokenInfo);

    void removeSortedSet(String key, AbstractTokenInfo tokenBucketInfo);

    long getCurrentScore(String key, long currentTimeMillis);

    long findCountWithinBeforeRange(String key, long currentTimeMillis);

    long findCountWithinAfterRange(String key, long currentTimeMillis);

    long betweenRateInSlidingWindowCounter(String key, long currentTimeMillis);

}
