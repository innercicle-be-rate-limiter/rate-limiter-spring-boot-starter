package com.innercicle.advice.exceptions;

/**
 * 락 획득 실패 예외
 */
public class LockAcquisitionFailureException extends RuntimeException {

    public LockAcquisitionFailureException(String message) {
        super(message);
    }

}
