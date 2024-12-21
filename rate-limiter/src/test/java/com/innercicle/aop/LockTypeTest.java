package com.innercicle.aop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockTypeTest {

    @Test
    void testLockTypeValues() {
        // when
        LockType[] values = LockType.values();

        // then
        assertThat(values).containsExactly(
            LockType.REDIS_REDISSON,
            LockType.CONCURRENT_HASH_MAP
        );
    }

    @Test
    void testLockTypeValueOf() {
        // when
        LockType redisLockType = LockType.valueOf("REDIS_REDISSON");
        LockType hashMapLockType = LockType.valueOf("CONCURRENT_HASH_MAP");

        // then
        assertThat(redisLockType).isEqualTo(LockType.REDIS_REDISSON);
        assertThat(hashMapLockType).isEqualTo(LockType.CONCURRENT_HASH_MAP);
    }

    @Test
    void testLockTypeToString() {
        // when
        String redisLockString = LockType.REDIS_REDISSON.toString();
        String hashMapLockString = LockType.CONCURRENT_HASH_MAP.toString();

        // then
        assertThat(redisLockString).isEqualTo("REDIS_REDISSON");
        assertThat(hashMapLockString).isEqualTo("CONCURRENT_HASH_MAP");
    }

}