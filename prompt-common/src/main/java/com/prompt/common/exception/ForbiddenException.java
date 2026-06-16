package com.prompt.common.exception;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(403, message);
    }
}
