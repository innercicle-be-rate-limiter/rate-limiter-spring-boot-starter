package com.innercicle.domain;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LeakyBucketInfo extends AbstractTokenInfo {

    /**
     * 큐에서 사용중인 갯수
     */
    private int usedCount;

    public static LeakyBucketInfo of(int dequeSize, int capacity) {
        LeakyBucketInfo leakyBucketInfo = new LeakyBucketInfo();
        leakyBucketInfo.capacity = capacity;
        leakyBucketInfo.usedCount = dequeSize;
        return leakyBucketInfo;
    }

    /**
     * <h2>현재 큐에서 사용 가능한 갯수</h2>
     * 전체 용량에서 현재 사용중인 갯수를 뺀다.<br/>
     *
     * @return 사용 가능한 갯수
     */
    @Override
    public int getRemaining() {
        return capacity - usedCount;
    }

}
