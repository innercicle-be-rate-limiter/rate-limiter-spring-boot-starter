package com.innercicle.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class TokenBucketInfo extends AbstractTokenInfo implements Serializable {

    public TokenBucketInfo(BucketProperties properties) {
        super(properties);
    }

    public boolean isAllowRequest() {
        return this.currentTokens > 0;
    }

    public void minusTokens() {
        this.currentTokens--;
    }

    public void calculateCurrentTokens(int tokensToAdd) {
        this.currentTokens = Math.min(this.currentTokens + tokensToAdd, this.capacity);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

}
