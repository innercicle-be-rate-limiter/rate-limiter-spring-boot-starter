package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowLoggingInfo;
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
     * @param clazz
     * @return
     */
    @Override
    public SlidingWindowLoggingInfo getSortedSetOrDefault(String redisKey, Class<? extends AbstractTokenInfo> clazz) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        Optional<AbstractTokenInfo> optionalAbstractTokenInfo = Optional.empty();
        long currentScore = 0;
        long currentTime = Instant.now().toEpochMilli();
        long minusTime = currentTime - bucketProperties.getRateUnit().toMillis();
        long plusTime = currentTime + bucketProperties.getRateUnit().toMillis();
        try {
            List<ScoredValue<AbstractTokenInfo>> scoredValues =
                commands.zrangebyscoreWithScores(redisKey, minusTime, plusTime);
            for (ScoredValue<AbstractTokenInfo> scoredValue : scoredValues) {
                log.info("Value: {}, Score: {}", scoredValue.getValue(), scoredValue.getScore());
                optionalAbstractTokenInfo = Optional.of(scoredValue.getValue());
                currentScore = commands.zcount(redisKey, minusTime, plusTime);
                break;
            }

            if (optionalAbstractTokenInfo.isPresent()) {
                SlidingWindowLoggingInfo result = (SlidingWindowLoggingInfo)optionalAbstractTokenInfo.get();
                result.setCurrentCount(currentScore);
                log.info("Current key score: {}", currentScore);
                return result;
            }
        } catch (Exception e) {
            log.error("Error fetching data from Redis", e);
        }

        try {
            return (SlidingWindowLoggingInfo)clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Error creating new instance", e);
        }
    }

    /**
     * Sorted Set에 데이터 저장
     *
     * @param key
     * @param tokenInfo
     */
    public void saveSortedSet(String key, AbstractTokenInfo tokenInfo) {
        long currentTimestamp = Instant.now().toEpochMilli();
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        log.info("create key : {}", key);
        commands.zadd(key, currentTimestamp, tokenInfo);
    }

    /**
     * 처리가 완료 된 애들은 SCORE를 -1로 변경
     *
     * @param redisKey
     */
    public void removeSortedSet(String redisKey) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        long currentTime = Instant.now().toEpochMilli();
        long minusTime = currentTime - bucketProperties.getRate();
        long plusTime = currentTime + bucketProperties.getRate();
        List<AbstractTokenInfo> values = commands.zrangebyscore(redisKey, minusTime, plusTime);
        values.stream()
            .findFirst()
            .ifPresent(lowestEntry -> {
                log.info("Adding entry with score -1: " + lowestEntry);
                commands.zadd(redisKey, -1, lowestEntry);
            });
    }

}
