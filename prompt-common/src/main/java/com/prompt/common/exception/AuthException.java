package com.prompt.common.exception;

public class AuthException extends BaseException {
    public AuthException(String message) {
        super(401, message);
    }
}
