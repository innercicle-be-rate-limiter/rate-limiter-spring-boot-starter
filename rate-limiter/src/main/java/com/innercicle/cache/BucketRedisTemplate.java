package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowLoggingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class BucketRedisTemplate implements CacheTemplate {

    private final RedisTemplate<String, AbstractTokenInfo> redisTokenInfoTemplate;
    private final BucketProperties bucketProperties;
    public static final String SLIDING_WINDOW_LOGGING_KEY_PREFIX = "sliding_window_logging:";

    @Override
    public AbstractTokenInfo getOrDefault(final String key, Class<? extends AbstractTokenInfo> clazz) {
        return Optional.ofNullable(redisTokenInfoTemplate.opsForValue().get(key))
            .orElseGet(() -> {
                try {
                    return clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    @Override
    public void save(String key, AbstractTokenInfo tokenInfo) {
        redisTokenInfoTemplate.opsForValue().set(key, tokenInfo, Duration.ofMillis(3_000));
    }

    @Override
    public SlidingWindowLoggingInfo getSortedSetOrDefault(String key, Class<? extends AbstractTokenInfo> clazz) {
        RedisOperations<String, AbstractTokenInfo> operations = redisTokenInfoTemplate.opsForZSet().getOperations();
        Set<AbstractTokenInfo> resultSet =
            operations.opsForZSet().rangeByScore(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key, 1, Double.MAX_VALUE);

        try {
            if (resultSet == null || resultSet.isEmpty()) {
                return (SlidingWindowLoggingInfo)clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return resultSet.stream()
            .filter(clazz::isInstance)
            .findFirst()
            .map(SlidingWindowLoggingInfo.class::cast)
            .orElse(new SlidingWindowLoggingInfo());
    }

    @Override
    public void saveSortedSet(String key, AbstractTokenInfo tokenInfo) {
        Long size = redisTokenInfoTemplate.opsForZSet().size(key);
        long increaseScore = size == null || size == 0L ? 1L : ++size;
        redisTokenInfoTemplate.opsForZSet().add(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key, tokenInfo, increaseScore);
    }

    @Override
    public void removeSortedSet() {
        RedisOperations<String, AbstractTokenInfo> operations = redisTokenInfoTemplate.opsForZSet().getOperations();
        Set<String> keys = operations.keys(SLIDING_WINDOW_LOGGING_KEY_PREFIX);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        ZSetOperations<String, AbstractTokenInfo> zSetOperations = redisTokenInfoTemplate.opsForZSet();

        for (String key : keys) {
            Set<ZSetOperations.TypedTuple<AbstractTokenInfo>> rangeWithScores =
                zSetOperations.rangeWithScores(key, 1, Integer.MAX_VALUE);
            if (rangeWithScores != null && !rangeWithScores.isEmpty()) {
                ZSetOperations.TypedTuple<AbstractTokenInfo> lowestEntry = rangeWithScores.iterator().next();
                AbstractTokenInfo abstractTokenInfo = lowestEntry.getValue();
                if (abstractTokenInfo != null) {
                    zSetOperations.add(key, abstractTokenInfo, -1);
                    return;
                }
            }
        }
    }

}
