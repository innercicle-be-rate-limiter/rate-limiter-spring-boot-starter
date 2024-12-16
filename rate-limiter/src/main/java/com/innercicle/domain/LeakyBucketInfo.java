package com.innercicle.domain;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

@Getter
public class LeakyBucketInfo extends AbstractTokenInfo {

    private final Deque<LeakyBucketInfo> deque;

    /**
     * 큐에서 사용중인 갯수
     */

    public LeakyBucketInfo(BucketProperties properties) {
        super(properties);
        this.deque = new ArrayDeque<>(properties.getCapacity());
    }

    /**
     * <h2>현재 큐에서 사용 가능한 갯수</h2>
     * 전체 용량에서 현재 사용중인 갯수를 뺀다.<br/>
     *
     * @return 사용 가능한 갯수
     */
    @Override
    public int getRemaining() {
        return capacity - deque.size();
    }

}
