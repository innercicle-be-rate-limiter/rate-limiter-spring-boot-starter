package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import io.lettuce.core.ScoredValue;
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

    /**
     * Sorted Set에서 데이터를 가져오거나 기본값을 반환
     *
     * @param redisKey
     * @param currentTimeMillis
     * @param clazz
     * @return
     */
    @Override
    public AbstractTokenInfo getSortedSetOrDefault(String redisKey, long currentTimeMillis, Class<? extends AbstractTokenInfo> clazz) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        long minusTime = currentTimeMillis - bucketProperties.getRateUnit().toMillis();

        List<ScoredValue<AbstractTokenInfo>> scoredValues =
            commands.zrangebyscoreWithScores(redisKey, minusTime, currentTimeMillis);
        if (!scoredValues.isEmpty()) {
            return scoredValues.getFirst().getValue();
        }
        try {
            return clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Error creating new instance", e);
        }
    }

    /**
     * 이동 윈도우 score 조회
     *
     * @param redisKey
     * @param currentTimeMillis
     * @return
     */
    @Override
    public long getCurrentScore(String redisKey, long currentTimeMillis) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        long minusTime = currentTimeMillis - bucketProperties.getRateUnit().toMillis();
        return commands.zcount(redisKey, minusTime, currentTimeMillis);
    }

    /**
     * Sorted Set에 데이터 저장
     *
     * @param key
     * @param tokenInfo
     */
    @Override
    public void saveSortedSet(String key, AbstractTokenInfo tokenInfo) {
        long currentTimestamp = Instant.now().toEpochMilli();
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        log.info("create key : {}", key);
        commands.zadd(key, currentTimestamp, tokenInfo);
    }

    /**
     * 처리가 완료 된 애들은 SCORE를 -1로 변경
     *
     * @param key
     * @param tokenBucketInfo
     */
    @Override
    public void removeSortedSet(String key, AbstractTokenInfo tokenBucketInfo) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        long minusTime = tokenBucketInfo.getLastRefillTimestamp() - bucketProperties.getRate();
        List<AbstractTokenInfo> values = commands.zrangebyscore(key, minusTime, tokenBucketInfo.getLastRefillTimestamp());
        values.stream()
            .findFirst()
            .ifPresent(lowestEntry -> {
                log.info("Adding entry with score -1: {}", lowestEntry);
                commands.zadd(key, -1, lowestEntry);
            });
    }

    @Override
    public long getSlidingWindowCount(String key, long currentTimeMillis) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();

        long currentWindowStart = currentTimeMillis - bucketProperties.getRateUnit().toMillis();
        long previousWindowStart = currentWindowStart - bucketProperties.getRateUnit().toMillis();

        double overlapRatio = (double)(currentTimeMillis - currentWindowStart) / bucketProperties.getRateUnit().toMillis();

        long currentWindowCount = commands.zcount(key, currentWindowStart, currentTimeMillis);
        long previousWindowCount = commands.zcount(key, previousWindowStart, currentWindowStart);

        log.info("current currentWindowCount::{}, previousWindowCount::{}, overlapRatio::{}",
                 currentWindowCount,
                 previousWindowCount,
                 overlapRatio);

        return Math.round(currentWindowCount + previousWindowCount * overlapRatio);
    }

}
