package com.innercicle;

import com.innercicle.aop.RateLimitAop;
import com.innercicle.aop.RateLimitingProperties;
import com.innercicle.cache.BucketRedisTemplate;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.domain.BucketProperties;
import com.innercicle.handler.RateLimitHandler;
import com.innercicle.handler.TokenBucketHandler;
import com.innercicle.lock.ConcurrentHashMapManager;
import com.innercicle.lock.LockManager;
import com.innercicle.lock.RedisRedissonManager;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@ConditionalOnClass(RedisProperties.class)
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RateLimiterAutoConfiguration {

    @Bean
    public RateLimitingProperties rateLimitingProperties() {
        return new RateLimitingProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public BucketProperties bucketProperties() {
        return new BucketProperties(); // 필요한 초기값 설정 가능
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

    @Bean
    @ConditionalOnBean({RedisConnectionFactory.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public RedisTemplate<String, AbstractTokenInfo> redisTokenInfoTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, AbstractTokenInfo> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    @ConditionalOnBean({RedisTemplate.class, BucketProperties.class})
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public BucketRedisTemplate bucketRedisTemplate(
        RedisTemplate<String, AbstractTokenInfo> redisTokenInfoTemplate,
        BucketProperties bucketProperties
    ) {
        return new BucketRedisTemplate(redisTokenInfoTemplate, bucketProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "lock-type", havingValue = "redis_redisson")
    public RedisRedissonManager redisRedissonManager(RedissonClient redissonClient) {
        return new RedisRedissonManager(redissonClient);
    }

    @Bean
    @ConditionalOnBean({BucketRedisTemplate.class, BucketProperties.class})
    public TokenBucketHandler tokenBucketHandler(
        BucketRedisTemplate bucketRedisTemplate,
        BucketProperties bucketProperties
    ) {
        return new TokenBucketHandler(bucketRedisTemplate, bucketProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "cache-type", havingValue = "redis")
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    @Bean
    @ConditionalOnBean({LockManager.class, TokenBucketHandler.class})
    public RateLimitAop rateLimitAop(RateLimitingProperties rateLimitingProperties,
                                     LockManager lockManager,
                                     RateLimitHandler rateLimitHandler) {
        return new RateLimitAop(rateLimitingProperties, lockManager, rateLimitHandler);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", value = "lock-type", havingValue = "concurrent_hash_map")
    public ConcurrentHashMapManager concurrentHashMapManager() {
        return new ConcurrentHashMapManager();
    }

}