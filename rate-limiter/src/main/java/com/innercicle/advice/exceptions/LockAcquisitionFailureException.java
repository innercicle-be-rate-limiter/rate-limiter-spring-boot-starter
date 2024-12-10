package com.innercicle.advice.exceptions;

public class LockAcquisitionFailureException extends RuntimeException {

    public LockAcquisitionFailureException(String message) {
        super(message);
    }

}
