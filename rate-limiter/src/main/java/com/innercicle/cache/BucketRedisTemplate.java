package com.innercicle.cache;

import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.domain.SlidingWindowLoggingInfo;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
        List<AbstractTokenInfo> resultSet = connection.sync().zrangebyscore(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key, 1, Double.MAX_VALUE);
        return resultSet == null || resultSet.isEmpty() ? createDefaultInstance(clazz) :
            resultSet.stream().filter(clazz::isInstance).findFirst().map(SlidingWindowLoggingInfo.class::cast).orElse(new SlidingWindowLoggingInfo());
    }

    public void saveSortedSet(String key, AbstractTokenInfo tokenInfo) {
        RedisCommands<String, AbstractTokenInfo> commands = connection.sync();
        commands.zadd(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key,
                      commands.zcard(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key) == null ? 1 :
                          commands.zcard(SLIDING_WINDOW_LOGGING_KEY_PREFIX + key) + 1,
                      tokenInfo);
    }

    public void removeSortedSet() {
        connection.sync().keys(SLIDING_WINDOW_LOGGING_KEY_PREFIX + "*").forEach(key -> connection.sync().zrangebyscore(key,
                                                                                                                       1,
                                                                                                                       Integer.MAX_VALUE).stream().findFirst().ifPresent(
            lowestEntry -> connection.sync().zadd(key, -1, lowestEntry)));
    }

    // 기본 인스턴스 생성 (헬퍼 메서드)
    private SlidingWindowLoggingInfo createDefaultInstance(Class<? extends AbstractTokenInfo> clazz) {
        try {
            return (SlidingWindowLoggingInfo)clazz.getDeclaredConstructor(BucketProperties.class).newInstance(bucketProperties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
