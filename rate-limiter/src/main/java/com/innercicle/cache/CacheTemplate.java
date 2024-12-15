package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.SlidingWindowLoggingInfo;

public interface CacheTemplate {

    AbstractTokenInfo getOrDefault(final String key, Class<? extends AbstractTokenInfo> tokenBucketInfoClass);

    void save(String key, AbstractTokenInfo tokenInfo);

    SlidingWindowLoggingInfo getSortedSetOrDefault(String key, Class<? extends AbstractTokenInfo> clazz);

    void saveSortedSet(String key, AbstractTokenInfo tokenInfo);

    void removeSortedSet();

}
