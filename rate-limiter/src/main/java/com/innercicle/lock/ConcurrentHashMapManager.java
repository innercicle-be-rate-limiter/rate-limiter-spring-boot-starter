package com.innercicle.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentHashMapManager extends LockManager {

    private final ConcurrentHashMap<String, Lock> lockMap = new ConcurrentHashMap<>();

    @Override
    public void getLock(String key) {
        this.lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
    }

}

