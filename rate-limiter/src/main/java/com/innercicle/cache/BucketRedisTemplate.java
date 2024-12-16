package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowLoggingInfo;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class BucketRedisTemplate implements CacheTemplate {

    private final StatefulRedisConnection<String, AbstractTokenInfo> connection;
    private final BucketProperties bucketProperties;

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
        List<AbstractTokenInfo> resultSet = connection.sync().zrangebyscore(key, 1, Double.MAX_VALUE);
        return resultSet == null || resultSet.isEmpty() ? (SlidingWindowLoggingInfo)createDefaultInstance(clazz) :
            resultSet.stream().filter(clazz::isInstance).findFirst().map(SlidingWindowLoggingInfo.class::cast).orElse(new SlidingWindowLoggingInfo());
    }

    public void saveSortedSet(String key, AbstractTokenInfo tokenInfo) {
        long currentTimestamp = Instant.now().toEpochMilli();
        String redisKey = key + ":" + currentTimestamp;
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        log.info("create key : {}", redisKey);
        if (commands.exists(redisKey) == 0L) {
            commands.zadd(redisKey, 1, tokenInfo);
        } else {
            double zcard = commands.zcard(redisKey);
            commands.zadd(redisKey, zcard + 1d, tokenInfo);
        }
    }

    public void removeSortedSet() {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();

        ScanCursor cursor = ScanCursor.INITIAL;
        ScanArgs scanArgs = ScanArgs.Builder.limit(100);

        do {
            var scanResult = commands.scan(cursor, scanArgs);
            cursor = scanResult;
            List<String> keys = scanResult.getKeys();
            for (String key : keys) {
                log.info("Processing key: {} ", key);

                List<AbstractTokenInfo> values = commands.zrangebyscore(key, 1, Integer.MAX_VALUE);
                values.stream()
                    .findFirst()
                    .ifPresent(lowestEntry -> {
                        log.info("Adding entry with score -1: " + lowestEntry);
                        commands.zadd(key, -1, lowestEntry);
                    });
            }
        } while (!cursor.isFinished());
    }

    private AbstractTokenInfo createDefaultInstance(Class<? extends AbstractTokenInfo> clazz) {
        try {
            return clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
