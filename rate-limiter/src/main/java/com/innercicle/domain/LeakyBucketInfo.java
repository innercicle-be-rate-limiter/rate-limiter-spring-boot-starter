package com.innercicle.domain;

import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Deque;

@Setter @NoArgsConstructor
public class LeakyBucketInfo extends AbstractTokenInfo {

    private Deque<LeakyBucketInfo> deque;

    public static LeakyBucketInfo of(int capacity) {
        LeakyBucketInfo leakyBucketInfo = new LeakyBucketInfo();
        leakyBucketInfo.capacity = capacity;
        return leakyBucketInfo;
    }

    @Override
    public int getLimit() {
        return deque.size();
    }

    @Override
    public int getRemaining() {
        return capacity - deque.size();
    }

}
