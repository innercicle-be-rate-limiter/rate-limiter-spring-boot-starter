package com.innercicle.lock;

import com.innercicle.annotations.RateLimiting;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;

@Slf4j
public abstract class LockManager {

    protected Lock lock;

    public boolean tryLock(RateLimiting rateLimiting) throws InterruptedException {
        return lock.tryLock(rateLimiting.waitTime(), rateLimiting.timeUnit());
    }

    public abstract void getLock(String key);

    public void unlock() {
        lock.unlock();
    }

}
