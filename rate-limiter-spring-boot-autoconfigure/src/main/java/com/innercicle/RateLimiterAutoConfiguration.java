package com.innercicle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercicle.aop.RateLimitAop;
import com.innercicle.aop.RateLimitingProperties;
import com.innercicle.cache.BucketRedisTemplate;
import com.innercicle.cache.CacheTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.handler.*;
import com.innercicle.lock.ConcurrentHashMapManager;
import com.innercicle.lock.LockManager;
import com.innercicle.lock.RedisRedissonManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Configuration
public class RateLimiterAutoConfiguration {

    @Bean
    public RateLimitingProperties rateLimitingProperties() {
        return new RateLimitingProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public BucketProperties bucketProperties() {
        return new BucketProperties(); // 필요한 초기값 설정 가능
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "lock-type", havingValue = "redis_redisson")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        String redisUri = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());
        Config config = new Config();
        config.useSingleServer().setAddress(redisUri);
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public RedisClient redisClient(RedisProperties redisProperties) {
        String redisUri = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());
        return RedisClient.create(redisUri);
    }

    @Bean
    @ConditionalOnBean({RedisClient.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public StatefulRedisConnection<String, AbstractTokenInfo> redisConnection(RedisClient redisClient) {
        return redisClient.connect(new AbstractTokenInfoCodec());
    }

    @Bean
    @ConditionalOnBean({StatefulRedisConnection.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public BucketRedisTemplate bucketRedisTemplate(
        StatefulRedisConnection<String, AbstractTokenInfo> redisTokenInfoTemplate,
        BucketProperties bucketProperties
    ) {
        return new BucketRedisTemplate(redisTokenInfoTemplate, bucketProperties);
    }

    @Bean
    @ConditionalOnBean({RedissonClient.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "lock-type", havingValue = "redis_redisson")
    public LockManager redisRedissonManager(RedissonClient redissonClient) {
        return new RedisRedissonManager(redissonClient);
    }

    @Bean
    @ConditionalOnBean({BucketRedisTemplate.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "rate-type", havingValue = "token_bucket")
    public RateLimitHandler tokenBucketHandler(
        BucketRedisTemplate bucketRedisTemplate,
        BucketProperties bucketProperties
    ) {
        return new TokenBucketHandler(bucketRedisTemplate, bucketProperties);
    }

    @Bean
    @ConditionalOnBean({CacheTemplate.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "rate-type", havingValue = "leaky_bucket")
    public RateLimitHandler leakyBucketHandler(CacheTemplate cacheTemplate, BucketProperties bucketProperties) {
        return new LeakyBucketHandler(cacheTemplate, bucketProperties);
    }

    @Bean
    @ConditionalOnBean({CacheTemplate.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "rate-type", havingValue = "fixed_window_counter")
    public RateLimitHandler fixedWindowCounterHandler(CacheTemplate cacheTemplate) {
        return new FixedWindowCounterHandler(cacheTemplate);
    }

    @Bean
    @ConditionalOnBean({CacheTemplate.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "rate-type", havingValue = "sliding_window_logging")
    public RateLimitHandler slidingWindowLoggingHandler(CacheTemplate cacheTemplate) {
        return new SlidingWindowLoggingHandler(cacheTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "lock-type", havingValue = "concurrent_hash_map")
    public ConcurrentHashMapManager concurrentHashMapManager() {
        return new ConcurrentHashMapManager();
    }

    @Bean
    @ConditionalOnBean({LockManager.class, RateLimitHandler.class})
    public RateLimitAop rateLimitAop(RateLimitingProperties rateLimitingProperties,
                                     LockManager lockManager,
                                     RateLimitHandler rateLimitHandler) {
        return new RateLimitAop(rateLimitingProperties, lockManager, rateLimitHandler); // 메서드 종료
    }

    static class AbstractTokenInfoCodec implements RedisCodec<String, AbstractTokenInfo> {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public AbstractTokenInfo decodeValue(ByteBuffer bytes) {
            String json = StandardCharsets.UTF_8.decode(bytes).toString();
            return deserialize(json);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StandardCharsets.UTF_8.encode(key);
        }

        @Override
        public ByteBuffer encodeValue(AbstractTokenInfo value) {
            String json = serialize(value);
            return StandardCharsets.UTF_8.encode(json);
        }

        private AbstractTokenInfo deserialize(String json) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                return objectMapper.readValue(json, AbstractTokenInfo.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private String serialize(AbstractTokenInfo value) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

}