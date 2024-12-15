package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowLoggingInfo;
import lombok.RequiredArgsConstructor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class BucketRedisTemplate implements CacheTemplate {

    private final StatefulRedisConnection<String, AbstractTokenInfo> connection;
    private final BucketProperties bucketProperties;
    public static final String SLIDING_WINDOW_LOGGING_KEY_PREFIX = "sliding_window_logging:";

    @Override
    public AbstractTokenInfo getOrDefault(final String key, Class<? extends AbstractTokenInfo> clazz) {
        RedisCommands<String, AbstractTokenInfo> syncCommands = connection.sync();

        return Optional.ofNullable(syncCommands.get(key))
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
        RedisCommands<String, AbstractTokenInfo> syncCommands = connection.sync();
        // Lettuce는 기본적으로 만료 시간을 세트할 때 `EX`(초) 또는 `PX`(밀리초) 옵션을 사용
        syncCommands.setex(key, Duration.ofMillis(3_000).toSeconds(), tokenInfo);
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
