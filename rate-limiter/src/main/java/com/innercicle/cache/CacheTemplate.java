package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;

public interface CacheTemplate {

    AbstractTokenInfo getOrDefault(final String key, Class<? extends AbstractTokenInfo> tokenBucketInfoClass);

    void save(String key, AbstractTokenInfo tokenInfo);

}
